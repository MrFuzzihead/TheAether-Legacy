# Aether Legacy — Codebase Audit Plan

A full audit of the Aether Legacy mod (Minecraft 1.7.10 / Forge) covering
vulnerabilities, performance issues, and bug fixes.

## Findings so far (from initial reconnaissance)

### Confirmed vulnerabilities (network layer — 1.7.10 mods trust packets far too much)

- `PacketSetTime` — any client can set the world time of **every dimension** on
  the server (no permission check, no proximity to a Sun Altar; the
  `dimensionId` field is read but ignored).
- `PacketOpenContainer` — any client can open any GUI by arbitrary ID at their
  own coords; combined with `AetherGuiHandler`'s **unchecked casts** of
  `world.getTileEntity(x, y, z)` to `TileEntityEnchanter`/`Freezer`/`Incubator`,
  a crafted packet at a mismatched TE position triggers a server-side
  `ClassCastException` (crash/DoS).
- `PacketExtendedAttack` — attack any entity by ID with **no reach/LOS check**
  (free reach/kill-aura exploit).
- `PacketPerkChanged` / `PacketCapeChanged` — any client can flip **any other
  player's** halo/glow/cape/moa-skin by entity ID, with no server-side
  `AetherRankings` donator/rank validation.
- `PacketSendSneaking` — any client can set **any player's** `mountSneaking`
  state.
- `PacketCheckKey` — writes to a static global (`AetherLore.hasKey`), letting
  any client flip global state for the whole server.
- `AetherPacket.onMessage` — handlers touch world/player state directly; in
  1.7.10 simpleimpl these arrive on Netty threads, not the main thread → race
  conditions/crashes (should use `addScheduledTask`).

### Confirmed bugs

- `TileEntityTreasureChest.unlock()` — `random.nextInt(1)` always returns 0
  (loot roll bug).
- `TileEntityTreasureChest.getDescriptionPacket()` — serializes **full inventory
  contents** to every client in the world (loot x-ray/leak vector vanilla
  chests avoid); `closeInventory` manual `--numPlayersUsing` can desync from
  `super.openInventory()`.
- `ItemLifeShard.onItemRightClick` — decrements `heldItem.stackSize` without
  nulling at 0, and the shard-count/max-health sync path looks fragile.

## Packet Audit Results

| Packet | Direction | Issue found | Fix applied |
|---|---|---|---|
| `PacketOpenContainer` | C→S | Client could open **any** GUI id at its own position with no whitelist; combined with the unchecked TE casts in `AetherGuiHandler`, a crafted id at a mismatched tile entity crashed the server (DoS) | Whitelist to `accessories` + `-1` (the only legitimately-sent ids) |
| `PacketExtendedAttack` | C→S | Client could attack **any entity by ID** with no reach/LOS check → free reach/kill-aura | Server-side: require a Valkyrie tool in hand, distance ≤ 9 blocks, `canEntityBeSeen`, entity not self/dead |
| `PacketPerkChanged` | C→S | Client could toggle **any player's** halo/glow/moa-skin by arbitrary entity id | Must target self (`entityID == player.getEntityId()`) |
| `PacketCapeChanged` | C→S | Same — could toggle any player's cape | Must target self |
| `PacketSendSneaking` | C→S | Client could set **any player's** mount-sneaking state | Must target self |
| `PacketInitiateValkyrieFight` | C→S | No server-side validation: arbitrary slot index, deleted the **entire** medal stack, could ready any queen | Validate slot bounds, item == victory_medal, stack ≥ 10, queen within 64 blocks, not already ready; consume exactly 10 via `decrStackSize` |
| `PacketCheckKey` | C→S | Set a **global static** (`AetherLore.hasKey`) — any client flipped lore-slot validity for the entire server | Handler is now a no-op; `SlotLore.isItemValid` computes deterministically on both sides via `StatCollector` |
| `PacketDisplayDialogue` | S→C | `toBytes` wrote `dialogue` before `dialogueName`, but `fromBytes` reads `dialogueName` first → the gui title/body were swapped | Fix write order to match read order |
| `PacketSetTime` (earlier session) | C→S | Any client could set time on **all** dimensions; no boss/permission gate | Aether-only, Sun Spirit gate, op/`sunAltarMultiplayer` check |
| All S→C packets (`Accessory`, `Achievement`, `SendPoison*`, `UpdateLifeShardCount`, `SendSeenDialogue`, `PortalItem`, `SendTime`, `SendShouldCycle`, `SendEternalDay`, `SwetJump`, `DisplayDialogue`) | S→C | Client trusts server by design; no cross-player client exploit | No change needed |

Notable: `AetherLore.hasKey` is retained (deprecated) for API compatibility.
`PacketCheckKey` remains registered in `AetherNetwork` so the discriminant ids
are unchanged; its handler is now inert.

## Plan Steps

1. ✅ **Complete the network packet audit (DONE)** — every packet in
   `network/packets/` read and every call site traced. See
   [Packet Audit Results](#packet-audit-results) below. All fixes applied and
   verified with `gradlew build`.

2. ✅ **Thread-safety: investigated, NOT needed** — traced the FML 1.7.10
   dispatch path (`NetworkDispatcher` → `FMLProxyPacket` →
   `NetworkManager.channelRead0` → `receivedPacketsQueue` → main-thread drain
   → `EmbeddedChannel.writeInbound` → `SimpleChannelHandlerWrapper`).
   `FMLProxyPacket` extends `Packet` and does not override `hasPriority()`, so
   it is queued and processed on the **main thread** (`MinecraftServer.run` /
   `Minecraft.runMainGameLoop`). `onMessage` already runs on the main thread;
   a scheduled-task rewrite would add risk for no benefit. (`MinecraftServer`
   in 1.7.10 has no `addScheduledTask` anyway — only client `Minecraft`
   does.)

3. ✅ **Harden `AetherGuiHandler` (DONE)** — the dangerous unchecked TE casts are
   now guarded with `instanceof` checks on **both** server and client, so a
   mismatched/blocks-replaced tile entity can no longer throw a
   `ClassCastException`. Server-side TE GUIs (enchanter/freezer/incubator/
   treasure chest) additionally require the player to be within 9 blocks
   (`MAX_OPEN_DISTANCE_SQ`), defense-in-depth layered on top of vanilla's
   6-block activation reach and the container's own 8-block
   `isUseableByPlayer` check. `PacketOpenContainer` was already whitelisted to
   `accessories`/`-1` in step 1 (no TE casts, no coords needed). Threat-model
   note: traced FML 1.7.10 and confirmed `FMLMessage.OpenGui` is server→client
   only (`OpenGuiHandler` uses `FMLClientHandler`), so a client cannot directly
   invoke `getServerGuiElement` with arbitrary coords — the `instanceof`
   guards are therefore defense-in-depth, not a remote-exploit fix, but they
   prevent real client/server crashes on TE desync.

4. ✅ **Audit all containers and slots (DONE)** — reviewed `ContainerAccessories`,
   `ContainerEnchanter`, `ContainerFreezer`, `ContainerIncubator`, `ContainerLore`,
   and all slot classes. Fixes:
   - `ContainerIncubator.transferStackInSlot`: the ambrosium-torch/moa-egg
     branches returned early, skipping the standard slot cleanup → phantom
     0-size stacks left in the source player-inventory slot (desync/ghost).
     Now falls through to the shared tail (matching enchanter/freezer).
   - `TileEntityEnchanter` / `TileEntityFreezer`: the output merge did
     `result.stackSize += existing` and **replaced** the stack, wiping the
     existing stack's NBT/enchantments and allowing the sum to exceed
     `getMaxStackSize()`, where `setInventorySlotContents` silently clamped →
     **item loss**. Now: recipe-start gate refuses to start a craft that would
     overflow the output slot, and completion merges into a copy of the
     existing stack (preserving its NBT).
   - `InventoryAccessories.decrStackSize`: NPE when called on an empty slot
     (crash). Added null guard.
   - `ContainerAccessories.transferStackInSlot`: quick-move used
     `accessorySlot.putStack(stack)` which bypasses the accessory slot's stack
     limit (1). Now only quick-moves when the stack fits the slot limit.
   - Confirmed OK: `SlotEnchanter`/`SlotFreezer` reject placement (output-only);
     `SlotIncubator` moa-egg-only + limit 1; `SlotAccessory` type/event
     validated; all block containers `canInteractWith` via
     `isUseableByPlayer` (TE-identity + 8-block distance); `mergeItemStack`
     used throughout so stack limits are respected by vanilla semantics.

5. ✅ **Audit player data (`PlayerAether`) persistence & sync (DONE)** —
   storage uses Forge extended properties
   (`registerExtendedProperties("aether_legacy:player_aether", ...)`), which
   are tied to the entity lifecycle and saved/loaded via the entity's NBT
   (`saveNBTData`/`loadNBTData` called automatically by `Entity.writeToNBT` /
   `readFromNBT`). **No static map keyed by player → no map memory leak**; the
   extended-properties HashMap lives on the entity and is GC'd with it. Fixes:
   - `updateShardCount` health-modifier accumulation: 1.7.10's
     `removeModifier(AttributeModifier)` removes by **object identity** from
     the operation sets (it only purges `mapByUUID` by UUID). The old code
     created a fresh instance each call, so stale modifier objects remained in
     `getModifiersByOperation(0)` → `computeValue()` summed them → max health
     **accumulated on every shard use and again on every login** (and
     `applyModifier` could throw "already applied"). Now tracks and removes the
     actual applied instance (plus a safe path for modifiers restored from NBT
     on a fresh login).
   - `onUpdate` broadcast spam: sent **6 packets to ALL players every tick**
     (halo/glow/cape/seen-dialogue/get-portal/poison-time). Added
     change-detection caches — each is now sent only when the value changes
     (halo/glow/cape/toggles are rare; poison time only while poisoned). ~7x
     packet reduction on idle, ~86x on running-around-without-poison.
   - `loadNBTData` unconditionally set `bedLocation(0,0,0)` for fresh players
     (no keys), which wrongly routed respawns to the Aether via
     `AetherWorldProvider.getRespawnDimension`. Now gated on `hasKey`.
   - Confirmed OK: `isDonator()` always true (by design — free perks); shard
     count is copied on death (`onPlayerAetherClone`); accessories dropped on
     death unless keepInventory; respawn re-applies the health modifier via
     `updateShardCount(0)`; `saveNBTData` writes shardCount/accessories/bed/
     poison/dialog/portal under the `aetherI` tag.

6. ✅ **Fix item logic bugs (DONE)** — reviewed the flagged items against the
   1.7.10 vanilla handlers:
   - `ItemLifeShard`: removed the redundant `updateShardCount(0)` before every
     use (extra broadcast + pointless modifier re-application). The
     `--heldItem.stackSize` was already vanilla-safe: `tryUseItem` sets the
     slot to null at stackSize 0 and restores the stack in creative.
   - `AetherEventHandler.onEntityDropLoot` (skyroot sword double drop): the
     duplicate `EntityItem` was created with `items.getEntityItem()` — the
     **same mutable ItemStack object** shared by two falling entities
     (both data watchers reference it). Pickup order determined whether the
     player got 1×, 2×, or partial stacks. Now uses `.copy()` so each
     EntityItem owns its stack — deterministic double drops, no shared-object
     hazard.
   - `ItemSkyrootBucket.onItemUseFirst`: `getMovingObjectPositionFromPlayer`
     can return `null`, and the alt-code dereferenced `movingobjectposition.blockX`
     → server/client NPE crash. Now null-checked. (Also verified the rest of
     the bucket: `fillBucket`/`onBucketUsed` handle 0-size and creative
     correctly; the creative water-drain matches vanilla behavior.)
   - `ItemGravititeTool.onItemUse`: `heldItem.damageItem(4, player)` ran on
     both sides (cosmetic client-side durability desync vs the server-authoritative
     value). Now damages only server-side with the spawn.
   - `ItemDeveloperStick`: investigated — vanilla `EntityPlayer.interactWith`
     restores the stack copy in creative and destroys the item at 0 in
     survival; the unguarded decrement is only reachable for unranked players
     (joke branches) and is cleaned up by vanilla. No change needed.
   - `DoubleDropHelper` / `ItemHolystoneTool`: verified 1.7.10's
     `tryHarvestBlock` only calls `harvestBlock` and `onBlockDestroyed` on the
     **survival** path (creative calls `removeBlock` and returns early), so the
     skyroot double-drop and holystone ambrosium bonus cannot be farmed in
     creative. `EnchantmentHelper.getEnchantmentLevel` is null-safe, so the
     fortune/silk calls are safe for bare-hand breaks. No change needed.

7. ✅ **Tile entity audit (DONE)** — fixes to `TileEntityTreasureChest` and
   `TileEntityIncubator`; the processing TEs were reviewed:
   - `TileEntityTreasureChest.unlock`: `random.nextInt(1)` always returned 0
     (loot was always exactly 5 items). Now `5 + nextInt(5)` (5–9, the
     intended range). Also, loot slots were rolled with replacement
     (`random.nextInt(getSizeInventory())` per drop) so two drops could land
     on the same slot and silently discard one; slots are now shuffled and
     picked distinctly.
   - `TileEntityTreasureChest.getDescriptionPacket`: called `writeToNBT`,
     which serialized the **full chest inventory** to every client that loads
     the chunk or receives the packet — an x-ray/leak vector vanilla chests
     don't have (base `TileEntity.getDescriptionPacket` returns null; vanilla
     `TileEntityChest` doesn't override it; contents sync via the container
     window only). Now sends just `locked` + `dungeonType`, which is all the
     client needs (`GuiTreasureChest` uses `getKind()` for its background).
     Tracked through `Chunk`/`S21PacketChunkData` source: 1.7.10 chunk-data
     packets don't serialize tile entities, so this packet is the only leak
     path — now closed.
   - `TileEntityIncubator.updateEntity`: `getStackInSlot(1).getItem()` was
     dereferenced without a null check when `progress >= ticksRequired`
     (hopper/edge races could leave the egg slot empty) → NPE crash. Guarded,
     and the egg stack is now fetched once.
   - Reviewed, no change needed (documented): `closeInventory` mirrors vanilla
     `TileEntityChest.closeInventory` exactly (treasure block `extends
     BlockChest`, so the unconditional decrement is equivalent); all three
     processing TEs persist `progress`/`powerRemaining` to NBT (Incubator's
     `ticksRequired` is a hardcoded 5700 constant — no need to save); no
     infinite loops in `updateEntity` (power drains, progress resets, fuel
     refill terminates); all inventory-mutating and entity-spawning logic runs
     inside `!worldObj.isRemote` (client-side execution is cosmetic — progress
     resets, achievement toasts — or event-post-only).

8. ✅ **Static/global state audit (DONE)** — swept all mutable static fields in
   the codebase and checked the plan's named targets:
   - `AetherLore.hasKey`: the global-static lore-slot gate was neutralized in
     phase 1 (PacketCheckKey no-op, SlotLore computes deterministically) and
     had zero remaining references — now **removed entirely**.
   - `RandomTracker.testRandom`: recursive retry discarded the recursion's
     result and fell through to `return -1` on a collision (consuming an extra
     RNG draw per retry and never actually retrying). Rewritten as a loop that
     rolls until different from the last value. (Per-instance state, so it was
     never global; the fix is a correctness bugfix.)
   - Verified benign (documented): `AetherEventHandler`/`AetherWorld` have no
     mutable statics; `AetherAPI`/`BlocksAether`/`ItemsAether`/
     `EntitiesAether`/`AetherCreativeTabs` registries and
     `AetherRankings.ranks` (UUID whitelist) are only written during
     initialization; `AetherConfig.config.save()` from the main-menu toggle is
     a client-side preference written on the client thread and read
     client-only; `AetherNameGen.rand`/`AetherTrivia.random`/`AetherKeybinds`
     are client or main-thread-confined; `AetherNetwork.discriminant` and
     `PotionInebriation.inebriation` are init-time only. Nothing
     client-controllable or cross-player remains.

9. ✅ **Performance pass (DONE)** — audited worldgen, per-tick/per-frame client
   paths, entity scans and packet fan-out:
   - **Removed the per-tick O(loaded-entities) dungeon-key scan**
     (`AetherEventHandler.onWorldTick` swept every `EntityItem` in every world
     at 20 tps). Now handled by `EntityJoinWorldEvent`, which fires exactly
     when the item spawns.
   - **Eternal-day / should-cycle packets no longer broadcast every tick**: the
     per-tick `sendToAll(PacketSendEternalDay/ShouldCycle)` was replaced with
     change-detection (cached on `AetherWorldProvider`) + `sendToDimension`.
     The values change ~once ever (Sun Spirit death).
   - **Aether-time packet throttled**: `calculateCelestialAngle` was sending
     `PacketSendTime` to all players every server call (≈20/s). Now at most
     once per 20 ticks (1/s) via `sendToDimension` — a second of latency is
     imperceptible on a 20-minute day cycle — plus redundant `AetherData`
     writes are skipped when unchanged.
   - **Fixed an operator-precedence bug** in the eternal-day catchup check:
     `(worldTime + 1 % 24000L)` / `(worldTime - 1 % 24000L)` bound `%` tighter
     than `+`/`-`, so the ±1 tolerance compared against unmodded `worldTime±1`
     (always true) — the intended early-catchup-termination never fired. Now
     uses `Math.floorMod`.
   - **Late-joiner sync preserved**: since the eternal-day packets are no
     longer broadcast every tick, `PlayerAetherEvents` now sends the current
     eternal-day / should-cycle / aether-time state to a player on login and on
     dimension change (only when entering the Aether).
   - **Narrowed packet fan-out** (was `sendToAll` = every player in every
     dimension): halo/glow/cape/accessories use `sendToAllAround` (512 blocks,
     covers max render distance for model rendering); poison/portal/dialog/
     shard-count/seen-dialog use `sendTo`(owner) since those consumers
     (AetherOverlay, spirit-dialog gating) are local to the owning player.
     Added `AetherNetwork.sendToAllAround/sendToDimension` helpers.
   - **Verified fine, no change**: `ChunkProviderAether` reuses its noise
     buffers (`pnr`/`ar`/`br`, `buffer`) across chunks; the 32768-block array
     per chunk matches vanilla; extended-reach AABB query is click-gated;
     `AetherOverlay`/`ScaledResolution` allocations are per-frame cheap;
     per-tick `AetherConfig` reads are in-memory map lookups.

10. **Client-side crash cleanup** — `GuiDialogue` bare
    `printStackTrace()`, `AetherMainMenu` reflective `Desktop` launch
    (verify try/catch coverage), `EntityAetherItem`/renderer null-safety on
    missing player data.

11. **Regression-verify** — after fixes, run `gradlew build` (compile check)
    and, where feasible, quick in-game smoke tests of each patched packet path
    on a local server with a modified client simulation.

## Priority

Recommended order: network packet fixes first (highest severity — remote
exploits and server crashes), then containers/dupe fixes, then tile entity and
item bugs, then performance and cleanup.

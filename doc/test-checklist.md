# Aether Legacy — In-Game Test Checklist

Test sheet for the audit work completed in phases 1–10 (plus the sleep/time
system fixes). Covers what changed and how to verify it in-game.

**Setup:** singleplayer exercises the integrated server (all server-side
validation runs in-process). Items marked **[MP]** need a LAN game with 2+
players or a dedicated server.

---

## Section 0 — Sleep & time system (pre-phase fixes + PacketSetTime)

- [x] Build a skyroot bed in the Aether; note overworld time (`/time query daytime`), sleep in the Aether
  - Aether reaches morning; **overworld time unchanged**
- [x] Sleep in a vanilla bed in the overworld
  - Overworld advances (when all overworld players asleep)
- [x] **[MP]** Player A asleep in the Aether, Player B awake in the overworld
  - Neither dimension advances
- [x] Place a Sun Altar **before** killing the Sun Spirit; right-click it
  - Message shown; **GUI does not open**
- [x] Kill the Sun Spirit (gold dungeon boss); wait for eternal-day catch-up; right-click the Sun Altar
  - Altar GUI opens (dedicated server: op required, or `sunAltarMultiplayer=true`)
- [x] Drag the Sun Altar slider
  - Only the **Aether's** time changes; overworld/nether untouched
- [x] **[MP]** Non-op player uses the Sun Altar on a dedicated server
  - Permission message; no time change
- [x] Start the Sun Spirit fight / check its boss bar
  - Random boss name renders properly (not "0/1, Sun Spirit")

---

## Section 1 — Packet validation (phase 1)

- [x] Open the accessories GUI from the inventory button and the keybind
  - Opens normally (whitelisted IDs still work)
- [x] Right-click enchanter / freezer / incubator / treasure chest
  - GUIs open normally (block path, bypasses the packet)
- [x] Hold a Valkyrie tool; left-click a mob ~7–8 blocks away with clear LOS
  - Hits (extended reach preserved)
- [x] Attack a mob >9 blocks away, or through a wall, or with a non-Valkyrie item
  - No hit (server reach + LOS enforced)
- [x] Toggle halo/glow in the perks GUI; watch a second player
  - **[MP]** Other player sees the halo/glow toggle
- [x] Toggle cape in chat options
  - **[MP]** Other players see it update
- [x] Ride a moa/swet and sneak
  - Mount sneaking still applies
- [x] Hold **12** victory medals; start the Valkyrie Queen fight
  - Exactly **10 consumed, 2 remain** (older code wiped the whole stack)
- [x] Try starting the Queen fight with <10 medals
  - Blocked; nothing consumed, no duel
- [x] Lore GUI: place a lore item; also try an item with no lore entry; close the GUI
  - Lore item accepted; non-lore item refused; remaining lore item drops on close

---

## Section 2 — Thread safety (phase 2)

No code change was made (verified FML 1.7.10 simpleimpl runs handlers on the
main thread). Nothing to test — included for completeness.

---

## Section 3 — GUI handler hardening (phase 3)

- [x] Right-click each block GUI; confirm it opens only within normal reach (~4.5 blocks)
- [x] Open a container and walk >8 blocks away
  - Container closes (existing `isUseableByPlayer` behavior preserved)
- [x] Open/close all GUIs repeatedly
  - No crashes (failed opens are silent no-ops, not `ClassCastException`s)

---

## Section 4 — Containers & slots (phase 4)

- [x] Shift-click a stack of ambrosium torches into the incubator; then a moa egg
  - Items move cleanly; **source slot shows correct remainder** (no ghost 0-size stack); incubator starts
- [x] Enchant two of the same stackable item consecutively in the enchanter
  - Output stacks correctly; existing output's enchantments/NBT not wiped
- [x] Fill enchanter output to near-max, then finish one more craft
  - No item loss; craft stops rather than overflowing (gate)
- [x] Do the same for the freezer
  - Same expectations
- [x] Shift-click an accessory from inventory to an empty accessory slot
  - Moves in; accessory slot respects limit of 1
- [x] Drag/drop items across all GUIs, including onto empty slots
  - No crashes (decrStackSize NPE guard)

---

## Section 5 — Player data (phase 5)

- [x] Use 1 life shard; check max health; use a 2nd shard; check again
  - Exact `20 + 2×shards` (22, then 24) — **not inflated**
- [x] Use 2 shards, exit and rejoin the world
  - Max health still 24 (modifier restored cleanly, no double-add)
- [x] Die and respawn (with shards used)
  - Max health still 24 (modifier reapplied on respawn)
- [x] Toggle halo off, relog
  - Halo still off (persisted to NBT)
- [x] New world/new player: die in the overworld
  - Respawns in the **overworld** (no phantom Aether bed at 0,0,0)
- [x] Sleep in a skyroot bed in the Aether, then die
  - Respawns at the Aether bed
- [x] **[MP]** Player A uses a shard / changes accessories
  - Player B sees correct health bar and accessories

---

## Section 6 — Item logic (phase 6)

- [x] Kill a mob with a **skyroot sword**; pick up both drops
  - Two full, independent stacks (double drop no longer shares one ItemStack —
    no 1×/partial-stack weirdness depending on pickup order)
- [x] Use a skyroot bucket: scoop water, place it, scoop again; also right-click
  while aiming at nothing/edge of range
  - Normal fill/place behavior; no NPE crash
- [x] Right-click with a gravitite tool (levitation block use); check durability
  - Works; durability decreases only server-side (no double wear / desync)
- [x] Use a life shard
  - Health +2 exactly (redundant broadcast removed; see also Section 5)

---

## Section 7 — Tile entities (phase 7)

- [ ] Unlock a bronze dungeon treasure chest; count the loot
  - **5–9 stacks** (not always exactly 5), each in a distinct slot (no lost drops)
- [ ] **[MP]** Second player loads the chunk containing a locked chest
  - Chest renders locked; its contents are **not** sent to them (description
    packet no longer serializes the inventory — x-ray leak closed)
- [ ] Incubate a moa egg to completion
  - Hatches normally
- [ ] (Edge) Remove the egg from the incubator right as it finishes (or hopper it out)
  - No crash (null guard on the egg slot)

---

## Section 8 — Static state (phase 8)

- [ ] Lore GUI end-to-end (place lore item, close, re-open)
  - Still works — `AetherLore.hasKey` removal is behavior-neutral (gate is now
    computed per-slot; see also Section 1 lore items)
- No other in-game surface changed in phase 8; the remaining findings were
  verified-benign statics (code-verified only).

---

## Section 9 — Performance & sync (phase 9)

- [ ] Drop a dungeon key (or kill the dungeon boss so it drops); pick it up
  - Key still unlocks its dungeon (per-tick entity scan replaced by
    EntityJoinWorldEvent — spawn-time handling)
- [ ] Kill the Sun Spirit; watch the Aether sky
  - Eternal-day catch-up completes in seconds; sky settles at world time
- [ ] **[MP]** Join a world where eternal day is already active (late joiner)
  - Sky/altar state correct immediately on entering the Aether (login/dimension
    sync replaces the old per-tick broadcast)
- [ ] **[MP]** Toggle halo/cape; other player within render distance sees it
  - `sendToAllAround` fan-out works for model rendering
- [ ] **[MP]** Get hit by a poison dart / use a life shard
  - Overlay & shard counter update for the affected player (owner-targeted send)
- [ ] Aether day/night cycle still advances smoothly for clients
  - Time sync throttled to 1/s is imperceptible on a 20-min cycle *(applies to
    the phase-9 branch state; the Section-0 branch currently re-sends per tick)*

---

## Section W — Weather independence

- [ ] Wait for (or force, `/weather rain` + `/toggledownfall`) overworld rain, then enter the Aether
  - Aether sky stays clear and bright: no lightmap darkening, sun unobscured, no rain
    particles or thunder (shared-WorldInfo rain leak closed — server & client
    strengths pinned to zero while in the Aether)
- [ ] Return to the overworld while raining
  - Overworld rain behaves normally (fix only touches the Aether dimension)

---

## Section 10 — Client crash hardening (phase 10)

- [ ] Click through Valkyrie Queen / Sun Spirit dialogue lines rapidly
  - No client crash if a dialogue action throws (logged, GUI survives)
- [ ] Repeatedly load/unload worlds (title screen ↔ world)
  - No NPE from the Aether HUD overlay during transitions
- [ ] **[MP]** Another player joins/leaves while you are watching them
  - No renderer NPE for accessories / Aether armor

---

## Quick exploit re-tests (optional, needs a modified client)

- [ ] Send `PacketOpenContainer` with a non-accessories GUI id
  - Silently ignored
- [ ] Send `PacketPerkChanged` / `PacketCapeChanged` / `PacketSendSneaking` targeting another player's entity id
  - Ignored
- [ ] Send `PacketSetTime` without having defeated the Sun Spirit / from outside the Aether
  - Ignored

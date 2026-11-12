# The Aether Legacy — Codebase Analysis

Analysis of `TheAether-Legacy` (Minecraft 1.7.10 / Forge 10.13.4.1614) at commit
`6fde73bd` with local dependency updates applied. The project is a large port
(390 Java files, ~34k lines of mod code). This document records bugs,
performance problems, refactoring opportunities, dead code, feature gaps, and
the dependency/reflection/ASM audit.

Build verification performed during the analysis:

```text
./gradlew clean build --no-configuration-cache
BUILD SUCCESSFUL
```

Before the test infrastructure was added, `test` was `NO-SOURCE`; the runtime
findings below are static-analysis findings unless a test now covers them. See
section 10 for the current unit-test and in-game test baseline.

---

## 0. Behavior-preservation rule

Do not trade intended Aether behavior for a mechanically cleaner-looking fix. Before
changing gameplay semantics, check the in-game lore text, language files, recipes,
config comments, docs, git history, and the upstream 1.7.10/Aether II behavior. A
code path that looks odd may be deliberate. In particular:

- A normal golden oak log drop is **skyroot logs** (plus golden amber from a
  suitable axe); it drops itself only through silk touch.
- Treat metadata as orientation/state unless the block explicitly defines species
  metadata. Do not infer species from `BlockLog` axis bits.
- If a fix necessarily changes behavior, document the intended behavior and add a
  regression assertion before committing it.
- When a finding conflicts with the lore or with an established mechanic, stop and
  confirm the intended behavior instead of “fixing” it.

The sixth batch and later changes should follow this rule.

---

## 1. Dependency audit (`dependencies.gradle`)

`dependencies.gradle` originally pinned:

```gradle
runtimeOnlyNonPublishable("com.github.GTNewHorizons:NotEnoughItems:2.8.135-GTNH:dev")
devOnlyNonPublishable("com.github.GTNewHorizons:Battlegear2-for-Backhand:1.6.8-backhand:dev")
```

I checked the GTNH Maven metadata and the upstream GitHub releases. Both pinned
versions were behind. I updated the file to:

```gradle
runtimeOnlyNonPublishable("com.github.GTNewHorizons:NotEnoughItems:2.9.4-GTNH:dev")
devOnlyNonPublishable("com.github.GTNewHorizons:Battlegear2-for-Backhand:1.7.0-backhand:dev")
```

| Dependency | Old | New | Latest release checked | Correctness |
|---|---|---|---|---|
| `NotEnoughItems` | `2.8.135-GTNH` | **`2.9.4-GTNH`** | `2.9.4-GTNH` | Latest `GTNH` release; `:dev` classifier exists. |
| `Battlegear2-for-Backhand` | `1.6.8-backhand` | **`1.7.0-backhand`** | `1.7.0-backhand` | Latest `-backhand` release; `:dev` classifier exists. |

Notes:

- `runtimeOnlyNonPublishable` is correct for NEI: the project does not import
  NEI classes, so this is only present in the development runtime and is not
  published as a dependency. This is the correct optional-tool behavior.
- `devOnlyNonPublishable` is correct for Battlegear: the project directly
  imports `mods.battlegear2.*` in a few optional client compatibility classes,
  so it needs compile-time access and a development runtime, but must not be
  published as a hard dependency.
- The new Battlegear release pulls `Backhand:1.8.8` and `GTNHLib:0.11.48`
  transitively. The full build resolved and compiled successfully against it,
  and the APIs used by this project (`BattlegearUtils.RENDER_BUS`,
  `BattlegearRenderHelper.renderItemInFirstPerson`,
  `RenderPlayerEventChild.PostRenderPlayerElement`) are still present in
  `1.7.0-backhand`.
- NEI has no compile-time use here. If NEI integration is added later, use
  `compileOnly` (or `devOnlyNonPublishable`) plus a `Loader.isModLoaded("NotEnoughItems")`
  guard rather than a hard dependency.
- The `dependencies.gradle` header comment still references Gradle 8.0.1. It is
  only documentation; the wrapper is currently 9.7.1 and the settings
  convention plugin is 2.0.34 (both newer than the versions the comments
  mention).
- If repeatable builds are important, consider a single dependency-version
  block/constraint for GTNHLib, since Battlegear and NEI both pull GTNHLib and
  Gradle currently resolves the highest requested version (0.11.48 in this
  build).

### Optional integration risk

`AetherItemRenderer` contains a direct call to
`mods.battlegear2.client.utils.BattlegearRenderHelper.renderItemInFirstPerson`
inside `if (Loader.isModLoaded("battlegear2"))`. The class is only resolved when
that bytecode executes on HotSpot, but direct references in a common client
class are still fragile. Prefer keeping every Battlegear reference in a
separate class that is loaded only when the mod is present (the project already
does this for `BattlegearClientEventHandler`).

---

## 2. Reflection, access transformers, ASM, and mixins

### Summary

| Category | Result |
|---|---|
| Access transformers | **None.** `accessTransformersFile` is empty and no `*_at.cfg` files exist. |
| Mixins | **Two early client mixins.** `usesMixins = true`, `mixinsPackage = mixins`, and `coreModClass = mixins.EarlyMixinsLoader`. The base `mixins.aether_legacy.json` is the manifest bootstrap; the early loader registers `mixins.aether_legacy.early.json`, which loads `GuiContainerCreativeMixin` and `EntityRendererMixin`. |
| ASM / coremod | **No custom ASM transformers.** `EarlyMixinsLoader` implements `IFMLLoadingPlugin` only as the GTNH early-mixin loader. There are no `org.objectweb.asm` imports, `ClassReader`/`ClassWriter`/`ClassVisitor` classes, or `IClassTransformer` implementations. |
| Reflection | **None remaining in project code.** The Desktop link uses `java.awt.Desktop` directly; the creative-tab `buttonList` access and `EntityRenderer.itemRenderer` assignment now use early client mixins. The original audit inventory below is preserved for context. |

The classpath does contain `org.ow2.asm:asm-debug-all:5.0.3` transitively from
Minecraft/FML. That is a library dependency required by Forge/launchwrapper, not
project ASM code. If the requirement is “no custom ASM transformers”, the project
already satisfies it.

### Reflection inventory

| File | Calls | What is accessed | Recommendation |
|---|---:|---|---|
| `AetherEventHandler.java:404-415` | 1 | `Entity.invulnerable` via `ObfuscationReflectionHelper.setPrivateValue(..., "invulnerable", "field_83001_bt")` for dungeon-key `EntityItem`s. | **Remove.** `EntityAetherItem` already exists and overrides `isEntityInvulnerable()` to `true`. On `EntityJoinWorldEvent`, replace a normal dungeon-key `EntityItem` with `EntityAetherItem` (copy stack/position/motion/pickup delay, cancel the original join), or spawn keys through a custom entity factory. |
| `PlayerAether.java:262-263` | 2 per player per tick | `EntityLivingBase.isJumping` via `ReflectionHelper.getPrivateValue(..., "isJumping", "field_70703_bu")`. | This is a **hot path** and a mapping-sensitive reflection lookup. There is no public getter in 1.7.10. Options: (a) track jump on the client from `EntityPlayerSP.movementInput.jump` and send a small packet/player-data field to the server; (b) add a narrow AT or mixin; (c) cache the reflected field at least. Pure API removal is difficult without an AT/mixin/packet. |
| `AetherClientEvents.java:313-320` | 4 per GUI open | `GuiContainer.guiLeft`, `guiTop`, `xSize`, `ySize` via `ObfuscationReflectionHelper.getPrivateValue(...)`. These are `protected` in the MCP source. | Avoid for `GuiInventory`: the vanilla dimensions are known (`176 x 166`) and the position can be computed from `event.gui.width/height`. For other `GuiContainer`s, use the public screen dimensions or a small layout hook. Because the fields are `protected`, an unrelated event handler cannot read them directly. |
| `AetherClientEvents.java` creative-tab handler | **Removed** | `GuiScreen.buttonList` via indexed reflection, plus `guiLeft/guiTop` again. | **Resolved.** `GuiContainerCreativeMixin` injects at the tail of `setCurrentCreativeTab` and updates the button list directly. |
| `AetherEntityRenderer.java` constructor | **Removed** | `EntityRenderer.itemRenderer` (`field_78516_c` / index 7). | **Resolved.** `EntityRendererMixin` mutates the final field at the tail of the `EntityRenderer` constructor, only when the instance is an `AetherEntityRenderer`. |
| `EntitiesAether.java:138-150` | 2 per spawn-egg spawn | `Class.getConstructor(World.class).newInstance(world)`. | Replace with an explicit `Map<Integer, Function<World, Entity>>` factory (or a `switch` on the ID). This also avoids the repeated exception path and makes entity registration easier to validate. |
| `AetherMainMenu.java` confirm handler | **Removed** | `java.awt.Desktop` reflective lookup, method lookup, and invocation. | **Resolved.** Uses `Desktop.isDesktopSupported()`, `Desktop.Action.BROWSE`, and direct `browse` with checked exception logging. |

### Reflection-free replacement sketch for dungeon keys

```java
@SubscribeEvent
public void onEntityJoinWorld(EntityJoinWorldEvent event) {
    if (event.world.isRemote || !(event.entity instanceof EntityItem)) return;

    EntityItem item = (EntityItem) event.entity;
    if (item.getEntityItem().getItem() != ItemsAether.dungeon_key
        || item instanceof EntityAetherItem) return;

    EntityAetherItem replacement = new EntityAetherItem(
        event.world, item.posX, item.posY, item.posZ, item.getEntityItem().copy());
    replacement.motionX = item.motionX;
    replacement.motionY = item.motionY;
    replacement.motionZ = item.motionZ;
    replacement.delayBeforeCanPickup = item.delayBeforeCanPickup;
    event.setCanceled(true);
    event.world.spawnEntityInWorld(replacement);
}
```

`EntityAetherItem` is already registered in `EntitiesAether`, so no new entity
registration is required. Verify pickup delay and ownership behavior in-game.

### Other `Class`-related issue (not reflection)

`AetherEventHandler.java:94` uses
`oclass.isAssignableFrom(this.getClass())` when validating a spawn egg. That
compares the egg’s entity class against `AetherEventHandler.class`, not the
interacted entity, so the condition is always false. This is a separate bug
(see B02).

---

## 3. Bugs

Severity is based on impact and likelihood. Line numbers are from the analyzed
commit.

### Critical

#### B01 — Custom HUD never renders

**File:** `client/gui/GuiAetherInGame.java:38-40`

```java
if (event.isCancelable() || event.type != ElementType.TEXT) {
    return;
}
```

`RenderGameOverlayEvent` is annotated `@Cancelable`, so `isCancelable()` is
`true` for `RenderGameOverlayEvent.Text` as well. This handler therefore returns
before rendering anything. The Aether HUD (poison/cure vignettes, iron-bubble
counter, hammer cooldown, Moa jumps, boss bar, portal overlay) is effectively
dead.

**Fix:** remove the `isCancelable()` test:

```java
if (event.type != ElementType.TEXT) {
    return;
}
```

or handle `RenderGameOverlayEvent.Post`/`.Text` and only guard on the event type.

#### B02 — Spawn eggs never produce babies

**File:** `AetherEventHandler.java:92-95`

```java
Class<?> oclass = EntitiesAether.getClassFromID(itemstack.getItemDamage());

if (oclass != null && oclass.isAssignableFrom(this.getClass())) {
```

`this` is the event handler, not the target entity. The check is always false,
so `createChild` and the egg consumption path are unreachable.

**Fix:** use the target class, or simply check the registry ID:

```java
if (oclass != null && oclass.isAssignableFrom(event.target.getClass())) {
```

or `oclass != null && oclass.isInstance(event.target)`.

#### B03 — Aether tools lose their attack-damage modifier

**File:** `items/tools/ItemAetherTool.java:18, 74-79`

`private float attackDamage;` is never assigned, but
`getItemAttributeModifiers()` adds a modifier with value `0` and the same UUID
as the modifier added by `ItemTool` (`field_111210_e`). `AttributeMap` applies
modifiers one by one and removes by UUID before applying; the zero-valued
modifier can replace the real tool attack modifier. At minimum the override is
wrong; at worst all Aether tools do no bonus attack damage.

**Fix:** assign `this.attackDamage = damage;` in the constructor, or remove the
override and let `ItemTool` own the modifier. Do not add a second modifier with
the same UUID.

#### B04 — Remedy bucket does not actually cure inebriation

**Files:**

- `entities/effects/EffectInebriation.java:23`
- `items/tools/ItemSkyrootBucket.java:331`

```java
new ItemStack(ItemsAether.skyroot_bucket, EnumSkyrootBucketType.Remedy.meta)
```

The two-argument `ItemStack(Item, int)` constructor sets the **stack size**, not
the metadata. This creates 3 empty skyroot buckets (damage `0`) rather than one
remedy bucket (damage `3`). `PotionEffect.isCurativeItem` compares item and
damage, so the cure check fails.

**Fix:**

```java
new ItemStack(ItemsAether.skyroot_bucket, 1, EnumSkyrootBucketType.Remedy.meta)
```

for both occurrences. The same mistake pattern should be checked anywhere an
enum `meta` is passed as the second argument.

#### B05 — Crystal/holiday leaves can create null-item drops

**File:** `blocks/natural/BlockAetherLeaves.java:124-135`

`getItemDropped` returns `null` for crystal leaves and holiday leaves, but
`BlockLeaves.getDrops` constructs an `ItemStack` from the return value. That
produces a stack with a null item, which can crash when the drop is processed or
picked up.

**Fix:** override `getDrops` fully (do not call `super` for null-drop leaves), or
return a non-null item (e.g. the appropriate sapling or nothing for those
leaves). Also guard the apple addition so it only applies when the base drop is
valid.

#### B06 — `setContainerItem(null)` mutates the skyroot bucket item globally

**File:** `registry/AetherRegistries.java:164-166`

```java
new AetherEnchantment(
    new ItemStack(ItemsAether.skyroot_bucket, 1, 2),
    new ItemStack(ItemsAether.skyroot_bucket.setContainerItem(null), 1, 3),
    1000)
```

`Item.setContainerItem` is a mutating setter. This disables the skyroot bucket’s
container behavior for the entire game when the enchanter registry initializes.
It is not limited to the output stack.

**Fix:** use `new ItemStack(ItemsAether.skyroot_bucket, 1, 3)` and handle the
container item in the enchanter/freezer logic, or create a separate item
instance if the output truly must not have a container.

#### B07 — Valkyrie teleport logic never teleports

**File:** `entities/bosses/EntityValkyrie.java:117-160`

The loop computes candidate coordinates but never assigns them to `newX/newY/newZ`
and never sets `flag = true`. The method always falls through to `teleFail()`.
The extra `rand.nextInt(...)` calls are also wasted.

**Fix:** assign the candidate coordinates when the safety checks pass, e.g.:

```java
newX = newY = newZ = ...; // computed candidate
flag = true;
```

Also reset `teleTimer` correctly and validate the destination against world
bounds and block collisions.

#### B08 — Slider boss bar is re-pointed at a dead boss

**File:** `entities/bosses/slider/EntitySlider.java:631-708`

The death branch calls `PlayerAether.get(player).setFocusedBoss(null)` around
line 640, but the method unconditionally calls
`PlayerAether.get(player).setFocusedBoss(this)` near line 708 after the damage
handling. After a killing blow, the player’s focused boss becomes the now-dead
Slider. The HUD usually clears it next frame, but this is wrong state and can
leave a stale boss reference.

**Fix:** only set the focused boss when `this.getHealth() > 0 && !this.isDead`,
or return immediately after the death block.

#### B09 — Time advancement is a side effect of a rendering/calculation getter

**File:** `world/AetherWorldProvider.java:50-87`

`calculateCelestialAngle` both **advances** `AetherData` time by 1 and writes it
back with `markDirty()`. This method is called whenever the world needs a
celestial angle (not strictly once per tick, and potentially by other mods).
Every call therefore advances Aether time; the day can run at the wrong speed,
and NBT data is dirtied continuously.

**Fix:** move the increment into a world-tick handler (for example, the
`WorldTickEvent` path already used by `AetherEventHandler`) and keep
`calculateCelestialAngle` read-only. Also cache `AetherData` values instead of
reading `NBTTagCompound` on every call.

### High

#### B10 — Zanite accessory mining bonus returns the wrong value

**File:** `inventory/InventoryAccessories.java:37-64`

The method starts with `float f = original`, multiplies `f` by the zanite
bonus, then returns:

```java
return f == original ? original : f + original;
```

When any bonus applies, it returns `f + original` instead of `f`. That roughly
doubles the intended multiplier and makes the bonus depend on the original dig
speed.

**Fix:** return `f` (or `Math.max(f, original)` if that is the intended rule).
The same logic should be extracted into a helper and unit-tested.

#### B11 — Accessory drops are not removed from the inventory

**File:** `inventory/InventoryAccessories.java:167-181`

`dropAccessories()` spawns item entities but never clears the slots. If it is
called more than once, or if the same inventory object is reused, accessories
can be duplicated. It also leaves a stale accessory inventory for any code
that reads it after the drop.

**Fix:** set each slot to `null` after dropping it, or collect a copy and clear
the slots before spawning the entities.

#### B12 — Removing the player from a portal leaves a stale half-in-portal state

**File:** `player/PlayerAether.java:305-321`

The bounds test checks the same minimum corner twice:

```java
getBlock((int) playerBounding.minX, (int) playerBounding.minY, (int) playerBounding.minZ)
&& getBlock((int) playerBounding.minX, (int) playerBounding.minY, (int) playerBounding.minZ)
```

The second check should almost certainly use `maxX/maxY/maxZ` (or check the
block under the player). As written, it does not reliably detect leaving a
portal.

**Fix:** use the correct corners, or simplify with `isInsideBlock(BlocksAether.aether_portal)`.

#### B13 — Removing dying clouds while iterating forward skips entries

**File:** `player/PlayerAether.java:230-236`

```java
for (int i = 0; i < this.clouds.size(); ++i) {
    if (this.clouds.get(i).isDead) {
        this.clouds.remove(i);
    }
}
```

Removing at index `i` shifts the next entity into `i`; the loop then increments
`i` and skips it. Use an `Iterator` or iterate backwards.

#### B14 — Swet sends a packet to every player every tick

**File:** `entities/passive/mountable/EntitySwet.java:166-168`

```java
this.midJump = !this.onGround;
AetherNetwork.sendToAll(new PacketSwetJump(this.getEntityId(), !this.onGround));
```

Every Swet on the server broadcasts every tick. This is one of the largest
avoidable network costs in the mod: `O(swets × players × 20)` packets per second.
The value is already derivable from `onGround` and can be synchronized through
`dataWatcher` or only sent on state changes to nearby players.

**Fix:** add a data-watcher byte (or use the existing one) for `midJump`, and
send nothing in `onUpdate`. If a packet is necessary, send only on
ground-state transitions with `sendToAllAround`.

#### B15 — Swet despawn and loot behavior is inverted/broken

**File:** `entities/passive/mountable/EntitySwet.java:391-402, 451-453`

- `canDespawn()` returns `this.isFriendly()`. Friendly ridden Swets are allowed
  to despawn; non-friendly Swets are never allowed to despawn. This is the
  opposite of the apparent intent (`return !this.isFriendly()`).
- `dropFewItems` calls `entityDropItem(new ItemStack(..., count), ...)` where
  `count` can be `0` (e.g. `rand.nextInt(2)`). A zero-size item is invalid and
  can cause odd pickup/merge behavior. Guard with `if (count > 0)` or use a
  normal `dropItem`.
- `capturePrey` changes the Swet’s hitbox to the player’s dimensions and never
  restores it when the prey dismounts/dies, leaving an oversized Swet.

#### B16 — Poison darts resurrect themselves

**File:** `entities/projectile/darts/EntityDartPoison.java:29-41`

```java
super.onDartHit(movingobjectposition);
...
this.isDead = false;
```

`super.onDartHit` may call `setDead()` on a successful hit; the next line clears
`isDead`. Poison darts can therefore survive hits and hit repeatedly. Remove the
assignment and let the base class manage death.

#### B17 — Hammer projectiles hit repeatedly and never stop on blocks

**Files:**

- `entities/projectile/EntityHammerProjectile.java:28-43`
- `entities/projectile/EntityProjectileBase.java:146, 270`

The base class calls `onImpact` whenever a collision is found, but
`EntityHammerProjectile.onImpact` only damages the entity and never calls
`setDead()` for a normal entity or block hit. It can therefore apply damage on
multiple consecutive ticks while overlapping an entity, and it passes through
terrain. `ticksInAir` is also incremented by both the base and the override, so
the configured `> 100` lifetime is reached in about half the intended time.

**Fix:** call `setDead()` after the first valid impact (or after the intended
piercing behavior), remove the duplicate `ticksInAir++`, and decide explicitly
whether the hammer should stop at blocks.

#### B18 — Zephyr snowballs pass through terrain

**File:** `entities/projectile/EntityZephyrSnowball.java:40-62`

`onImpact` handles entity hits only. A block hit does nothing, so the projectile
keeps moving through the block and can hit entities on the other side. Add
`this.setDead()` (and optional impact particles/sound) for
`MovingObjectType.BLOCK`, or set `inGround`/motion to zero.

#### B19 — MiniCloud can NPE on the client and in its spawn packet

**File:** `entities/passive/EntityMiniCloud.java:105-117, 170-188`

On the client, `owner` is resolved from the spawn data and may be `null`. The
client path does not guard it before `this.owner.isDead`, causing an NPE.
`writeSpawnData` also calls `this.owner.getEntityId()` without a null check.

**Fix:** guard `owner == null` before all uses; write a safe sentinel (e.g. `-1`)
when there is no owner and handle it in `readSpawnData`. Consider using
`EntityPlayer` lookup by UUID rather than entity ID.

#### B20 — EntityCrystal can NPE on its target and loses its type on reload

**File:** `entities/projectile/crystals/EntityCrystal.java`

- `onUpdate` for `THUNDER` calls `faceEntity(getAttackTarget(), ...)` and
  `moveTowardsTarget(...)` on both sides. On the client the target can be
  `null`; `faceEntity(null)`/`moveTowardsTarget(null)` can NPE.
- `writeSpawnData` writes `getAttackTarget().getEntityId()` for `THUNDER` without
  a null check.
- `type` is not written to NBT and there is no `readEntityFromNBT` override, so
  a crystal reloaded from a chunk loses its fire/ice/cloud/thunder type.
- `EnumCrystalType.get(int)` indexes `values()[id]` without bounds checking;
  corrupted data or a bad packet can throw `ArrayIndexOutOfBoundsException`.

**Fix:** validate target before client calls and spawn data; persist `type` in
NBT (`writeEntityToNBT`/`readEntityFromNBT`); make the enum lookup defensive.

#### B21 — Skyroot bed spawn handling (reviewed; no bug found)

**File:** `blocks/BlockSkyrootBed.java:95-106, 189-191`

The `NOT_POSSIBLE_NOW` branch already has an explicit inner
`player.dimension == AetherConfig.getAetherDimensionID()` guard around the
`setSpawnChunk`/`PlayerAether.setBedLocation` writes, so an overworld bed does
not write an Aether spawn. The `isBedFoot` override matches the Forge
`Block.isBedFoot` default in this environment; it was left unchanged. The
earlier report entry was a false positive (see the correction note near the end
of the document).

#### B22 — Berry bush checks the wrong block for enchanted grass

**File:** `blocks/natural/BlockBerryBush.java:46, 70`

Both checks inspect the block at the berry bush’s own position:

```java
if (world.getBlock(x, y, z) == BlocksAether.enchanted_aether_grass) {
```

The berry bush is at `x/y/z`, so this can never be true. The intended check is
the block below (`x, y - 1, z`). As written, the higher 1–4 berry drop range on
enchanted grass is unreachable.

#### B23 — Sun Spirit performs very expensive work repeatedly, including while dead

**File:** `entities/bosses/sun_spirit/EntitySunSpirit.java`

- `evapWater()` scans `21 × 21 × 8 = 3,528` blocks and may spawn 8 particles for
  every water block on every call (`line 372`). It runs every active tick.
- `unlockTreasure()` scans `40 × 9 × 40 = 14,400` blocks (`line 630`). It is
  called from `onUpdate` when `isDead()` is true, which can run for multiple
  death-update ticks.
- `burnEntities()` scans a large bounding box every active tick.
- The `isDead()` method is a custom helper that returns true when health <= 0;
  the death branch is not guarded by a one-shot flag, so achievements, door
  unlocks and chat messages can be processed repeatedly.
- `chatLine` gating is inverted-looking:
  `chatCount <= 0 || (!repeat && seen)` sends in odd/unreachable cases. In
  particular, death/chat messages may be skipped or repeated depending on the
  config and side.
- `summonFire`, `evapWater` and `unlockTreasure` modify the world on both sides;
  guard them with `!worldObj.isRemote` for authoritative changes.

**Fix:** add a `hasDefeated`/`hasUnlocked` flag, run the expensive scans once
(or on a timer), and split client particles from server-side block changes.

#### B24 — Valkyrie Queen logic errors

**File:** `entities/bosses/valkyrie_queen/EntityValkyrieQueen.java`

- `updateEntityActionState` uses:
  ```java
  if (state != BlocksAether.locked_angelic_stone
      || state != BlocksAether.locked_light_angelic_stone)
  ```
  This condition is always true because a block cannot equal both. It should
  use `&&` if the intent is “neither of the two”.
- `int pokey = rand.nextInt(2); if (pokey == 2)` is unreachable; `nextInt(2)`
  returns `0` or `1`. The peaceful message branch never runs.
- `readEntityFromNBT` calls `nbttaglist.func_150309_d(0..2)` without checking
  the list size. Older/corrupt saves can throw `IndexOutOfBoundsException`.
- `otherDimension()` always returns true, so the `timeLeft` countdown is dead
  code. `getMedals` is also unused.

#### B25 — Parachute collision test is effectively random

**File:** `entities/passive/mountable/EntityParachute.java:108-120`

`isCollided()` returns `collision_list != this.ridingPlayer.boundingBox` for
the first collision box in the list. It compares object identity, and returns
from the loop immediately. A terrain collision almost always makes the
parachute die; if the player’s box happens to be first, terrain collisions are
ignored. The result is timing/order dependent.

**Fix:** iterate the collision boxes and ignore the rider’s box explicitly, or
use a proper `checkBlockCollision`/`isAABBInMaterial` check.

#### B26 — Moa type IDs depend on HashMap iteration order

**File:** `api/AetherAPI.java:226-243`

`getMoaTypeId` uses `getMoaTypeValues().indexOf(type)`, where
`getMoaTypeValues()` builds an `ArrayList` from a `HashMap`. The ID is therefore
the current hash iteration order, not registration order. It may differ across
JVM versions, mod loading orders, or added moa types. These IDs are written to
entity NBT and network packets. A reorder changes a saved Moa’s type.

**Fix:** use a `LinkedHashMap` (registration order) or an explicit stable ID
map. `getRandomMoaType` also creates a new `Random` per call; use a shared RNG.
`AetherAPI.getMoaType(int)` indexes the list without bounds checking, and
`EntityMoa.readEntityFromNBT` passes a saved integer into it; an invalid ID can
throw `IndexOutOfBoundsException` or produce a null `AetherMoaType` that later
NPEs in `setMoaType`/`getMaxJumps`. Validate the ID and fall back to a default.

#### B27 — `MapGenSilverDungeon` reads the wrong NBT tag

**File:** `world/gen/MapGenSilverDungeon.java:97`

```java
this.xTendency = tagCompound.getInteger("zTendency");
```

This should be `"xTendency"`. Saved structures lose their X cloud tendency on
load.

#### B28 — Bronze Dungeon generates rooms twice

**File:** `world/dungeon/BronzeDungeon.java:340-344`

```java
if (!determineRoomPosition(...)) {
    return false;
}
return determineRoomPosition(...);
```

A failed first attempt is immediately followed by a second attempt with the
same arguments. This can generate a room/doors even when the first call failed
and doubles the random consumption. Make it a single call.

Also, `AetherDungeon`’s `addLineX/Y/Z`, planes and solid-box helpers contain:

```java
if (this.chance == 0) {
    this.setBlockAndNotifyAdequately(...);
    return;
}
```

The `return` exits after placing the first block. If any caller uses
`setBlocks(Block)` (chance 0), the structure is only one block thick instead of
a line/plane/box. Current Bronze Dungeon code uses non-zero chances, so this is
latent, but it is a real trap for future content.

#### B29 — `AetherGenDungeonOakTree` is effectively unreachable

**File:** `world/biome/decoration/AetherGenDungeonOakTree.java:41`

The tree requires `aether_grass` metadata `3`. Normal Aether grass is metadata
`0`; metadata `3` is only used on the golden-island components. The only caller
(`AetherBiomeDecorator:129`) runs the tree 25 times per chunk at
`world.getHeightValue`, i.e. normal surface grass, so it never generates there.
It is also redundant with `AetherGenOakTree` (the two generators are almost
identical).

**Fix:** call it from the golden-island component with the meta-3 grass, or
remove the 25-per-chunk attempts and use `AetherGenOakTree` on the surface.

#### B30 — Freezer “icestone” bonus checks its own block

**File:** `tileentity/TileEntityFreezer.java:50-55`

```java
if (this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord)
    == BlocksAether.icestone) {
```

The freezer’s own block is `BlocksAether.freezer`, never `icestone`, so the
2× speed bonus can never trigger. This should probably check the block below,
matching the enchanter’s `enchanted_gravitite` check.

Also, `TileEntityEnchanter:102` and `TileEntityFreezer:101` call
`AetherHooks.onItemEnchant/onItemFreeze` outside the
`if (!this.worldObj.isRemote)` block. The events therefore fire on both client
and server; event listeners may run twice or perform client-only effects on the
server.

#### B31 — Some mobs spawn projectiles on the client

**Files:**

- `entities/hostile/EntityAechorPlant.java:135`
- `entities/hostile/EntityCockatrice.java:109`
- `entities/ai/aechorplant/AechorPlantAIShootPlayer.java:70` (dead AI class)

These spawn poison needles/arrows without a `!worldObj.isRemote` guard. On the
client this creates ghost entities and can cause visual/duplication
inconsistencies. Guard the spawn (and ideally the AI logic) with the server
side.

#### B32 — Parachute items spawn client-side too

**File:** `items/tools/ItemAetherParachute.java:18-35`

`onItemRightClick` calls `world.spawnEntityInWorld(...)` and consumes/ damages
the stack on both sides. The server also spawns the parachute, so the client can
end up with a locally created, unsynced parachute in addition to the server
entity. Guard the spawn and inventory mutation with `!world.isRemote`.

#### B33 — `AetherLore` can NPE for unregistered/custom items

**File:** `registry/AetherLore.java:9-22`

`GameRegistry.findUniqueIdentifierFor(stack.getItem())` can return `null` for
items that are not registered through the GameRegistry (custom stacks, some
vanilla/foreign items, or a null stack). The code immediately dereferences
`.modId`. `SlotLore.isItemValid` calls this for any item placed in the lore slot.

**Fix:** null-check both `stack` and the `UniqueIdentifier`, and return a
sentinel key or `""` for unknown items.

#### B34 — Log double-drop uses rotation metadata

**File:** `blocks/natural/BlockAetherLog.java:42, 107-114`

`int size = meta == 0 ? 2 : 1;` treats the log block metadata as a species flag,
but `BlockLog` metadata is the log axis/orientation (`0..3`). One quarter of
logs in each orientation get double drops from a skyroot tool.

**Fix:** base double-drop on the block type, not on `meta`. Golden oak logs keep
their intended normal drop of skyroot logs; they only drop themselves through
the silk-touch path, and still drop golden amber from a suitable axe.

#### B35 — Player accessory renderer has state bugs

**File:** `client/renders/entity/PlayerAetherRenderer.java`

- `line 129`: `this.modelWings.isSneak` is assigned twice; `modelGlow.isSneak`
  is never set.
- `line 143`: `model.onGround` is set to `player.getSwingProgress(partialTicks)`
  rather than the interpolated limb-swing amount (`f6`). `ModelBiped.onGround`
  is a float animation value, not the entity's boolean `onGround`; the current
  value makes arm/leg animation depend on swing progress.
- Shield texture selection at lines 396-405 binds `shield.texture_inactive`
  even when `hasInactiveTexture()` is false; a shield accessory without an
  inactive texture can try to bind a null texture. Guard the null case and use
  the active texture as a fallback.
- `isCapeRendering` is set during `RenderLivingEvent.Post`, but read during
  `RenderPlayerEvent.Specials.Pre`. The value is therefore from the previous
  player/frame and can hide/show the vanilla cape incorrectly for one frame.

#### B36 — Loading-screen framebuffer is never deleted

**File:** `client/gui/AetherLoadingScreen.java:34-35`

Every `AetherLoadingScreen` allocates a full-resolution `Framebuffer` and there
is no `deleteFramebuffer()` call when the loading screen is replaced. Repeated
world loads can leak VRAM. The framebuffer is also sized once at construction
and not resized if the window changes size.

**Fix:** override/implement cleanup when the loading screen is discarded, or
reuse a single framebuffer and resize it through `createBindFramebuffer`.

#### B37 — World tick handler runs twice per tick and creates AetherData in every world

**File:** `AetherEventHandler.java:357-399`

`TickEvent.WorldTickEvent` is fired for both `START` and `END`, but the method
does not check `event.phase`. It also calls
`AetherData.getInstance(event.world)` before checking whether the world is an
Aether world, which creates/loads the Aether data map in the overworld and every
other dimension.

**Fix:** return unless `event.phase == TickEvent.Phase.END` (or START), and move
the `AetherData` lookup inside the `provider instanceof AetherWorldProvider`
branch.

#### B38 — Config code has duplicate/dead state

**File:** `AetherConfig.java:36-44, 100-106`

- `book_of_lore` is assigned twice; the second assignment reads the Golden
  Parachute option into the Book of Lore field.
- `golden_parachute` is never assigned or read.
- `toggle_music` and `boss_names` are loaded into fields but all runtime uses
  read `AetherConfig.config.get(...)` directly instead.
- `shouldFloatWithBlock()`, `menuEnabled()` and `menuButtonEnabled()` have no
  callers.

This is not currently a gameplay bug (the gameplay paths read the config
directly), but it is misleading and makes future changes easy to get wrong.

#### B39 — Mod version is hardcoded and stale

**File:** `Aether.java:25`

```java
@Mod(modid = Aether.MOD_ID, version = "v1.1.2.5")
```

The build generates `com.gildedgames.the_aether.Tags.VERSION`
(`generateGradleTokenClass = com.gildedgames.the_aether.Tags`), currently
something like `2.0.0-main+...`, but the mod annotation ignores it. The mod
metadata and reported version disagree.

**Fix:** use `version = Tags.VERSION` (same package, no import needed), or
remove the build token configuration if the hardcoded version is intentional.

#### B40 — Slab translation keys are missing the `tile.` prefix

**Files:** `src/main/resources/assets/aether_legacy/lang/en_US.lang:221-235` (and
the corresponding lines in every other language).

The half-slab entries are written as:

```text
skyroot_slab.name=Skyroot Slab
carved_slab.name=Carved Slab
...
```

But `ItemBlock`/`Block` resolve block item names as `tile.<blockName>.name`.
The double-slab keys already use `tile.`. As a result, normal slabs show raw
translation keys in inventory.

**Fix:** prefix all eight half-slab keys with `tile.`.

#### B41 — Packet-based perks are not validated server-side

**File:** `network/packets/PacketPerkChanged.java:90-112`

The handler only checks that the packet’s entity ID matches the sender. It does
not check that the player is ranked/developer for HALO/GLOW, nor that the player
is permitted to use a custom Moa skin. The renderer gates halo/glow, but
`MoaRenderer` accepts any non-default `donatorMoaSkin` for a player. A client can
therefore spoof donator/developer cosmetic data.

**Fix:** validate `AetherRankings.isRankedPlayer` / `isDeveloper` on the server
before changing the corresponding fields; ignore unauthorized values.

#### B42 — `ItemZaniteSword.getItemAttributeModifiers()` returns null

**File:** `items/weapons/ItemZaniteSword.java:24-27`

`getAttributeModifiers(ItemStack)` is overridden correctly, so normal
`ItemStack` use works, but any caller of the deprecated `getItemAttributeModifiers`
gets `null` and can NPE. Return an empty multimap (or the super value) instead
of null.

#### B43 — `ItemMoaEgg` can NPE in creative placement

**File:** `items/ItemMoaEgg.java:55-62`

In creative mode, `onItemUse` calls `stack.getTagCompound().getInteger("typeId")`
without a null check. An egg without NBT (custom/legacy stack, or one created
via `/give` without tags) throws. Use
`getMoaTypeFromItemStack(stack)`, which already handles a null tag.

### Medium/Low robustness issues

- `block/dungeon/BlockTreasureChest.java:29-47` casts the tile entity to
  `TileEntityTreasureChest` without an `instanceof` check and does not null-check
  in `getExplosionResistance`/`onBlockActivated`. A missing/replaced tile entity
  causes a `ClassCastException` or NPE.
- `entity/bosses/slider/EntitySlider.java:386-401` `checkIsAir` calls
  `blockList.get(1)` and can throw if the checked region contains one block.
  Callers currently use larger regions, but the helper is unsafe.
- `entity/bosses/EntityValkyrie.java:296-308` saves `safeX/safeY/safeZ` as
  `double` but reads them with `getInteger`, truncating the coordinates.
  `EntityValkyrieQueen:405-410` also reads the safe-position list without a size
  check.
- `entity/hostile/EntityAechorPlant.java` has an arbitrary `knockBack` guard:
  `if (this.getHealth() >= 0) return;` means the knockback only happens at
  negative health. For an immobile plant, returning unconditionally (or using
  `> 0`) is clearer.
- `entity/hostile/EntityCockatrice.java:applyEntityAttributes` calls
  `setHealth(10)` but never sets the max-health base value, so the max remains
  the default 20 while the current health starts at 10. Confirm the intended
  value and set `maxHealth`.
- `entity/passive/EntityAerbunny.java:110-117` and `EntityFlyingCow`/`EntityPhyg`
  contain dead inner conditions (`if (!isOnGround()) { if (isOnGround()) ... }`).
- `entity/passive/EntityAetherAnimal.java:14` has an unused `Random` field;
  `ItemAetherTool.random`, `AetherDungeon.random` and several other fields are
  similarly unused.
- `util/FilledList.add(int, E)` ignores the index and calls `List.add` on a
  fixed-size `Arrays.asList` view; it should throw/implement insertion
  correctly or be removed if unused.
- `items/weapons/ItemVampireBlade.java:36` casts `attacker` to `EntityPlayer`
  unconditionally; a non-player attacker (command/mod-spawned equipped mob) can
  crash. Use `instanceof`.
- `items/weapons/ItemCandyCaneSword.java:30` and `ItemHolystoneSword.java:29`
  create a new `Random` per hit and call `damageItem(..., attacker)` without a
  null check on the attacker.
- `items/tools/ItemSkyrootBucket.java:onBucketUsed` decrements the stack on the
  client too; combined with the curative-item bug this can produce client/server
  drift. Most item actions should be server-authoritative.
- `blocks/BlockIcestone.java:onBlockAdded` converts a 7×7×7 region on both
  client and server, with no side guard and no metadata update. It also never
  uses `setTickRandomly` because there is no `updateTick`.
- `world/biome/decoration/AetherGenFloatingIsland.java:89-93, 124-128, 167-171`
  contains tautological conditions such as `if (z != 0 || z != 1)` (always true)
  and `if (x != 0 || x != 1)` (always true). If those were meant to skip specific
  offsets, use `&&` or explicit set membership. This changes which blocks are
  overwritten during island generation.
- `blocks/natural/BlockAetherOre.java:85` and `AetherEventHandler.java:327`
  create a new `Random` for every drop/particle event. Use `world.rand`/a shared
  `Random`.
- `world/biome/AetherBiomeDecorator.java:66` prints `"Already decorating"` to
  stdout. Use the ModLog/logger and reset state in a `finally` block; an
  exception inside `genDecorations` currently leaves `this.world` set and
  permanently disables decoration for subsequent chunks.
- `network/packets/AetherPacket.java:17-25` dispatches client messages using
  `Aether.proxy.getPlayer()`, which is not null-safe and assumes the packet is
  handled on the client thread. Packet handlers should use the supplied
  `MessageContext` and null-check the player/entity properties.
- `network/packets/PacketAccessory.java:19-24, 44-55` stores the raw `ByteBuf`
  (`readBuf`) and reads it later in `handleClient`. FML currently processes the
  message synchronously enough for this to work, but it is use-after-release
  waiting to happen. Parse the accessory data during `fromBytes` into fields.
- `network/packets/PacketDisplayDialogue.java` and `PacketCheckKey.java` have no
  senders (see dead code), so the generic server-dialogue system and the old
  lore-key packet are effectively unused.
- `inventory/slots/SlotLore.java:21` and `inventory/InventoryAccessories.java`
  call registry/lookup helpers without null checks; see B33 and B11.
- `client/gui/dialogue/entity/GuiValkyrieDialogue.java:getMedalDiaulogue` returns
  as soon as it finds the first victory-medal stack. If that stack has fewer
  than 10 medals, the GUI reports “not enough” even when another stack in the
  inventory would bring the total to 10. `PacketInitiateValkyrieFight` also
  requires all 10 medals in one slot. Sum the medal count across the inventory
  (or update the UI/server check to accept multiple stacks) if the intended
  entry fee is 10 total medals.

---

## 4. Performance and optimization opportunities

### P01 — Reflection in the player tick

`PlayerAether.onUpdate` performs a reflective field lookup for `isJumping` on
every player every tick. Cache the `Field` or, better, replace the data flow
with a client-side input flag plus a small server packet (see reflection table).

### P02 — AetherData NBT reads/writes on every check

`AetherData.isEternalDay()`, `isShouldCycleCatchup()` and `getAetherTime()` all
read from `NBTTagCompound` on every call. `setAetherTime` calls `markDirty()` on
every increment. Keep authoritative primitive fields in memory, read NBT only on
load, and write on save/change.

### P03 — Sun Spirit and Whirlwind scans

- `EntitySunSpirit.evapWater` and `unlockTreasure` are quadratic scans run from
  the entity update path (see B23).
- `EntityWhirlwind.onLivingUpdate` performs
  `getEntitiesWithinAABBExcludingEntity` over a 5×5×5 region every tick and
  allocates an `ArrayList`/`HashSet` in `checkRegion`/`AerwhaleAIUnstuck`.
- `EntityFloatingBlock` scans nearby entities every tick.
- `EntityHook.onUpdate` performs three separate target checks
  (`getAttackTarget`, `getEntityToAttack`, `getAITarget`) for every living
  non-player entity. One `getAITarget()` check plus a null/instanceof guard is
  sufficient.

### P04 — Network

- Swet jump packets (B14) are the largest offender.
- `PacketUpdateLifeShardCount` is sent with `sendToAll` when the player is at
  max shards (`PlayerAether.java:645`). Only the owning player needs this value.
- Several packet classes instantiate new objects every tick even when the value
  did not change; `PlayerAether` already has a good change-detection pattern for
  perks/cape, extend it to shards and other repeated state.

### P05 — World generation

- `AetherGenDungeonOakTree` is attempted 25 times per chunk but is effectively
  unreachable at the surface (B29).
- `AetherGenClouds` calls `setBlockAndNotifyAdequately` for every cloud block and
  can drift far outside the current chunk (the initial x/z is `chunkX + rand(16)`
  and the amount loop moves up to ±50). Add chunk-bounds checks and use a
  lighter block setter during generation.
- `AetherGenFloatingIsland` scans ~7,400 blocks per attempt and contains
  tautological conditions (see B29/refactor list).
- `AetherBiomeDecorator` allocates a new `WorldGenLakes`/tree generator per
  decoration; reuse the existing fields/tree generator cache.
- `ComponentGoldenIsland` contains commented-out tree/lake generation; the
  golden islands are currently barren (feature gap) while the surface decorator
  spends attempts on a tree generator that cannot match.

### P06 — Client rendering / loading

- `AetherLoadingScreen` allocates a full-resolution framebuffer per instance and
  never deletes it (B36).
- `AetherItemRenderer` copies almost the entire vanilla first-person item
  renderer. Maintainability and future 1.7.10 patches are a concern; prefer
  per-item `IItemRenderer`s or a `RenderHandEvent` hook.
- `AetherClientEvents.onClientTick` calls `GameSettings.isKeyDown`/`Mouse` event
  state every tick; `handleExtendedReach` checks `Mouse.getEventButton` outside
  the actual mouse-event loop. This can use stale event state and trigger more
  than once per click. Use `MouseEvent`/`InputEvent` where possible.
- `AetherConfig.config.get(...)` is called repeatedly in GUI/music tick paths
  instead of cached getters. Negligible individually, but it appears on every
  tick/GUI open and makes the config API harder to reason about.

### P07 — Repeated registry key construction

`AetherAPI` builds a new `ResourceLocation` by concatenating unlocalized names
for every lookup. Extract:

```java
private static ResourceLocation key(ItemStack stack) { ... }
```

and cache entries where practical. The six `HashMap`s are also initialized with
capacity 4096 while holding a few dozen entries; use default capacity or
`LinkedHashMap` for stable iteration order.

### P08 — Trivia file reads

`AetherTrivia.getNewTrivia()` may read the same localization file up to three
times (locale, en_US fallback), and `AetherLoadingScreen` calls it whenever the
loading screen is initialized. Cache the parsed lists per language code.

---

## 5. Refactoring opportunities

1. **Unify the two dungeon-system base classes.**
   `world/dungeon/util/AetherDungeon` and `world/gen/AetherStructure` duplicate
   almost the same `addLine*`, `addPlane*`, `addHollowBox`, `addSolidBox` and
   block-placement logic. `AetherDungeon` also has the early-return bug in every
   `chance == 0` branch. Keep one implementation and make both dungeon
   generators use it.

2. **Extract the repeated container block-break code.**
   `BlockEnchanter`, `BlockFreezer` and `BlockIncubator` each contain nearly
   identical `breakBlock` item-spawning code. Move it to a helper accepting an
   `AetherTileEntity`.

3. **Replace the duplicated output/merge logic in tile entities.**
   `TileEntityEnchanter` and `TileEntityFreezer` share most of the progress,
   fuel, NBT transfer and output merging code. A small shared base class with
   type-specific hooks would reduce the chance of fixing a bug in only one of
   them.

4. **Make ability access typed.**
   `PlayerAetherEvents` accesses `getAbilities().get(3)` for repulsion and casts
   the accessory inventory to the concrete `InventoryAccessories`. Add named
   ability getters/interface methods to avoid brittle index/implementation
   casts.

5. **Centralize item drop logic.**
   `BlockAetherOre`, `BlockAetherLog`, `DoubleDropHelper` and
   `BlockEnchanter/Freezer/Incubator` all reimplement parts of the harvest/drop
   algorithm. One helper with explicit policies would remove the metadata and
   null-drop bugs.

6. **Remove the unused `name` parameter from `AetherRegistries.register`/
   `registerShapeless`**, or actually use it for recipe IDs/debugging.
   Currently every call passes a name that is silently discarded.

7. **Use a stable registry abstraction in `AetherAPI`.**
   Replace the repeated `HashMap<ResourceLocation, ...>` boilerplate with a
   small typed registry class, `LinkedHashMap` for registration-order-sensitive
   content, and a shared key helper.

8. **Localize hardcoded GUI strings.**
   `GuiSunAltar` (“Sun Altar”, “Select Time”), `GuiAetherPerks` (many labels),
   `GuiTreasureChest` fallback “platinum”, etc. Should use `I18n.format`.

9. **Separate optional-mod integration.**
   Keep all Battlegear/NEI references behind dedicated classes loaded only when
   `Loader.isModLoaded` is true.

10. **Add tests for the pure logic.**
    `AetherWorldProvider` time math, `PacketSetTime.setTime`, zanite durability
    scaling, accessory strength calculation, moa ID mapping, and the dungeon
    structure helpers are testable without a full Minecraft client. The initial
    infrastructure and baseline suite are described in section 10.

---

## 6. Dead code and unreachable features

The following classes are not referenced from anywhere outside themselves (or
are referenced only by other dead code):

| Class | Notes |
|---|---|
| `entities/ai/aechorplant/AechorPlantAIShootPlayer` | Duplicated logic inside `EntityAechorPlant`; never registered. |
| `entities/ai/EntityAIUpdateState` | Never added to any entity task list. |
| `entities/ai/zephyr/ZephyrAIShootTarget` | Duplicated in `EntityZephyr.updateEntityActionState()`. |
| `entities/ai/zephyr/ZephyrAITravelCourse` | Duplicated in `EntityZephyr.updateEntityActionState()`. |
| `entities/particles/EntityBlueFX` | Never spawned. |
| `entities/particles/EntityCloudSmokeFX` | Never spawned. |
| `entities/particles/EntityGoldenFX` | Never spawned. |
| `entities/particles/EntityPurpleFX` | Never spawned. |
| `items/util/EnumGummySwetType` | `ItemGummySwet` hardcodes the two type names instead. |
| `client/gui/button/GuiAetherButton` | Never instantiated. |
| `client/gui/button/GuiDescButton` | Never instantiated; also compares strings with `==`. |
| `world/biome/decoration/AetherGenCave` | Never called. |
| `world/biome/decoration/AetherGenLakes` | Declared in `AetherBiomeDecorator` but `aether_lakes` is never used. |
| `world/biome/decoration/AetherGenLiquids` | Declared in `AetherBiomeDecorator` but `liquid_overhang` is never used. |
| `network/packets/PacketDisplayDialogue` | No sender; generic dialogue GUI is unreachable. |
| `network/packets/PacketCheckKey` | No sender; handler is intentionally empty. |
| `client/gui/inventory/GuiAetherPerks` | Its only button is commented out in `GuiAccessories`; also has `Integer.decode("0x" + text)` calls that can throw on invalid input. |

Other dead/unused members:

- `AetherConfig`: `book_of_lore`, `golden_parachute`, `toggle_music`, `boss_names`
  fields and `shouldFloatWithBlock()`, `menuEnabled()`, `menuButtonEnabled()`
  getters.
- `AetherOverlay.getCureAlpha` (renderCure hardcodes `0.5F`).
- `PlayerAether.isDonator()` returns `true` unconditionally and is only used in a
  commented-out GUI block.
- `PlayerAether.clouds` loop skips removals (B13).
- `EntityValkyrie.timeLeft`, `EntityValkyrieQueen.timeLeft`/`otherDimension()`
  and `getMedals()`.
- `EntityWhirlwind.isRainbow` is never set to true; `canDropItems` is only read
  from NBT and never changed.
- `EntityMoa` `secsUntilFlying`/`secsUntilWalking` fields (declared, never used).
- `ItemAetherTool.random`, `AetherDungeon.random`, `EntityAetherAnimal.random`,
  `AetherGenLakes` dead fields.
- `AetherMainMenu` demo button IDs 11/12 are handled but no demo buttons are
  added.
- `ComponentGoldenIsland`/`ComponentGoldenIslandStub` have commented-out golden
  oak tree and lake generation (`AetherGenUtils.generateGoldenOakTree(...)`).
- `AetherGenDungeonOakTree` is effectively dead in its current call site (B29).
- `EntitySlider` `hasActivated`/`timeFloated`-like unused fields (if retained;
  check with an IDE's unused-field inspection).

---

## 7. Feature ideas / gaps

1. **OreDictionary integration**: register Aether logs, planks, ores, gems,
   stone types and sticks (`logWood`, `plankWood`, `oreZanite`, `gemZanite`,
   `blockHolystone`, etc.). This is currently almost absent and limits
   cross-mod recipes.
2. **NEI integration**: since NEI is already a development runtime dependency,
   add optional recipe handlers for the enchanter/freezer/incubator and guard
   them behind a separate compatibility class.
3. **Golden-island vegetation**: re-enable the commented-out golden oak trees and
   lakes in `ComponentGoldenIsland`/`Stub`.
4. **Generic dialogue system**: `PacketDisplayDialogue` and `GuiServerDialogue`
   already exist but are never used. Wire them into a dialogue registry/API
   instead of hardcoded boss chat.
5. **Donator perk customization**: `GuiAetherPerks` is unreachable; either wire
   the button in `GuiAccessories` or remove the dead feature. The Moa skin
   payload should be permission-checked server-side.
6. **Config validation**: clamp/validate dimension IDs, biome IDs, potion IDs and
   spawn rates; reject collisions with other mods rather than crashing during
   registration.
7. **Data-driven loot tables**: dungeon loot is currently a set of `switch`
   statements. Move it to JSON/NBT-backed tables for pack authors and easier
   testing.
8. **Boss arena state**: make the Slider/Valkyrie Queen/Sun Spirit unlock and
   door logic explicitly stateful and idempotent; currently several checks run
   every tick and can repeat achievements/messages.
9. **Localization coverage**: finish the missing slab keys and replace hardcoded
   GUI strings; several languages only translate a subset of the newer content.
10. **Tests and CI**: add unit tests for packet math/time/loot and a headless
    `runServer` smoke test. The current CI only compiles/builds.

---

## 8. Suggested priority order

1. **Immediate user-visible correctness**
   - B01 custom HUD.
   - B02 spawn-egg breeding.
   - B03 tool attack damage.
   - B04 remedy cure.
   - B05 null leaf drops.
   - B06 global bucket mutation.
   - B07 Valkyrie teleport.
   - B08 Slider focus.
2. **Crash/NPE hardening**
   - B19 MiniCloud, B20 Crystal, B33 AetherLore, B43 Moa egg,
     B40/B41 packet/config issues, B19/B30 tile-entity casts.
3. **Boss and world logic**
   - B23 Sun Spirit, B24 Valkyrie Queen, B27/B28 worldgen, B30 freezer.
4. **Performance**
   - B14 Swet networking, B09/P02 time data, P03 entity scans, B36 framebuffer.
5. **Reflection removal**
   - Dungeon keys via `EntityAetherItem`, spawn-egg factory, remove
     `Class.forName`, then tackle the GUI/EntityRenderer/`isJumping` cases.
6. **Dead code/refactors/features**
   - Remove or wire the dead classes, finish localization, add OreDictionary
     integration and tests.

---

## 9. Verification notes

- `dependencies.gradle` was updated and the project builds successfully with
  `./gradlew build --no-configuration-cache`.
- `./gradlew test`, `./gradlew jacocoTestReport`, and `./gradlew gameTestServer`
  pass.
- A `clean build` can hit a Windows file lock on
  `build/rfg/recompiled_minecraft-1.7.10.jar` when the Gradle daemon/IDE still
  holds it; `./gradlew --stop` resolves that local-only issue.
- `spotlessCheck` and `checkstyleMain` pass.
- Unit tests and an in-game server test mod now exist (section 10). The
  behavioral bugs above should still be validated in a client/server test
  world; add regression tests as they are fixed.
- No access-transformer files, mixin files, or ASM transformer code were found.
- Reflection was searched with:
  `ObfuscationReflectionHelper|ReflectionHelper|Class.forName|getConstructor|getDeclared|setAccessible|MethodHandles|newInstance`
  and all hits are listed in section 2.

---

## 10. Test baseline infrastructure (implemented)

The project now has a headless unit-test suite and an in-game dedicated-server test
mod. Neither test source set is included in the published mod jar.

### Unit tests

- **Build wiring:** `addon.gradle.kts`
  - JUnit 4.13.2
  - Mockito 4.11.0 (`mockito-core` + `mockito-inline`)
  - Java 8 test launcher, matching the mod's Java 8 bytecode
  - `testCompileClasspath`/`testRuntimeClasspath` extend `compileOnly` so tests
    can load production classes that reference optional dependencies.
- **Sources:** `src/test/java`
  - `UtilityTest`: `FilledList`, `RandomTracker`
  - `EnumMappingTest`: bucket, dungeon-key, dart, accessory and tool enums
  - `AetherAPIRegistryTest`: accessory/enchantment/fuel/freezable registry
    round-trips using Mockito mocks
  - `PacketRoundTripTest`: `PacketSetTime`, `PacketDisplayDialogue`,
    `PacketInitiateValkyrieFight` serialization
- **Commands:**
  - `./gradlew test`
  - `./gradlew jacocoTestReport`
  - `./gradlew check` (runs the suite and generates the JaCoCo report)

### JaCoCo baseline

JaCoCo 0.8.16 is attached to the unit-test JVM and `check` now depends on
`jacocoTestReport`.

Current baseline from this workspace (24 tests, all passing):

| Counter | Covered | Missed | Coverage |
|---|---:|---:|---:|
| LINE | 404 | 19,132 | 2.07% |
| BRANCH | 77 | 9,248 | 0.83% |
| METHOD | 102 | 2,567 | 3.82% |
| CLASS | 26 | 377 | 6.45% |

The low baseline is expected: the initial suite only covers pure utility/API
code, while most of the mod needs the game. This is a regression baseline, not
a target. Reports are written to:

- `build/reports/jacoco/test/html/index.html`
- `build/reports/jacoco/test/jacocoTestReport.xml`
- `build/jacoco/test.exec`

### In-game game test mod

- **Source set:** `src/gameTest/java`
- **Test-only mod:** `com.gildedgames.the_aether.gametest.GameTestMod`
  - Mod ID `aether_legacy_gametest`
  - `required-after:aether_legacy`
  - Never included in the published jar
- **Assertions:** `ServerGameTests`
  - Block and item registry presence for representative Aether content
  - Creative-tab icons
  - Aether dimension/provider/biome registration
  - Aether entity mappings
  - Aether API accessory/enchantment/fuel/freezable/moa registries
  - Lore lookup
  - Crafting and furnace recipe presence
  - A live block placement/read/restore in the overworld
- **Command:**
  - `./gradlew gameTestServer`
  - The `prepareGameTestServer` task creates `run/server` and writes
    `eula=true` if missing.
  - The mod prints `AETHER_GAMETEST_OK` and exits 0 when all checks pass; it
    prints `AETHER_GAMETEST_FAILED` and exits 1 on failure.

Current result:

```text
[AETHER-GAMETEST] 125/125 checks passed
AETHER_GAMETEST_OK
BUILD SUCCESSFUL
```

The game-test run currently uses the plain Java 8 `runServer` task. If a future
dependency requires lwjgl3ify, switch the `gameTestServer` dependency to
`runServer25` and keep the same classpath/system-property injection.


### First batch fix status

The first batch of findings has been applied:

- B01 — custom HUD no longer returns on `event.isCancelable()`.
- B02 — spawn-egg breeding compares the target entity class, not the event handler.
- B03 — removed the duplicate tool attack-damage modifier; tools use `ItemTool`'s real damage.
- B04 — remedy bucket cure stack now uses `(item, 1, metadata)`.
- B05 — invalid null-item stacks are removed from leaf drops.
- B06 — enchanter registration no longer mutates the global skyroot bucket container item.
- B07 — Valkyrie teleport now selects and assigns a safe destination.
- B08 — Slider no longer re-focuses itself after death.

Regression checks for B03, B04, B05 and B06 were added to the game-test mod.
After the first batch the in-game baseline was **94/94 checks passed**; it is
now **100/100** after the second batch.

### Second batch fix status

- B09 — Aether time now advances once per server world tick in
  `AetherWorldProvider.updateAetherTime()`; `calculateCelestialAngle()` is
  read-only and preserves the independent Aether clock.
- B37 — `WorldTickEvent` now handles only `Phase.END`, and `AetherData` is
  resolved only for Aether worlds.
- B10 — zanite accessory dig-speed bonuses are applied to `BreakSpeed.newSpeed`
  and the bonus is no longer added to the original value a second time.
- B11 — accessory drops clear their inventory slots, and
  `setAccessorySlot(type, null)` now clears instead of throwing.
- B12 — portal exit detection checks the player's bounding box rather than
  comparing the same minimum corner twice.
- B13 — dead clouds are removed while iterating in reverse, so no entry is
  skipped.

Added game-test coverage for B09/B37 and unit coverage for the accessory drop
and removal paths. After the second batch the baseline was **100/100 in-game
checks** and **20 unit tests**.


### Third batch fix status

- B16 — poison darts no longer clear `isDead` after a hit.
- B17 — hammer projectiles die on impact and no longer double-count
  `ticksInAir`.
- B18 — Zephyr snowballs die on block/other impacts and no longer double-count
  `ticksInAir`.
- B19 — MiniCloud handles a missing owner in updates and spawn data.
- B20 — crystal targets are null-safe, crystal type is persisted through NBT,
  and `EnumCrystalType.get` is bounds-checked.
- B33 — lore lookup handles null/unregistered stacks; the lore slot and GUI are
  null-safe.
- B42 — `ItemZaniteSword#getItemAttributeModifiers` no longer returns null.
- B43 — Moa eggs without NBT or with invalid type ids fall back safely; Moa
  type lookup is bounds-checked.

Added game-test checks for MiniCloud spawn data, crystal NBT/spawn data, Moa egg
fallbacks, invalid Moa ids, lore null handling, and the Zanite sword attribute
map; added a unit test for crystal-type bounds. The baseline is now
**109/109 in-game checks** and **21 unit tests**.


### Fourth batch fix status

- B14 — Swet jump packets are sent only on ground-state changes, and only to
  nearby players, instead of broadcasting every tick.
- B15 — non-friendly Swets can despawn again; zero-size loot stacks are
  skipped; a captured Swet’s hitbox is restored after its prey leaves; invalid
  Swet type ids fall back safely.
- B22 — Berry bushes check the soil below them for the enchanted-grass bonus.
- B24 — fixed the Valkyrie Queen’s always-true locked-stone condition, made
  the peaceful dialogue branch reachable, and guarded the saved safe-position
  list.
- B27 — `MapGenSilverDungeon` reads the correct `xTendency` NBT tag.
- B30 — the freezer checks the icestone block below it and fires its Forge hook
  only on the server.
- B31 — Aechor Plants and Cockatrices no longer spawn projectiles client-side.
- B32 — parachute items are server-authoritative, with a null held-item guard.

Added a Swet enum bounds unit test, a `PacketSwetJump` round-trip test, a
game-test Swet despawn assertion, and a game-test freezer/icestone progress
assertion. After the fourth batch the baseline was **112/112 in-game checks**
and **23 unit tests**.

### B21 correction

The skyroot-bed spawn path already guards the respawn-point write with an
explicit Aether-dimension check. The `isBedFoot` override matches the Forge
default in this environment (`isBlockHeadOfBed`), so no behavior change was
made; the earlier report entry was based on a false positive.


### Fifth batch fix status

- B25 — parachute collision now ignores the rider box and checks every
  colliding box; it no longer depends on list ordering.
- B26 — Moa types are stored in a `LinkedHashMap`, giving stable persisted IDs
  in registration order.
- B34 — skyroot double-drop behavior is based on the block type instead of log
  rotation metadata. Golden oak logs still drop skyroot logs normally and only
  drop themselves with silk touch.
- B38 — the golden parachute config value is assigned to its own field instead
  of overwriting the Book of Lore field.
- B39 — the `@Mod` version uses the generated `Tags.VERSION` token.
- B40 — all half-slab localization keys use the required `tile.` prefix in every
  language file.

Added a language-key unit test, game-test assertions for Moa type order, log
normal-drop species, and generated mod version. The baseline is now
**119/119 in-game checks** and **24 unit tests**.

### Sixth batch fix status

- B23 — the Sun Spirit defeat path now runs once and persists `hasDefeated`;
  achievements, eternal day, door removal and treasure unlocking are
  server-authoritative. Fire/water/door/treasure/burn world changes are
  server-side, the water scan runs on a 10-tick interval, and death/dialogue
  chat is no longer gated by a stale chat cooldown.
- B28 — Bronze Dungeon calls `determineRoomPosition` once. The latent
  `chance == 0` branches in `AetherDungeon` now `continue` instead of returning
  after the first block, so lines, planes and boxes fill correctly.
- B35 — glow sneak state is assigned to `modelGlow`, `ModelBiped.onGround` uses
  the interpolated limb-swing float, shields without an inactive texture fall
  back to the active texture, and cape rendering is read directly from the
  accessory inventory instead of cached state from the previous frame.
- B36 — `AetherLoadingScreen` reuses a single shared framebuffer and resizes it
  when the window changes, instead of leaking a framebuffer per loading screen.
- B41 — perk packets are validated server-side: halo requires a ranked player,
  glow requires a developer, and Moa skins require a donator/ranked player.

Added unit coverage for the dungeon fill helpers and perk permissions, plus
game-test coverage for Sun Spirit defeat-state persistence. The baseline is now
**121/121 in-game checks** and **31 unit tests**.

### Seventh batch fix status

Medium/low robustness batch:

- `BlockTreasureChest` now uses `instanceof` checks and safe fallbacks when the
  tile entity is missing or replaced; missing tile entities no longer crash
  hardness, explosion-resistance, or activation checks.
- `EntitySlider.checkIsAir` is static and safe for single-block regions; it no
  longer reads index 1 of a one-element list, and it uses `World.isAirBlock`
  instead of relying on the block registry being bootstrapped.
- `EntityValkyrie` reads its persisted safe position with `getDouble`, preserving
  fractional coordinates instead of truncating them to integers.
- `EntityAechorPlant` is explicitly immune to knockback for its entire lifetime,
  matching its rooted, immovable behavior.
- `EntityCockatrice` sets its max-health attribute to 10 instead of leaving the
  default 20 while starting at 10 health.
- `ItemVampireBlade` only heals player attackers and no longer casts a
  command/mod-spawned non-player attacker to `EntityPlayer`.
- `ItemCandyCaneSword` and `ItemHolystoneSword` use the struck entity's world RNG
  and guard the durability call when the attacker is null.
- `BlockIcestone` only converts water/lava on the server; clients receive the
  resulting block updates.
- `PacketAccessory` copies its payload out of the pooled Netty buffer during
  `fromBytes`, avoiding use-after-release. `AetherPacket` also null-checks the
  client player and server handler.
- `AetherBiomeDecorator` logs re-entry instead of printing to stdout and resets
  its world/random state in a `finally` block, so one failed decoration cannot
  disable decoration for every later chunk.

Added unit coverage for treasure chests, slider air checks, icestone side
handling, biome-decorator cleanup, Vampire Blade attackers, and accessory packet
decoding; added game-test checks for icestone conversion, Valkyrie safe-position
persistence, Aechor Plant knockback immunity, and Cockatrice health. The baseline
is now **125/125 in-game checks** and **42 unit tests**.

B29 remains deferred pending a worldgen design decision.

### Eighth batch fix status

More medium/low cleanup:

- `ItemSkyrootBucket.onBucketUsed` is now server-authoritative: the client no
  longer consumes the stack or applies potion/cure effects, preventing
  client/server drift.
- `BlockAetherOre` uses the world RNG for experience drops when the block access
  is a `World`, instead of allocating a fresh `Random`.
- The pig-slayer particle path uses the target world's RNG instead of creating a
  new `Random` inside the particle loop.
- `FilledList` is explicitly fixed-size; the broken `add(int, E)` override that
  ignored its index was removed.
- Removed unused `Random` fields from `EntityAetherAnimal`, `ItemAetherTool` and
  `AetherDungeon`.
- Removed unreachable duplicate `isOnGround()` checks from `EntityFlyingCow` and
  `EntityPhyg`; the grounded jump-count reset already happens in `onUpdate`.

Added unit coverage for skyroot bucket client/server consumption and the
fixed-size `FilledList` contract. The baseline is now **125/125 in-game checks**
and **45 unit tests**.

Remaining medium/low items intentionally deferred:

- `AetherGenFloatingIsland` tautological `z != 0 || z != 1` conditions, because
  changing them alters worldgen output and needs an intended-shape decision.
- `GuiValkyrieDialogue`/`PacketInitiateValkyrieFight` accepting medals across
  multiple inventory stacks, because that changes the entry-fee protocol and
  needs a gameplay decision.
- B29 remains deferred as previously noted.

### Refactor batch 1 status

Completed refactors:

- **Container block-break logic (item 2)** — `BlockEnchanter`, `BlockFreezer`
  and `BlockIncubator` now share `BlockAetherContainer.dropContainerItems`
  instead of carrying three copies of the item-spawning loop.
- **Typed ability access (item 4)** — `PlayerAether` keeps a typed
  `AbilityRepulsion` reference and exposes `getRepulsionAbility()`;
  `PlayerAetherEvents` no longer reads `getAbilities().get(3)`. Break-speed
  handling uses the `IAccessoryInventory` interface instead of casting to
  `InventoryAccessories`.
- **Recipe registration names (item 6)** — removed the unused `name` parameter
  and all 82+7 passing name strings from `AetherRegistries` recipe helpers.
- **Sun Altar localization (part of item 8)** — the title and time-selector
  strings now use `I18n` and `en_US` keys.
- **Optional Battlegear integration (item 9)** — the direct Battlegear class
  reference moved out of `AetherItemRenderer` into
  `compatibility/client/BattlegearRenderCompat`, loaded only when the
  compatibility hook is actually invoked.

Added unit coverage for container item dropping and Sun Altar localization
keys. The baseline is now **125/125 in-game checks** and **48 unit tests**.

Still open refactor items: unify the two dungeon base classes, share tile-entity
progress/output logic, centralize item-drop policies, introduce a typed registry
abstraction, finish GUI localization, and add the remaining pure-logic tests.

### Refactor batch 2 status

Completed refactors:

- **Shared tile-entity processor base (item 3)** — added
  `TileEntityAetherProcessor<T>`. `TileEntityEnchanter` and
  `TileEntityFreezer` now only provide recipe/fuel/block/hook implementations;
  progress, power, output merging, container-item handling, NBT and sided
  inventory access live in the shared base.
- **More GUI localization (item 8)** — the active Lore GUI strings (`Prev.`,
  `Next`, `Book`, `of Lore:`, `Item :`) now use `I18n` keys, in addition to the
  Sun Altar strings localized in the previous batch.

Added unit coverage for the shared processor loop. The baseline is now
**125/125 in-game checks** and **51 unit tests**.

Remaining refactor items: unify the two dungeon base classes, centralize item
drop policies, introduce a typed registry abstraction, finish the remaining GUI
localization (`AetherMainMenu`, dead `GuiAetherPerks`), and add the remaining
pure-logic tests.

### Refactor batch 3 status

Completed refactors:

- **Typed registry abstraction (item 7)** — added `AetherRegistry<T extends
  RegistryEntry>`. `AetherAPI` now uses one typed registry for each content
  type instead of six duplicated `HashMap`/`LinkedHashMap` blocks. Item-backed
  keys are built in one place, and the Moa registry remains insertion-ordered
  for stable persisted IDs.
- **Registry coverage (item 10, partial)** — added tests for damageable-stack
  key normalization and ordered registry iteration.

The baseline is now **125/125 in-game checks** and **53 unit tests**.

Remaining refactor items: unify the two dungeon base classes, centralize item
drop policies, finish optional GUI localization (`AetherMainMenu`, dead
`GuiAetherPerks`), and add the remaining pure-logic tests.

### Refactor batch 4 status

Completed refactors:

- **Perk GUI localization (item 8)** — all `GuiAetherPerks` labels/buttons now
  use `I18n` keys, completing the originally listed Sun Altar, Lore and perk
  GUI strings.
- **Pure-logic coverage (item 10, partial)** — extracted reusable zanite
  damage-speed and accessory strength calculations and added tests for both.

The baseline is now **125/125 in-game checks** and **57 unit tests**.

Remaining refactor items: unify the two dungeon base classes, centralize item
drop policies, optional `AetherMainMenu` localization, and the remaining
pure-logic tests.

### Refactor batch 5 status

Completed refactors:

- **Centralized drop primitives (item 5)** — added `AetherDropHelper` for mining
  stats, fortune/silk-touch checks, stacked-block creation and item spawning.
  `BlockAetherLog`, `BlockAetherOre` and `DoubleDropHelper` now use it instead
  of repeating the harvest/enchantment/item-spawn logic.
- **Main-menu localization (item 8 finish)** — the main-menu `Mods` button and
  information text are localized; this closes the remaining active GUI strings
  listed for this refactor item.
- **More pure-logic tests (item 10)** — extracted the forward time delta used by
  `PacketSetTime` and added boundary tests.

The baseline is now **125/125 in-game checks** and **59 unit tests**.

Only one refactor item remains: unifying the `AetherDungeon` and
`AetherStructure` base classes. That is a larger structural rewrite because one
is a `WorldGenerator` using absolute coordinates and the other is a
`StructureComponent` using bounding-box-relative coordinates and metadata.

### Reflection batch 1 status

Removed:

- **Dungeon keys** — `EntityJoinWorldEvent` now swaps a normal `EntityItem` for
  `EntityAetherItem` instead of setting `Entity.invulnerable` reflectively.
- **Player jump state** — the client reads `EntityPlayerSP.movementInput.jump`
  and sends `PacketJumping` on change; the server updates
  `PlayerAether.isJumping`. The per-tick reflective `isJumping` lookup is gone.
- **GUI positions** — accessory-button placement computes the known inventory /
  creative-screen dimensions instead of reading `guiLeft`, `guiTop`, `xSize` and
  `ySize` reflectively.
- **Entity construction** — `EntitiesAether.createEntityByID` uses an explicit
  ID switch instead of `Class.getConstructor(...).newInstance(...)`.

All previously audited reflection sites are now resolved:

- The `AetherMainMenu` Desktop link uses the direct `java.awt.Desktop` API.
- Creative-tab button access uses `GuiContainerCreativeMixin`.
- `EntityRenderer.itemRenderer` assignment uses `EntityRendererMixin`.

The baseline is now **129/129 in-game checks** and **60 unit tests**.

### Performance batch 1 status

Completed:

- **P01** — resolved by the jump-state packet; the per-tick reflective
  `isJumping` lookup is gone.
- **P02** — `AetherData` keeps eternal-day, cycle-catchup and Aether time in
  memory; NBT is read on load and written on save/change.
- **P03 (partial)** — Whirlwind’s per-entity block/leaf checks now run once per
  whirlwind tick, and its particle cleanup iterates backwards. `EntityHook` uses
  a no-allocation `wearingAccessory(Item)` overload.
- **P04** — the life-shard packet is now sent only to the owning player, including
  the max-shard branch.
- **P07** — resolved by the typed `AetherRegistry` work.
- **P08** — trivia files are parsed once per language and cached.

Still open:

- **P03** — `EntityFloatingBlock` still scans nearby entities every tick;
  `EntityHook` still performs three separate target checks.
- **P05** — worldgen issues, including the deferred B29 work.
- **P06** — first-person renderer duplication, mouse-event state, and repeated
  config lookups.

The baseline is now **129/129 in-game checks** and **64 unit tests**.

### Dead-code cleanup status

Removed as genuinely unreferenced:

- Unused AI: `AechorPlantAIShootPlayer`, `EntityAIUpdateState`,
  `ZephyrAIShootTarget`, `ZephyrAITravelCourse`.
- Never-spawned particles: `EntityBlueFX`, `EntityCloudSmokeFX`,
  `EntityGoldenFX`, `EntityPurpleFX`.
- `EnumGummySwetType`.
- Unused GUI buttons: `GuiAetherButton`, `GuiDescButton`.
- Unused worldgen: `AetherGenCave`, `AetherGenLakes`, `AetherGenLiquids`
  (including the two unused `AetherBiomeDecorator` fields).
- Empty/unregistered `PacketCheckKey` and its network registration.
- Unused `AetherOverlay.getCureAlpha`.

Deliberately retained:

- Generic dialogue scaffolding was removed in R10. The shared `GuiDialogue` /
  `DialogueOption` classes remain because the Valkyrie Queen uses them, but the
  unused server-dialogue packets, event, and `GuiServerDialogue` were deleted.
- `GuiAetherPerks` and `GuiButtonPerks` were removed; the working perk system
  (`PacketPerkChanged`, `GuiCustomizationScreen`, `AetherRankings`, and
  `DonatorMoaSkin`) remains.
- Golden-island vegetation and lake scaffolding — resolved by R01. Golden oaks
  are generated directly, default trees are suppressed on meta-3 surface grass,
  and the unused `AetherGenDungeonOakTree` plus commented lake code were removed.
- Public config getters and NBT-backed legacy fields — left for addon/API and
  save compatibility rather than treated as dead code.

The baseline remains **129/129 in-game checks** and **64 unit tests**.

### Follow-up batch status

- **Valkyrie medal fee** — the client now sums victory medals across the whole
  inventory, and the server consumes ten total medals across stacks. The packet
  no longer requires all ten to be in one slot.
- **P03** — `EntityFloatingBlock` now performs its entity-push scan on the
  authoritative server only. `EntityHook` still has three distinct target
  fields to clear; those are semantically different, so they were not merged.
- **P06** — extended-reach attacks moved from the tick-loop `Mouse` event query
  to Forge `MouseEvent`, so they no longer read stale event state. The
  first-person renderer duplication and repeated config lookups remain.

Added unit coverage for cross-stack medal counting and consumption. The
baseline is now **129/129 in-game checks** and **66 unit tests**.

### Final safe cleanup status

- Removed unused `EntityMoa.secsUntilFlying` and `secsUntilWalking`.
- Removed the unreachable `AetherMainMenu` demo-world fields, action handlers,
  confirmation/deletion branch, and related imports.
- Consolidated the menu-toggle config property lookup so `mouseReleased` reads
  and writes one `Property` instance instead of querying the config repeatedly.

The baseline remains **129/129 in-game checks** and **66 unit tests**.

---

## 11. Remaining Work Decision Register

This section tracks every item intentionally left open after the refactor,
reflection, performance, and dead-code batches. The batch-status sections above
are historical; this is the current remaining-work register.

Status key:

- **Safe** - can be changed without a gameplay/worldgen/API decision.
- **Decision** - needs a product, worldgen, or compatibility decision.
- **Deferred** - deliberately postponed until a later batch.
- **Won't fix** - not worth the behavior/performance tradeoff.
- **Done** - completed and verified.
- **Partial** - part of the item is implemented; the rest still needs a decision.

| ID | Item | Status | Recommendation | Main decision |
|---|---|---|---|---|
| R01 | B29 dungeon oak / golden-island vegetation | Done | Golden oaks restored; default trees suppressed; dead lake code removed | None |
| R02 | Floating-island tautologies | Done | Early exit added; canopy shape unchanged | None |
| R03 | Creative `buttonList` reflection | Done | Early client mixin replaces reflection and polling | None |
| R04 | `EntityRenderer.itemRenderer` reflection | Done | Early client mixin mutates the final field only for AetherEntityRenderer | None |
| R05 | Desktop reflection | Done | Completed with direct `java.awt.Desktop` | None |
| R06 | `EntityHook` target checks | Won't fix | Keep all three distinct checks | None |
| R07 | P05 worldgen leftovers | Partial | Generator caching completed; cloud changes deferred | Should clouds cross chunks? |
| R08a | First-person renderer duplication | Won't fix | Keep custom renderer for glove/Battlegear parity | None |
| R08b | Repeated client config lookups | Done | Use cached getters; menu toggle updates cache | None |
| R09 | Unify dungeon base classes | Won't fix | Keep both documented classes; revisit only for new dungeon types | None |
| R10 | Dialogue/perk scaffolding | Done | Removed unused generic dialogue and unreachable perk UI | None |
| R11 | Feature gaps | Decision | Start with OreDictionary + localization, then config validation | Which features are in scope? |

### R01 - B29 and golden-island vegetation

**Status:** completed.

`ComponentGoldenIsland` and `ComponentGoldenIslandStub` now generate golden oaks
directly with `AetherGenUtils.generateGoldenOakTree(...)`.

The normal three-tree decoration pass and the holiday-tree pass skip columns
whose surface is meta-3 golden-island grass. Normal trees still generate on
meta-0 terrain in the same chunk. The 25 per-chunk surface dungeon-oak attempts
were removed, and the now-unused `AetherGenDungeonOakTree` class was deleted.

The commented golden-island lake branches were also removed. Lakes are
explicitly out of scope for golden dungeon islands, so no lake generator is
retained for them.

**Decision needed:** none.

### R02 - `AetherGenFloatingIsland` tautologies and scan cost

**Status:** completed as a behavior-preserving optimization.

`AetherGenFloatingIsland.generate` now checks the height limit before scanning
and returns immediately on the first non-air block. The 18x23x18 scan and the
existing full-row/column canopy shape are unchanged.

The generator's tautological conditions remain because they define the current
visual shape. Changing them is a separate worldgen/design decision rather than a
safe cleanup.

Added unit tests cover the early exit and the height-limit short circuit.

**Decision needed:** none for the optimization. If the canopy shape is ever
changed, that should be a separate visual comparison.

### R03 - Creative-inventory `buttonList` reflection

**Status:** completed with an early client mixin.

`GuiContainerCreativeMixin` injects at the tail of `setCurrentCreativeTab` and
adds or removes the accessory button directly. This covers the initial tab set
during `initGui`, mouse tab changes, and keyboard/search tab changes without
reflection or render-time polling.

`AetherClientEvents` no longer tracks `previousSelectedTabIndex` or handles the
creative button in `DrawScreenEvent.Post`. The non-creative inventory and
accessories-screen button paths remain event-based.

**Decision needed:** none.

### R04 - `EntityRenderer.itemRenderer` reflection

**Status:** completed with an early client mixin.

`EntityRendererMixin` injects at the tail of the `EntityRenderer` constructor.
When the instance is an `AetherEntityRenderer`, it replaces the final
`itemRenderer` field with `AetherItemRenderer`. Vanilla and other mods'
`EntityRenderer` instances keep their original renderer.

The reflection call and `ObfuscationReflectionHelper` import were removed from
`AetherEntityRenderer`.

**Decision needed:** none. Event-based first-person rendering remains a possible
future refactor, but it is no longer needed to remove reflection.

### R05 - `AetherMainMenu` Desktop reflection - completed

**Previous state:** link opening used `Class.forName("java.awt.Desktop")` and
reflective method invocation.

**Implemented:** direct `java.awt.Desktop` usage with
`Desktop.isDesktopSupported()` and `Desktop.Action.BROWSE` checks. Unsupported
platforms are logged instead of throwing, and link-opening failures are still
reported through the existing logger.

**Result:** removed all reflection from the link-opening path. This was
client-only code with no gameplay effect.

**Decision needed:** none.

### R06 - P03 `EntityHook` target checks

**Current state:** `EntityHook` checks `getAttackTarget()`, `getEntityToAttack()`,
and `getAITarget()` separately. These are distinct fields.

**Options:**

1. Keep all three checks; the per-check `ItemStack` allocation has already been
   removed with `wearingAccessory(Item)`.
2. Merge to one target check; faster but changes which AI targets are cleared.
3. Add a helper that preserves all three fields while reducing repeated player
   lookups.

**Recommendation:** keep all three. The remaining cost is small and merging them
would change AI behavior.

**Decision needed:** none; mark this as won't-fix unless profiling shows a real
problem.

### R07 - P05 remaining worldgen work

**Status:** partial. Generator caching is complete; cloud behavior is deferred.

- `AetherGenFloatingIsland` now exits on the first obstruction and skips
  out-of-height placements. Successful placements still require the full
  clearance scan.
- `AetherBiomeDecorator` now reuses cached golden-oak, skyroot, and water-lake
  generators. RNG order and generated output are unchanged.
- `AetherGenClouds` can still drift outside the current chunk. It uses
  `setBlockAndNotifyAdequately`, which already uses the non-notifying flag for
  this generator, so a direct setter would be only a negligible micro-optimization.

**Remaining options:**

1. Clip clouds to chunk bounds. This changes where clouds appear at chunk edges.
2. Leave cloud placement unchanged and treat cross-chunk drift as intended.
3. If cloud code is touched later, use a direct `setBlock(..., 2)` call only to
   preserve the existing update behavior while avoiding the helper call.

**Recommendation:** leave cloud placement unchanged for now. Revisit only if
cross-chunk cloud writes cause a concrete lighting, population, or compatibility
problem.

**Decision needed:** should clouds be allowed to cross chunk boundaries?

### R08 - P06 first-person renderer and config lookups

#### R08a - First-person renderer duplication

**Status:** won't fix.

`AetherItemRenderer` copies much of vanilla's first-person renderer, but the copy
exists for real behavior: first-person glove overlays and Battlegear offhand
compatibility. Replacing it with event-based rendering would require reproducing
vanilla map rendering, item-use animations, swing transforms, and compatibility
behavior. The 1.7.10 renderer is stable, so the duplication is not causing drift
or a correctness problem.

Keep the custom renderer unless a rendering bug forces a rewrite.

#### R08b - Repeated client config lookups

**Status:** completed.

Added `AetherConfig.setMenuEnabled(...)`, which updates both the cached value and
the Forge config property. The menu toggle now uses it, so cached values cannot
become stale.

`AetherClientEvents`, `AetherMusicHandler`, and `GuiMenuToggleButton` now use the
cached `menuEnabled()`, `menuButtonEnabled()`, and `musicEnabled()` getters
instead of repeatedly resolving config properties.

**Decision needed:** none.

### R09 - Unify `AetherDungeon` and `AetherStructure`

**Status:** won't fix for now; the boundary is documented.

`AetherDungeon` is an absolute-coordinate `WorldGenerator` used by
`BronzeDungeon` during chunk population. `AetherStructure` is a bounded
`StructureComponent` with relative offsets, metadata, and NBT persistence.

The classes share geometry loops, but they use different coordinate models and
block-update APIs. Extracting a common helper would require an adapter that
preserves both behaviors and their random-call order, creating a large worldgen
regression surface for no current gameplay benefit.

Both classes now document why they are intentionally separate. Revisit this only
if another absolute-coordinate dungeon needs the same geometry, or if a shared
geometry bug has to be fixed in both places.

**Decision needed:** none.

### R10 - Dialogue and perk scaffolding

**Status:** completed by removal.

The unused generic server-dialogue path was removed: `PacketDisplayDialogue`,
`PacketDialogueClicked`, `GuiServerDialogue`, `DialogueClickedEvent`, their
network registrations, and their round-trip test.

The shared `GuiDialogue`, `DialogueOption`, and `GuiValkyrieDialogue` classes
remain because the Valkyrie Queen uses them directly. Her fight flow still uses
`PacketInitiateValkyrieFight`.

The unreachable `GuiAetherPerks` and `GuiButtonPerks` were removed, along with
the dead id-24 handler and `gui.perks.*` localization keys. The working perk
system remains: `PacketPerkChanged`, `GuiCustomizationScreen`, `AetherRankings`,
`DonatorMoaSkin`, and `EnumAetherPerkType`.

**Decision needed:** none.

### R11 - Feature gaps and recommended order

R11 mixes small compatibility work with large gameplay refactors. The current
scope is:

| Item | Current state | Effort | Risk |
|---|---|---:|---:|
| OreDictionary coverage | Only `stickWood` is registered | Low | Low |
| Optional NEI handlers | No NEI integration beyond the runtime dependency | Medium | Low-medium |
| Config validation | IDs and spawn rates are loaded without validation | Low-medium | Medium |
| Data-driven loot tables | Dungeon loot is hardcoded in switch statements | High | Medium-high |
| Boss-arena state cleanup | Door/unlock/achievement logic is spread across boss entities | High | High |
| Localization beyond `en_US` | Every non-English file is missing the same 9 keys | Low | Low |
| More tests / CI | Unit tests, game tests, and CI already exist | Ongoing | Low |

#### OreDictionary coverage

Currently only `stickWood` is registered for `ItemsAether.skyroot_stick`.

Add compatibility tags for:

- `logWood` - skyroot and golden oak logs, using metadata wildcards.
- `plankWood` - skyroot planks.
- `stickWood` - already present.
- `treeSapling` - skyroot and golden oak saplings.
- `treeLeaves` - Aether leaves.
- Possibly `oreAmbrosium`, `oreZanite`, `oreGravitite`.
- Possibly `gemAmbrosium`, `gemZanite`, `gemGravitite`.

This is the smallest, highest-value compatibility task. Add a game-test check
that each expected tag contains the expected item.

#### Optional NEI handlers

There are no NEI recipe handlers today. The useful targets are:

- Enchanter recipes from the `AetherEnchantment` registry.
- Freezer recipes from the `AetherFreezable` registry.
- Incubator recipes only if there is a meaningful recipe representation.

This needs a compile-time NEI API dependency, a compatibility plugin loaded only
when NEI is present, and guarded imports so NEI remains optional.

#### Config validation

Current risks:

- Spawn rates can be set to `0`, causing `nextInt(0)` crashes.
- Biome IDs can be outside the valid range.
- Potion IDs can conflict or be out of range.
- Dimension IDs can conflict with reserved or other mod dimensions.

Recommended validation:

- Clamp spawn rates to at least `1`.
- Validate biome IDs against `0..255`.
- Validate potion IDs and avoid occupied IDs where possible.
- Warn on dimension conflicts; only fall back silently for values that are
  definitely invalid.
- Log clear warnings instead of crashing during registration.

Dimension IDs need care because changing them can affect existing worlds. Warn
rather than silently migrate.

#### Data-driven dungeon loot tables

Current dungeon loot is hardcoded in:

- `BronzeDungeon.getBronzeLoot`
- `ComponentSilverDungeon.getSilverLoot`
- `ComponentGoldenDungeon.getGoldLoot`

There are also fall-through bugs in the silver/gold switches.

A data-driven system would need:

- A JSON or NBT schema for entries, weights, stack sizes, and conditions.
- A loader that runs after items are registered.
- A fallback to the built-in tables if data is missing or invalid.
- Tests for weighted selection, empty tables, and invalid entries.

Fix the hardcoded loot behavior first by extracting it into a small code-level
table structure, then add JSON loading as a separate feature.

#### Boss-arena state cleanup

The Slider, Valkyrie Queen, and Sun Spirit each manage parts of their arena
directly:

- Door/trapdoor changes.
- Locked-block unlocking.
- Achievement/message triggering.
- Focused-boss state.
- Death and despawn cleanup.

The long-term fix is an explicit per-arena state machine, for example
`DORMANT -> ACTIVE -> DEFEATED -> RESET`, with idempotent transitions persisted
in NBT. This is the riskiest gameplay refactor and needs extensive in-game tests.

#### Localization beyond `en_US`

Every non-English language file is missing the same nine keys:

- `gui.aether_menu.info`
- `gui.aether_menu.mods`
- `gui.lore.book`
- `gui.lore.item`
- `gui.lore.next`
- `gui.lore.of_lore`
- `gui.lore.previous`
- `gui.sun_altar.select_time`
- `gui.sun_altar.title`

Minecraft normally falls back to English, so this is not a crash. Add a
key-parity test and fill the missing entries. If real translations are
unavailable, use English fallback text and mark them for later translation.

#### Tests and CI

The project already has unit tests, a dedicated game-test server, and build/test,
game-test, and release workflows. The remaining work is feature-specific coverage:

- OreDictionary registration checks.
- Config validation tests.
- Loot table selection tests.
- More boss-arena game tests.

#### Recommended order

1. OreDictionary integration - small, useful, low risk.
2. Localization key parity - mechanical and quick.
3. Config validation - prevents crashes and is testable.
4. Optional NEI handlers - compatibility work with more setup.
5. Loot table extraction/refactor - fix behavior first, then data-drive it.
6. Boss-arena state machines - last, because it is the riskiest gameplay refactor.

Start with OreDictionary plus the missing localization keys as one small batch,
then move to config validation.

**Decision needed:** which of these are in scope for this project cycle?

### R12 - Optional dependency follow-up

The dependency versions are current and the optional Battlegear reference has
been moved into a compatibility class. Remaining minor notes:

- `dependencies.gradle` comments still mention older Gradle versions.
- GTNHLib is pulled transitively by both Battlegear and NEI; consider a version
  constraint if reproducible resolution becomes important.

**Recommendation:** documentation-only cleanup unless dependency resolution
problems appear.

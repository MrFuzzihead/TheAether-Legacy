# Aether Legacy — In-Game Test Checklist

Test sheet for the audit work completed in phases 1–5 (plus the sleep/time
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

- [ ] Shift-click a stack of ambrosium torches into the incubator; then a moa egg
  - Items move cleanly; **source slot shows correct remainder** (no ghost 0-size stack); incubator starts
- [ ] Enchant two of the same stackable item consecutively in the enchanter
  - Output stacks correctly; existing output's enchantments/NBT not wiped
- [ ] Fill enchanter output to near-max, then finish one more craft
  - No item loss; craft stops rather than overflowing (gate)
- [ ] Do the same for the freezer
  - Same expectations
- [ ] Shift-click an accessory from inventory to an empty accessory slot
  - Moves in; accessory slot respects limit of 1
- [ ] Drag/drop items across all GUIs, including onto empty slots
  - No crashes (decrStackSize NPE guard)

---

## Section 5 — Player data (phase 5)

- [ ] Use 1 life shard; check max health; use a 2nd shard; check again
  - Exact `20 + 2×shards` (22, then 24) — **not inflated**
- [ ] Use 2 shards, exit and rejoin the world
  - Max health still 24 (modifier restored cleanly, no double-add)
- [ ] Die and respawn (with shards used)
  - Max health still 24 (modifier reapplied on respawn)
- [ ] Toggle halo off, relog
  - Halo still off (persisted to NBT)
- [ ] New world/new player: die in the overworld
  - Respawns in the **overworld** (no phantom Aether bed at 0,0,0)
- [ ] Sleep in a skyroot bed in the Aether, then die
  - Respawns at the Aether bed
- [ ] **[MP]** Player A uses a shard / changes accessories
  - Player B sees correct health bar and accessories

---

## Quick exploit re-tests (optional, needs a modified client)

- [ ] Send `PacketOpenContainer` with a non-accessories GUI id
  - Silently ignored
- [ ] Send `PacketPerkChanged` / `PacketCapeChanged` / `PacketSendSneaking` targeting another player's entity id
  - Ignored
- [ ] Send `PacketSetTime` without having defeated the Sun Spirit / from outside the Aether
  - Ignored

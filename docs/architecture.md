# MaidMind Architecture

## Target

- Minecraft 1.20.1
- Forge 47.3.0
- Java 17
- Touhou Little Maid 1.20.1 (mod id: `touhou_little_maid`)

## AI structure

MaidMind uses a shared strategic/tactical neural policy with per-maid memory.

- Strategic NN selects goals, task assignments, and priorities.
- Tactical NN selects concrete actions for an assigned goal.
- Per-maid memory stores individual experience and local context.
- World-scoped storage holds shared model parameters and training experience.

## Runtime boundaries

- Minecraft world/entity access is performed on the logical server thread.
- AI training runs on a background executor using immutable snapshots.
- The action controller validates every proposed action before applying it to the world.
- Model updates are applied at safe synchronization points.
- Existing Touhou Little Maid behavior remains active until an explicit, tested handoff is implemented.

## Touhou Little Maid integration

MaidMind is an addon mod and uses the official extension mechanism:

- `@LittleMaidExtension` / `ILittleMaid` registers the integration entry point.
- `EntityJoinLevelEvent` detects maid entities on the logical server.
- `EntityLeaveLevelEvent` removes observations when a maid leaves the loaded world.
- `MaidObservationManager` tracks loaded maids by UUID and refreshes their observations every 20 server ticks (about one second).
- `MaidSnapshot` copies precise coordinates, health, task id, and inventory slot values into an immutable value-only snapshot.
- `MaidObservationManager.INSTANCE.getSnapshots()` exposes a read-only copy of the latest observations for future AI consumers.
- The manager compares each refreshed snapshot with the previous one and logs changes to block coordinates, health, task id, and inventory slots. Position logs are emitted when the maid crosses a block boundary to keep movement logging readable.

The manager retains live entity references only while those entities are loaded, and all access occurs on the logical server thread. Consumers receive snapshots rather than entity references. The current registry is in-memory; world-scoped persistence and neural-network integration are later milestones.

## Terrain observation

- `TerrainObservation` captures a square horizontal grid centered on a maid (default radius: 8 blocks).
- Each cell records ground height relative to the maid, ground/feet/head block identifiers, clearance, water, and lava.
- A cell is marked standable when it has a collision-bearing ground block, two clear body blocks, and no lava in the body space.
- The grid is immutable and contains only primitive values and registry identifiers; it can be handed to background AI code.
- Terrain capture is on-demand through `MaidObservationManager.captureTerrain(UUID)`, on the logical server thread. It is not scanned every server tick.
- This is a local terrain representation, not a complete pathfinder. Navigation must still validate reachability and calculate a safe route between candidate cells.

### Terrain debug rendering

- Press `K` to toggle the client-side terrain grid overlay.
- The overlay refreshes every 10 client ticks and displays grids for loaded maids within 32 blocks of the player.
- Green: standable ground at roughly the maid's ground height.
- Purple: standable ground more than one block higher.
- Cyan: standable ground more than one block lower.
- Blue: water; red: lava; orange: blocked body space.
- The maid's own cell is white. Cells without detected ground are omitted.
- Debug rendering and observation capture run only on the client when the overlay is enabled; server-side observation remains independent.

## MVP

Three maids cooperate to maintain a farm after the player sets a food-production goal.

1. Detect farmable terrain and available resources.
2. Assign complementary roles.
3. Execute validated farming actions.
4. Record outcomes and per-maid memory.
5. Update shared model parameters.
6. Persist learned state per world.

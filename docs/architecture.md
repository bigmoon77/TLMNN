# MaidMind Architecture

## Target

- Minecraft 1.20.1
- Forge 47.3.0
- Java 17
- Touhou Little Maid 1.20.1 (mod id: `touhou_little_maid`)

## AI structure

The first combat experiment uses one neural network policy for all combat decisions.

- A single policy observes the maid, nearby entities, terrain, and combat visibility.
- The policy will select movement, attack, dodge, and retreat actions from one action space.
- Per-maid memory stores individual experience and local context.
- World-scoped storage holds shared model parameters and training experience.
- Hierarchical strategic/tactical networks are postponed until a single-policy baseline is evaluated.

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
- `MaidObservationManager` tracks loaded maids by UUID and refreshes their observations every 20 server ticks.
- `MaidSnapshot` copies coordinates, health, task id, and inventory values into an immutable snapshot.
- `MaidObservationManager.INSTANCE.getSnapshots()` exposes a read-only copy of the latest observations.

The manager retains live entity references only while those entities are loaded, and all access occurs on the logical server thread. The current registry is in-memory; world-scoped persistence and neural-network integration are later milestones.

## Terrain and combat perception

- `TerrainObservation` captures a square horizontal grid centered on a maid (default radius: 8 blocks).
- Each cell records ground height, block identifiers, water, lava, and body clearance.
- Passability is checked against the maid's full bounding-box width and height using block collision shapes. This detects walls and narrow openings that a feet/head point check can miss.
- `standable` is false when the candidate body volume intersects block collision shapes or contains lava.
- `LineOfSightObservation` performs a block-collision ray test between two points. The entity helper uses eye positions; the point-based `trace` method lets a weapon controller supply its projectile launch point and intended impact point.
- A blocked ray reports the blocking block identifier and hit position. This shared result can inform both melee and ranged decisions.
- Projectile arc, gravity, projectile size, and entity hits remain weapon/controller responsibilities. The point-to-point ray is the initial obstruction test; curved trajectories can be sampled as multiple segments later.
- Terrain capture is on-demand through `MaidObservationManager.captureTerrain(UUID)`, on the logical server thread. It is not scanned every server tick.
- Observations contain immutable value data and can be passed to the background AI worker. World/entity access remains on the game thread.

### Terrain debug rendering

- Press `K` to toggle the client-side terrain grid overlay.
- The overlay refreshes every 10 client ticks and displays grids for loaded maids within 32 blocks of the player.
- Green: standable ground at roughly the maid's ground height.
- Purple: standable ground more than one block higher.
- Cyan: standable ground more than one block lower.
- Blue: water; red: lava; dark red: body volume blocked by a wall or other collision shape; light orange: other non-standable ground.
- The maid's own cell is white. Cells without detected ground are omitted.

## MVP

The first learning baseline is combat with a single policy:

1. Observe nearby allies, enemies, terrain passability, and line-of-sight.
2. Select movement, attack, dodge, or retreat actions.
3. Validate and execute the selected action through the action controller.
4. Record damage dealt/received, defeats, survival, and time to defeat.
5. Update shared model parameters and per-maid memory.
6. Persist learned state per world.

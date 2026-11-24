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
- `MaidSnapshot` copies position, health, selected task id, and inventory slot values into a value-only snapshot.
- The snapshot does not retain live entity or inventory-handler references.

The current integration milestone is observation only. Brain behavior registration, task ownership, and action execution will be added after the snapshot and scheduling boundaries are validated.

## MVP

Three maids cooperate to maintain a farm after the player sets a food-production goal.

1. Detect farmable terrain and available resources.
2. Assign complementary roles.
3. Execute validated farming actions.
4. Record outcomes and per-maid memory.
5. Update shared model parameters.
6. Persist learned state per world.

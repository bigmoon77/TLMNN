# Architecture

## Design goals

MaidMind is a standalone Forge add-on. Minecraft integration, decision making, learning, and persistence should remain separate so the AI implementation can evolve without coupling it to Minecraft classes.

## Hierarchical decision model

### Strategic layer

A shared model evaluates village-level context and player-assigned goals. It proposes priorities and task assignments.

### Tactical layer

A shared model selects actions for an individual maid using the current observation, assigned task, and that maid's private memory.

### Individual memory

Each maid retains personal experience and state. Memory is not merged into the shared model; selected experience may be sampled for group learning.

### Learning pipeline

1. Collect transitions from completed or interrupted actions.
2. Store experiences in a world-scoped replay buffer.
3. Train candidate model parameters away from the Minecraft server thread.
4. Evaluate candidates against a stable validation set.
5. Publish accepted model versions at a safe synchronization point.

## Threading rules

- World reads and mutations must happen on the appropriate Minecraft server thread.
- Neural inference should initially be bounded and predictable.
- Training must not block the server tick.
- Background workers must exchange immutable snapshots with the game thread.
- Model updates must be applied atomically at controlled synchronization points.

## Initial implementation milestones

1. Launch a minimal Forge mod in a development client.
2. Verify Touhou Little Maid presence and discover stable integration hooks.
3. Read maid observations without changing behavior.
4. Add a goal/task representation and safe action adapter.
5. Implement deterministic baseline behavior.
6. Add a fixed-topology neural policy and measurable training loop.
7. Explore dynamic topology evolution only after baseline learning is reproducible.

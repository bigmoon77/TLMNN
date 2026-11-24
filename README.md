# MaidMind

A Minecraft Forge 1.20.1 add-on for Touhou Little Maid, exploring cooperative NPC intelligence powered by a shared neural network and per-maid memory.

> Working title: MaidMind (repository: TLMNN)

## Project goals

- Let the player assign high-level goals and constraints.
- Coordinate multiple maids through a shared strategic model.
- Preserve individual experience and state in per-maid memory.
- Learn from outcomes and improve group behavior over time.
- Explore dynamic neural-network structure evolution after a reliable learning baseline exists.

## Initial technical direction

- Minecraft: 1.20.1
- Mod loader: Minecraft Forge 47.x
- Java: 17
- Build: Gradle / ForgeGradle
- Base mod: Touhou Little Maid (installed separately as a runtime dependency)
- AI: hierarchical decision system (strategic layer + tactical layer)
- Learning: designed behind replaceable interfaces; initial implementation will prioritize deterministic, testable behavior before online reinforcement learning.

## Development status

Early scaffold. The current milestone is to build and launch an empty MaidMind mod in a Forge development client, then verify Touhou Little Maid integration points before implementing AI behavior.

## Build

Use JDK 17.

```powershell
.gradlew.bat build
.gradlew.bat runClient
```

The Gradle wrapper will be added with the first local development setup if it is not already present.

## Dependencies and assets

Touhou Little Maid is a separate mod dependency. This repository does not bundle its code, models, textures, sounds, or other assets. Install the compatible Touhou Little Maid release separately when running the development client.

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for third-party project and asset notes.

## License

The license for MaidMind's original source code will be selected before the first public release. Third-party projects and assets remain under their respective licenses.

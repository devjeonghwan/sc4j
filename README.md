<p align="center">
  <img src="docs/branding/sc4j-logo.svg" alt="SC4J logo" width="160">
</p>

<h1 align="center">SC4J</h1>

<p align="center">A real-time strategy engine in Java, with a mod runtime, a launcher and peer-to-peer online play.</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/org.fogofwarlabs.sc4j/sc4j-engine"><img src="https://img.shields.io/maven-central/v/org.fogofwarlabs.sc4j/sc4j-engine" alt="Maven Central"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache--2.0-blue" alt="License: Apache-2.0"></a>
</p>

<p align="center"><b>English</b> | <a href="README.ko.md">한국어</a></p>

---

## What SC4J is

SC4J (Simulation Core for Java) is an engine for real-time strategy games. A game for it is a **mod**: one jar that the runtime loads and the launcher lists. The engine gives a mod a deterministic simulation, a client framework, replays and saved games, LAN play and Beacon.Link, the peer-to-peer network for rooms, chat and presence. Nobody runs a central server: every SC4J is a peer.

This repository holds the engine, the launcher, the runtime, Beacon.Link and the demo mod:

| Mod | Folder | Mod id | What it is |
|---|---|---|---|
| Demo | [`mods/demo/`](mods/demo/) | `demo` | "SC4J Demo: Dawnhold and Cindermaw", a small real-time strategy game made only of the engine |

The SC:R mod and its clone SC:R Crossover have repositories of their own: [sc4j-scr](https://github.com/fogofwarlabs/sc4j-scr) and [sc4j-scr-crossover](https://github.com/fogofwarlabs/sc4j-scr-crossover).

The [mod author guide](docs/mod-guide.md) shows how to make a new mod for the engine.

## Features

- **Simulation**: lockstep and deterministic, with a state hash per game loop. Default systems for path finding, movement, collision, visibility, economy, harvest, combat, projectiles, production, orders, triggers and computer players; a mod can replace any of them.
- **Content**: the standard data pack (JSON definitions, PNG pictures, WAV sounds) and one-file JSON maps.
- **Client**: scenes, input, an OpenGL renderer, square and isometric projections, a default in-game screen with HUD, audio and video.
- **Replays and saved games**: they load only with the same mod identity as the run that wrote them.
- **Network play**: LAN rooms and lockstep games; Beacon.Link rooms, chat channels, whispers, friends and presence over the Internet.
- **Launcher**: lists the mods of an install, asks for their settings, starts the runtime and runs the computer's Beacon.Link node; installs mods from GitHub releases and updates itself.
- **Mod runtime**: one mod per session in its own class loader; a mod identity from the engine version, the mod and the jar's content.
- **API listings**: the engine API is listed per module and checked by the build ([API policy](docs/api-policy.md)).

## Repository layout

| Path | Contents |
|---|---|
| `sc4j-base` | exceptions, logging, binary, file, JSON and text helpers |
| `sc4j-mod-manifest` | the mod manifest `sc4j-mod.json`, its settings and versions, the install folder layout |
| `sc4j-engine-core` | leaf models: fixed-point math, map grid, terrain, players, state hash, content types, mod identity |
| `sc4j-engine-sim` | the simulation: game, units, default systems, orders, commands, data pack, maps, replays, saves |
| `sc4j-engine-net` | the network layer: lockstep transport, LAN discovery, network lobby, the Beacon.Link interfaces |
| `sc4j-engine-client` | the client framework: host, scenes, input, renderer, projections, HUD, console, audio, video |
| `sc4j-engine` | the mod API and the client application that mods compile against |
| `sc4j-runtime` | runs one mod jar, in a window or headless |
| `sc4j-launcher` | the launcher (JavaFX) |
| `sc4j-beaconlink-protocol`, `sc4j-beaconlink-peer`, `sc4j-beaconlink-node`, `sc4j-beaconlink` | Beacon.Link: the wire protocol, the peer library, the headless node and the client that the runtime gives a mod |
| [`mods/`](mods/) | the demo mod `demo` |
| [`tools/`](tools/) | `api-check` (API listings) |
| [`docs/`](docs/) | the guides and references ([index](docs/README.md)); `docs/branding/`: logos, banners and the launcher art of the demo mod |
| [`licenses/`](licenses/) | the licence texts of the third-party libraries |

## Build and run

You need JDK 25. The Gradle wrapper downloads Gradle itself. Commands run in PowerShell from the repository root; on Linux use `./gradlew`.

| Command | What it does |
|---|---|
| `.\gradlew.bat build` | compiles the engine and the demo, runs their unit tests and checks |
| `.\gradlew.bat devInstall` | assembles the development install `build/dev-install/` (launcher, runtime, `mods/demo.jar`) |
| `.\gradlew.bat runDevLauncher` | opens the launcher of the development install |
| `.\gradlew.bat runDevInstall -Psc4j.mod=<id>` | runs the runtime of the development install with `mods/<id>.jar` (default `demo`) |
| `.\gradlew.bat distribution` | builds the user package `build/distributions/SC4J-<version>.zip` |
| `.\gradlew.bat :sc4j-beaconlink-node:installDist` | builds the headless Beacon.Link node into `sc4j-beaconlink-node/build/install/` |

A first run:

```powershell
.\gradlew.bat devInstall
.\gradlew.bat runDevLauncher
```

The launcher lists every jar in `build/dev-install/mods/`. Choose a mod and start it. A mod of another repository gets in with its own `installMod -Psc4j.install=<this repository>\build\dev-install`; `devInstall` keeps it.

The user package holds the launcher, the runtime, the demo mod, a Java runtime and the start files `SC4J.bat` and `SC4J`. It holds no user data: a fresh install makes `userdata/` at its first start.

The launcher checks the GitHub releases of SC4J at its start and on the Settings screen, and updates the install in place; `userdata/` and `mods/` stay as they are.

Install mod on the Mods screen installs a mod from the latest GitHub release of a repository (`owner/name`), typed or chosen from the mod catalog.

Tests and the code style are in [CONTRIBUTING.md](CONTRIBUTING.md).

## Documentation

The guides are in [`docs/`](docs/README.md), each in English and Korean.

| Guide | Topic |
|---|---|
| [Mod author guide](docs/mod-guide.md) | making a mod for the engine, from the first mod to network play |
| [Engine API policy](docs/api-policy.md) | what the engine API is, its versions and the internal elements |
| [Beacon.Link protocol](docs/beaconlink/protocol.md) | the wire protocol and the files of Beacon.Link |
| [Beacon.Link operator guide](docs/beaconlink/operator.md) | running a peer for other players: the headless node and the launcher's public mode |
| [Branding](docs/branding/README.md) | the logos, banners and the demo mod's art, and how to export them |
| [Contributing](CONTRIBUTING.md) | setup, tests, code style, commits and pull requests |

## Licence

SC4J is licensed under the [Apache License 2.0](LICENSE). [NOTICE](NOTICE) lists the third-party material of the repository and its distributions; the licence texts are in [`licenses/`](licenses/).

| Material | Licence | Details |
|---|---|---|
| Demo pictures: renders of KayKit models and Poly Haven textures | CC0 1.0 | [`mods/demo/THIRD-PARTY.md`](mods/demo/THIRD-PARTY.md) |
| Libraries of the distributions (LWJGL, JavaCPP, FFmpeg, OpenJFX and others) | their own licences | [NOTICE](NOTICE), [`licenses/`](licenses/) |

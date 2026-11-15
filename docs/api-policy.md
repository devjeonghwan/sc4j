# SC4J engine API policy

**English** | [한국어](api-policy.ko.md)

This file states what the engine API is, how its version changes, and what stays compatible across engine versions. It applies to the artifact mods compile against (`sc4j-engine`). The [mod author guide](mod-guide.md#16-api-and-versions) sums it up for mod authors.

**Contents**

1. [What the API is](#1-what-the-api-is)
2. [Internal elements](#2-internal-elements)
3. [Versions](#3-versions): [the engine API version](#31-the-engine-api-version), [what changes which number](#32-what-changes-which-number), [deprecation](#33-deprecation), [before 1.0 is declared](#34-before-10-is-declared)
4. [Determinism, replays and saves](#4-determinism-replays-and-saves)
5. [Wire and file formats](#5-wire-and-file-formats)
6. [Updating the listings](#6-updating-the-listings)

## 1. What the API is

The API is every `public` or `protected` class, interface, constructor, method and field (constant values included) of the main classes of these modules, except the internal elements of section 2:

| Module | Contents | Listing |
|---|---|---|
| `sc4j-base` | exceptions, logging, binary, file, JSON and text helpers | `sc4j-base/api/sc4j-base.api` |
| `sc4j-mod-manifest` | the mod manifest `sc4j-mod.json`, its validation, settings, versions, the install folder layout | `sc4j-mod-manifest/api/sc4j-mod-manifest.api` |
| `sc4j-engine-core` | JSON, fixed-point math, map grid, terrain, players, state hash, content types, mod identity, resource models | `sc4j-engine-core/api/sc4j-engine-core.api` |
| `sc4j-engine-sim` | the game, units, default systems, content, animation programs, orders, commands, computer players, triggers, data pack, maps, lockstep, replays, saves | `sc4j-engine-sim/api/sc4j-engine-sim.api` |
| `sc4j-engine-net` | datagram links, lockstep transport, LAN discovery, network lobby, the Beacon.Link and community interfaces | `sc4j-engine-net/api/sc4j-engine-net.api` |
| `sc4j-engine-client` | host, scenes, input, renderer abstraction, projections, text, HUD, command card, console, dialogs, cursors, skins, world view, audio, video, settings store | `sc4j-engine-client/api/sc4j-engine-client.api` |
| `sc4j-engine` | the mod API (`engine.mod`), the client application (`engine.client.app`) and the default parts of the community screens (`engine.client.community`) | `sc4j-engine/api/sc4j-engine.api` |

A mod depends on `sc4j-engine`; the other modules come with it. `sc4j-base` and `sc4j-mod-manifest` are listed because every mod compiles against them through the engine.

Each listing is the JDK's `javap -protected -constants` output of the module's compiled classes: one block per class, sorted by class name, with its members sorted. A class is listed when it and every class enclosing it are `public` or `protected`. Sorting keeps the listing independent of the order of declarations, so moving a method in a source file changes nothing. The listing also shows the bridge methods the compiler adds for generic overrides. It does not show annotations, so a change of a nullability annotation is a contract change that the review must catch (section 3).

The Javadoc of an API element is part of its contract. Every API element has Javadoc, and `javadoc` runs without warnings.

## 2. Internal elements

These elements are public for the engine's own modules and the runtime but are not API. The listings leave them out, their package-info says so, and mods must not use them:

| Element | Why |
|---|---|
| `org.fogofwarlabs.sc4j.engine.mod.loading` | the runtime's mod loader |
| `org.fogofwarlabs.sc4j.engine.client.render.gl` | the OpenGL default of the renderer; mods draw through the renderer abstraction (`engine.client.render`: `GraphicsDevice.getCurrent()` creates the `GpuCanvas` and the `ShaderProgram`s of a mod's own passes) |
| `org.fogofwarlabs.sc4j.engine.client.audio.openal` | the OpenAL default of the audio output; mods play sound through `engine.client.audio` (`AudioDevice.openDefault()`) |
| `engine.client.host.GlfwClientHost`, `GlfwHostWindow`, `GlfwInput`, `GlfwKeyCodes`, `GlfwCursorSurface` | the GLFW implementations of the runtime host; mods use the `engine.client.host` interfaces (`ClientHost.initialize()`, `HostWindow` with its window services, `WindowInput`) and value types, which are API |
| `engine.client.host.WindowsTextInputMethod`, `WindowsSystemPointerSpeed`, `WindowsHostTickClock` | the Windows implementations of the platform services; mods use `HostWindow.getTextInputMethod()`, `SystemPointerSpeed.getDefault()` and `HostTickClock.getDefault()`, whose portable defaults (`NoTextInputMethod`, `FixedSystemPointerSpeed`, `MonotonicHostTickClock`) are API |

A package covers its subpackages and a class its nested classes. The list in the root `build.gradle` (`internalApiElements`) is the one the build checks; a change of it changes this table and the package-info in the same commit.

`checkApiUse` scans the main sources of the mods of this repository (the root `build.gradle` lists them) for these elements in code (comments and literals do not count) and fails on every use. A mod built outside this repository compiles against the engine jars of an install, which hold the internal elements too: nothing checks it, so it keeps to the listings.

## 3. Versions

### 3.1 The engine API version

The engine API version is `EngineVersion.CURRENT` (`sc4j-mod-manifest`), `MAJOR.MINOR`. A mod states the version it is built against in the `engine` field of `sc4j-mod.json`. The launcher and the runtime run a mod only when the major versions are equal and the mod's minor version is not higher than the runtime's.

The distribution version (`version` in `gradle.properties`) is separate and says nothing about API compatibility.

### 3.2 What changes which number

| Change | Version |
|---|---|
| A listing line removed or changed incompatibly: a class, member or constructor removed or renamed; a parameter, return or field type changed; access narrowed (`public` to `protected`); `final`, `abstract` or `static` added or removed where a mod's code can tell; an abstract method added to an interface or class that mods implement or extend; a checked exception added; a constant value changed (compilers copy constants into mod classes) | major |
| A documented contract changed without a listing change: what a method returns or accepts, when it throws, its threading or lifecycle rule, a nullability annotation narrowed for input or widened for output | major |
| A simulation result changed for the same inputs (section 4) | at least minor |
| Additions only: a new class, member, constructor, default method, enum constant (where mods do not switch over the enum exhaustively), or a contract widened compatibly | minor |
| A deprecation (section 3.3) | minor |
| No listing change and no contract change (implementation, performance, internal elements) | none |

A changed listing line shows as one line removed and one added, so the review decides by the table, not by the diff shape.

### 3.3 Deprecation

An element that will go away gets `@Deprecated` and a Javadoc `@deprecated` tag that names its replacement, in a minor release. `forRemoval = true` marks the release that removes it. A deprecated element stays for at least one minor release and is removed only in a major release.

### 3.4 Before 1.0 is declared

`EngineVersion.CURRENT` is 0.1. While the engine API is at 0.x, nothing promises stability: the listings record every change and incompatible changes may happen without a version change; each one is visible as an `.api` diff in its commit. The compatibility rule of section 3.1 already applies to 0.x versions. From 1.0 on, sections 3.2 and 3.3 apply in full.

## 4. Determinism, replays and saves

Lockstep games and replays need the same simulation results from the same inputs on every machine. A change of simulation results is therefore incompatible for them even without an API change, and an engine release that changes them raises at least the minor version. The mod identity (`ModIdentity`) carries the engine API version, so peers of different engine versions refuse each other in a lobby and a room.

Engine replays (`.sc4r`, magic `SC4R`) and engine saves (magic `SC4S`) store the mod identity of the run that wrote them and load only with an equal identity: the same engine API version, mod id, mod version and content. No compatibility of replays or saves across engine versions is promised.

## 5. Wire and file formats

Each format has its own version number, independent of the API version. Before 1.0 every SC4J format and protocol number is 1 and a format may change in place; nothing reads or migrates an older layout. The two ends of a link with different numbers refuse each other, and a reader refuses a file of another number.

| Format | Version | Where |
|---|---|---|
| Lockstep transport over datagrams: turns, resends, departures and the drop agreement | 1 | `DatagramLockstepTransport.PROTOCOL_VERSION` (`sc4j-engine-net`) |
| LAN room discovery | 1 | `LanRoomDiscovery.PROTOCOL_VERSION` (`sc4j-engine-net`) |
| Network lobby | 1 | `NetworkLobby.PROTOCOL_VERSION` (`sc4j-engine-net`) |
| Beacon.Link protocol: the envelope, its messages and records, the channel signals and datagrams | 1 | `Envelope.PROTOCOL_VERSION` (`sc4j-beaconlink-protocol`, see the [protocol reference](beaconlink/protocol.md)) |
| Beacon.Link files: node list, learned peers, key file, account, friends and ignored players | 1 | `NodeListFile.FORMAT`, `PeerStore.FORMAT`, `IdentityKeyFileUtils.VERSION`, `AccountStore`, `PlayerListStore.FORMAT` ([protocol reference, "Files"](beaconlink/protocol.md#files)) |
| Beacon.Link account backup (`.sc4jaccount`) | 1 | `AccountBackup.VERSION` (`sc4j-beaconlink-peer`) |
| Engine replay | 1 | `EngineReplayFormat` (`sc4j-engine-sim`) |
| Engine save | 1 | `EngineSaveFormat` (`sc4j-engine-sim`) |
| JSON map | 1 | `JsonMapLoader.VERSION` (`sc4j-engine-sim`) |
| Standard pack manifest (`pack.json`) | 1 | `PackManifest` (`sc4j-engine-sim`, the `engine.pack` package description) |
| Standard pack content (`content.json`) | 1 | `PackContentReader` (`sc4j-engine-sim`, the `engine.pack` package description) |
| Mod identity wire form | with the lobby, discovery and replay versions | `ModIdentity` (`sc4j-engine-core`) |

## 6. Updating the listings

`check` (and so `build`) runs `apiCheck` on every module of section 1. It fails when the compiled classes differ from the module's `.api` file and prints the removed (`-`) and added (`+`) lines per class.

An intended API change:

1. Make the change, with its Javadoc.
2. Run `gradlew apiDump` (every module) or `gradlew :<module>:apiDump`.
3. Review the `.api` diff against section 3.2 and note the version effect in the commit message.
4. Commit the `.api` files with the change. A change made in a separate branch updates the files in its own commit; after a merge of two API changes, `apiDump` runs once more on the merged state.

An unintended `apiCheck` failure means the change touched the API by accident: undo that part instead of updating the listing.

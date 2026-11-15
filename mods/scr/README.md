# SC:R mod

**English** | [한국어](README.ko.md)

StarCraft: Remastered 1.23.10 reimplemented as an SC4J mod: mod id `scr`, jar `scr.jar`. It reads the game data of the player's own StarCraft: Remastered installation at run time; the repository and the jar hold none of it.

It runs on the [SC4J engine](../../README.md) with its own application, so that its frames follow the native game ([mod author guide, chapter 20](../../docs/mod-guide.md#20-two-ways-a-mod-runs)).

- [Layout](#layout)
- [Game data](#game-data)
- [Build and run](#build-and-run)
- [Tests](#tests)
- [Frame recorder](#frame-recorder)
- [Launcher art](#launcher-art)
- [Contributing](#contributing)
- [Licence](#licence)

## Layout

| Path | Contents |
|---|---|
| `scr-data` | the installation's data files and the content tables; the extension API for added units |
| `scr-game` | the game: units, orders, combat, computer players, triggers, replays and saved games |
| `scr-render` | the renderer of the game screen |
| `scr-net` | LAN and Beacon.Link play |
| `scr-client` | the client application: menus, the in-game screen, audio, replay playback |
| `scr-benchmark` | JMH benchmarks; not part of `scr.jar` |
| `sc-recorder` | the frame recorder of the native game ([below](#frame-recorder)); not part of `scr.jar` |
| `mod.properties` | the modules whose classes and resources make up `scr.jar`, and their compiler arguments |
| `docs/` | the SCRECBIN format and the launcher art |

## Game data

The Gradle property `sc4j.data` names the StarCraft: Remastered installation folder (the folder that holds `.build.info` and `x86_64/StarCraft.exe`). Set it in one of three ways:

| Where | How |
|---|---|
| Your Gradle user properties | the line `sc4j.data=<installation folder>` in `%USERPROFILE%\.gradle\gradle.properties` (`GRADLE_USER_HOME`) |
| The command line | `-Psc4j.data=<installation folder>` |
| An environment variable (CI) | `ORG_GRADLE_PROJECT_sc4j.data=<installation folder>` |

In a `.properties` file write the backslashes of a Windows path twice, or use forward slashes. With `sc4j.data` set, `devInstall` also writes it as the shared game folder setting of the development install when that install has none yet.

The launcher looks for the StarCraft: Remastered folder where Windows records the game; if it is not found there, set it once in the mod's settings.

Game data never goes into the repository: no file of the installation, and nothing extracted or converted from it (graphics, sounds, data files, maps). The SC:R mod reads it from the installation at run time, and so do the tests.

## Build and run

You need JDK 25; playing needs a StarCraft: Remastered 1.23.10 installation, the build does not. Commands run in PowerShell from the repository root; on Linux use `./gradlew`.

| Command | What it does |
|---|---|
| `.\gradlew.bat :mods:scr:devInstall` | assembles the development install `build/dev-install/` with `mods/scr.jar` |
| `.\gradlew.bat runDevLauncher` | opens the launcher of the development install |
| `.\gradlew.bat runDevInstall -Psc4j.mod=scr` | runs the runtime of the development install with `mods/scr.jar` |

## Tests

| Command | What runs | What it needs |
|---|---|---|
| `.\gradlew.bat build` | compile, unit tests (`test`) and the checks | nothing outside the repository |
| `.\gradlew.bat integrationTest` | tests tagged `integration` (`@RequiresInstallation`), including the GPU render smoke tests | `sc4j.data`; a GPU for the smoke tests |
| `.\gradlew.bat corpusTest` | tests tagged `corpus` (`@CorpusTest`): long runs and tests over external replay and map folders | `sc4j.data`; a test whose folder property is not set is skipped |

`integrationTest` fails before it starts when `sc4j.data` is not set or names no folder. The GPU render smoke tests run when the Gradle property `sc4j.render.smoke` is `true` (the default in `gradle.properties`); `-Psc4j.render.smoke=false` turns them off.

The external folders of the corpus tier are Gradle properties as well:

| Property | Folder |
|---|---|
| `sc4j.replays` | the root of a replay corpus |
| `sc4j.replays.limit` | the number of files the replay parse test reads |
| `sc4j.maps` | a maps folder with large EUD maps |
| `sc4j.corpus` | a replay folder of any layout that the corpus survey plays |

## Frame recorder

`sc-recorder` is a read-only frame recorder of the native game, used to compare the SC:R mod with it. It writes the observed state of a game, frame by frame, into SCRECBIN files: [SCRECBIN format](docs/sc-recorder-format.md).

## Launcher art

| File | What it is |
|---|---|
| `docs/branding/icon.svg`, `docs/branding/banner.svg` | launcher icon and banner of the SC:R mod |
| `docs/branding/banner-wide.svg` | wide banner of the SC:R mod, source (950 x 150) |
| `docs/branding/banner-wide-950x150.png`, `docs/branding/banner-wide-1900x300.png` | wide banner of the SC:R mod, 950 x 150 and 1900 x 300 pixels |

`scr-client/src/main/resources/meta/icon.png` and `banner.png` are exported from `icon.svg` and `banner.svg`. Sizes and the export are in the engine's [branding guide](../../docs/branding/README.md#exporting-png).

## Contributing

Setup, code style, commits and pull requests are in the engine's [contributing guide](../../CONTRIBUTING.md). If the change touches the SC:R mod and you have the installation, run `integrationTest` too.

## Licence

The SC:R mod is licensed under the Apache License 2.0.

StarCraft is a trademark of Blizzard Entertainment, Inc. SC4J is not affiliated with or endorsed by Blizzard Entertainment. The SC:R mod reads the game data of the player's own StarCraft: Remastered installation; the repository and its distributions carry no such data.

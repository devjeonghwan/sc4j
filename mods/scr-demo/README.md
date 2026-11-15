# SC:R Clone Demo

**English** | [한국어](README.ko.md)

Demo 2 of SC4J: a clone of the [SC:R mod](../scr/README.md) under its own identity, mod id `scr-demo`, jar `scr-demo.jar`. It has its own LAN join version, changed values of the Terran Marine and two added units with their own graphics: the Terran Unity-chan, trained at the Barracks, and the Zerg Myriarch, a dragon a Hive morphs into.

Like `scr.jar`, the jar holds no StarCraft: Remastered assets or data files: the game's graphics, sounds and data are read from the player's own installation at run time.

The [SC:R clone modder guide](docs/scr-demo.md) shows every step with this demo as the example.

- [Relation to the SC:R mod](#relation-to-the-scr-mod)
- [Layout](#layout)
- [Clone and overlay](#clone-and-overlay)
- [Build and run](#build-and-run)
- [Art licences](#art-licences)
- [Licence](#licence)

## Relation to the SC:R mod

The clone is not checked in: the build copies `src/main` of every module of the SC:R mod ([`../scr/`](../scr/README.md), the list `modules` in its `mod.properties`) and puts the overlay over it. Unchanged SC:R sources compile with the SC:R mod's compiler arguments, so their classes match `scr.jar`.

The clone is another mod: its own settings, storage and log folders. Online it never lists the rooms of the SC:R mod; on LAN its join version refuses SC:R mod players and theirs refuse it. Both mods share one StarCraft: Remastered folder setting.

## Layout

| Path | Contents |
|---|---|
| `overlay/` | the files that replace or add to the cloned sources, in the layout `overlay/<module>/src/main/...` |
| `overlay.lock`, `overlay-base/` | the SHA-256 and a copy of each SC:R mod file an overlay file was made from; written by `acceptOverlay` |
| `src/clone` | the clone tool; build time only |
| `src/convert` | the art converter; build time only |
| `src/mod` | the demo's own classes and resources: the content extension, the values, the added units |
| `src/test` | the tests; they read the built jars |
| `third-party/` | the art sets of the added units with their licence files |
| `docs/` | the SC:R clone modder guide |

## Clone and overlay

| Task | What it does |
|---|---|
| `cloneScrMod` | checks `overlay.lock`, copies the SC:R mod's sources into `build/clone` and copies `overlay/` over them |
| `overlayDiff` | shows the SC:R mod's changes under your overlay files |
| `acceptOverlay` | records the current SC:R mod files in `overlay.lock` and `overlay-base/` |
| `convertArt` | converts the baked frames of every art set into the units' graphics files |
| `jar` | builds `build/libs/scr-demo.jar` |

When the SC:R mod changes a file the overlay replaces, the build stops and names the file. Run `overlayDiff`, merge the change into the overlay file by hand, then run `acceptOverlay`.

Keep the overlay small: prefer replacing a small resource, then own classes behind the SC:R mod's extension points, and only then an SC:R mod source. Demo 2 replaces two resources (`sc4j-mod.json`, `lan-lobby.properties`) and adds one content extension.

## Build and run

You need JDK 25; playing needs a StarCraft: Remastered 1.23.10 installation, the build does not. Commands run in PowerShell from the repository root; on Linux use `./gradlew`.

| Command | What it does |
|---|---|
| `.\gradlew.bat -p mods\scr-demo jar` | builds `mods/scr-demo/build/libs/scr-demo.jar` |
| `.\gradlew.bat -p mods\scr-demo test` | the tests that need nothing outside the repository |
| `.\gradlew.bat -p mods\scr-demo integrationTest` | the tests that need the installation (Gradle property `sc4j.data`) |
| `.\gradlew.bat -p mods\scr-demo devInstall` | adds `mods/scr.jar` and `mods/scr-demo.jar` to the development install `build/dev-install/` |
| `.\gradlew.bat runDevLauncher` | opens the launcher of the development install |

For any other install, copy `scr-demo.jar` into its `mods/` folder.

## Art licences

| Material | Licence | Details |
|---|---|---|
| Unit art: Unity-chan | Unity-chan License 3.0 | [`third-party/unity-chan/`](third-party/unity-chan/) |
| Unit art: the rifle Unity-chan holds (Quaternius) | CC0 1.0 | [`third-party/unity-chan/`](third-party/unity-chan/) |
| Unit art: the Myriarch (DRAGO 3D model by OSCAR CREATIVO) | CC BY 4.0 | [`third-party/myriarch/`](third-party/myriarch/) |

The build packs each art set's `NOTICE.txt` and `license/` into the jar under `third-party/<set>/`, and the manifest's `license` line names the notation. Keep them when you publish a jar with these frames.

## Licence

The SC:R Clone Demo is licensed under the Apache License 2.0; the art sets keep their own licences above.

StarCraft is a trademark of Blizzard Entertainment, Inc. SC4J is not affiliated with or endorsed by Blizzard Entertainment. The SC:R Clone Demo reads the game data of the player's own StarCraft: Remastered installation; the repository and its distributions carry no such data.

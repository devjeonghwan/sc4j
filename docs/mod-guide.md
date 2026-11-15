# SC4J mod author guide

**English** | [한국어](mod-guide.ko.md)

This guide shows how to make a game for the SC4J engine: a mod. Chapter 2 takes you from nothing to a first mod that builds, installs and runs; the chapters after it explain each part of a mod and name the classes of demo 1 that implement it. Demo 1 ([`mods/demo/`](../mods/demo/), mod id `demo`, "SC4J Demo: Dawnhold and Cindermaw") is a small real-time strategy game made only of the engine, with art, sound, menus, computer players, replays, saved games and network play.

The root [README](../README.md) lists every mod of this repository.

Apart from short examples, the guide links to sources instead of copying them; the Javadoc of each class is its contract. Commands run in PowerShell from the repository root unless the step says otherwise; on Linux or macOS use `./gradlew` and the start scripts without `.bat`.

**Contents**

| Getting started | The parts of a mod | Going further |
|---|---|---|
| [1. What a mod is](#1-what-a-mod-is) | [5. Content](#5-content-the-standard-data-pack) | [13. Replays and saved games](#13-replays-and-saved-games) |
| [2. Your first mod](#2-your-first-mod) | [6. Rules](#6-rules-the-game-definition-systems-orders-and-commands) | [14. Network play](#14-network-play) |
| [3. Manifest and identity](#3-manifest-and-identity) | [7. The build and the jar](#7-the-build-and-the-jar) | [15. Building, installing and running](#15-building-installing-and-running) |
| [4. The entry](#4-the-entry-initialize-register-close) | [8. Maps and first-run content](#8-maps-and-first-run-content) | [16. API and versions](#16-api-and-versions) |
| | [9. The headless runner](#9-the-headless-runner) | [17. Licences for assets](#17-licences-for-assets) |
| | [10. Client screens](#10-client-screens) | [18. Clone an existing mod](#18-clone-an-existing-mod) |
| | [11. Computer players](#11-computer-players) | [19. Testing your mod](#19-testing-your-mod) |
| | [12. Determinism](#12-determinism) | [20. Two ways a mod runs](#20-two-ways-a-mod-runs) |

## 1. What a mod is

A mod is **one jar**. At its root lies the manifest `sc4j-mod.json`, which names the mod (its id, name, version), the engine API version it was built against and its entry class. The engine and its libraries are not in the jar: the runtime of an SC4J install provides them.

- One mod runs per runtime session, in its own class loader, so two mods never meet, even with equal package names.
- The **mod id** names everything the mod owns in an install: its settings file `userdata/settings/<id>.json`, its storage folder `userdata/mods/<id>/` (replays, saves, maps, options; `ModStorage`) and its log folder `userdata/logs/<id>/`.
- The **mod identity** (`ModIdentity`) is the engine API version, the mod id and version, the digest of the jar's content and the digest of the data the mod adds itself. Two machines play together, and a replay plays back, only with equal identities: every change of the jar is a new identity.
- The runtime creates the entry class (a public constructor without arguments), calls `Mod.initialize(ModContext)`, then `Mod.register(ModRegistration)`, then runs what the mod registered, and calls `Mod.close()` at the end.

What you need: the JDK the repository builds with (JDK 25), this repository (for its Gradle wrapper and the dev install) and nothing else; the mod itself lives in a folder of its own and compiles against an SC4J install.

## 2. Your first mod

### 2.1 An SC4J install

A mod compiles against the engine jars of an SC4J install (`<install>/runtime/lib/`) and is installed into its `mods/` folder. The repository's development install is `build/dev-install/` (launcher, runtime and the repository's mods):

```powershell
.\gradlew.bat :mods:demo:devInstall
```

Any other SC4J install works the same way: its folder holds `launcher/`, `runtime/`, `mods/` and, once used, `userdata/`.

### 2.2 The mod's files

A mod is a Gradle project of its own. Here it is the folder `my-mod` next to the repository folder:

| File | What |
|---|---|
| `settings.gradle` | the project name: `rootProject.name = 'my-mod'` |
| `build.gradle` | the mod's build (section 7.1) |
| `gradle.properties` | `sc4j.install=<install>`: the install the build compiles against and installs into (a Windows path with forward slashes) |
| `src/main/resources/sc4j-mod.json` | the manifest (section 3) |
| `src/main/java/com/example/mymod/` | the mod's classes: the entry `MyMod` and the screen `FirstScene` |

The mod id is lower case letters and digits in groups joined by single hyphens (`my-mod`); pick an id nobody else uses. The manifest:

```json
{
  "format": 1,
  "id": "my-mod",
  "name": {"en": "My Mod"},
  "version": "0.1.0",
  "engine": "0.1",
  "entry": "com.example.mymod.MyMod"
}
```

The entry registers a headless runner that logs one line, and the first screen of the window run (chapter 4):

```java
package com.example.mymod;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.engine.mod.Mod;
import org.fogofwarlabs.sc4j.engine.mod.ModContext;
import org.fogofwarlabs.sc4j.engine.mod.ModRegistration;

public final class MyMod implements Mod {
    private Logger logger;

    @Override
    public void initialize(ModContext context) {
        this.logger = context.getLogger();
    }

    @Override
    public void register(ModRegistration registration) {
        Logger log = this.logger;

        registration.registerHeadlessRunner(launch -> log.info("MOD: headless run of {0}", launch.getIdentity()));
        registration.registerFirstScene(FirstScene::new);
    }

    @Override
    public void close() {
        this.logger = null;
    }
}
```

The screen fills the window with one colour (packed `0xAARRGGBB`) and ends the run on Escape (chapter 10):

```java
package com.example.mymod;

import org.fogofwarlabs.sc4j.engine.client.app.ClientScene;
import org.fogofwarlabs.sc4j.engine.client.app.SceneClient;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;

final class FirstScene implements ClientScene {
    private static final int BACKGROUND = 0xFF204060;

    private final SceneClient client;

    FirstScene(SceneClient client) {
        this.client = client;
    }

    @Override
    public void open(SceneClient context) {
    }

    @Override
    public void update(long time) {
    }

    @Override
    public void draw(Canvas surface) {
        surface.fillRectangle(0, 0, surface.getWidth(), surface.getHeight(), BACKGROUND);
    }

    @Override
    public boolean handleInput(InputEvent event) {
        if (event.getType() == InputEvent.Type.KEY_PRESSED && event.getKey() == Key.ESCAPE) {
            this.client.requestQuit();

            return true;
        }

        return false;
    }

    @Override
    public void close() {
    }
}
```

### 2.3 Build and install

The repository's Gradle wrapper builds the mod's folder:

```powershell
.\gradlew.bat -p ..\my-mod installMod
```

builds `..\my-mod\build\mods\my-mod.jar` and copies it into `<install>/mods/`. `modJar` instead of `installMod` only builds the jar.

### 2.4 Run headless

```text
<install>\runtime\bin\sc4j-runtime.bat --mod <install>\mods\my-mod.jar --headless
```

runs the mod's headless runner and ends with exit code 0. The runtime prints its log; the last lines are

```text
... INFO  [main] RuntimeSession - IDENTITY: my-mod 0.1.0 (engine 0.1, content ..., data ...)
... INFO  [main] MyMod - MOD: headless run of my-mod 0.1.0 (engine 0.1, content ..., data ...)
... INFO  [main] RuntimeSession - RUNTIME END: exit code 0 (SUCCESS)
```

The start scripts run the JDK named by `JAVA_HOME` (or `java` on the `PATH`): JDK 25. The mod's own arguments come after `--` (`ModContext.getArguments()`, chapter 9).

### 2.5 Run from the launcher

```text
<install>\launcher\bin\sc4j-launcher.bat --list
```

prints every jar of `mods/` with its status; the new mod has no pre-run settings, so it is `READY`:

```text
Mods in <install>\mods
READY   my-mod               my-mod.jar
```

`<install>\launcher\bin\sc4j-launcher.bat` opens the launcher window; choose "My Mod" and start it. The runtime opens a window with `FirstScene` in one colour; Escape ends the run.

From here on, turn the mod into your game. Section 2.6 lists the parts of a small game in the usual order; the rest of this guide explains each part.

### 2.6 Make it a game

A small game needs units that move and fight, a building that trains them and a way to win:

1. **Content** (chapter 5): a standard data pack under `src/main/resources/<id>/pack/` (`pack.json`, `content.json`). `initialize` opens it with `StandardPack.openResources(MyMod.class.getClassLoader(), "my-mod/pack")` and reads it with `readContent`; `register` adds its types to the game's content (`PackContent.register(registration.getContent())`) and registers the pack as the resource provider.
2. **Rules** (chapter 6): the game definition `registration.getSimulation()` with the engine default systems and their frame phases.
3. **Map** (chapter 8): a map of the engine map format under `src/main/resources/<id>/seed/maps/`, copied into the storage at the first run, with `"victory": "annihilation"` (a player without units loses) or `"elimination"` (a player without buildings loses).
4. **Units** in `content.json` (chapter 5): a building with `"traits": ["building"]`, `placementWidth`, `placementHeight` and a `collision` box; the faction's `townHall` is placed at the start location of each player of the faction, its `startUnits` next to it. Fighters get a `groundWeapon` (a `weapons` entry), a `targetAcquisitionRange`, a `buildTime` (game loops) and `"idleOrders": {"human": "guard", "computer": "guard"}`. A `cost` needs `resourceKinds` and a start amount (`game.getEconomy().setAmount` after `Game.create`), `supplyRequired` a unit with `supplyProvided`. A `requirements` entry such as `"spearman": [{"actor": "keep"}]` lets the building train the unit. The spatial index (`StandardSpatialIndex`) is created with the width and height of the largest unit: it finds no unit larger than that.
5. **Orders** (chapter 6): the order types the idle orders and your commands name (`guard`, `attack`, `attack-move`) go to `readContent`, their behaviors to the order system: `GuardBehavior`, `AttackBehavior` and `AttackMoveBehavior` over `(StandardCombatSystem) game.getCombat()`.
6. **Training.** Register a `TrainUnitCommandType` in the command registry under the next id (the registry's size is the highest id plus one). A controller or a scene sends `train.create(game, building, type)` through its `CommandSink`; the production queue `StandardProductionSystem` runs it (`new StandardProductionSystem.Builder(capacity)` with `StandardProductionSystem.addColumns(builder, capacity)`).
7. **Winning.** Run the map's triggers (`StandardTriggerSystem` over `StandardMapData.getTriggers()`) as the last frame phase: `((StandardTriggerSystem) game.getTriggers()).isGameOver()` tells when the game is decided and `getOutcome(player)` gives `VICTORY` or `DEFEAT`. Stop the headless runner's loop and the scene's game loops there.
8. **Screen and headless runner** (chapters 9 and 10): a scene that shows the game in a session, a headless runner that plays it with computer players and records its replay.
9. **Check** (chapter 12): two headless runs of the same game and the playback of its replay give the same state hash after every game loop.

## 3. Manifest and identity

`sc4j-mod.json` at the jar root (section 2.2, [demo 1](../mods/demo/src/main/resources/sc4j-mod.json)). The launcher and the runtime read it without loading a class (`ModManifestReader`); a problem makes the mod `ERROR` in the launcher.

| Field | Required | Value |
|---|---|---|
| `format` | yes | `1` |
| `id` | yes | the mod id (section 2.2) |
| `name` | yes | per language: `{"en": "My Mod", "ko": "..."}` |
| `version` | yes | the mod's version, part of its identity |
| `engine` | yes | the engine API version built against, `MAJOR.MINOR`; the runtime runs the mod when the majors are equal and the mod's minor is not higher (`EngineVersion.CURRENT`, [API policy 3.1](api-policy.md#31-the-engine-api-version)) |
| `entry` | yes | the fully qualified entry class |
| `description`, `authors`, `website`, `license` | no | shown in the launcher's mod details; `license` is the place for the notation that the licences of your assets ask for (chapter 17) |
| `notices` | no | short legal notices, each per language like `name`: `[{"en": "Example Game is a trademark of Example Company.", "ko": "..."}]`; the launcher's About box lists them under the mod's name, in the display language or else English |
| `images` | no | `icon`, `banner`, `screenshots`: PNG paths in the jar, shown by the launcher |
| `settings` | no | the pre-run settings the launcher asks for before a start (below) |

**Pre-run settings.** A mod that needs something before it starts (an external data folder, a language) declares it in `settings`: entries with `key`, `type` (`directory`, `file`, `choice`, `integer`, `string`, `boolean`), `label`, `description`, `required`, `default`, `message` and the fields of the type (`SettingsSchema` in `sc4j-mod-manifest`). The launcher shows the form, validates the values and saves them to `userdata/settings/<id>.json`; the mod reads them with `ModContext.getSettings()`. A `choice` of locales with `"systemDefault": true` defaults to the option of the operating system's locale (`koKR` for Korean in Korea), else to its `default`. A mod with required settings is `WARNING` until they are saved. The first mod of chapter 2 has none.

**Directory settings.** A `directory` setting has a folder picker in the form and three optional fields:

- `check.contains`: relative paths the folder must hold; `message` is shown when it does not.
- `detect`: where a folder of this kind is usually recorded: the names of its uninstall entries (`uninstallEntries`) and its folder names below the program folders (`programFolders`). While the setting has no value, the launcher (and the runtime) looks there, read-only, and saves the first folder that passes `check`.
- `shared`: a name under which mods share one value, such as the folder of a game that several mods read. The value is saved once in `userdata/shared-settings.json` and every mod that declares the same name shows and uses it. Without `shared` the value is the mod's own.

The mod reads the folder like any setting, with `ModContext.getSettings().getDirectory(key)`. A mod that reads the data of another game's installation declares that game's folder this way, with `detect` and `shared`.

**Identity data.** A mod that reads external data adds that data's version or digest with `ModRegistration.addIdentityData`, so two machines with different external data do not play together.

## 4. The entry: initialize, register, close

`MyMod` (section 2.2), [`DemoMod`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/DemoMod.java).

- `initialize(ModContext)`: open and check what the mod needs; throw `ModException` to refuse the run. `ModContext` gives the manifest, the saved settings, the storage (`ModStorage`), the install folder, the mod's own command line arguments (after `--`) and the logger. Read your resources through your own class loader (`MyMod.class.getClassLoader()`).
- `register(ModRegistration)`: tell the engine what the mod adds. The registration closes when the method returns.

| Registration | What | First mod | Demo 1 |
|---|---|---|---|
| `getSimulation()` | the game definition: limits, systems, frame phases, columns, hooks (chapter 6) | - | `DemoRules.define` |
| `getContent()` | the content registry: types and definitions (chapter 5) | - | `DemoContent.register` |
| `registerResourceProvider` | the pack, the resources of the client | - | the pack |
| `registerReplayFormat`, `registerSaveFormat` | the formats of replays and saves (chapter 13) | - | engine formats |
| `registerHeadlessRunner` | the `--headless` run (chapter 9) | a lambda that logs one line | `DemoHeadlessRunner` |
| `registerFirstScene` | the first screen of the window run (chapter 10) | `FirstScene` | `MainMenuScene` |
| `addIdentityData` | data that joins the identity | - | - |

- `close()`: release what `initialize` opened, in reverse order.

A mod registers either a first scene and a headless runner (as the first mod and demo 1 do) or a `ModApplication` that runs its own frames; not both. Chapter 20 compares the two.

## 5. Content: the standard data pack

An engine-only mod keeps its data in the engine standard data pack: JSON definitions, PNG pictures and WAV sounds under one folder of the jar, opened with `StandardPack.openResources(loader, root)`. The pack format (`pack.json`, sprite sheets, animations, tilesets, fonts, dialogs, pictures) is the Javadoc of the package `org.fogofwarlabs.sc4j.engine.pack` ([package-info](../sc4j-engine-sim/src/main/java/org/fogofwarlabs/sc4j/engine/pack/package-info.java)).

`content.json` defines the content types: `resourceKinds`, `motionProfiles` (how units move: top speed, acceleration, stopping distance, turn rate), `weapons`, `factions` (worker, town hall, supply provider, resource kinds, start units), `units` (faction, motion profile, hit points, armor, sight, weapons, cost, build time, supply, collision box, placement size, traits, idle orders) and `requirements`. `StandardPack.readContent` reads it into a `PackContent`; the mod registers its types in the game's content (`PackContent.register`).

Demo 1 ([pack](../mods/demo/src/main/resources/demo/pack/), [`DemoContent`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/content/DemoContent.java)): two factions with workers, buildings, fighters and gold; sprite sheets, tilesets, a font, dialogs, sounds, pictures.

To add a unit: add it to `content.json` (and to a faction's `startUnits`, or place it on the map with `units`), and give it orders it understands (chapter 6).

## 6. Rules: the game definition, systems, orders and commands

**Game definition.** `ModRegistration.getSimulation()` is a `GameDefinition.Builder`. The mod sets the entity limits (`EngineLimits`), chooses a system for each slot with `useSystem(KEY, factory)` (the engine defaults are the `Standard*` classes: spatial index, path finding, movement, collision, visibility, economy, harvest, requirements, combat, projectiles, production, orders, triggers, computer players), adds the extension columns the defaults need (`StandardCombatSystem.addColumns`, ...), hooks (`addHook`) and the frame phases in their order (`addFramePhase`). A factory runs per game, so a system can read the game's map. A mod may replace any system with its own class of the same interface.

The settings of the defaults are neutral, so a mod declares the values of its game: the motion model of the movement and projectile systems (`new MotionModel.Builder(directionCount)`: directions per turn, instant stop speed, braking before a turn), the queue capacity of the production system (`new StandardProductionSystem.Builder(capacity)`; refunds are the whole cost and a building starts with its full hit points unless the mod sets shares), and the splash shares and minimum damage of the combat system (`StandardCombatSystem.Builder`; without them every unit a splash reaches takes the whole damage and a hit may deal nothing).

A unit that is placed, sees, finds its path and moves needs the unit bounds (`TableUnitBounds`), the spatial index, path finding, movement, collision and visibility, with the frame phases orders, movement, collision and visibility. Each slot takes a factory, each phase the system's frame step:

```java
registration.getSimulation()
        .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(new MotionModel.Builder(64).build()))
        .addFramePhase(MovementSystem.KEY, system -> (StandardMovementSystem) system);
```

Demo 1: [`DemoRules`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/rules/DemoRules.java): every default system, harvest, combat, production, triggers and computer players.

**New games.** A `GameSetup` names the map (`MapReference`), the player slots (`PlayerSlot`: slot, controller kind, team, colour, faction) and the seed. `Game.create(definition, setup)` creates the game, `MapPlacementUtils.placeUnits` places the map's units. Demo 1: [`DemoGames`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/game/DemoGames.java).

**Orders.** An order type (`OrderType`) is content; its behavior (`MoveBehavior`, `AttackBehavior`, `GatherBehavior`, `BuildBehavior`, ..., or your own `OrderBehavior`) is registered per game in the order system, which takes the registration as a function: `new StandardOrderSystem((game, behaviors) -> behaviors.register(move, new MoveBehavior(16)))` gives the order type `move` its behavior with an arrival distance of 16 world pixels. Demo 1: eleven orders ([`DemoOrders`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/order/DemoOrders.java)).

**Commands.** Everything a player does reaches the game as a command of the mod's command stream (`CommandRegistry` of `CommandType`s with fixed ids: `IssueOrderCommandType`, `TrainUnitCommandType`, `ConstructBuildingCommandType`, or your own). Commands go through a `CommandSink`; the frame's `ControllerCommandSource` applies them before the frame's phases. Replays and network turns carry exactly these commands, so the ids and the command encoding are part of your formats: change them only with a new mod version.

**Mechanics of your own.** The engine holds the parts that most real-time strategy games share; a mechanic of one game lives in its mod and plugs into these points:
- a value that upgrades or effects change: register a `Stat` of your own after the engine stats (`ContentRegistry.getStats().register(new Stat("..."))`) and attach a `StatModifier` to it (`ContentRegistry.addModifier`);
- a per-type value: an extension column of the type table (`ContentRegistry.addColumn`); a per-unit value: an extension column of the unit pool, or a field of your own `Unit` subclass when the mod pools its units itself;
- a layer that takes damage before the hit points (shields, a barrier, armour plates): a `DamageLayer` with its own points and rules, added to the chain of each unit type that has it while the game is created (`GameContent.addDamageLayer` in a system's `attach`); the standard combat system asks the layers in chain order, and the HUD shows a bar per layer under its name;
- a movement rule: your own movement code over the engine `MotionModel` or your own motion algorithm over the same `MovingEntity` fields, which may move the prospective `MotionStep` before it is applied (such as air units that push each other apart), or your own system for the movement slot (`useSystem`).

## 7. The build and the jar

### 7.1 The mod's own build

The `build.gradle` of a mod outside this repository:

```groovy
plugins {
    id 'java'
}

def install = file(providers.gradleProperty('sc4j.install').get())

// The engine API: the runtime provides it, so it is not packed into the mod jar.
dependencies {
    compileOnly fileTree(new File(install, 'runtime/lib')) {
        include 'sc4j-base-*.jar', 'sc4j-mod-manifest-*.jar', 'sc4j-engine-*.jar', 'annotations-*.jar'
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

// The mod jar: no file times and a fixed order, so the same sources give the same jar and the same identity.
def modJar = tasks.register('modJar', Jar) {
    archiveFileName = 'my-mod.jar'
    destinationDirectory = layout.buildDirectory.dir('mods')
    preserveFileTimestamps = false
    reproducibleFileOrder = true
    duplicatesStrategy = DuplicatesStrategy.FAIL
    from(sourceSets.main.output)
}

tasks.register('installMod', Copy) {
    from(modJar)
    into new File(install, 'mods')
}
```

- It compiles against the engine API jars of the install named by `sc4j.install` (`sc4j-base`, `sc4j-mod-manifest`, `sc4j-engine*` and the nullability annotations) as `compileOnly`: the runtime provides them, so they are not in the jar.
- `modJar` packs the classes and resources reproducibly (no timestamps, fixed order, duplicates fail): the same sources give the same jar, so the same identity.
- `installMod` copies the jar into `<install>/mods/`.

A library your mod needs at run time must go into the jar (or come with the install: the runtime's `lib/` carries the engine's libraries such as LWJGL and FFmpeg). Check its licence first (chapter 17).

### 7.2 A mod inside this repository

Demo 1 is a project of the repository build ([`mods/demo/build.gradle`](../mods/demo/build.gradle)): `implementation project(':sc4j-engine')`, its own `modJar` and `devInstall` (copies `demo.jar` into `build/dev-install/mods/`), and checks: `checkApiUse` (no internal engine element, chapter 16), `checkModBoundary` and `checkDemoNames` (the demo names no other game), and `determinismCheck` (chapter 12). Its tests run the jar through the runtime (`DemoModTest`) and the client offscreen (`DemoClientScenario`).

## 8. Maps and first-run content

**Map format.** The engine map format is one JSON file (`"format": "sc4j-map"`, version 1): name, tileset name, tile and walk cell size, size in tiles, terrain as rows of one-character codes with a legend (walkable, buildable, elevation), the neutral slot, start locations, preplaced units, resource containers, locations, the victory rule and triggers. The full field list is the Javadoc of `JsonMapLoader` ([source](../sc4j-engine-sim/src/main/java/org/fogofwarlabs/sc4j/engine/map/JsonMapLoader.java)). The mod reads maps with `new JsonMapLoader(unitTypes, resourceKinds)` and keeps them in a `MapCatalog`, which gives each map its `MapReference` (name and digest). The victory rule and the triggers take effect only in a game with a trigger system: the mod registers `StandardTriggerSystem` over the triggers of the game's map (`new StandardTriggerSystem.Builder().addTriggers(data.getTriggers()).build()` with the map's `StandardMapData`, the victory rule first) as the last frame phase.

**Grids and projection.** The simulation works in world pixels on a square grid of walk cells (`TerrainGrid`). How the client shows it is the client's projection: `SquareProjection` (from above) or `IsometricProjection` (diamonds; demo 1, [`DemoWorldUtils`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/client/DemoWorldUtils.java)). Hexagonal or staggered grids are not part of the engine today; the grid and projection interfaces leave room for them.

**First-run content.** The storage of a new install is empty. A mod that keeps its maps in the storage (where players add their own) must put its own maps there itself, or its first start finds none (a clone, chapter 18, starts with an empty map list for this reason). The general way: the jar carries the maps under `<id>/seed/`, and `initialize` copies every listed seed file into the storage **only when the storage has no file of that path**:

```java
for (String file : List.of("maps/first-field.json")) {
    Path target = context.getStorage().resolve(file);

    if (Files.exists(target)) {
        continue;
    }

    try (InputStream input = MyMod.class.getClassLoader().getResourceAsStream("my-mod/seed/" + file)) {
        if (input == null) {
            throw new IOException("The mod jar has no seed file " + file);
        }

        Files.createDirectories(target.getParent());
        Files.copy(input, target);
    }
}
```

A new install starts with the mod's maps, a player's own maps sit next to them, and a map the player changed is never overwritten (a deleted one comes back at the next start). Then the mod reads every `*.json` of `maps/` in file name order and leaves out, with a warning, a file that is no valid map. To ship another map, put it under `src/main/resources/<id>/seed/maps/` with a `name` no other map has, add its path relative to `<id>/seed/` (`"maps/second-field.json"`) to the list, rebuild and reinstall (`installMod`); the next start copies it into the storage. Seed any other first-run file (options, a tutorial replay) the same way. A seed map you change or remove in the jar does not change the copy an earlier run left in the storage: while you work on a map, delete that copy (or run with another `--storage`).

Demo 1 reads its built-in maps straight from the jar ([`DemoMaps`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/map/DemoMaps.java)) and keeps the maps a network host sent it next to them; both ways are fine.

## 9. The headless runner

`HeadlessRunner.run(ModLaunch)` is the `--headless` run: no window, audio or input; for tests, servers and research. `ModLaunch` says whether a replay (`getReplay()`, `getReplayFormat()`) or a game to join (`getJoin()`) was asked for, and holds the identity, the game definition, the resource provider, the replay and save formats and the run's Beacon.Link. The mod's own arguments are `ModContext.getArguments()`.

Demo 1: [`DemoHeadlessRunner`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/DemoHeadlessRunner.java) plays a game of two computer players on `--map` to its end and records its replay, or plays `--replay` back and fails on a state hash checkpoint that differs; `--frame-hashes` writes one hash per game loop. It also plays network games on 127.0.0.1 (`--host`, the runtime's `--join`) and over Beacon.Link (`--beaconlink-host`, `--beaconlink-join`, with the runtime's `--seed <host:port>`).

## 10. Client screens

The runtime opens the window, the audio and the input, creates the mod's first scene with its `FirstSceneFactory` and runs the frames on a `SceneClient` (chapter 20). A screen is a `ClientScene` (`open`, `update`, `draw`, `handleInput`, `close`); a scene switches to the next one with `client.getScenes().replace(...)` and opens popups with `pushModal`; `client.requestQuit()` ends the run. The `SceneClient` gives the launch request, the storage, the `Canvas`, the `AudioMixer` and the cursor surface.

**Showing a game.** `GameSessionFactory` connects a `Game` to the client: `createLocal` (a person and computer players), `createReplay`, `createNetwork`. The session's `GameView` is the read-only view the client draws from; the person's commands go to the session's `CommandSink`. A `CameraScroll` scrolls the `Camera` by keys and screen edges, continuous (`ContinuousScrollRate`) or in table steps per tick (`SteppedScrollRate`). The world is a `WorldView` of `WorldLayer`s through the `Camera` and a projection:

- without art: `TerrainLayer` (plain colours of `TerrainColors`) and `UnitLayer` (unit footprints in team colours);
- with art: `TilesetLayer` and `UnitSpriteLayer` over the pack's tilesets and sprite sheets; on a scaled display they draw from the pack's detail versions `<id>-2x` up to `<id>-4x` (`DetailUtils`; detail sprite sheets are listed under `spriteSheetDetails`);
- `GameScreen`: the engine default in-game screen with selection, command card, minimap, resource bar, selection panel and fog, in a HUD skin; it needs a font from the pack.

Demo 1's client ([package](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/client/)): `DemoClient` (shared services: font, sprites, skin, sounds, cursors), `MainMenuScene` and `DemoMenuScene` (pack dialogs as menus), `GameSetupScene`, `LoadingScene`, `DemoGameScene` and `DemoGameScreen` (the `GameScreen` with the command card and the demo's smart right press `DemoSmartAction`), `ReplayViewerScene` (the engine's stored-game list `GameFileListScene`), `OutcomeScene`, `NetworkScene`, `NetworkRoomScene`, `BeaconLinkScene`, `DemoSkin`, `DemoSprites`, `DemoSounds`, `DemoNotifications`.

**Testing screens without a window.** An `ImageCanvas` is a `Canvas` in memory: create a `SceneClient` over it with a silent audio device, start the first scene, send input with `client.handleInput` and run frames with `client.runFrame`; demo 1's `DemoClientScenario` does so. The runtime has no offscreen mode, so a mod outside this repository does the same from its own build: a source set with a main class whose class path holds the mod's classes and every jar of `<install>/runtime/lib`; a `ModStorage` over a scratch folder (`getRoot` and `resolve`; the runtime's own implementation is not API); a `ModLaunch` with `ModIdentity.create(EngineVersion.CURRENT, manifest, digest, List.of())` and a game definition the mod built as in `register`; and `ImageIO.write(canvas.getImage(), "png", file)` for a picture. Slash commands and key bindings of the client are the mod's choice (`ConsoleCommandRegistry`, `KeyBindingTable`).

## 11. Computer players

A computer player is a `PlayerController` whose commands depend only on the game. The engine default is a system: `StandardComputerPlayerSystem` with `StandardComputerController` (workers gather, buildings and fighters are made, attack waves go out; `ComputerSettings`), used by `DemoRules`. Slots of `ControllerKind.COMPUTER` run on every machine of a network game alike.

## 12. Determinism

Lockstep network games and replays need the same simulation results from the same commands on every machine. The engine's own simulation packages are checked for this (`checkSimulationDeterminism` in the root `build.gradle`); your simulation code (systems, behaviors, controllers, anything that changes the game) must follow the same rules:

- no `Math.random`, no clock (`System.currentTimeMillis`, `System.nanoTime`, `java.time`), no `java.util` random generator (`Random`, `SecureRandom`, `SplittableRandom`, `ThreadLocalRandom`): randomness comes from the game's random generator, seeded by the setup;
- no hash-ordered collection (`HashMap`, `HashSet`, `Hashtable`, `IdentityHashMap`, `WeakHashMap`, `ConcurrentHashMap`) whose order the simulation sees, no identity hash code: use lists, arrays, `LinkedHashMap`, or the entity pools in slot order;
- no floating point (`float`, `double`): positions, speeds and damage are integers or engine fixed-point values;
- the game changes only through commands applied at frame start and the systems' frame phases; never from a scene, an input handler or another thread directly;
- the order of everything is fixed: systems and phases in registration order, controllers in list order, commands in submission order.

The client (drawing, sound, menus) may use anything. Check it the way demo 1's `determinismCheck` does: play the same game twice and compare the state hash after every game loop (`Game.computeStateHash`, `--frame-hashes`), and play the replay back to the same hashes.

## 13. Replays and saved games

`EngineReplayFormat` (`.sc4r`, magic `SC4R`) records the setup and every command with a state hash checkpoint every n game loops (`createRecorder`, set on the `ControllerCommandSource`); `read` and `createController` play it back and count checkpoint mismatches. `EngineSaveFormat` (magic `SC4S`) stores a game's state. Both are created with the run's identity and load only with an equal identity: the same engine API version, mod id, version and jar. A mod with its own formats implements `ReplayFormat` / `SaveFormat`. Keep them in the storage (`replays/`, `saves/`); the engine's `GameFileListScene` lists them. Demo 1: replay viewer and saves in its client, replays in its headless runner.

## 14. Network play

The engine's network layer (`sc4j-engine-net`) runs lockstep games: every machine runs the same simulation and exchanges only commands in turns.

- `NetworkLobby`: the room before the start: members, slots, factions, ready flags, map transfer (the mod's `LobbyMaps`; the default `FolderLobbyMaps` over `ModStorage.getMapFolders()` finds a map by digest in `maps/` and saves a received one in `maps/downloads/`), the start (`NetworkGameStart` with the setup); the lobby compares identities and refuses another mod or version.
- `DatagramLockstepTransport` and `LockstepSession`: the turns of the running game; `GameSessionFactory.createNetwork` shows it.
- `LanRoomDiscovery`: rooms on the local network.
- Beacon.Link (`ModLaunch.getBeaconLink()`): rooms, games and the community over the Internet, through the peers every SC4J runs (sections 14.1 and 14.2).

Demo 1 uses all of these: [`DemoLobby`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/net/DemoLobby.java), [`DemoNetworkGame`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/net/DemoNetworkGame.java), [`DemoNetworkSettings`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/net/DemoNetworkSettings.java), its network scenes and its headless network runs. Test network code on 127.0.0.1 only, as demo 1's tests and `determinismCheck` do.

### 14.1 Beacon.Link rooms

`ModLaunch.getBeaconLink()` is the run's `BeaconLink`, or `null` when the run offers none. A run offers it only when its command line names a seed source (`--seeds <file>`, `--seed <host:port>` or `--peers <file>`); `--account <dir>` names the account, and without it the run plays as a guest named `Guest` (`BeaconLink.GUEST_NAME`). The launcher passes them all. Nothing opens before `BeaconLink.open()`, which returns a `BeaconLinkSession`: one connection of the account (`getParticipant()`, a `LinkParticipant`: the player and the connection number). Every call only starts work: the session's thread calls `poll()` from its loop, and `close()` at the end. `getFailure()` (`LinkFailure`) tells why no node is reachable, `getNodes()` gives the state of each node.

- **Host.** `publishRoom(settings, password)` publishes a room (an empty password makes it public) and returns a `HostedRoom`: `getState()` (`PUBLISHING`, `PUBLISHED`, `WITHDRAWN`, `FAILED`), `update(settings)` when players come and go, `acceptChannel()` for each joiner that passed the join check, `withdraw()`. `RoomSettings` holds the room name and the map name (at most 48 UTF-8 bytes each), the players, the capacity, whether the game started, and up to 64 bytes of mod data that the mod defines and reads back from the rooms it finds (demo 1: its turn rate).
- **Search.** `searchRooms()` returns a `RoomSearch`: `getRooms()` are the rooms of the run's own mod identity, `getOtherRooms()` the rooms of the mod's other versions with the refusal their hosts would give (`FoundRoom.getJoinRefusal()`), and `isComplete()` turns true once every node answered or failed. A `FoundRoom` gives the host, the settings, `isJoinable()`, `isPrivate()`, the mod id and version and `getLatencyEstimate()` (`LatencyClass`: `GOOD` up to 100 ms, `FAIR` up to 200 ms, `POOR`, `UNKNOWN`).
- **Join.** `joinRoom(room, password)` opens a `ParticipantChannel` to the host: `CONNECTING`, then `OPEN`, or `CLOSED` with a `ChannelFailure`; `JOIN_REFUSED` comes with the `JoinRefusal` (another engine version or mod identity, a full room, a started game, a wrong password).
- **Play.** `getOverlay()` returns the session's `LinkOverlay`, which carries datagrams by participant over the channels (at most 1023 bytes, not reliable). Port 1 (`LOBBY_PORT`) is the engine's `NetworkLobby`, port 2 (`GAME_PORT`) its lockstep transport (`LinkLockstepTransport`); ports 16 to 255 (from `FIRST_MOD_PORT`) are free for the mod's own protocols. `NetworkLobby.host(room, overlay, ...)` and `NetworkLobby.join(channel, overlay, ...)` give the same lobby and lockstep game as on the local network.

Demo 1: [`DemoLobby`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/net/DemoLobby.java) (host, join and the game over the overlay), [`BeaconLinkScene`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/client/BeaconLinkScene.java) (the room list, host, join and chat) and `DemoHeadlessRunner` (`--beaconlink-host`, `--beaconlink-join`).

### 14.2 Beacon.Link community

`BeaconLinkSession.getCommunity()` returns the session's `CommunitySession`, created at the first call:

- **Presence.** `getSelf()`, `setStatus(PlayerStatus)` (`AVAILABLE`, `AWAY`, `IN_ROOM`, `IN_GAME`), `setAwayText` (at most 32 UTF-8 bytes) and `setAppearOffline`. `getPresence(player)` gives the player's `PlayerPresence`, merged over its connections (the launcher, a game, another computer): name and tag, status, mod id and version, `canPlayTogether()`, channel, away text, latency estimate and device count.
- **Chat.** `joinChannel(name)` (at most 4 channels at once; channels are shared by every mod) returns a `ChatChannel`: `say` and `emote` (at most 255 UTF-8 bytes; one line per second after a burst of five), `getLines()`, `getMembers()` and `leave()`. `listChannels()` lists the known channels with their member counts.
- **Whispers.** `whisper(player, text)` sends a whisper; the outcome (`DELIVERED`, `NOT_ONLINE`) comes to the listeners.
- **Friends and ignored players.** `addFriend`, `removeFriend` (at most 16 friends), `ignore`, `unignore`, `isIgnored`: the ignore list belongs to the account. Lines and whispers of ignored players are not handed out; the mod hides their rooms.
- **Listeners.** `addListener(CommunityListener)`: `onChatLine`, `onMembersChanged`, `onWhisper` and `onWhisperResult`, called inside `poll()`.

`BeaconLinkSession.signOutOtherDevices()` ends the account's other connections. The package `engine.client.community` of `sc4j-engine` holds the default parts of community screens: `ChannelChat` (the channel chat with its slash commands, friends and blocked players), `RoomFilter`, `ChatLogBox`, `TableBox` and their texts; demo 1's `BeaconLinkScene` uses them. The wire protocol is in the [Beacon.Link protocol reference](beaconlink/protocol.md); running a peer for other players is in the [Beacon.Link operator guide](beaconlink/operator.md).

## 15. Building, installing and running

- **Jar**: `sc4j-mod.json` at the root, your classes and resources; no engine jar; reproducible. Every change is a new identity: players need exactly the same jar to play together and to play each other's replays.
- **Install**: put the jar into `<install>/mods/` (`installMod`, or by hand). The launcher lists every jar there: `READY`, `WARNING` (settings to fix) or `ERROR` (broken jar or manifest, an engine version this runtime cannot run, a missing image, an id another jar has).
- **Runtime**: `sc4j-runtime --mod <jar> [--install <dir>] [--settings <json>] [--storage <dir>] [--log <dir>] [--account <dir>] [--seeds <file>] [--seed <host:port>]... [--peers <file>] [--headless] [--replay <file>] [--join <address>] [-- <mod arguments>]` (`--help` prints it). Exit codes: 0 normal, 1 run failure, 2 command line or settings error, 3 mod load or validation failure. The log is `<install>/userdata/logs/<id>/sc4j.log`.
- **Launcher**: lists the mods, asks for their settings, starts the runtime with the mod's settings, storage and log folders.

## 16. API and versions

The policy itself is the [engine API policy](api-policy.md). In short:

- The API is the public and protected classes and members of `sc4j-base`, `sc4j-mod-manifest`, `sc4j-engine-core`, `-sim`, `-net`, `-client` and `sc4j-engine`, listed in their `api/*.api` files. Their Javadoc is the contract.
- **Internal** and not for mods: `engine.mod.loading`, `engine.client.render.gl`, `engine.client.audio.openal`, the GLFW host implementations (`GlfwClientHost`, `GlfwHostWindow`, `GlfwInput`, `GlfwKeyCodes`, `GlfwCursorSurface`) and the Windows platform services (`WindowsTextInputMethod`, `WindowsSystemPointerSpeed`, `WindowsHostTickClock`). The mods of this repository are checked for them (`checkApiUse` fails on every use). A mod's own build (section 7.1) does not run that check, and the install's jars hold the internal classes too: keep to the listed API. Draw through `engine.client.render` (a renderer of your own creates its GPU canvas and shader programs with `GraphicsDevice.getCurrent()`), play sound through `engine.client.audio` (`AudioDevice.openDefault()`), and run a window of your own through the `engine.client.host` interfaces (`ClientHost.initialize()`): the `HostWindow` serves keys and pointer (`createInput`, `isKeyDown`, `setPointerMode`, `createCursorSurface`), focus, clipboard, dropped files, the fullscreen gamma ramp and the input method (`getTextInputMethod`); `SystemPointerSpeed` and `HostTickClock` are the platform services.
- The engine API version is `MAJOR.MINOR` (`EngineVersion.CURRENT`, now 0.1); put it into the manifest's `engine` field. A new major version may break your mod; a new minor version adds only. While the API is at 0.x nothing is stable: incompatible changes can happen and are visible as `.api` diffs.
- A change of simulation results raises at least the minor version. Replays and saves are not compatible across engine versions.

## 17. Licences for assets

Everything in your jar is given to every player who gets the jar.

- Use your own work, or material whose licence allows it to be redistributed in a mod (public domain such as CC0, or a licence whose conditions you meet). Keep the licence files and a notice with your assets, put them into the jar, and show the notation the licence asks for in the manifest's `license` and `description`.
- Demo 1: [`mods/demo/THIRD-PARTY.md`](../mods/demo/THIRD-PARTY.md) names the sources of its pictures and is in `demo.jar`.
- Art under a licence that asks for a notice: keep its `NOTICE.txt` and `license/` folder with the art, let the build pack them into the jar, and put the notation into the manifest.
- Never put files of another game's installation (graphics, sounds, data, maps) into your jar or repository. A mod that uses another game's data reads it from the player's own installation at run time, through a directory setting (chapter 3).

## 18. Clone an existing mod

To change an existing mod instead of starting from nothing, clone it: copy its sources at build time and put your own versions of a few files over them (an overlay of normal, editable files), under your own mod id; a lock stops the build when the cloned mod changes a file you replaced. The clone of this repository, demo 2, has its own guide, the [SC:R clone modder guide](mods/scr-demo.md): the clone step, the overlay and its lock, the identity, the content extension, changed values, an added unit with its own graphics, the art licences, building, installing and the launcher.

- A clone is another mod: its own id, settings, storage and log folders. Its storage starts empty too: put the maps your players need into it (chapter 8).
- A clone plays only with players of the same identity (chapter 1). A network protocol of its own that carries no mod id needs a value of the clone's own, such as a version that the host checks on a join.

## 19. Testing your mod

- A mod outside this repository tests from its own build: build the jar, install it into a scratch install, run the install's runtime headless twice and once on the replay, and compare the frame hashes (chapter 12).
- Inside the repository, run the jar through `RuntimeSession.run` in the test JVM (`DemoModTest`), the client offscreen (`DemoClientScenario`), and two runtime JVMs against each other (`determinismCheck`).

## 20. Two ways a mod runs

In `register` a mod tells the runtime how it runs. There are two ways, and a mod uses one of them: the registration throws an `IllegalStateException` when a mod registers both.

| | Engine scenes | Own application |
|---|---|---|
| Registration | `registerFirstScene(FirstSceneFactory)` and `registerHeadlessRunner(HeadlessRunner)` | `registerApplication(ModApplication)` |
| Window run | the runtime opens the window, the canvas, the audio, the cursor and the input, starts the first scene on a `SceneClient` and runs the frames through `WindowClient` on the engine host (`ClientHost`) | the runtime opens no window and calls `ModApplication.run(ModLaunch)`; the mod opens its window and runs its own frames on the same engine host (`ClientHost.initialize()`, `createWindow`, `ClientHost.run` with a `HostApplication` of its own) |
| `--headless` run | the `HeadlessRunner` | the same `run`, if `isHeadlessCapable()` returns `true` |
| Used by | the first mod of chapter 2, demo 1 | the SC:R mod, for its native frame timing |

**Engine scenes.** `WindowClient` is a `HostApplication`: every frame the host polls the window events, `WindowClient` hands the input to the scenes, runs the `SceneClient` frame and presents it with vertical sync. The mod writes scenes (chapter 10) and does not see the frame loop.

**Own application.** The mod writes the frame loop itself: its `HostApplication` decides what one frame does (`runFrame`) and the frame rate the host paces to (`getFrameRateLimit`). It also does what `WindowClient` does for engine scenes: it opens and closes the window, the canvas, the audio and the cursor in order. The SC:R mod uses this way because its frames follow the native game: the simulation advances by the host's time, and the native game's frame rate limits apply in menus and in games.

**Which to choose.** Almost always engine scenes. Choose an own application only when the mod must control the frame loop itself. The rest of a mod is the same for both: the manifest, the content, the simulation, replays and saves, network play and Beacon.Link (`ModLaunch.getBeaconLink()`).

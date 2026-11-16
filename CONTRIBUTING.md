# Contributing to SC4J

**English** | [한국어](CONTRIBUTING.ko.md)

Setup, tests, code style, commits and releases. Commands run as in the [README](README.md#build-and-run).

## Setup

- JDK 25; Gradle 9.1.0 through the wrapper.
- IntelliJ IDEA: open the repository root, import the Gradle build, set the project SDK and the Gradle JVM (Settings, Build Tools, Gradle) to JDK 25.

## Build and test

| Command | Runs |
|---|---|
| `.\gradlew.bat build` | compile, unit tests (`test`), `check`: API listings, module boundaries, determinism |
| `.\gradlew.bat :sc4j-runtime:distributionTest` | builds and unpacks the user package, starts its runtime headless |
| `.\gradlew.bat :sc4j-beaconlink:simulationTest` | Beacon.Link simulations of about a thousand peers (minutes each) |
| `.\gradlew.bat :sc4j-engine-sim:test` | one module |
| `.\gradlew.bat :mods:demo:test --tests org.fogofwarlabs.sc4j.demo.DemoModTest` | one test class |

The OpenGL and GLFW checks of `sc4j-engine-client` open a hidden window while the Gradle property `sc4j.render.smoke` is `true` (default in `gradle.properties`); `-Psc4j.render.smoke=false` turns them off.

## Code style

Follow the surrounding code and these rules.

- Four spaces, no tabs, lines up to about 150 columns; break long argument lists by meaning.
- Braces on every `if`, `else`, `for`, `while` and `do` body.
- Blank lines separate groups of statements; simple assignments in a row align their `=`.
- Member order: constants, static fields, nested types, static methods, instance fields, constructors, instance methods.
- Keep a file's line endings.
- Full words (`index`, `packetLength`); `Id` is fine.
- Methods start with a verb: `get`, `find`, `read`; `set`; `is`, `has`, `can`; `create`, `of`, `from`, `parse`. Opposites match (`readPacket`, `writePacket`).
- Meaningful numbers are `static final` constants or enums; plain `0`, `1` and `-1` may stay.
- Implementation classes are `final` unless meant to be extended. Helpers are `final` `*Utils` classes with a private constructor. No `record` types: data classes have explicit getters.
- Instance fields are set in the constructor and read through `this.`. Local variables have explicit types (no `var`).
- Every reference field, parameter and return value is `@NotNull` or `@Nullable` (JetBrains annotations).
- New exceptions come from `org.fogofwarlabs.sc4j.base.exception.Exceptions` with indexed placeholders: `throw Exceptions.newIllegalStateException("Invalid frame index: {0}", index);`
- Every public class and public API method and constructor has Javadoc; `javadoc` runs without warnings.
- Javadoc and comments are English. Summaries start with a third-person verb (`Creates`, `Returns`); use `{@code}` and `{@link}`, not Markdown. Tags in the order `@param`, `@return`, `@throws`; descriptions start in lower case without a final period; `@throws` says `if ...`.
- Comments say what non-obvious code does, not the history of a change.
- Engine-side modules name no mod; `checkModBoundary` fails on the SC:R mod's packages or types.
- Simulation code is deterministic ([mod author guide, chapter 12](docs/mod-guide.md#12-determinism)).
- An engine API change updates its listings ([API policy](docs/api-policy.md#6-updating-the-listings)).
- Test classes are `<Subject>Test`; test methods have `snake_case` names that say what they check.

## Commits and pull requests

- One topic per commit. The subject is a short imperative English sentence with identifiers in backticks (``Fix `ReplayReader` for empty files``); a body, when needed, lists the parts as `* ` lines.
- A version change is ``Bump version to `X.Y.Z` ``.
- Pull requests go into `develop` and are squash-merged; the pull request title is the commit subject.
- `.\gradlew.bat build` passes before a pull request; name the tests you ran.
- An API change commits its regenerated `.api` files and states its version effect in the commit message.
- Behaviour changes come with tests. No build output, local paths or game data.
- New assets need a licence that allows redistribution in a mod: licence files and a notice next to them and an entry in [NOTICE](NOTICE).
- CI runs `test` on every pull request into `main` or `develop`; the check `test` must pass before the merge.

## Releases

`develop` holds `X.Y.Z-SNAPSHOT` and publishes snapshots to the Central Portal. A release sets `version=X.Y.Z` in `gradle.properties` on `develop` and opens a pull request from `develop` into `main` titled ``Release version `X.Y.Z` ``, merged with a merge commit. Each push to `main` publishes to Maven Central and creates the tag and GitHub release `vX.Y.Z` with `SC4J-X.Y.Z.zip` and `SC4J-X.Y.Z.zip.sha256`.

## Licence of contributions

Unless you state otherwise, a contribution is licensed under the [Apache License 2.0](LICENSE) (section 5).

# Contributing to SC4J

**English** | [한국어](CONTRIBUTING.ko.md)

Thank you for helping. This page covers the developer setup, the test tiers, the code style and what a commit and a pull request should look like. The project overview is in the [README](README.md).

- [Setup](#setup)
- [Build and test](#build-and-test)
- [Code style](#code-style)
- [Commits and pull requests](#commits-and-pull-requests)
- [Releases](#releases)
- [Licence of contributions](#licence-of-contributions)

## Setup

| Tool | Version |
|---|---|
| JDK | 25 |
| Gradle | 9.1.0, through the wrapper (`gradlew`, `gradlew.bat`); no own installation needed |
| IDE | any; IntelliJ IDEA works with the Gradle import |

IntelliJ IDEA: open the repository root folder and let the IDE import it as a Gradle project. Set the project SDK and the Gradle JVM (Settings, Build Tools, Gradle) to JDK 25.

Commands below run in PowerShell from the repository root. On Linux use `./gradlew`.

## Build and test

The tests come in tiers. The first tier needs nothing outside the repository; the others need the player's own StarCraft: Remastered installation or other external folders.

| Command | What runs | What it needs |
|---|---|---|
| `.\gradlew.bat build` | compile, unit tests (`test`) and the checks of `check`: API listings, module boundaries, determinism | nothing outside the repository |
| `.\gradlew.bat integrationTest` | tests tagged `integration` (`@RequiresInstallation`), including the GPU render smoke tests | `sc4j.data`; a GPU for the smoke tests |
| `.\gradlew.bat corpusTest` | tests tagged `corpus` (`@CorpusTest`): long runs and tests over external replay and map folders | `sc4j.data`; a test whose folder property is not set is skipped |
| `.\gradlew.bat :sc4j-runtime:distributionTest` | builds the user package, unpacks it and starts its runtime headless | nothing outside the repository |

`integrationTest` fails before it starts when `sc4j.data` is not set or names no folder. The GPU render smoke tests run when the Gradle property `sc4j.render.smoke` is `true` (the default in `gradle.properties`); `-Psc4j.render.smoke=false` turns them off.

A single module or test:

```powershell
.\gradlew.bat :sc4j-engine-sim:test
.\gradlew.bat :mods:scr-demo:test --tests org.fogofwarlabs.sc4j.scrdemo.convert.ArtConverterTest
```

## Code style

Follow the code around your change first. The rules below are the ones the code base keeps everywhere.

**Layout**

- Four spaces, no tabs. Lines up to about 150 columns; break long argument lists by meaning, not by length.
- Braces on every `if`, `else`, `for`, `while` and `do` body, also for a single statement.
- Blank lines separate groups of statements by meaning. Simple assignments in a row line up their `=`.
- Class members in this order: constants, static fields, nested types, static methods, instance fields, constructors, instance methods.
- Keep a file's existing line endings.

**Names and types**

- Full words, no needless abbreviations (`index`, `image`, `packetLength`). `Id` is fine.
- Methods start with a verb: `get`, `find`, `read` to look up, `set` to change, `is`, `has`, `can` for checks, `create`, `of`, `from`, `parse` to make. Opposite actions use matching names (`readPacket` and `writePacket`).
- Meaningful numbers are named `static final` constants or enums: sizes, ids, flags, offsets, times. Plain `0`, `1` and `-1` may stay.
- Implementation classes are `final` unless they are meant to be extended. Helper classes are named `*Utils`, are `final` and have a private constructor. No `record` types: data objects are normal classes with explicit getters.
- Instance fields are set in the constructor and read through `this.`. Local variables have explicit types (no `var`).
- Every reference field, parameter and return value carries `@NotNull` or `@Nullable` (JetBrains annotations).

**Exceptions**

New exceptions come from the factory methods of `org.fogofwarlabs.sc4j.base.exception.Exceptions`, with indexed placeholders instead of string concatenation:

```java
throw Exceptions.newIllegalStateException("Invalid frame index: {0}", index);
```

**Javadoc and comments**

- Every public class and every public API method and constructor has Javadoc; `javadoc` runs without warnings.
- Javadoc and comments are in English. A summary starts with a verb in the third person (`Creates`, `Reads`, `Returns`). Use `{@code}` and `{@link}`, not Markdown.
- Tags in the order `@param`, `@return`, `@throws`; descriptions start in lower case without a final period; `@throws` says `if ...`.
- Comments explain what the code does and why. They do not tell the history of a change.

**Engine rules**

- The engine-side modules name no mod: the build fails when they depend on or mention the SC:R mod (`checkModBoundary`).
- Simulation code is deterministic: no clock, no `java.util` random generator, no hash-ordered collection the simulation sees, no floating point ([mod author guide, chapter 12](docs/mod-guide.md#12-determinism)).
- A change of the engine API updates its listings: see the [engine API policy](docs/api-policy.md#6-updating-the-listings).

**Tests**

Test classes are named `<Subject>Test`; test methods use `snake_case` names that say what they check.

## Commits and pull requests

- One topic per commit. The first line is a short English summary; the body says what changed and why.
- Before a pull request, `.\gradlew.bat build` passes. If the change touches the SC:R mod and you have the installation, run `integrationTest` too. Name the tiers you ran in the pull request.
- An API change commits the regenerated `.api` files with the change and states its version effect in the commit message.
- Behaviour changes come with tests. Do not commit build output, local paths or game data.
- New art, sound or other assets need a licence that allows redistribution in a mod. Add the licence files and a notice next to them and an entry in [NOTICE](NOTICE).
- The CI workflow runs the unit tests (`test`) of every pull request into `main` or `develop`; its check `test` must pass before the merge.

## Releases

`develop` holds the next version as `X.Y.Z-SNAPSHOT` and publishes snapshots to the Central Portal. `main` holds release versions and publishes each push to Maven Central with the tag `vX.Y.Z`.

A release is a pull request from `develop` into `main` that sets `version=X.Y.Z` in `gradle.properties`.

## Licence of contributions

SC4J is licensed under the [Apache License 2.0](LICENSE). Unless you state otherwise, everything you submit for inclusion is licensed under the same terms (section 5 of the License).

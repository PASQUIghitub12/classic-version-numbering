# Classic Version Numbering

A small **client-side** Minecraft mod that shows classic-style version numbers
(`1.22.3`, `1.23.1`, …) instead of the year-based identifiers (`26.x`, `27.x`) in
the places where the running client shows its own version. The final component of
the real version is preserved: `26.3` is shown as `1.22.3`, `27.4` as `1.23.4`.

The real game version is untouched. Networking, save format, crash reports, mod
metadata and the protocol version are all unaffected.

- Target game version: **Minecraft 26.3** (Java 25). Both loaders declare support
  for 26.3 only (Fabric pins `26.3`; NeoForge allows `[26.3, 26.4)`).
- Loaders: **Fabric** (`0.19.5`, Loom `1.19.1`) and **NeoForge** (`26.3.0.92`,
  ModDevGradle `2.0.148`)
- Default mapping: `26.x → 1.22.x`, `27.x → 1.23.x` (trailing component preserved)

## What changes

Only the *displayed* version text changes, and only in these verified locations
(Minecraft 26.3, checked against decompiled sources):

| Screen / surface | Where | Result |
| --- | --- | --- |
| Main menu version label (bottom-left) | `TitleScreen.extractRenderState` | `Minecraft 1.22.3` |
| F3 debug overlay, first line | `DebugEntryVersion.display` | `Minecraft 1.22.3 (…)` |
| OS window title bar | `Minecraft.createTitle` | `Minecraft 1.22.3` |
| World-upgrade warning | `WorldOpenFlows.openWorldCheckVersionCompatibility` | `… playing version 1.22.3` |

See [`docs/compatibility.md`](docs/compatibility.md) for the full list of what is
deliberately **not** changed (network headers, server-reported versions, crash
reports, launcher UIs, …).

## Install

1. Build the jar for your loader (see below).
2. Drop it into your `mods/` folder.
3. Launch Minecraft 26.3 with Fabric or NeoForge.

On first launch the mod writes `config/classic-version-numbering.json`. Edit it
and restart the client to change the mapping. See
[`docs/configuration.md`](docs/configuration.md).

## Building

Requirements: **JDK 25** (Minecraft 26.3 targets Java 25). The Gradle wrapper is
included; Gradle itself must run on Java 25.

```powershell
# From the repository root, with JAVA_HOME pointing at a JDK 25
$env:JAVA_HOME = "C:\path\to\jdk-25"
.\gradlew.bat :common:test        # run the unit tests (plain JVM, no Minecraft needed)
.\gradlew.bat :fabric:build       # -> fabric/build/libs/classic_version_numbering-fabric-1.0.0.jar
.\gradlew.bat :neoforge:build     # -> neoforge/build/libs/classic_version_numbering-neoforge-1.0.0.jar
```

`.\gradlew.bat build` builds everything, including the tests.

The toolchain is declared with `languageVersion = 25`, and the
`foojay-resolver-convention` plugin is applied, so Gradle can auto-provision a
JDK 25 if one is not installed. Gradle itself still has to start on Java 25.

## Project layout

| Path | Contents |
| --- | --- |
| `common/` | Loader-independent mapping logic (`ParsedVersion`, `VersionRule`, `VersionMappingRegistry`, `ModConfig`, `ConfigLoader`, `ClassicVersion`) plus the unit tests. No Minecraft dependency. |
| `shared-mixins/` | Loader-agnostic (Mojmap) mixins, `MappedWorldVersion`, and the shared `classic-version-numbering.mixins.json`. |
| `fabric/` | Fabric entry point, `fabric.mod.json`, Loom build. |
| `neoforge/` | NeoForge entry point, `neoforge.mods.toml`, ModDevGradle build. |
| `docs/` | Configuration schema and compatibility notes. |

Both loader modules compile the `common` and `shared-mixins` sources directly
into their own jar (via `srcDir`), so each jar is self-contained without shading
or jar-in-jar packaging.

## Testing

```powershell
.\gradlew.bat :common:test
```

The suite (58 tests) covers the mapping contract: `26.1 → 1.22.1`,
`26.2 → 1.22.2`, `26.3 → 1.22.3`, `26.4 → 1.22.4`, `27.1 → 1.23.1`,
`27.4 → 1.23.4`, `26.1.2 → 1.22.1.2`, exact-rule precedence, wildcard
specificity and tail-segment preservation, release candidates and pre-releases
(suffix preserved), snapshots and weekly snapshots (never mapped), unknown future
versions, unrelated strings, `null`/empty input, malformed configuration and
duplicate/conflicting rules, empty or invalid mapping arrays, schema-version
fallbacks, whitespace tolerance and exact-rule pre-release suffixes.

## Status

- Unit tests: passing (58/58).
- `:fabric:build` and `:neoforge:build`: passing; both jars verified to contain
  the expected classes, mixin config and metadata.
- **In-game behaviour has not been verified by launching the game.** The mixin
  targets were verified statically against the decompiled 26.3 sources and the
  NeoForge-patched jar, but no client was started. See
  [`docs/compatibility.md`](docs/compatibility.md).

## License

MIT — see [`LICENSE`](LICENSE).

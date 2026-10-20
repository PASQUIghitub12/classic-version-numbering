# Compatibility and injection notes

This document records exactly which version strings the mod rewrites, which ones
it deliberately leaves alone, and how the targets were verified.

## Guiding rules

1. The real game version is never changed. `SharedConstants.getCurrentVersion()`
   is untouched; only its **displayed name** is wrapped, at four call sites.
2. The wrapper (`MappedWorldVersion`) delegates **everything** to the real
   version except `name()`. As a result `id()`, `dataVersion()`,
   `protocolVersion()`, `packVersion(...)`, `buildTime()` and `stable()` all keep
   their real values.
3. Nothing that is sent over the network, written to disk, or included in a crash
   report is modified.

## Injection points (verified for Minecraft 26.3)

Targets were verified by decompiling the official 26.3 client (Fabric Loom
`genSources`) and by inspecting NeoForge's patched `26.3.0.92` merged jar. In
each case the `SharedConstants.getCurrentVersion()` call appears exactly once in
the targeted method.

| Mixin | Target | Rewrites |
| --- | --- | --- |
| `TitleScreenMixin` | `TitleScreen.extractRenderState(GuiGraphicsExtractor, int, int, float)` | Main-menu version label |
| `DebugEntryVersionMixin` | `DebugEntryVersion.display(DebugScreenDisplayer, Level, LevelChunk, LevelChunk)` | First line of the F3 overlay |
| `MinecraftWindowTitleMixin` | `Minecraft.createTitle()` | OS window title |
| `WorldOpenFlowsMixin` | `WorldOpenFlows.openWorldCheckVersionCompatibility(...)` | Current-version half of the world-upgrade warning |

Each mixin uses `@Redirect` on the `getCurrentVersion()` invocation, so only the
one local expression is affected — never the surrounding `"Minecraft "` prefix,
the launched version, the client brand, or any other string.

`@Redirect` (core Mixin) is used instead of `@ModifyExpressionValue` because the
Mixin build bundled with 26.3 does not ship that annotation. `JAVA_25` is a valid
`compatibilityLevel` for the bundled Mixin, and no refmap is required because the
maintainers ship the 26.x game unobfuscated.

## Deliberately not changed

Every other client-side use of `SharedConstants.getCurrentVersion()` was
inspected. The following are intentionally left alone:

| Location | Reason |
| --- | --- |
| `DownloadedPackSource.createDownloadHeaders()` — `X-Minecraft-Version` | This is an **HTTP header** sent when downloading server resource packs. Changing it would be a network change. |
| `RenderSystem` — SDL app metadata (`SDL.app.metadata.version`) | OS/SDL metadata, set during early startup, not a game screen. |
| `Main` — `TracyClient.reportAppInfo(...)` | Profiler metadata, not user interface. |
| `IntegratedServer` log line | Log output only. |
| `ServerData.version` initial value and `ServerStatusPinger` | The multiplayer server list shows the **server's** version (reported over the network). The only client-version text is a transient placeholder that is replaced as soon as the server responds, so it is left as the real value to avoid flicker and to avoid rewriting network-reported data. |
| `WorldSelectionList` / `LevelSummary` world entries | These show the **saved world's** version, not the running client's. |
| `PauseScreen`, `CreateWorldScreen$GameTab` | They only check `stable()` / `dataVersion().isSideSeries()` to enable buttons; no version text is displayed. |
| Crash reports (`SystemReport`), `Options` diagnostics file, save data | Must keep the real version for support and compatibility. |

### Launcher UI

Third-party launchers (including the one used during development) render the game
version **before the mod is loaded**, using their own metadata. That text cannot
be changed by a client-side game mod and is out of scope.

## Verification status

* Mixin targets were checked against decompiled 26.3 sources and the
  NeoForge-patched jar; the expected methods and single call sites are present.
* `:common:test` passes (58 tests).
* `:fabric:build` and `:neoforge:build` both succeed, and the produced jars
  contain the expected classes, `classic-version-numbering.mixins.json`, icon and
  metadata (with `${version}` / `${mod_version}` substituted).
* **The game has not been launched.** No client was started, so the runtime
  behaviour of the mixins (and the exact rendered text) has not been observed in
  game. The mixins are written to fail loudly (`defaultRequire: 1`) if a target
  cannot be found, rather than silently doing nothing.

## Version-specific nature

The injection targets reference concrete 26.3 method names. A future Minecraft
release that renames these methods will require updating the mixin targets. Each
mixin declares `require = 1` semantics through `defaultRequire`, so a missing
target surfaces as a clear mixin error instead of a silent no-op.

To match the verified target, the loader metadata declares 26.3 support only:
`fabric.mod.json` depends on `minecraft: "26.3"` (exact), and
`neoforge.mods.toml` allows `"[26.3,26.4)"` — the same range the mixin targets
were verified against.

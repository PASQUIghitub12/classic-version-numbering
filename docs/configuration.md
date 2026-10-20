# Configuration

Classic Version Numbering reads a single JSON file:

```
config/classic-version-numbering.json
```

The file is created with the default contents on first launch if it does not
exist. It is read **once**, when the client starts; there is no hot reload.
Restart the client after editing it.

## Schema

```json
{
  "schemaVersion": 1,
  "enabled": true,
  "mapPreReleases": true,
  "mappings": [
    { "match": "26.x", "display": "1.22" },
    { "match": "27.x", "display": "1.23" }
  ]
}
```

| Field | Type | Default | Meaning |
| --- | --- | --- | --- |
| `schemaVersion` | integer | `1` | Format version. Newer values are accepted with a warning; unknown fields are ignored. |
| `enabled` | boolean | `true` | Master switch. When `false`, every version string is left exactly as the game produced it. |
| `mapPreReleases` | boolean | `true` | When `true`, pre-releases and release candidates keep their marker (`26.3-rc-1 → 1.22.3-rc-1`). When `false`, they are left unchanged. |
| `mappings` | array | `26.x/27.x` | Ordered list of `{ "match", "display" }` rules. |

### Rules

* `match` is a dotted version pattern such as `26.x`, `26.1`, `27.2.x`.
  * Segments are matched **whole and anchored**: `1.21` never matches `11.21`
    and never matches `1.21.4`.
  * A trailing `x` or `*` is a wildcard that matches **one or more** remaining
    segments: `26.x` matches `26.1`, `26.2`, `26.3.5`, … but not `26`.
  * Only numeric segments and the trailing wildcard are accepted.
* `display` must look like a classic version: two or more dotted numeric
  segments, e.g. `1.22` or `1.22.1`.
* **The segments consumed by a wildcard are appended to `display`.** This is what
  preserves the final version component: with the default `26.x → 1.22` rule,
  `26.3` is shown as `1.22.3`, `26.4` as `1.22.4` and `26.1.2` as `1.22.1.2`.
  Exact rules consume nothing, so their `display` is used unchanged (a `26.2`
  rule with `display` `1.30` always shows exactly `1.30`).
* **Exact rules win over wildcards.** Among wildcards, the most specific (most
  literal segments) wins; ties are resolved in file order.
* The **first** rule for a given pattern wins. A later rule that redefines the
  same pattern is ignored (with a warning) so conflicting duplicates can never
  change behaviour silently.

### What is never mapped

These are returned exactly as-is, regardless of the rules:

* Development snapshots (`26.3-snapshot-5`).
* Weekly snapshots (`25w46a`, `26w14a`).
* Unrecognised strings (`26.3-beta-1`, `Bedrock 1.21`, free text, …).
* Anything with no matching rule (for example future `28.1`).

Release candidates and pre-releases are **never** shown as a finished release:
the original `-rc-N` / `-pre-N` marker is preserved and appended to the mapped
value (unless `mapPreReleases` is `false`).

## Validation and failure handling

The loader never throws and never disables the mod:

* Missing file → the default file is written and the built-in rules are used.
* Unreadable / invalid JSON / non-object root → defaults are used, with a warning.
* Invalid individual entries (wrong type, missing fields, bad pattern or
  display) → that entry is skipped and logged.
* All entries invalid → the built-in default rules are used.
* Wrong types for `enabled` / `mapPreReleases` / `schemaVersion` → the default
  for that field is used.

Warnings are written to the client log under the `ClassicVersionNumbering`
logger.

## Examples

Wildcard rules append the matched trailing segments to `display`; exact rules do
not — so an exact `27.1 → 1.30` always shows exactly `1.30`, while `27.3` under
`27.x → 1.23` shows as `1.23.3`.

Map the whole 27 series to 1.23 but force `27.1` to 1.30:

```json
{
  "mappings": [
    { "match": "27.1", "display": "1.30" },
    { "match": "27.x", "display": "1.23" }
  ]
}
```

Disable the mod without removing it:

```json
{ "enabled": false }
```

Show `rc`/`pre` builds as their real year-based version:

```json
{ "mapPreReleases": false }
```

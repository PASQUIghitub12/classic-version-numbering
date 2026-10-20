package dev.classicversion.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for classic-style version mappings.
 *
 * <p>All user-facing version labels must go through this single registry so the
 * mapping rules only exist in one place.</p>
 *
 * <p>Matching order:</p>
 * <ol>
 *   <li>Exact rules (no wildcard) are tried first, in configuration order.</li>
 *   <li>Wildcard rules are tried next, most specific first, then configuration order.</li>
 *   <li>If nothing matches, the original string is returned unchanged.</li>
 * </ol>
 *
 * <p>Suffix policy:</p>
 * <ul>
 *   <li>Stable releases are mapped to the display value with the segments consumed
 *       by a wildcard appended, so the trailing version component is preserved
 *       ({@code 26.3} becomes {@code 1.22.3} and {@code 26.1.2} becomes
 *       {@code 1.22.1.2}); exact rules map to their display value unchanged.</li>
 *   <li>Pre-releases and release candidates keep their original suffix appended to
 *       the mapped value (for example {@code 26.3-rc-1} becomes
 *       {@code 1.22.3-rc-1}), so a pre-release is never shown as a stable release.
 *       This can be disabled with {@code mapPreReleases = false}.</li>
 *   <li>Snapshots and unrecognised strings are never mapped.</li>
 * </ul>
 */
public final class VersionMappingRegistry {

    /**
     * The mappings shipped as defaults: {@code 26.x -> 1.22.x} and
     * {@code 27.x -> 1.23.x}. The trailing {@code .x} is not part of the literal
     * display; it is the version's trailing component appended by the wildcard, so
     * {@code 26.3} is displayed as {@code 1.22.3}.
     */
    public static final List<VersionRule> DEFAULT_RULES = List.of(
            VersionRule.parse("26.x", "1.22"),
            VersionRule.parse("27.x", "1.23"));

    private final boolean enabled;
    private final boolean mapPreReleases;
    private final List<VersionRule> exactRules;
    private final List<VersionRule> wildcardRules;

    private VersionMappingRegistry(boolean enabled, boolean mapPreReleases, List<VersionRule> rules) {
        this.enabled = enabled;
        this.mapPreReleases = mapPreReleases;

        // Keep the first occurrence of a pattern so that conflicting duplicate
        // rules cannot produce surprising results, and so that order is stable.
        Map<String, VersionRule> unique = new LinkedHashMap<>();
        for (VersionRule rule : rules) {
            unique.putIfAbsent(rule.pattern(), rule);
        }

        List<VersionRule> exact = new ArrayList<>();
        List<VersionRule> wildcards = new ArrayList<>();
        for (VersionRule rule : unique.values()) {
            if (rule.isWildcard()) {
                wildcards.add(rule);
            } else {
                exact.add(rule);
            }
        }
        // Stable sort: specificity descending, configuration order preserved for ties.
        wildcards.sort(Comparator.comparingInt(VersionRule::specificity).reversed());

        this.exactRules = List.copyOf(exact);
        this.wildcardRules = List.copyOf(wildcards);
    }

    public static VersionMappingRegistry defaults() {
        return new VersionMappingRegistry(true, true, DEFAULT_RULES);
    }

    public static VersionMappingRegistry of(boolean enabled, boolean mapPreReleases, List<VersionRule> rules) {
        return new VersionMappingRegistry(enabled, mapPreReleases, rules == null ? List.of() : rules);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean mapsPreReleases() {
        return mapPreReleases;
    }

    public List<VersionRule> exactRules() {
        return exactRules;
    }

    public List<VersionRule> wildcardRules() {
        return wildcardRules;
    }

    /**
     * Maps a version string to its classic-style display value.
     *
     * @param rawVersion the real Minecraft version string, may be {@code null}
     * @return the display value, or the original string whenever the mapping is
     *         disabled, unknown, ambiguous or unsafe
     */
    public String map(String rawVersion) {
        if (rawVersion == null || rawVersion.isEmpty() || !enabled) {
            return rawVersion;
        }

        ParsedVersion version = ParsedVersion.parse(rawVersion);
        if (!version.isEligibleForMapping()) {
            return rawVersion;
        }

        VersionRule rule = findRule(version);
        if (rule == null) {
            return rawVersion;
        }
        // The display value plus the trailing segments the wildcard consumed.
        String mappedBase = rule.displayFor(version);
        if (version.kind() == ParsedVersion.Kind.RELEASE) {
            return mappedBase;
        }
        if (!mapPreReleases) {
            return rawVersion;
        }
        // Preserve the original pre-release / release-candidate marker so that a
        // development build is never displayed as a finished release.
        return mappedBase + "-" + version.suffix();
    }

    private VersionRule findRule(ParsedVersion version) {
        for (VersionRule rule : exactRules) {
            if (rule.matches(version)) {
                return rule;
            }
        }
        for (VersionRule rule : wildcardRules) {
            if (rule.matches(version)) {
                return rule;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "VersionMappingRegistry[enabled=" + enabled
                + ", mapPreReleases=" + mapPreReleases
                + ", rules=" + exactRules + " + " + wildcardRules + "]";
    }
}

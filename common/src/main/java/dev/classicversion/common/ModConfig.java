package dev.classicversion.common;

import java.util.List;

/**
 * Immutable in-memory representation of the mod configuration.
 *
 * <p>Loaded from {@code config/classic-version-numbering.json} by
 * {@link ConfigLoader}. See {@code docs/configuration.md} for the JSON schema.</p>
 */
public final class ModConfig {

    /** Current schema version understood by this build. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final int schemaVersion;
    private final boolean enabled;
    private final boolean mapPreReleases;
    private final List<VersionRule> rules;

    public ModConfig(int schemaVersion, boolean enabled, boolean mapPreReleases, List<VersionRule> rules) {
        this.schemaVersion = schemaVersion;
        this.enabled = enabled;
        this.mapPreReleases = mapPreReleases;
        this.rules = List.copyOf(rules == null ? List.of() : rules);
    }

    public static ModConfig defaults() {
        return new ModConfig(CURRENT_SCHEMA_VERSION, true, true, VersionMappingRegistry.DEFAULT_RULES);
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean mapPreReleases() {
        return mapPreReleases;
    }

    public List<VersionRule> rules() {
        return rules;
    }

    public VersionMappingRegistry toRegistry() {
        return VersionMappingRegistry.of(enabled, mapPreReleases, rules);
    }
}

package dev.classicversion.common;

/**
 * Process-wide entry point for version display mapping.
 *
 * <p>Every user-facing hook calls {@link #map(String)}, so the mapping rules and
 * their precedence live in exactly one place ({@link VersionMappingRegistry}).</p>
 *
 * <p>The registry defaults to the built-in rules, which means display mapping is
 * already active during early client startup (for example while building the
 * window title) even if the configuration file has not been read yet.</p>
 */
public final class ClassicVersion {

    private static volatile VersionMappingRegistry registry = VersionMappingRegistry.defaults();

    private ClassicVersion() {
    }

    /** Replaces the active registry. Passing {@code null} restores the defaults. */
    public static void setRegistry(VersionMappingRegistry newRegistry) {
        registry = newRegistry == null ? VersionMappingRegistry.defaults() : newRegistry;
    }

    /** The active registry, never {@code null}. */
    public static VersionMappingRegistry registry() {
        return registry;
    }

    public static boolean isEnabled() {
        return registry.isEnabled();
    }

    /**
     * Maps a real Minecraft version string to the classic-style value shown in the
     * user interface.
     *
     * @param version the real version string, may be {@code null}
     * @return the display value, or the original value whenever mapping is
     *         disabled, unknown or unsafe
     */
    public static String map(String version) {
        String mapped = registry.map(version);
        return mapped == null ? version : mapped;
    }
}

package dev.classicversion.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Reads and validates {@code classic-version-numbering.json}.
 *
 * <p>The loader is intentionally forgiving: a missing, unreadable or malformed
 * file never throws and never disables the mod. Every problem is reported to the
 * supplied warning sink and the affected setting falls back to a safe default.</p>
 *
 * <p>The file is only read when the client starts; there is no hot reload. See
 * {@code docs/configuration.md} for the schema and reload behaviour.</p>
 */
public final class ConfigLoader {

    /** File name looked up inside the loader's config directory. */
    public static final String FILE_NAME = "classic-version-numbering.json";

    private static final String DEFAULT_JSON = """
            {
              "schemaVersion": 1,
              "enabled": true,
              "mapPreReleases": true,
              "mappings": [
                { "match": "26.x", "display": "1.22" },
                { "match": "27.x", "display": "1.23" }
              ]
            }
            """;

    private ConfigLoader() {
    }

    /** The JSON written when no config file exists yet. */
    public static String defaultJson() {
        return DEFAULT_JSON;
    }

    /**
     * Loads the configuration from disk, creating the default file when it does
     * not exist. Never throws.
     *
     * @param configFile the config file path, may be {@code null}
     * @param warnings   sink for human-readable warnings, may be {@code null}
     * @return the loaded configuration, or {@link ModConfig#defaults()} when the
     *         file is missing or cannot be read
     */
    public static ModConfig loadOrCreate(Path configFile, Consumer<String> warnings) {
        Consumer<String> warn = warnings == null ? message -> {
        } : warnings;
        if (configFile == null) {
            warn.accept("No configuration path was provided; using built-in defaults.");
            return ModConfig.defaults();
        }
        try {
            if (!Files.exists(configFile)) {
                Path parent = configFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(configFile, DEFAULT_JSON, StandardCharsets.UTF_8);
                return ModConfig.defaults();
            }
            return parse(Files.readString(configFile, StandardCharsets.UTF_8), warn);
        } catch (IOException | RuntimeException e) {
            warn.accept("Could not read " + configFile + " (" + e + "); using built-in defaults.");
            return ModConfig.defaults();
        }
    }

    /**
     * Parses configuration JSON. This method never throws: malformed documents
     * and invalid entries fall back to defaults.
     *
     * @param json     the raw JSON text, may be {@code null} or blank
     * @param warnings sink for human-readable warnings, may be {@code null}
     */
    public static ModConfig parse(String json, Consumer<String> warnings) {
        Consumer<String> warn = warnings == null ? message -> {
        } : warnings;
        if (json == null || json.isBlank()) {
            warn.accept("Configuration is empty; using built-in defaults.");
            return ModConfig.defaults();
        }

        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            warn.accept("Configuration is not valid JSON (" + e.getMessage() + "); using built-in defaults.");
            return ModConfig.defaults();
        }
        if (!root.isJsonObject()) {
            warn.accept("Configuration root must be a JSON object; using built-in defaults.");
            return ModConfig.defaults();
        }
        JsonObject object = root.getAsJsonObject();

        int schemaVersion = readInt(object, "schemaVersion", ModConfig.CURRENT_SCHEMA_VERSION, warn);
        if (schemaVersion > ModConfig.CURRENT_SCHEMA_VERSION) {
            warn.accept("Configuration schemaVersion " + schemaVersion
                    + " is newer than this build understands (" + ModConfig.CURRENT_SCHEMA_VERSION
                    + "); unknown fields are ignored.");
        } else if (schemaVersion < 1) {
            warn.accept("Configuration schemaVersion " + schemaVersion + " is invalid; assuming 1.");
            schemaVersion = ModConfig.CURRENT_SCHEMA_VERSION;
        }

        boolean enabled = readBoolean(object, "enabled", true, warn);
        boolean mapPreReleases = readBoolean(object, "mapPreReleases", true, warn);
        List<VersionRule> rules = readRules(object, warn);

        return new ModConfig(schemaVersion, enabled, mapPreReleases, rules);
    }

    private static List<VersionRule> readRules(JsonObject object, Consumer<String> warn) {
        JsonElement mappings = object.get("mappings");
        if (mappings == null || !mappings.isJsonArray()) {
            warn.accept("'mappings' is missing or not an array; using the default 26.x/27.x rules.");
            return VersionMappingRegistry.DEFAULT_RULES;
        }

        JsonArray array = mappings.getAsJsonArray();
        // Preserve configuration order while rejecting duplicate patterns. The
        // first rule for a pattern wins, so conflicting duplicates are ignored
        // instead of silently changing behaviour.
        Map<String, VersionRule> byPattern = new LinkedHashMap<>();
        int index = 0;
        for (JsonElement element : array) {
            index++;
            if (!element.isJsonObject()) {
                warn.accept("mappings[" + index + "] is not an object; entry skipped.");
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String match = readString(entry, "match");
            String display = readString(entry, "display");
            if (match == null || display == null) {
                warn.accept("mappings[" + index + "] must have string 'match' and 'display' fields; entry skipped.");
                continue;
            }
            VersionRule rule;
            try {
                rule = VersionRule.parse(match, display);
            } catch (IllegalArgumentException e) {
                warn.accept("mappings[" + index + "] (" + match + " -> " + display + ") is invalid: "
                        + e.getMessage() + "; entry skipped.");
                continue;
            }
            VersionRule existing = byPattern.get(rule.pattern());
            if (existing != null) {
                if (!existing.display().equals(rule.display())) {
                    warn.accept("mappings[" + index + "] redefines '" + rule.pattern() + "' as '"
                            + rule.display() + "' but it is already mapped to '" + existing.display()
                            + "'; the first definition is kept.");
                }
                continue;
            }
            byPattern.put(rule.pattern(), rule);
        }

        if (byPattern.isEmpty()) {
            warn.accept("No valid mappings were found; using the default 26.x/27.x rules.");
            return VersionMappingRegistry.DEFAULT_RULES;
        }
        return new ArrayList<>(byPattern.values());
    }

    private static int readInt(JsonObject object, String key, int fallback, Consumer<String> warn) {
        JsonElement element = object.get(key);
        if (element == null) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException e) {
            warn.accept("'" + key + "' must be a number; using " + fallback + ".");
            return fallback;
        }
    }

    private static boolean readBoolean(JsonObject object, String key, boolean fallback, Consumer<String> warn) {
        JsonElement element = object.get(key);
        if (element == null) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            warn.accept("'" + key + "' must be true or false; using " + fallback + ".");
            return fallback;
        }
        return element.getAsBoolean();
    }

    private static String readString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return null;
        }
        String value = element.getAsString().trim();
        return value.isEmpty() ? null : value;
    }
}

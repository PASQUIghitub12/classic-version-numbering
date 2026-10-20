package dev.classicversion.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Robustness tests for configuration loading. */
class ConfigLoaderTest {

    private final List<String> warnings = new ArrayList<>();

    private ModConfig parse(String json) {
        return ConfigLoader.parse(json, warnings::add);
    }

    @Test
    void parsesValidConfiguration() {
        ModConfig config = parse("""
                {
                  "schemaVersion": 1,
                  "enabled": false,
                  "mapPreReleases": false,
                  "mappings": [
                    { "match": "26.x", "display": "1.22" },
                    { "match": "27.x", "display": "1.23" }
                  ]
                }
                """);
        assertEquals(ModConfig.CURRENT_SCHEMA_VERSION, config.schemaVersion());
        assertFalse(config.enabled());
        assertFalse(config.mapPreReleases());
        assertEquals(2, config.rules().size());
        assertTrue(warnings.isEmpty(), "expected no warnings but got " + warnings);
    }

    @Test
    void missingFileIsHandledAndDefaultsWritten(@TempDir Path dir) {
        Path file = dir.resolve("nested").resolve(ConfigLoader.FILE_NAME);
        ModConfig config = ConfigLoader.loadOrCreate(file, warnings::add);
        assertEquals(ModConfig.defaults().rules(), config.rules());
        assertTrue(Files.exists(file), "default config should have been created");
    }

    @Test
    void unreadableOrMalformedFileFallsBackToDefaults(@TempDir Path dir) throws Exception {
        Path file = dir.resolve(ConfigLoader.FILE_NAME);
        Files.writeString(file, "{ this is not json ]");
        ModConfig config = ConfigLoader.loadOrCreate(file, warnings::add);
        assertEquals(ModConfig.defaults().rules(), config.rules());
        assertTrue(config.enabled());
        assertFalse(warnings.isEmpty());
    }

    @Test
    void nullAndBlankInputUseDefaults() {
        assertEquals(ModConfig.defaults().rules(), parse(null).rules());
        assertEquals(ModConfig.defaults().rules(), parse("").rules());
        assertEquals(ModConfig.defaults().rules(), parse("   ").rules());
    }

    @Test
    void nonObjectRootUsesDefaults() {
        assertEquals(ModConfig.defaults().rules(), parse("[1, 2, 3]").rules());
        assertEquals(ModConfig.defaults().rules(), parse("\"just a string\"").rules());
        assertFalse(warnings.isEmpty());
    }

    @Test
    void malformedEntriesAreSkippedAndValidOnesKept() {
        ModConfig config = parse("""
                {
                  "mappings": [
                    "not an object",
                    { "match": "26.x" },
                    { "match": "bad.x", "display": "1.22" },
                    { "match": "26.x", "display": "not-a-version" },
                    { "match": "27.x", "display": "1.23" }
                  ]
                }
                """);
        assertEquals(1, config.rules().size());
        assertEquals("27.x", config.rules().get(0).pattern());
        assertFalse(warnings.isEmpty());
    }

    @Test
    void allInvalidMappingsFallBackToDefaults() {
        ModConfig config = parse("""
                { "mappings": [ { "match": "bad", "display": "x" } ] }
                """);
        assertEquals(VersionMappingRegistry.DEFAULT_RULES, config.rules());
        assertFalse(warnings.isEmpty());
    }

    @Test
    void missingMappingsUsesDefaults() {
        ModConfig config = parse("{}");
        assertEquals(VersionMappingRegistry.DEFAULT_RULES, config.rules());
        assertTrue(config.enabled());
        assertTrue(config.mapPreReleases());
    }

    @Test
    void emptyMappingsArrayFallsBackToDefaults() {
        ModConfig config = parse("{ \"mappings\": [] }");
        assertEquals(VersionMappingRegistry.DEFAULT_RULES, config.rules());
        assertFalse(warnings.isEmpty());
    }

    @Test
    void zeroOrNegativeSchemaVersionIsResetToOne() {
        ModConfig zero = parse("{ \"schemaVersion\": 0 }");
        assertEquals(ModConfig.CURRENT_SCHEMA_VERSION, zero.schemaVersion());
        ModConfig negative = parse("{ \"schemaVersion\": -3 }");
        assertEquals(ModConfig.CURRENT_SCHEMA_VERSION, negative.schemaVersion());
        assertFalse(warnings.isEmpty());
    }

    @Test
    void duplicatePatternsKeepTheFirstDefinition() {
        ModConfig config = parse("""
                {
                  "mappings": [
                    { "match": "26.x", "display": "1.22" },
                    { "match": "26.x", "display": "9.9" }
                  ]
                }
                """);
        assertEquals(1, config.rules().size());
        assertEquals("1.22", config.rules().get(0).display());
        assertFalse(warnings.isEmpty());
    }

    @Test
    void wrongFieldTypesUseDefaults() {
        ModConfig config = parse("""
                { "enabled": "yes", "mapPreReleases": 3, "schemaVersion": "one" }
                """);
        assertTrue(config.enabled());
        assertTrue(config.mapPreReleases());
        assertEquals(ModConfig.CURRENT_SCHEMA_VERSION, config.schemaVersion());
        assertFalse(warnings.isEmpty());
    }

    @Test
    void newerSchemaVersionIsAcceptedWithAWarning() {
        ModConfig config = parse("""
                { "schemaVersion": 99, "enabled": false }
                """);
        assertEquals(99, config.schemaVersion());
        assertFalse(config.enabled());
        assertFalse(warnings.isEmpty());
    }

    @Test
    void configRoundTripsThroughRegistry() {
        ModConfig config = parse("""
                { "mappings": [ { "match": "26.1", "display": "1.30" }, { "match": "26.x", "display": "1.22" } ] }
                """);
        VersionMappingRegistry registry = config.toRegistry();
        assertEquals("1.30", registry.map("26.1"));
        assertEquals("1.22.2", registry.map("26.2"));
    }

    @Test
    void nullWarningSinkIsTolerated() {
        assertEquals(VersionMappingRegistry.DEFAULT_RULES,
                ConfigLoader.parse("{ bad json", null).rules());
    }
}

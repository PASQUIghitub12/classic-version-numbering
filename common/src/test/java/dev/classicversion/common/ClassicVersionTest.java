package dev.classicversion.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the process-wide facade used by the UI hooks. */
class ClassicVersionTest {

    @AfterEach
    void restoreDefaults() {
        ClassicVersion.setRegistry(null);
    }

    @Test
    void defaultsAreActiveBeforeConfigurationLoads() {
        assertEquals("1.22.3", ClassicVersion.map("26.3"));
        assertTrue(ClassicVersion.isEnabled());
    }

    @Test
    void nullVersionPassesThrough() {
        assertEquals(null, ClassicVersion.map(null));
    }

    @Test
    void installingAnEmptyRegistryIsSafe() {
        ClassicVersion.setRegistry(VersionMappingRegistry.of(true, true, java.util.List.of()));
        assertEquals("26.3", ClassicVersion.map("26.3"));
    }

    @Test
    void nullRegistryRestoresDefaults() {
        ClassicVersion.setRegistry(VersionMappingRegistry.of(false, true, java.util.List.of()));
        assertFalse(ClassicVersion.isEnabled());
        ClassicVersion.setRegistry(null);
        assertTrue(ClassicVersion.isEnabled());
        assertEquals("1.22.3", ClassicVersion.map("26.3"));
    }
}

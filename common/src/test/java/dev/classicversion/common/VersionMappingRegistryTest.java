package dev.classicversion.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the shipped default mappings and the mapping rules required by the brief. */
class VersionMappingRegistryTest {

    private final VersionMappingRegistry defaults = VersionMappingRegistry.defaults();

    @Test
    void mapsTwentySixFamilyPreservingTrailingComponent() {
        assertEquals("1.22.1", defaults.map("26.1"));
        assertEquals("1.22.2", defaults.map("26.2"));
        assertEquals("1.22.3", defaults.map("26.3"));
        assertEquals("1.22.4", defaults.map("26.4"));
    }

    @Test
    void mapsTwentySevenFamilyPreservingTrailingComponent() {
        assertEquals("1.23.1", defaults.map("27.1"));
        assertEquals("1.23.2", defaults.map("27.2"));
        assertEquals("1.23.4", defaults.map("27.4"));
    }

    @Test
    void wildcardAppendsAllConsumedTrailingSegments() {
        assertEquals("1.22.1.2", defaults.map("26.1.2"));
        assertEquals("1.22.3.5", defaults.map("26.3.5"));
        assertEquals("1.22.0", defaults.map("26.0"));
    }

    @Test
    void unknownFutureVersionsAreLeftAlone() {
        assertEquals("28.1", defaults.map("28.1"));
        assertEquals("30.0", defaults.map("30.0"));
    }

    @Test
    void unrelatedStringsAreNeverTransformed() {
        assertEquals("1.21.4", defaults.map("1.21.4"));
        assertEquals("1.21", defaults.map("1.21"));
        assertEquals("Bedrock 1.21", defaults.map("Bedrock 1.21"));
        assertEquals("126.1", defaults.map("126.1"));
        assertEquals("26", defaults.map("26"));
        assertEquals("not a version", defaults.map("not a version"));
        assertEquals("26.3!", defaults.map("26.3!"));
    }

    @Test
    void releaseCandidatesKeepTheirMarker() {
        assertEquals("1.22.3-rc-1", defaults.map("26.3-rc-1"));
        assertEquals("1.23.1-rc-2", defaults.map("27.1-rc-2"));
    }

    @Test
    void preReleasesKeepTheirMarker() {
        assertEquals("1.22.3-pre-2", defaults.map("26.3-pre-2"));
    }

    @Test
    void snapshotsAreNeverMapped() {
        assertEquals("26.3-snapshot-4", defaults.map("26.3-snapshot-4"));
        assertEquals("26.4-snapshot-1", defaults.map("26.4-snapshot-1"));
    }

    @Test
    void weeklySnapshotsAreNeverMapped() {
        assertEquals("25w46a", defaults.map("25w46a"));
        assertEquals("26w14a", defaults.map("26w14a"));
    }

    @Test
    void nullAndEmptyInput() {
        assertNull(defaults.map(null));
        assertEquals("", defaults.map(""));
    }

    @Test
    void exactRulesTakePrecedenceOverWildcards() {
        VersionMappingRegistry registry = VersionMappingRegistry.of(true, true, java.util.List.of(
                VersionRule.parse("26.x", "1.22"),
                VersionRule.parse("26.2", "1.30")));
        assertEquals("1.30", registry.map("26.2"));
        assertEquals("1.22.1", registry.map("26.1"));
        assertEquals("1.22.2.1", registry.map("26.2.1"));
    }

    @Test
    void mostSpecificWildcardWins() {
        VersionMappingRegistry registry = VersionMappingRegistry.of(true, true, java.util.List.of(
                VersionRule.parse("26.x", "1.22"),
                VersionRule.parse("26.1.x", "9.9")));
        assertEquals("9.9.2", registry.map("26.1.2"));
        assertEquals("1.22.3", registry.map("26.3"));
    }

    @Test
    void firstDefinitionWinsForDuplicatePatterns() {
        VersionMappingRegistry registry = VersionMappingRegistry.of(true, true, java.util.List.of(
                VersionRule.parse("26.x", "1.22"),
                VersionRule.parse("26.x", "9.9")));
        assertEquals("1.22.3", registry.map("26.3"));
    }

    @Test
    void disablingMappingLeavesEverythingUntouched() {
        VersionMappingRegistry registry = VersionMappingRegistry.of(false, true,
                VersionMappingRegistry.DEFAULT_RULES);
        assertFalse(registry.isEnabled());
        assertEquals("26.1", registry.map("26.1"));
        assertEquals("26.3-rc-1", registry.map("26.3-rc-1"));
    }

    @Test
    void disablingPreReleaseMappingOnlyAffectsSuffixedVersions() {
        VersionMappingRegistry registry = VersionMappingRegistry.of(true, false,
                VersionMappingRegistry.DEFAULT_RULES);
        assertTrue(registry.mapsPreReleases() == false);
        assertEquals("26.3-rc-1", registry.map("26.3-rc-1"));
        assertEquals("26.3-pre-2", registry.map("26.3-pre-2"));
        assertEquals("1.22.3", registry.map("26.3"));
    }

    @Test
    void exactRuleWithPreReleaseAppendsSuffixToDisplay() {
        VersionMappingRegistry registry = VersionMappingRegistry.of(true, true, java.util.List.of(
                VersionRule.parse("26.3", "1.30")));
        assertEquals("1.30", registry.map("26.3"));
        assertEquals("1.30-rc-1", registry.map("26.3-rc-1"));
        assertEquals("1.30-pre-2", registry.map("26.3-pre-2"));

        VersionMappingRegistry noPreReleases = VersionMappingRegistry.of(true, false, java.util.List.of(
                VersionRule.parse("26.3", "1.30")));
        assertEquals("26.3-rc-1", noPreReleases.map("26.3-rc-1"));
    }

    @Test
    void emptyRuleListNeverMaps() {
        VersionMappingRegistry registry = VersionMappingRegistry.of(true, true, java.util.List.of());
        assertEquals("26.3", registry.map("26.3"));
    }

    @Test
    void nullRuleListIsTolerated() {
        VersionMappingRegistry registry = VersionMappingRegistry.of(true, true, null);
        assertEquals("26.3", registry.map("26.3"));
    }

    @Test
    void unknownSuffixWordingIsTreatedAsUnrecognised() {
        // "26.3-beta-1" is not a Mojang spelling, so we must not guess.
        assertEquals("26.3-beta-1", defaults.map("26.3-beta-1"));
        assertEquals("26.3-ReleaseCandidate-1", defaults.map("26.3-ReleaseCandidate-1"));
    }
}

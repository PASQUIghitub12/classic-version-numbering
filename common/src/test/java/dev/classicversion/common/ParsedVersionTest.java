package dev.classicversion.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Classification tests for version strings. */
class ParsedVersionTest {

    @Test
    void parsesPlainRelease() {
        ParsedVersion version = ParsedVersion.parse("26.3");
        assertEquals(ParsedVersion.Kind.RELEASE, version.kind());
        assertArrayEquals(new String[]{"26", "3"}, version.segments());
        assertEquals("26.3", version.base());
        assertEquals("", version.suffix());
        assertTrue(version.isEligibleForMapping());
    }

    @Test
    void parsesReleaseCandidate() {
        ParsedVersion version = ParsedVersion.parse("26.3-rc-1");
        assertEquals(ParsedVersion.Kind.RELEASE_CANDIDATE, version.kind());
        assertEquals("26.3", version.base());
        assertEquals("rc-1", version.suffix());
        assertTrue(version.isEligibleForMapping());
    }

    @Test
    void parsesPreRelease() {
        ParsedVersion version = ParsedVersion.parse("26.3-pre-2");
        assertEquals(ParsedVersion.Kind.PRE_RELEASE, version.kind());
        assertEquals("pre-2", version.suffix());
        assertTrue(version.isEligibleForMapping());
    }

    @Test
    void parsesSnapshot() {
        ParsedVersion version = ParsedVersion.parse("26.3-snapshot-5");
        assertEquals(ParsedVersion.Kind.SNAPSHOT, version.kind());
        assertFalse(version.isEligibleForMapping());
    }

    @Test
    void recognisesWeeklySnapshotAsUnrecognised() {
        ParsedVersion version = ParsedVersion.parse("25w46a");
        assertEquals(ParsedVersion.Kind.UNRECOGNIZED, version.kind());
        assertArrayEquals(new String[0], version.segments());
        assertFalse(version.isEligibleForMapping());
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> ParsedVersion.parse(null));
    }
}

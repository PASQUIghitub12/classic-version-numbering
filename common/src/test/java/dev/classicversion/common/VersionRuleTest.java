package dev.classicversion.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validation and matching tests for individual rules. */
class VersionRuleTest {

    @Test
    void parsesWildcardRule() {
        VersionRule rule = VersionRule.parse("26.x", "1.22");
        assertEquals("26.x", rule.pattern());
        assertEquals("1.22", rule.display());
        assertTrue(rule.isWildcard());
        assertEquals(1, rule.specificity());
    }

    @Test
    void parsesExactRule() {
        VersionRule rule = VersionRule.parse("26.1", "1.22");
        assertFalse(rule.isWildcard());
        assertEquals(2, rule.specificity());
    }

    @Test
    void starIsAnAcceptedWildcardSpelling() {
        VersionRule rule = VersionRule.parse("26.*", "1.22");
        assertTrue(rule.isWildcard());
    }

    @Test
    void surroundingWhitespaceIsTolerated() {
        assertEquals(VersionRule.parse("26.x", "1.22"), VersionRule.parse("  26.x  ", "  1.22  "));
    }

    @Test
    void rejectsBlankPattern() {
        assertThrows(IllegalArgumentException.class, () -> VersionRule.parse("", "1.22"));
        assertThrows(IllegalArgumentException.class, () -> VersionRule.parse("   ", "1.22"));
        assertThrows(IllegalArgumentException.class, () -> VersionRule.parse(null, "1.22"));
    }

    @Test
    void rejectsPatternWithoutLiteralSegment() {
        assertThrows(IllegalArgumentException.class, () -> VersionRule.parse("x", "1.22"));
        assertThrows(IllegalArgumentException.class, () -> VersionRule.parse("*", "1.22"));
    }

    @Test
    void rejectsNonNumericSegments() {
        assertThrows(IllegalArgumentException.class, () -> VersionRule.parse("abc.x", "1.22"));
        assertThrows(IllegalArgumentException.class, () -> VersionRule.parse("26.x.x", "1.22"));
        assertThrows(IllegalArgumentException.class, () -> VersionRule.parse("26.x1", "1.22"));
    }

    @Test
    void rejectsInvalidDisplayValue() {
        assertThrows(IllegalArgumentException.class, () -> VersionRule.parse("26.x", "not-a-version"));
        assertThrows(IllegalArgumentException.class, () -> VersionRule.parse("26.x", "1"));
        assertThrows(IllegalArgumentException.class, () -> VersionRule.parse("26.x", null));
    }

    @Test
    void exactRuleRequiresMatchingSegmentCount() {
        VersionRule rule = VersionRule.parse("26.1", "1.22");
        assertTrue(rule.matches(ParsedVersion.parse("26.1")));
        assertFalse(rule.matches(ParsedVersion.parse("26.1.2")));
        assertFalse(rule.matches(ParsedVersion.parse("26.2")));
    }

    @Test
    void wildcardRequiresAtLeastOneTrailingSegment() {
        VersionRule rule = VersionRule.parse("26.x", "1.22");
        assertTrue(rule.matches(ParsedVersion.parse("26.1")));
        assertTrue(rule.matches(ParsedVersion.parse("26.1.2")));
        assertFalse(rule.matches(ParsedVersion.parse("26")));
    }

    @Test
    void displayForAppendsTheSegmentsTheWildcardConsumed() {
        VersionRule wildcard = VersionRule.parse("26.x", "1.22");
        assertEquals("1.22.3", wildcard.displayFor(ParsedVersion.parse("26.3")));
        assertEquals("1.22.1.2", wildcard.displayFor(ParsedVersion.parse("26.1.2")));

        VersionRule star = VersionRule.parse("26.*", "1.22");
        assertEquals("1.22.3.1", star.displayFor(ParsedVersion.parse("26.3.1")));

        VersionRule specific = VersionRule.parse("26.1.x", "9.9");
        assertEquals("9.9.2", specific.displayFor(ParsedVersion.parse("26.1.2")));
    }

    @Test
    void displayForIsUnchangedForExactRules() {
        VersionRule exact = VersionRule.parse("26.1", "1.30");
        assertEquals("1.30", exact.displayFor(ParsedVersion.parse("26.1")));
    }

    @Test
    void matchingIsSegmentAnchored() {
        VersionRule rule = VersionRule.parse("1.21", "9.9");
        assertFalse(rule.matches(ParsedVersion.parse("11.21")));
        assertFalse(rule.matches(ParsedVersion.parse("1.21.4")));
    }

    @Test
    void equalityIsValueBased() {
        assertEquals(VersionRule.parse("26.x", "1.22"), VersionRule.parse("26.x", "1.22"));
    }
}

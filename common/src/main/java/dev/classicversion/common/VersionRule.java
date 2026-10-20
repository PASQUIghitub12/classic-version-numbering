package dev.classicversion.common;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One configurable version mapping rule.
 *
 * <p>A rule matches version strings segment by segment (version segments are
 * separated by {@code .}). An optional trailing {@code x} (or {@code *}) segment
 * acts as a wildcard that matches one or more remaining segments:</p>
 *
 * <ul>
 *   <li>{@code 26.x} matches {@code 26.1}, {@code 26.2}, {@code 26.1.2}, ...</li>
 *   <li>{@code 26.1} matches only {@code 26.1}</li>
 *   <li>{@code 1.21} never matches {@code 11.21} or {@code 1.21.4} (anchored segments)</li>
 * </ul>
 *
 * <p>The segments a wildcard consumed are preserved in the mapped value: the
 * display value is the base, and the consumed segments are appended. With the
 * shipped {@code 26.x -> 1.22} rule, {@code 26.3} maps to {@code 1.22.3} and
 * {@code 26.1.2} to {@code 1.22.1.2}. Exact rules consume nothing, so their
 * display value is used unchanged.</p>
 *
 * <p>Suffix handling ({@code -pre-N}, {@code -rc-N}, {@code -snapshot-N}) is
 * performed by the registry, not by the rule.</p>
 */
public final class VersionRule {

    /** Display values must look like a classic version: {@code 1.22}, {@code 1.22.1}, ... */
    private static final Pattern VALID_DISPLAY = Pattern.compile("^\\d+(?:\\.\\d+)+$");
    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("^\\d+$");

    private final String pattern;
    private final String[] literalSegments;
    private final boolean wildcard;
    private final String display;

    private VersionRule(String pattern, String[] literalSegments, boolean wildcard, String display) {
        this.pattern = pattern;
        this.literalSegments = literalSegments;
        this.wildcard = wildcard;
        this.display = display;
    }

    /**
     * Parses and validates a rule.
     *
     * @throws IllegalArgumentException if the pattern or display is invalid
     */
    public static VersionRule parse(String pattern, String display) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("rule pattern must not be empty");
        }
        String normalizedDisplay = display == null ? null : display.trim();
        if (normalizedDisplay == null || !VALID_DISPLAY.matcher(normalizedDisplay).matches()) {
            throw new IllegalArgumentException(
                    "display value '" + display + "' must look like a classic version such as 1.22");
        }

        String normalized = pattern.trim();
        String[] segments = normalized.split("\\.", -1);
        boolean wildcard = false;
        int literalCount = segments.length;
        String last = segments[segments.length - 1];
        if (last.equals("x") || last.equals("*")) {
            wildcard = true;
            literalCount--;
        }
        if (literalCount == 0) {
            throw new IllegalArgumentException("rule pattern '" + pattern + "' has no literal segment");
        }
        for (int i = 0; i < literalCount; i++) {
            if (!NUMERIC_SEGMENT.matcher(segments[i]).matches()) {
                throw new IllegalArgumentException(
                        "rule pattern '" + pattern + "' must contain only numeric segments and an optional "
                                + "trailing 'x' wildcard");
            }
        }
        return new VersionRule(normalized, Arrays.copyOf(segments, literalCount), wildcard, normalizedDisplay);
    }

    public String pattern() {
        return pattern;
    }

    public String display() {
        return display;
    }

    public boolean isWildcard() {
        return wildcard;
    }

    /**
     * Number of literal (non-wildcard) segments. Higher values are more specific
     * and take precedence when several wildcard rules match.
     */
    public int specificity() {
        return literalSegments.length;
    }

    /**
     * Matches the dotted base of a parsed version, ignoring suffixes. Callers are
     * responsible for rejecting versions that are not eligible for mapping.
     */
    boolean matchesBase(String[] versionSegments) {
        if (!wildcard) {
            if (versionSegments.length != literalSegments.length) {
                return false;
            }
        } else if (versionSegments.length < literalSegments.length + 1) {
            // A wildcard must consume at least one segment: 26.x does not match "26".
            return false;
        }
        for (int i = 0; i < literalSegments.length; i++) {
            if (!literalSegments[i].equals(versionSegments[i])) {
                return false;
            }
        }
        return true;
    }

    /** Whether this rule can be applied to the given base (exact rules require equality). */
    boolean matches(ParsedVersion version) {
        return matchesBase(version.segments());
    }

    /**
     * The display value for a version this rule matches.
     *
     * <p>For a wildcard rule the segments consumed by the wildcard are appended to
     * the configured display, so the trailing version component of the real version
     * is preserved: {@code 26.x -> 1.22} maps {@code 26.3} to {@code 1.22.3} and
     * {@code 26.1.2} to {@code 1.22.1.2}. For an exact rule nothing is consumed and
     * the display is returned as-is.</p>
     *
     * <p>Callers must only invoke this after {@link #matches(ParsedVersion)} has
     * returned {@code true}.</p>
     */
    String displayFor(ParsedVersion version) {
        if (!wildcard) {
            return display;
        }
        String[] versionSegments = version.segments();
        StringBuilder mapped = new StringBuilder(display);
        for (int i = literalSegments.length; i < versionSegments.length; i++) {
            mapped.append('.').append(versionSegments[i]);
        }
        return mapped.toString();
    }

    @Override
    public String toString() {
        return pattern + " -> " + display;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VersionRule rule
                && pattern.equals(rule.pattern)
                && display.equals(rule.display)
                && wildcard == rule.wildcard
                && Arrays.equals(literalSegments, rule.literalSegments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, display, wildcard, Arrays.hashCode(literalSegments));
    }
}

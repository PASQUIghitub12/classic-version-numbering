package dev.classicversion.common;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parsed Minecraft version string.
 *
 * <p>Modern (year-based) Minecraft versions use the scheme
 * {@code year.release[.patch]} with optional {@code -pre-N}, {@code -rc-N} and
 * {@code -snapshot-N} suffixes, for example {@code 26.3}, {@code 26.1.2},
 * {@code 26.3-rc-2} or {@code 26.2-snapshot-5}. Weekly snapshots from before the
 * scheme change look like {@code 25w46a}.</p>
 *
 * <p>This type deliberately classifies suffixes so that the mapping layer can
 * refuse to transform anything it cannot identify with certainty (weekly
 * snapshots, April Fools versions, pre-release wordings, ...).</p>
 */
public record ParsedVersion(String raw, String[] segments, Kind kind, String suffix) {

    /** How a version string relates to a releasable game version. */
    public enum Kind {
        /** A stable release, e.g. {@code 26.3}. */
        RELEASE,
        /** A pre-release, e.g. {@code 26.3-pre-2}. */
        PRE_RELEASE,
        /** A release candidate, e.g. {@code 26.3-rc-1}. */
        RELEASE_CANDIDATE,
        /** A development snapshot, e.g. {@code 26.3-snapshot-5}. Never mapped. */
        SNAPSHOT,
        /** Anything else, e.g. {@code 26w14a} or {@code 3D Shareware v1.34}. Never mapped. */
        UNRECOGNIZED
    }

    /**
     * {@code <numeric dotted base>} optionally followed by {@code -(rc|pre|snapshot)-<n>}.
     * Only the exact spellings used by Mojang are recognised.
     */
    private static final Pattern STABLE_OR_SUFFIXED =
            Pattern.compile("^(\\d+(?:\\.\\d+)*)(?:-(rc|pre|snapshot)-(\\d+))?$");

    public static ParsedVersion parse(String raw) {
        Objects.requireNonNull(raw, "raw");
        Matcher matcher = STABLE_OR_SUFFIXED.matcher(raw);
        if (!matcher.matches()) {
            return new ParsedVersion(raw, new String[0], Kind.UNRECOGNIZED, "");
        }
        String[] segments = matcher.group(1).split("\\.");
        String suffixType = matcher.group(2);
        if (suffixType == null) {
            return new ParsedVersion(raw, segments, Kind.RELEASE, "");
        }
        String suffix = suffixType + "-" + matcher.group(3);
        Kind kind = switch (suffixType) {
            case "rc" -> Kind.RELEASE_CANDIDATE;
            case "pre" -> Kind.PRE_RELEASE;
            default -> Kind.SNAPSHOT;
        };
        return new ParsedVersion(raw, segments, kind, suffix);
    }

    /** The dotted numeric base, e.g. {@code 26.1} for {@code 26.1-rc-1}. */
    public String base() {
        return String.join(".", segments);
    }

    /** Whether a rule may ever be applied to this version. */
    public boolean isEligibleForMapping() {
        return kind == Kind.RELEASE || kind == Kind.PRE_RELEASE || kind == Kind.RELEASE_CANDIDATE;
    }

    @Override
    public String toString() {
        return raw;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ParsedVersion p
                && raw.equals(p.raw)
                && Arrays.equals(segments, p.segments)
                && kind == p.kind
                && suffix.equals(p.suffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, Arrays.hashCode(segments), kind, suffix);
    }
}

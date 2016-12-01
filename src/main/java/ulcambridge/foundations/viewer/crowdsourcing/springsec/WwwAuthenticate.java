package ulcambridge.foundations.viewer.crowdsourcing.springsec;

import io.jsonwebtoken.lang.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

public final class WwwAuthenticate {

    public static final String HEADER_NAME = "WWW-Authenticate";

    public static final Pattern TOKEN_PATTERN = Pattern.compile(
        // token = 1*<any CHAR except CTLs or separators>
        // CHAR = 0 - 127
        "^[\\x00-\\x7f &&\n" +
        // CTL = 0 - 31, or 127
        "  [^\\x00-\\x1f \\x7f] &&\n" +
        // separators are:
        "  [^()<>@,;: \\\\ \\\" / \\[ \\] ?={} \\x20 \\t]\n" +
        " ]+$",
        Pattern.COMMENTS);

    private static final Pattern QS_ESCAPED_CHARS = Pattern.compile(
        "([\\\\\"])");

    private static final String TOKEN_ERR =
        "does not match the \"token\" production: ";

    public interface Encodable {
        StringBuilder encodeTo(StringBuilder sb);
        String encode();
    }

    public interface EncodedLengthAware {
        int getEncodedLengthHint();
    }

    public interface Challenge extends Encodable {
        String getAuthScheme();
        MultiValueMap<String, String> getParams();
    }

    public static final class DefaultChallenge
        implements Challenge, EncodedLengthAware {

        private final String authScheme;
        private final String[] authParams;

        public DefaultChallenge(
            String authScheme, String...authParams) {

            Assert.notNull(authScheme);
            if(!isToken(authScheme))
                throw new IllegalArgumentException(
                    "Invalid authScheme, " + TOKEN_ERR + authScheme);
            this.authScheme = authScheme;

            this.authParams = Arrays.copyOf(authParams, authParams.length);
            authParams = this.authParams;

            if(authParams.length % 2 != 0)
                throw new IllegalArgumentException(
                    "Missing value for last param: " +
                        authParams[authParams.length - 1]);

            for(int i = 0; i < authParams.length; i += 2) {
                if(!isToken(authParams[i])) {
                    throw new IllegalArgumentException(String.format(
                        "Param %d name %s%s",
                        (i / 2) + 1, TOKEN_ERR, authParams[i]));
                }
            }
        }

        @Override
        public String getAuthScheme() {
            return this.authScheme;
        }

        @Override
        public MultiValueMap<String, String> getParams() {
            // Don't expect this to be used much, hence simple implementation

            MultiValueMap<String, String> m = new LinkedMultiValueMap<>();
            for(int i = 0; i < authParams.length; i+= 2) {
                m.add(authParams[i], authParams[i + 1]);
            }
            return m;
        }

        @Override
        public int getEncodedLengthHint() {
            // Calculate an upper bound for the number of chars needed to encode
            // this Challenge.
            int l = this.authScheme.length();

            if(authParams.length > 0) {
                // Separating space
                l += 1;

                for(int i = 0; i < authParams.length; i += 2) {
                    // Separating ", "
                    if(i > 0) {
                        l += 2;
                    }
                    // +1 for equals
                    l += authParams[i].length() + 1;
                    // Worst case is every char in the value is escaped (* 2)
                    // and a start and end quote.
                    l += authParams[i + 1].length() * 2 + 2;
                }
            }

            return l;
        }

        @Override
        public StringBuilder encodeTo(StringBuilder sb) {
            sb.append(assertToken(authScheme));

            if(authParams.length > 0) {
                sb.append(' ');

                for (int i = 0; i < authParams.length; i += 2) {
                    if(i > 0) {
                        sb.append(", ");
                    }

                    sb.append(assertToken(authParams[i]))
                        .append('=')
                        .append(encodeTokenOrQuotedString(authParams[i + 1]));
                }
            }

            return sb;
        }

        @Override
        public String encode() {
            int initialCapacity = getEncodedLengthHint();
            StringBuilder sb = new StringBuilder(initialCapacity);

            this.encodeTo(sb);

            assert sb.capacity() == initialCapacity :
                   "StringBuilder unexpectedly grew capacity";
            return sb.toString();
        }

        @Override
        public String toString() { return encode(); }
    }

    public static StringBuilder encodeChallengesTo(
        StringBuilder sb, Collection<Challenge> challenges) {

        Assert.notNull(challenges);
        challenges.forEach(Assert::notNull);

        boolean isFirst = true;
        for(Challenge c : challenges) {
            if(!isFirst) {
                sb.append(", ");
            }

            c.encodeTo(sb);
            isFirst = false;
        }

        return sb;
    }

    private static int getEncodedLengthHint(Object o, int _default) {
        if(o instanceof EncodedLengthAware)
            return ((EncodedLengthAware)o).getEncodedLengthHint();
        return _default;
    }

    public static String encodeChallenges(Collection<Challenge> challenges) {
        int capacity = challenges.stream()
            .mapToInt(c -> getEncodedLengthHint(c, 0))
            .reduce(0, Integer::sum) + (2 * (challenges.size() - 1));

        StringBuilder sb = new StringBuilder(capacity);

        encodeChallengesTo(sb, challenges);

        assert sb.capacity() == capacity;
        return sb.toString();
    }

    public static boolean isToken(String s) {
        return TOKEN_PATTERN.matcher(s).matches();
    }

    public static String encodeQuotedString(String s) {
        return new StringBuilder(s.length() * 2 + 2)
            .append('"')
            .append(QS_ESCAPED_CHARS.matcher(s).replaceAll("\\\\$1"))
            .append('"')
            .toString();
    }

    public static String encodeTokenOrQuotedString(String s) {
        if(isToken(s))
            return encodeToken(s);
        return encodeQuotedString(s);
    }

    private static String assertToken(String s) {
        assert isToken(s);
        return s;
    }

    public static String encodeToken(String s) {
        if(!isToken(s))
            throw new IllegalArgumentException("Not a token: " + s);

        return s;
    }

    private WwwAuthenticate() {
        throw new RuntimeException();
    }
}

package ulcambridge.foundations.viewer.crowdsourcing.jwt;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.WwwAuthenticate;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.WwwAuthenticate.DefaultChallenge;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(JUnitParamsRunner.class)
public class WwwAuthenticateTest {

    @Test
    @Parameters
    public void testTokenProduction(String token, boolean shouldMatch) {
        assertThat(WwwAuthenticate.isToken(token), is(shouldMatch));
    }
    public Object parametersForTestTokenProduction() {
        return new Object[][] {
            {"foo", true},
            {"abd-def", true},
            {"foo_bar", true},
            {"abc def", false},
            {"abc@def", false},
            {"abc\tdef", false}
        };
    }

    @Test
    @Parameters
    public void testEncodeQuotedString(String input, String encoded) {
        assertThat(WwwAuthenticate.encodeQuotedString(input), equalTo(encoded));
    }
    public Object parametersForTestEncodeQuotedString() {
        return new Object[][]{
            {"", "\"\""},
            {"foo", "\"foo\""},
            {"\\foo", "\"\\\\foo\""},
            {"\\", "\"\\\\\""},
            {"\\\"", "\"\\\\\\\"\""}
        };
    }

    @Test
    @Parameters
    public void testChallengeToString(DefaultChallenge c, String expected) {
        assertThat(c.toString(), equalTo(expected));
    }
    public Object parametersForTestChallengeToString() {
        return new Object[][]{
            {new DefaultChallenge("Foo"), "Foo"},
            {new DefaultChallenge("Foo", "bar", "baz"), "Foo bar=baz"},
            {
                new DefaultChallenge("Foo", "bar", "baz", "wee", "Abc def"),
                "Foo bar=baz, wee=\"Abc def\""},
            {
                new DefaultChallenge("Foo", "bar", "baz", "AbC", "\"\\woot"),
                "Foo bar=baz, AbC=\"\\\"\\\\woot\""
            }
        };
    }

    @Test
    public void testChallengeEncodedLengthHint() {
        DefaultChallenge c = new DefaultChallenge("foo",
            "abc", "\"\"\"",
            "def", "\\\\\\");
        assertThat(c.toString().length(), is(equalTo(c.getEncodedLengthHint())));
    }
}

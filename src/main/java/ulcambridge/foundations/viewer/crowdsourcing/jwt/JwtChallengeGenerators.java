package ulcambridge.foundations.viewer.crowdsourcing.jwt;

import io.jsonwebtoken.JwtException;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.ChallengeGenerator;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.WwwAuthenticate.DefaultChallenge;

import java.util.Arrays;
import java.util.Optional;

public final class JwtChallengeGenerators {

    public static ChallengeGenerator reportJwtError(
        String authScheme) {

        return (request, authException) -> {
            if(authException.getCause() instanceof JwtException) {
                JwtException cause = (JwtException)authException.getCause();

                return Optional.of(Arrays.asList(new DefaultChallenge(
                    authScheme,
                    "error", "invalid_token",
                    "error_description", cause.getMessage()
                )));
            }

            return Optional.empty();
        };
    }

    private JwtChallengeGenerators() {
        throw new RuntimeException();
    }
}

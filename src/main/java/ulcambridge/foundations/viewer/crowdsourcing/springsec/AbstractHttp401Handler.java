package ulcambridge.foundations.viewer.crowdsourcing.springsec;

import io.jsonwebtoken.lang.Assert;
import org.springframework.security.core.AuthenticationException;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.WwwAuthenticate.Challenge;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

class AbstractHttp401Handler {
    protected final ChallengeGenerator challengeGenerator;

    public AbstractHttp401Handler(
        ChallengeGenerator challengeGenerator) {

        Assert.notNull(challengeGenerator);
        this.challengeGenerator = challengeGenerator;
    }

    public void handle(
        HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception)
        throws IOException, ServletException {

        Collection<Challenge> challenges = challengeGenerator
            .getChallenges(request, exception)
            .filter(c -> !c.isEmpty())
            // This shouldn't normally happen if canHandleFailure() has been
            // used.
            .orElseThrow(() -> new RuntimeException(
                "ChallengeGenerator produced no challenges"));

        response.addHeader(WwwAuthenticate.HEADER_NAME,
            WwwAuthenticate.encodeChallenges(challenges));
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
            exception.getMessage());
    }
}

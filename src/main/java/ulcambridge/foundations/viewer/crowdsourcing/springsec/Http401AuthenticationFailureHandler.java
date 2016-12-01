package ulcambridge.foundations.viewer.crowdsourcing.springsec;

import org.springframework.security.core.AuthenticationException;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.DelegatingAuthenticationFailureHandler.PredicatedAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Http401AuthenticationFailureHandler extends AbstractHttp401Handler
    implements PredicatedAuthenticationFailureHandler {

    public Http401AuthenticationFailureHandler(
        ChallengeGenerator challengeGenerator) {

        super(challengeGenerator);
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception)
        throws IOException, ServletException {

        this.handle(request, response, exception);
    }

    @Override
    public boolean canHandleFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception) {

        return this.challengeGenerator.getChallenges(request, exception)
                                      .isPresent();
    }
}

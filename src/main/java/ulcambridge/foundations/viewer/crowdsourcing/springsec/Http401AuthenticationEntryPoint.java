package ulcambridge.foundations.viewer.crowdsourcing.springsec;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Http401AuthenticationEntryPoint extends AbstractHttp401Handler implements AuthenticationEntryPoint {

    public Http401AuthenticationEntryPoint(
        ChallengeGenerator challengeGenerator) {

        super(challengeGenerator);
    }

    @Override
    public void commence(
        HttpServletRequest request, HttpServletResponse response,
        AuthenticationException authException)
        throws IOException, ServletException {

        this.handle(request, response, authException);
    }
}

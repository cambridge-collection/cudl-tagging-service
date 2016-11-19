package ulcambridge.foundations.viewer.crowdsourcing.springsec;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * An {@link AuthenticationSuccessHandler} that does nothing. Useful for
 * stateless auth schemes which occur on each request, and don't require things
 * like redirection after login.
 */
public class NoopAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, HttpServletResponse response,
        Authentication authentication)
        throws IOException, ServletException {

        // Do nothing!
    }
}

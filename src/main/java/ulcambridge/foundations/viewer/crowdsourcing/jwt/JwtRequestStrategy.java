package ulcambridge.foundations.viewer.crowdsourcing.jwt;

import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface JwtRequestStrategy {
    RequestMatcher getMatcher();

    Optional<String> getJwt(HttpServletRequest request);
}

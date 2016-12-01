package ulcambridge.foundations.viewer.crowdsourcing.springsec;

import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Optional;

@FunctionalInterface
public interface ChallengeGenerator {
    Optional<Collection<WwwAuthenticate.Challenge>> getChallenges(
        HttpServletRequest request, AuthenticationException authException);
}

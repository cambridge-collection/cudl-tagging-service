package ulcambridge.foundations.viewer.crowdsourcing.jwt;

import io.jsonwebtoken.lang.Assert;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Function;

import static ulcambridge.foundations.viewer.crowdsourcing.jwt.JwtRequestStrategies.AUTH_BEARER_JWT_STRATEGY;

public class JwtAuthenticationFilter
    extends AbstractAuthenticationProcessingFilter {

    private final JwtRequestStrategy jwtRequestStrategy;
    private final Function<String, ? extends AbstractJwtAuthenticationToken<?>>
        tokenCreator;

    public JwtAuthenticationFilter(
        AuthenticationManager authenticationManager) {

        this(authenticationManager, AUTH_BEARER_JWT_STRATEGY,
            DefaultJwtAuthenticationToken::unauthenticated);
    }

    public JwtAuthenticationFilter(
        AuthenticationManager authenticationManager,
        JwtRequestStrategy jwtRequestStrategy,
        Function<String, ? extends AbstractJwtAuthenticationToken<?>>
            tokenCreator) {

        super(jwtRequestStrategy.getMatcher());

        Assert.notNull(authenticationManager);
        Assert.notNull(tokenCreator);

        super.setAuthenticationManager(authenticationManager);

        this.jwtRequestStrategy = jwtRequestStrategy;
        this.tokenCreator = tokenCreator;
    }

    @Override
    public void setAuthenticationManager(
        AuthenticationManager authenticationManager) {

        throw new RuntimeException("Use constructor argument");
    }

    @Override
    public Authentication attemptAuthentication(
        HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException, IOException, ServletException {

        AbstractJwtAuthenticationToken<?> token = this.jwtRequestStrategy
            .getJwt(request)
            .map(this.tokenCreator)
            .orElseThrow(() -> new BadCredentialsException(
                "No JWT found in request"));

        return getAuthenticationManager()
            .authenticate(token);
    }
}

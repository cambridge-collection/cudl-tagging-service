package ulcambridge.foundations.viewer.crowdsourcing.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.lang.Assert;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.function.Function;
import java.util.function.Supplier;

public class JwtAuthenticationProvider<T extends Jwt<?, ?>>
    implements AuthenticationProvider {

    private final Function<String, T> parseFunction;

    public JwtAuthenticationProvider(Function<String, T> parseFunction) {
        Assert.notNull(parseFunction);

        this.parseFunction = parseFunction;
    }

    public static JwtAuthenticationProvider<Jws<Claims>> create(
        JwtParser parser) {

        return create(() -> parser);
    }

    public static JwtAuthenticationProvider<Jws<Claims>> create(
        Supplier<JwtParser> parserSupplier) {

        return new JwtAuthenticationProvider<>(
            s -> parserSupplier.get().parseClaimsJws(s));
    }

    @Override
    public Authentication authenticate(Authentication authentication)
        throws AuthenticationException {

        AbstractJwtAuthenticationToken<T> token =
            (AbstractJwtAuthenticationToken<T>) authentication;

        T t;
        try {
            t = this.parseFunction.apply(token.getJwt());
        }
        catch(JwtException e) {
            throw new BadCredentialsException("JWT was not valid", e);
        }

        AbstractJwtAuthenticationToken<T> authenticatedToken =
            token.authenticate(t);

        if(!authenticatedToken.isAuthenticated())
            throw new RuntimeException(
                "token's authenticate() method returned an unauthenticated " +
                "token");

        return authenticatedToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return AbstractJwtAuthenticationToken.class
            .isAssignableFrom(authentication);
    }
}

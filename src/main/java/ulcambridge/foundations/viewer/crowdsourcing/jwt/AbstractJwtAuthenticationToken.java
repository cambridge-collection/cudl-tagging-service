package ulcambridge.foundations.viewer.crowdsourcing.jwt;

import io.jsonwebtoken.Jwt;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractJwtAuthenticationToken<T extends Jwt<?, ?>>
    extends AbstractAuthenticationToken {

    private final String jwt;
    private final Instant received;
    private final Optional<T> parsedJwt;

    public AbstractJwtAuthenticationToken(
        String jwt, Instant received, Optional<T> parsedJwt) {

        super(null);

        Assert.hasText(jwt, "JWT was empty");
        Assert.notNull(received);
        Assert.notNull(parsedJwt);

        this.jwt = jwt;
        this.received = received;
        this.parsedJwt = parsedJwt;
    }

    public Instant getReceivedTime() {
        return this.received;
    }

    public String getJwt() {
        return this.jwt;
    }

    public Optional<T> getParsedJwt() {
        return this.parsedJwt;
    }

    /**
     * Create an authenticated version of this JWT authentication token.
     *
     * @param parsedJwt The result of parsing and verifying {{@link #getJwt()}}.
     * @return An authenticated version of this instance.
     */
    public abstract AbstractJwtAuthenticationToken<T> authenticate(
        T parsedJwt);

    @Override
    public boolean isAuthenticated() {
        return this.parsedJwt.isPresent();
    }

    @Override
    public Object getCredentials() {
        return this.getJwt();
    }

    @Override
    public String toString() {
        String s = String.format("JWT: %s; authenticated: %b",
            this.getJwt(), this.isAuthenticated());

        if(this.isAuthenticated()) {
            s += String.format("; principal: %s, granted authorities: %s",
                this.getPrincipal(), this.getAuthorities()
                    .stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining(", ", "[", "]")));
        }

        return s;
    }
}

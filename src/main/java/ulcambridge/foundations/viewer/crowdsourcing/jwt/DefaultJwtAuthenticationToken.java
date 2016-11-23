package ulcambridge.foundations.viewer.crowdsourcing.jwt;

import com.google.common.collect.ImmutableList;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A Spring security authentication token implementation backed by a normal JWT,
 * i.e. a cryptographically signed JSON object containing auth claims.
 *
 * @param <P> The type of the principle extracted from the claims.
 */
public class DefaultJwtAuthenticationToken<P>
    extends AbstractJwtAuthenticationToken<Jws<Claims>> {

    /**
     * A principle extraction function which returns a string for the auth
     * principle which is obtained from the JWT Subject claim.
     */
    public static final Function<Jws<Claims>, String>
        SUBJECT_STRING_PRINCIPLE_EXTRACTOR = jws -> {

        String subject = jws.getBody().getSubject();
        if(subject == null)
            throw new BadCredentialsException(
                "Missing \"sub\" (Subject) claim");
        return subject;
    };

    /**
     * A granted authority extraction function which always returns no
     * authorities.
     */
    public static final Function<Jws<Claims>, Collection<GrantedAuthority>>
        ROLE_USER_AUTHORITY_EXTRACTOR =
        fixedAuthorityList(authoritiesFromRoles("user"));

    public static final Function<Jws<Claims>, Collection<GrantedAuthority>>
    fixedAuthorityList(GrantedAuthority... grantedAuthorities) {

        return fixedAuthorityList(Arrays.asList(grantedAuthorities));
    }

    public static final Function<Jws<Claims>, Collection<GrantedAuthority>>
    fixedAuthorityList(Collection<GrantedAuthority> grantedAuthorities) {

        List<GrantedAuthority> authorities = ImmutableList.copyOf(
            grantedAuthorities);

        return jws -> authorities;
    }

    public static final Collection<GrantedAuthority> authoritiesFromRoles(
        String... roleNames) {

        return authoritiesFromRoles(Arrays.asList(roleNames));
    }

    public static final Collection<GrantedAuthority> authoritiesFromRoles(
        Collection<String> roleNames) {

        return roleNames.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }

        private final Function<Jws<Claims>, P> principleExtractor;
    private final Function<Jws<Claims>, Collection<GrantedAuthority>>
        authoritiesExtractor;

    /**
     * Create an unauthenticated instance. As
     * {@link #DefaultJwtAuthenticationToken(String, Instant, Optional, Function, Function)}
     * except the parsed JWT is always not present.
     */
    public DefaultJwtAuthenticationToken(
        String jwt, Instant received,
        Function<Jws<Claims>, P> principleExtractor,
        Function<Jws<Claims>, Collection<GrantedAuthority>> authoritiesExtractor
    ) {
        this(jwt, received, Optional.empty(), principleExtractor,
             authoritiesExtractor);
    }

    /**
     * Create an authenticated or unauthenticated instance.
     *
     * @param jwt The unparsed JWT string
     * @param received The time the JWT was received
     * @param parsedJwt The validated and parsed version of {@link #jwt}.
     *                  Typically this is passed via a call to
     *                  {@link #authenticate(Jws)}.
     *
     * @param principleExtractor A function which is used to obtain a principle
     *                           from the parsed JWT.
     * @param authoritiesExtractor A function which is used to obtain the
     *                             granted authorities from the JWT.
     */
    public DefaultJwtAuthenticationToken(
        String jwt, Instant received, Optional<Jws<Claims>> parsedJwt,
        Function<Jws<Claims>, P> principleExtractor,
        Function<Jws<Claims>, Collection<GrantedAuthority>> authoritiesExtractor
    ) {

        super(jwt, received, parsedJwt);

        Assert.notNull(principleExtractor);

        this.principleExtractor = principleExtractor;
        this.authoritiesExtractor = authoritiesExtractor;
    }

    /**
     * Obtain a JWT authentication token from the given encoded JWT, considered
     * to have been received at the specified instant in time.
     *
     * <p>The string value of the subject is used as the principle, and the
     * granted authority list will be empty.
     */
    public static DefaultJwtAuthenticationToken<String> unauthenticated(
        String jwt, Instant received) {

        return new DefaultJwtAuthenticationToken<>(jwt, received,
            SUBJECT_STRING_PRINCIPLE_EXTRACTOR, ROLE_USER_AUTHORITY_EXTRACTOR);
    }

    /**
     * Obtain a JWT authentication token from the given encoded JWT.
     *
     * <p>As {@link #unauthenticated(String, Instant)} except the current
     * instant from the default UTC system clock is used.
     */
    public static DefaultJwtAuthenticationToken<String> unauthenticated(
        String jwt) {

        return unauthenticated(jwt, Instant.now());
    }

    protected RuntimeException missingJwt() {
        return new IllegalStateException("Parsed JWT not present");
    }

    @Override
    public DefaultJwtAuthenticationToken<P> authenticate(
        Jws<Claims> parsedJwt) {

        return new DefaultJwtAuthenticationToken<>(
            this.getJwt(), this.getReceivedTime(), Optional.of(parsedJwt),
            this.principleExtractor, this.authoritiesExtractor);
    }

    @Override
    public P getPrincipal() {
        return this.mapParsedJwt(this.principleExtractor);
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return this.mapParsedJwt(this.authoritiesExtractor);
    }

    private <X> X mapParsedJwt(Function<Jws<Claims>, X> func) {
        return this.getParsedJwt()
            .map(func)
            .orElseThrow(this::missingJwt);
    }
}

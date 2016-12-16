package ulcambridge.foundations.viewer.crowdsourcing.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.lang.Assert;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import ulcambridge.foundations.viewer.crowdsourcing.jwt.DefaultJwtAuthenticationToken;
import ulcambridge.foundations.viewer.crowdsourcing.jwt.JwtAuthenticationFilter;
import ulcambridge.foundations.viewer.crowdsourcing.jwt.JwtAuthenticationProvider;
import ulcambridge.foundations.viewer.crowdsourcing.jwt.JwtChallengeGenerators;
import ulcambridge.foundations.viewer.crowdsourcing.jwt.JwtRequestStrategies;
import ulcambridge.foundations.viewer.crowdsourcing.jwt.JwtRequestStrategy;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.ChallengeGenerators;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.DelegatingAuthenticationFailureHandler;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.Http401AuthenticationEntryPoint;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.Http401AuthenticationFailureHandler;

import java.util.Optional;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter
    implements BeanFactoryAware {

    private BeanFactory beanFactory;

    public static final String AUTH_SCHEME = "Bearer";

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Bean
    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
        AuthenticationManager authenticationManager,
        @Qualifier("jwtAuthFailureHandler") AuthenticationFailureHandler
            authenticationFailureHandler) {

        JwtAuthenticationFilter f =
            new JwtAuthenticationFilter(authenticationManager);
        f.setAuthenticationFailureHandler(authenticationFailureHandler);

        return f;
    }

    private static final String QUERY_PARAM_TOKEN_FIELD_NAME = "__token__";

    /**
     * A {@link JwtRequestStrategy} which obtains tokens from the URL query
     * param <code>{@value #QUERY_PARAM_TOKEN_FIELD_NAME}</code>.
     */
    @Bean
    public JwtRequestStrategy queryParamJwtRequestStrategy() {
        return JwtRequestStrategies.strategy(req -> {
            String token = req.getParameter(QUERY_PARAM_TOKEN_FIELD_NAME);
            return Optional.ofNullable(
                token == null || token.isEmpty() ? null : token);
        });
    }

    /**
     * A secondary method of providing a JWT auth token for authentication.
     *
     * <p>This is required for situations where an Authorization header can't be
     * set, for example POSTing a form, or a browser download.
     */
    @Bean
    public JwtAuthenticationFilter jwtQueryParamAuthenticationFilter(
        AuthenticationManager authenticationManager,
        @Qualifier("queryParamJwtRequestStrategy")
            JwtRequestStrategy jwtRequestStrategy,
        @Qualifier("jwtAuthFailureHandler") AuthenticationFailureHandler
            authenticationFailureHandler) {

        JwtAuthenticationFilter f = new JwtAuthenticationFilter(
            authenticationManager, jwtRequestStrategy,
            DefaultJwtAuthenticationToken::unauthenticated);

        f.setAuthenticationFailureHandler(authenticationFailureHandler);

        return f;
    }

    @Bean
    public JwtAuthenticationProvider<Jws<Claims>> jwtAuthenticationProvider() {
        BeanFactory beanFactory = this.beanFactory;
        Assert.notNull(beanFactory);

        return new JwtAuthenticationProvider<>(
            jwtString -> beanFactory.getBean(JwtParser.class)
                .parseClaimsJws(jwtString));
    }

    @Bean
    public AuthenticationEntryPoint jwtAuthEntryPoint() {
        return new Http401AuthenticationEntryPoint(
            ChallengeGenerators.singleSchemeWithoutParams(AUTH_SCHEME)
        );
    }

    @Bean
    public AuthenticationFailureHandler jwtAuthFailureHandler() {
        return DelegatingAuthenticationFailureHandler.create(
            new Http401AuthenticationFailureHandler(ChallengeGenerators.chainOf(
                JwtChallengeGenerators.reportJwtError(AUTH_SCHEME)
            )),
            new SimpleUrlAuthenticationFailureHandler()
        );
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
            // CSRF protection is not required because we use an Authorization
            // header rather than a cookie for authentication. 3rd party sites
            // can't obtain an auth token, so they can't make cross site
            // requests.
            .csrf()
                .disable()
            .headers()
                .frameOptions()
                    .disable()
                .and()
            .authorizeRequests()
                // We use annotations on controller methods for enable auth
                .anyRequest().permitAll()
                .and()
            .addFilterAfter(
                beanFactory.getBean("jwtAuthenticationFilter",
                                    JwtAuthenticationFilter.class),
                AbstractPreAuthenticatedProcessingFilter.class)
            .addFilterAfter(
                beanFactory.getBean("jwtQueryParamAuthenticationFilter",
                    JwtAuthenticationFilter.class),
                AbstractPreAuthenticatedProcessingFilter.class)
            .exceptionHandling()
                .authenticationEntryPoint(
                    beanFactory.getBean(
                        "jwtAuthEntryPoint", AuthenticationEntryPoint.class))
                .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        super.configure(auth);

        auth.authenticationProvider(
            beanFactory.getBean(JwtAuthenticationProvider.class));
    }
}

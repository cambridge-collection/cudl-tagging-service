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
import ulcambridge.foundations.viewer.crowdsourcing.jwt.JwtAuthenticationFilter;
import ulcambridge.foundations.viewer.crowdsourcing.jwt.JwtAuthenticationProvider;
import ulcambridge.foundations.viewer.crowdsourcing.jwt.JwtChallengeGenerators;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.ChallengeGenerators;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.DelegatingAuthenticationFailureHandler;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.Http401AuthenticationEntryPoint;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.Http401AuthenticationFailureHandler;

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
            .authorizeRequests()
                .antMatchers("/crowdsourcing/**").authenticated()
                .anyRequest().permitAll()
                .and()
            .addFilterAfter(
                beanFactory.getBean(JwtAuthenticationFilter.class),
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

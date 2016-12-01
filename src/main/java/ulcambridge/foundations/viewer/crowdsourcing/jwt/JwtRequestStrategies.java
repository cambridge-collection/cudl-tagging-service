package ulcambridge.foundations.viewer.crowdsourcing.jwt;

import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JwtRequestStrategies {

    private static final Pattern JWT_BEARER = Pattern.compile("^\\s*Bearer\\s+([^\\s]+)\\s*$");

    public static final JwtRequestStrategy AUTH_BEARER_JWT_STRATEGY = strategy(
        req -> {
            String auth = req.getHeader("Authorization");

            if(auth != null) {
                Matcher m = JWT_BEARER.matcher(auth);
                if (m.matches()) {
                    return Optional.of(m.group(1));
                }
            }

            return Optional.empty();
        });

    public static JwtRequestStrategy strategy(
        Function<HttpServletRequest, Optional<String>> extractor) {

        return strategy(req -> extractor.apply(req).isPresent(), extractor);
    }

    public static JwtRequestStrategy strategy(
        RequestMatcher matcher,
        Function<HttpServletRequest, Optional<String>> tokenExtractor) {

        return new JwtRequestStrategy() {
            @Override
            public RequestMatcher getMatcher() {
                return matcher;
            }

            @Override
            public Optional<String> getJwt(HttpServletRequest request) {
                return tokenExtractor.apply(request);
            }
        };
    }

    private JwtRequestStrategies() {
        throw new RuntimeException();
    }
}

package ulcambridge.foundations.viewer.crowdsourcing.springsec;

import com.google.common.collect.ImmutableList;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.util.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A more generic version of
 * {@link org.springframework.security.web.authentication.DelegatingAuthenticationFailureHandler}.
 * Uses predicate functions rather than hardcoding an exception type as the
 * method of delegation control.
 */
public class DelegatingAuthenticationFailureHandler
    implements AuthenticationFailureHandler {

    private final List<PredicatedAuthenticationFailureHandler> handlers;

    public DelegatingAuthenticationFailureHandler(
        Collection<PredicatedAuthenticationFailureHandler> handlers) {

        this.handlers = ImmutableList.copyOf(handlers);
        this.handlers.forEach(Assert::notNull);
        Assert.notEmpty(this.handlers);
    }

    /**
     * Create an instance from the provided handlers. If a handler isn't a
     * {@link PredicatedAuthenticationFailureHandler} it's treated as if it's
     * always applicable.
     *
     * @param first The first handler(at least one is required).
     * @param rest Further handlers (may be empty).
     */
    public static DelegatingAuthenticationFailureHandler create(
        AuthenticationFailureHandler first,
        AuthenticationFailureHandler...rest) {

        return new DelegatingAuthenticationFailureHandler(
            ImmutableList.<PredicatedAuthenticationFailureHandler>builder()
                .add(wrapAsPredicated(first))
                .addAll(Arrays.stream(rest)
                    .map(DelegatingAuthenticationFailureHandler::wrapAsPredicated)
                    .iterator())
                .build());
    }

    private static PredicatedAuthenticationFailureHandler wrapAsPredicated(
        AuthenticationFailureHandler handler) {

        if(handler instanceof PredicatedAuthenticationFailureHandler)
            return (PredicatedAuthenticationFailureHandler)handler;
        return predicatedHandler(handler);
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception)
        throws IOException, ServletException {

        this.handlers.stream()
            .filter(h -> h.canHandleFailure(request, response, exception))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "No handler handled an authentication failure", exception))
            .onAuthenticationFailure(request, response,  exception);
    }

    @FunctionalInterface
    public interface AuthenticationFailureHandlerPredicate {
        boolean canHandleFailure(
            HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception);
    }

    public interface PredicatedAuthenticationFailureHandler
        extends AuthenticationFailureHandlerPredicate,
                AuthenticationFailureHandler { }

    /**
     * Create a {@link PredicatedAuthenticationFailureHandler} from an existing
     * handler and a predicate function.
     *
     * <p>
     * For example:
     * <pre>{@code
     * predicatedHandler(myHandler, (req, resp, e) -> e instanceof MyException);
     * }</pre>
     */
    public static PredicatedAuthenticationFailureHandler predicatedHandler(
        AuthenticationFailureHandler handler,
        AuthenticationFailureHandlerPredicate predicate) {

        return new PredicatedAuthenticationFailureHandler() {

            @Override
            public void onAuthenticationFailure(
                HttpServletRequest request, HttpServletResponse response,
                AuthenticationException exception)
                throws IOException, ServletException {

                handler.onAuthenticationFailure(request, response, exception);
            }

            @Override
            public boolean canHandleFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
                return predicate.canHandleFailure(request, response, exception);
            }
        };
    }

    public static PredicatedAuthenticationFailureHandler predicatedHandler(
        AuthenticationFailureHandler handler) {

        return predicatedHandler(handler, (a, b, c) -> true);
    }
}

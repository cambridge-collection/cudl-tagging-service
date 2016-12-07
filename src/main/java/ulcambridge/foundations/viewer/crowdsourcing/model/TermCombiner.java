package ulcambridge.foundations.viewer.crowdsourcing.model;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Encapsulates methods for combining terms of different types into a single set
 * of terms.
 *
 * <p>Terms of different types are added to the combiner via the
 * {@link #addTerms} method. Once terms have been added, the combined result can
 * be obtained using {@link #getCombinedTerms}.
 *
 * @param <T> Type of identifier for term types
 */
public interface TermCombiner<
    T, TIn extends Term, TOut extends Term, C> {

    TermCombiner<T, TIn, TOut, C> addTerms(
        T termType, Supplier<Stream<? extends TIn>> terms);
    C getCombinedTerms();

    interface Factory<T, TIn extends Term, TOut extends Term, C> {
        TermCombiner<T, TIn, TOut, C> newInstance();

        default <C2> Factory<T, TIn, TOut, C2> postProcessedBy(
            Function<? super C, ? extends C2> postProcessor) {

            return () -> TermCombiners.postProcessedBy(
                this.newInstance(), postProcessor);
        }
    }

}

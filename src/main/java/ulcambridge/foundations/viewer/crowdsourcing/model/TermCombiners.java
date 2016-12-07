package ulcambridge.foundations.viewer.crowdsourcing.model;

import io.jsonwebtoken.lang.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Static methods related to {@link TermCombiner}s.
 */
public class TermCombiners {

    /**
     * Create a term combiner which scales term values according to the
     * specified weights, and combines matching terms by summing their weights.
     *
     * <p>Terms are matched for merging based on their
     * {@link Term#getName() name}.
     *
     * @param weights The weights to scale each term type by.
     * @param <T> The type of the term type identifier values.
     */
    public static <T> TermCombiner.Factory<T, Term, Term, Map<String, Term>>
        weightedToMap(Map<T, Double> weights) {

        return () -> new DefaultTermCombiner<>(
            (type, terms) -> {
                Double weight = weights.get(type);
                if(weight == null)
                    throw new NoSuchElementException("No weight for: " + type);
                return terms.map(Terms.weightTerms(weight));
            },
            () -> Terms.mergeTerms(true));
    }

    /**
     * Modify the return value of {@link TermCombiner#getCombinedTerms()} by
     * applying a function.
     *
     * <p>The specified TermCombiner is wrapped with a new TermCombiner
     * implementation which delegates to the specified instance without changing
     * its behaviour, other than to apply the specified mapping function to the
     * combined result each time it's requested.
     *
     * @param combiner The combiner whose output should be mapped
     * @param postProcessor The function which will be invoked to map the output
     * @param <CIn> The output type of the specified combiner
     * @param <COut> The output type of the new wrapped combiner
     * @return A new combiner which acts as the origional but with a different
     *         output type.
     * @see TermCombiner.Factory#postProcessedBy(Function)
     */
    public static <T, TIn extends Term, TOut extends Term, CIn, COut>
    TermCombiner<T, TIn, TOut, COut> postProcessedBy(
        TermCombiner<T, TIn, TOut, CIn> combiner,
        Function<? super CIn, ? extends COut> postProcessor) {

        return new TermCombiner<T, TIn, TOut, COut>() {
            @Override
            public TermCombiner<T, TIn, TOut, COut> addTerms(
                T termType, Supplier<Stream<? extends TIn>> terms) {

                combiner.addTerms(termType, terms);
                return this;
            }

            @Override
            public COut getCombinedTerms() {
                return postProcessor.apply(combiner.getCombinedTerms());
            }
        };
    }

    /**
     * Skeleton implementation of {@link TermCombiner}, all that needs to be
     * provided is a {@link Collector} via the abstract {@link #getCollector()}
     * method.
     */
    public abstract static class AbstractTermCombiner
        <T, TIn extends Term, TOut extends Term, C extends Map<K, TOut>, K>
        implements TermCombiner<T, TIn, TOut, C> {

        protected final Map<T, List<Supplier<Stream<? extends TIn>>>> termSources;

        public AbstractTermCombiner() {

            this.termSources = new HashMap<>();
        }

        @Override
        public AbstractTermCombiner<T, TIn, TOut, C, K> addTerms(
            T termType, Supplier<Stream<? extends TIn>> terms) {

            Assert.notNull(termType);
            Assert.notNull(terms);

            List<Supplier<Stream<? extends TIn>>> typeTermSources =
                this.termSources.get(termType);

            if(typeTermSources == null) {
                typeTermSources = new ArrayList<>();
                this.termSources.put(termType, typeTermSources);
            }

            typeTermSources.add(terms);
            return this;
        }

        protected Stream<? extends TIn> getTerms(T type) {
            List<Supplier<Stream<? extends TIn>>> typeSources =
                this.termSources.get(type);

            if(typeSources == null)
                throw new NoSuchElementException("No sources for: " + type);

            return typeSources.stream().flatMap(Supplier::get);
        }

        /**
         * @return a collector to perform the combining of terms.
         */
        protected abstract Collector<
            ? super TIn, ?, ? extends C> getCollector();

        @Override
        public C getCombinedTerms() {
            return this.termSources.keySet().stream()
                .flatMap(this::getTerms)
                .collect(this.getCollector());
        }
    }

    /**
     * An implementation of {@link TermCombiner} which is paramertised with
     * functions to customise its behaviour.
     */
    public static class DefaultTermCombiner<
        T, TIn extends Term, TOut extends Term, C extends Map<K, TOut>, K>
        extends AbstractTermCombiner<T, TIn, TOut, C, K> {

        private final BiFunction<T, Stream<? extends TIn>, Stream<? extends TIn>> beforeCombine;
        private final Supplier<Collector<? super TIn, ?, ? extends C>> collectorSupplier;

        /**
         *
         * @param beforeCombine A function responsible for modifying the term
         *                      stream for a given type of term before its
         *                      merged with terms of different types.
         * @param collectorSupplier A function which produces a combiner to
         *                          merge the token streams.
         */
        public DefaultTermCombiner(
            BiFunction<T, Stream<? extends TIn>,
                       Stream<? extends TIn>> beforeCombine,
            Supplier<Collector<? super TIn, ?, ? extends C>>
                collectorSupplier) {

            Assert.notNull(beforeCombine);
            Assert.notNull(collectorSupplier);

            this.beforeCombine = beforeCombine;
            this.collectorSupplier = collectorSupplier;
        }

        @Override
        protected Stream<? extends TIn> getTerms(T type) {
            return beforeCombine.apply(type, super.getTerms(type));
        }

        @Override
        protected Collector<? super TIn, ?, ? extends C> getCollector() {
            return collectorSupplier.get();
        }
    }

    private TermCombiners() { throw new RuntimeException(); }
}

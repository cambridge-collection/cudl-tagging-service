package ulcambridge.foundations.viewer.crowdsourcing.model;


import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Static functions related to {@link Term}s.
 */
public final class Terms {

    /**
     * A function capable of creating a term of one type, given a term of
     * another.
     */
    @FunctionalInterface
    public interface TermFactory<TIn extends Term, TOut extends Term> {
        TOut createTerm(TIn term);
    }

    /**
     * A function which performs some modification to an output term which has
     * been derived from an input term.
     */
    @FunctionalInterface
    public interface MappedTermModifier<TIn extends Term, TOut extends Term> {
        /**
         * Do something to output. The input term is for reference only and must
         * not be modified.
         */
        void modifyOutputTerm(TIn input, TOut output);
    }

    /**
     * Create a function which maps terms from one type to another.
     *
     * @param factory A function responsible for the type conversion
     * @param modifier A function responsible for making required changes to the
     *                 resulting term
     * @param <TIn> The type of the input term
     * @param <TOut> The type of the output term
     * @return
     */
    public static <TIn extends Term, TOut extends Term> Function<TIn, TOut>
        mapTerm(TermFactory<? super TIn, ? extends TOut> factory,
                MappedTermModifier<? super TIn, ? super TOut> modifier) {

        return t -> {
            TOut output = factory.createTerm(t);
            modifier.modifyOutputTerm(t, output);
            return output;
        };
    }

    /**
     * Create a function which scales the value of the specified term by the
     * specified amount.
     */
    public static <TIn extends Term, TOut extends Term>
        MappedTermModifier<TIn, TOut> weightValueBy(double amount) {

        return (tin, tout) -> tout.setValue(tin.getValue() * amount);
    }

    /**
     * Create a copy the specified term (which can be an {@link Annotation} or
     * other more specific type).
     */
    public static Term createTerm(Term term) {
        return new Term(term.getName(), term.getRaw(), term.getValue());
    }

    /**
     * Create a function which weights terms (creating a new term, not modifying
     * the original) by scaling their values by the specified amount.
     *
     * @param amount The scaling factor
     * @param factory A function to create the output term from the input
     * @param <TIn> The type of the input terms
     * @param <TOut> The type of the output terms
     * @see #weightValueBy(double)
     */
    public static <TIn extends Term, TOut extends Term> Function<TIn, TOut>
        weightTerms(double amount,
                    TermFactory<? super TIn, ? extends TOut> factory) {

        return mapTerm(factory, Terms.weightValueBy(amount));
    }

    /**
     * As {@link #weightTerms(double, TermFactory)} except the output terms are
     * always {@link Term} instances.
     */
    public static <TIn extends Term> Function<TIn, Term> weightTerms(double amount) {
        return weightTerms(amount, Terms::createTerm);
    }

    /**
     * Merge two terms into one by summing their raw and value properties.
     *
     * <p>The name from the first term is used for the output term (it's assumed
     * that both share the same name).
     */
    public static Term mergeTermsByAddingValues(Term t1, Term t2) {
        Term t = new Term();
        t.setName(t1.getName());
        t.setRaw(t1.getRaw() + t2.getRaw());
        t.setValue(t1.getValue() + t2.getValue());
        return t;
    }

    /**
     * Create a Collector which merges terms with the same name by summing their
     * raw and value properties.
     *
     * <p>This can be used to reduce a stream of Terms, potentially containing
     * duplicates, into a unique set of terms.
     *
     * @param concurrent Whether the collector should support concurrent
     *                   merging.
     * @param <T> The type of input term to merge e.g. {@link Annotation}
     * @return A term-merging Collector
     */
    public static <T extends Term> Collector<T, ?, ? extends Map<String, Term>>
    mergeTerms(boolean concurrent) {
        return mergeTerms(concurrent, Term::getName, Terms::createTerm,
                          Terms::mergeTermsByAddingValues);
    }

    /**
     * Create a Collector which merges terms.
     *
     * @param concurrent Whether the collector should support concurrent
     *                   merging.
     * @param key A function which produces a key for each term. Multiple terms
     *            with the same key will be merged.
     * @param value A function which produces an output term for each input
     *              term. For example, converting from {@link Annotation} to
     *              {@link Term}.
     * @param merge A function which takes two terms and returns a merged term.
     * @param <TIn> The type of input terms
     * @param <TOut> The type of output terms
     * @param <K> The type of key used to identify merge candidates
     */
    public static <TIn extends Term, TOut extends Term, K>
        Collector<TIn, ?, ? extends Map<K, TOut>>
        mergeTerms(boolean concurrent,
                   Function<TIn, K> key,
                   Function<TIn, TOut> value,
                   BinaryOperator<TOut> merge) {

        if(concurrent)
            return Collectors.toConcurrentMap(key, value, merge);
        return Collectors.toMap(key, value, merge);
    }

    /**
     * Predicate for terms with positive values.
     */
    public static boolean hasPositiveValue(Term t) {
        return t.getValue() > 0;
    }

    private Terms() { throw new RuntimeException(); }
}

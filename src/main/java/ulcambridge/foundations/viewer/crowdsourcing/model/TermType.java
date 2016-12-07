package ulcambridge.foundations.viewer.crowdsourcing.model;

/**
 * The types of terms used by CUDL.
 */
public enum TermType {

    /** Terms from annotations, created by users. */
    ANNOTATION,

    /** Terms from text-mined data. */
    TAG,

    /**
     * Terms which have been marked by users as unhelpful (values are negative).
     */
    REMOVED_TAG
}

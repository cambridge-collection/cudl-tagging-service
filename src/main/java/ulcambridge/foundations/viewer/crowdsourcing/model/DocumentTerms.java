package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.springframework.util.Assert;

import java.util.List;

/**
 *
 * @author Lei
 *
 */
public class DocumentTerms {

    @JsonProperty("oid")
    private final String userId;

    @JsonProperty("docId")
    private final String documentId;

    private final ImmutableList<Term> terms;

    @JsonCreator
    public DocumentTerms(
        @JsonProperty("oid") String userId,
        @JsonProperty("docId") String documentId,
        Iterable<? extends Term> terms) {

        Assert.notNull(documentId);
        Assert.notNull(terms);

        this.userId = userId;
        this.documentId = documentId;
        this.terms = ImmutableList.copyOf(terms);
        this.terms.forEach(Assert::notNull);
    }

    public String getUserId() {
        return userId;
    }

    public String getDocumentId() {
        return documentId;
    }

    @JsonProperty("terms")
    public List<? extends Term> getTerms() {
        return terms;
    }
}

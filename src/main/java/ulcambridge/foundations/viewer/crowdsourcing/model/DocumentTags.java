package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.lang.Assert;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Lei
 *
 */
@JsonIgnoreProperties({"terms"})
public class DocumentTags extends DocumentTerms {

    @JsonCreator
    public DocumentTags(
        @JsonProperty("oid") String userId,
        @JsonProperty("docId") String documentId,
        Iterable<? extends Tag> tags) {

        super(userId, documentId, tags);
    }

    public DocumentTags(String userId, String documentId) {
        this(userId, documentId, Collections.emptyList());
    }

    // The terms list is immutable, so the cast is safe
    @SuppressWarnings("unchecked")
    @JsonProperty("tags")
    @Override
    public List<? extends Tag> getTerms() {
        return (List)super.getTerms();
    }
}

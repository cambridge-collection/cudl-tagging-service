package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 *
 * @author Lei
 *
 */
@JsonIgnoreProperties({"terms"})
public class DocumentTags extends DocumentTerms {

    public DocumentTags(String userId, String documentId,
                        Iterable<? extends Tag> tags) {
        super(userId, documentId, tags);
    }

    // The terms list is immutable, so the cast is safe
    @SuppressWarnings("unchecked")
    @JsonProperty("tags")
    @Override
    public List<? extends Tag> getTerms() {
        return (List)super.getTerms();
    }
}

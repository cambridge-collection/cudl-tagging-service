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
public class DocumentAnnotations extends DocumentTerms {

    public DocumentAnnotations(
        String userId, String documentId,
        Iterable<? extends Annotation> annotations) {

        super(userId, documentId, annotations);
    }

    // The terms list is immutable, so the cast is safe
    @SuppressWarnings("unchecked")
    @JsonProperty("annotations")
    @Override
    public List<? extends Annotation> getTerms() {
        return (List)super.getTerms();
    }
}

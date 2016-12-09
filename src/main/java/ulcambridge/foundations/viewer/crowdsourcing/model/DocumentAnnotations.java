package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Lei
 *
 */
@JsonIgnoreProperties({"terms"})
public class DocumentAnnotations extends DocumentTerms {

    @JsonProperty("annotations")
    private final List<Annotation> annotations;

    public DocumentAnnotations(
        String userId, String documentId, Collection<Annotation> annotations) {

        // FIXME: Get rid of these duplicated lists in subclasses
        super(userId, documentId, annotations.size(), ImmutableList.of());

        this.annotations = ImmutableList.copyOf(annotations);
    }

    public List<Annotation> getAnnotations() {
        return Collections.unmodifiableList(annotations);
    }

    @Override
    // annotations is immutable, so the cast is safe, people can't add a Term
    // to the Annotations list.
    @SuppressWarnings("unchecked")
    public List<Term> getTerms() {
        return (List)annotations;
    }

}

package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;

/**
 *
 * @author Lei
 *
 */
@JsonIgnoreProperties({"terms"})
public class DocumentTags extends DocumentTerms {

    @JsonProperty("tags")
    private final List<Tag> tags;

    public DocumentTags(String userId, String documentId,
                        Collection<Tag> tags) {
        super(userId, documentId, tags.size(), ImmutableList.of());

        this.tags = ImmutableList.copyOf(tags);
        this.tags.forEach(Assert::notNull);
    }

    public List<Tag> getTags() {
        return tags;
    }

    // This is safe as tags is immutable.
    @SuppressWarnings("unchecked")
    public List<Term> getTerms() {
        return (List)this.tags;
    }
}

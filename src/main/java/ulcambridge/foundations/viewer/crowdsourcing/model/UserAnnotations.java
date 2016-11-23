package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonCreator;
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
public class UserAnnotations {

    private final String userId;
    private final List<DocumentAnnotations> documentAnnotations;

    @JsonCreator
    public UserAnnotations(
        @JsonProperty("oid") String userId,
        @JsonProperty("annotations")
            Collection<DocumentAnnotations> documentAnnotations) {

        Assert.hasText(userId);
        Assert.notNull(documentAnnotations);

        this.userId = userId;
        this.documentAnnotations = ImmutableList.copyOf(documentAnnotations);
    }

    @JsonProperty("oid")
    public String getUserId() {
        return userId;
    }

    @JsonProperty(value = "total", access = JsonProperty.Access.READ_ONLY)
    public int getTotal() {
        return getDocumentAnnotations().stream()
            .mapToInt(da -> da.getAnnotations().size())
            .sum();
    }

    @JsonProperty("annotations")
    public List<DocumentAnnotations> getDocumentAnnotations() {
        return documentAnnotations;
    }
}

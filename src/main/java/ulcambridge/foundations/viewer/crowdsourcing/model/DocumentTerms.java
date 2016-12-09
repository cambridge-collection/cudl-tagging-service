package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;
import org.springframework.util.Assert;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Lei
 *
 */
public class DocumentTerms {

    @JsonProperty("oid")
    @SerializedName("oid")
    private final String userId;

    @JsonProperty("docId")
    @SerializedName("docId")
    private final String documentId;

    @JsonProperty("total")
    private final int total;

    @JsonProperty("terms")
    private final List<Term> terms;

    public DocumentTerms(String documentId, Collection<Term> terms) {
        this(null, documentId, terms.size(), new ArrayList<>(terms));
    }

    public DocumentTerms(String userId, String documentId, int total, Collection<Term> terms) {
        this.userId = userId;
        this.documentId = documentId;
        this.total = total;
        this.terms = ImmutableList.copyOf(terms);
        this.terms.forEach(Assert::notNull);
    }

    public String getUserId() {
        return userId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public int getTotal() {
        return total;
    }

    public List<Term> getTerms() {
        return terms;
    }
}

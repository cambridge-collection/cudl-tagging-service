package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 *
 * @author Lei
 *
 */
public class Term {

    private final String name;
    private final int raw;
    private final double value;

    @JsonCreator
    public Term(String name, int raw, double value) {
        this.name = name;
        this.raw = raw;
        this.value = value;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("raw")
    public int getRaw() {
        return raw;
    }

    @JsonProperty("value")
    public double getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Term))
            return false;
        if (obj == this)
            return true;

        Term rhs = (Term) obj;

        return new EqualsBuilder().append(name, rhs.getName()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(99, 97).append(name).toHashCode();
    }

}

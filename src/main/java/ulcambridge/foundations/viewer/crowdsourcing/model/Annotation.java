package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author Lei
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Annotation extends Term {

    private final String target;
    private final String type;
    private final int page;
    private final UUID uuid;
    private final Date date;
    private final Position position;

    public Annotation(String name, int raw, Double value, String target,
                      String type, int page, UUID uuid, Date date,
                      Position position) {

        super(name, raw, value);

        this.target = target;
        this.type = type;
        this.page = page;
        this.uuid = uuid;
        this.date = date;
        this.position = position;
    }

    @JsonProperty("target")
    public String getTarget() {
        return target;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("page")
    public int getPage() {
        return page;
    }

    @JsonProperty("uuid")
    public UUID getUuid() {
        return uuid;
    }

    @JsonProperty("date")
    @JsonSerialize(converter = JsonDateFormat.Serializer.class)
    public Date getDate() {
        return date;
    }

    @JsonProperty("position")
    public Position getPosition() {
        return position;
    }

    @JsonCreator
    static Annotation createAnnotation(
        @JsonProperty("name") String name, @JsonProperty("raw") int raw,
        @JsonProperty("value") Optional<Double> value,
        @JsonProperty("target") String target,
        @JsonProperty("type") String type,
        @JsonProperty("page") int page,
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("date")
        @JsonDeserialize(converter = JsonDateFormat.Deserializer.class)
            Date date,
        @JsonProperty("position") Position position) {

        return new Annotation(name, raw, value.orElse((double)raw), target,
                              type, page, uuid, date, position);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Annotation))
            return false;
        if (obj == this)
            return true;

        Annotation rhs = (Annotation) obj;

        // compare target, type and page number if it is 'doc'
        //
        if (rhs.getTarget().equals(this.getTarget()) && rhs.getTarget().equals("doc")) {
            return new EqualsBuilder().
                append(page, rhs.page).
                append(target, rhs.target).
                append(type, rhs.type).
                isEquals();
        }
        // otherwise (it is 'tag') compare name, target, type, name and position
        //
        else {
            return new EqualsBuilder().
                // if deriving: appendSuper(super.equals(obj)).
                append(getName(), rhs.getName()).
                append(page, rhs.getPage()).
                append(target, rhs.getTarget()).
                append(type, rhs.getType()).
                append(position, rhs.getPosition()).
                isEquals();
        }
    }

    @Override
    public int hashCode() {
        if (this.getTarget().equals("doc")) {
            return new HashCodeBuilder(99, 101).
                    append(page).
                    append(target).
                    append(type).
                    toHashCode();
        } else {
            return new HashCodeBuilder(99, 101).
                    append(getName()).
                    append(page).
                    append(target).
                    append(type).
                    append(position).
                    toHashCode();
        }
    }

}

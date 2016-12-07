package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonCreator;
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
public class Annotation extends Term {

    private String target;
    private String type;
    private int page;
    private UUID uuid;
    private Date date;
    private Position position;

    private Annotation() {}

    @JsonProperty("target")
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("page")
    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    @JsonProperty("uuid")
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @JsonProperty("date")
    @JsonSerialize(converter = JsonDateFormat.Serializer.class)
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @JsonProperty("position")
    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
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

        Annotation a = new Annotation();
        a.setName(name);
        a.setRaw(raw);
        a.setValue(value.orElse((double)raw));
        a.setTarget(target);
        a.setType(type);
        a.setPage(page);
        a.setUuid(uuid);
        a.setDate(date);
        a.setPosition(position);

        return a;
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
                append(position.getType(), rhs.getPosition().getType()).
                append(position.getCoordinates(), rhs.getPosition().getCoordinates()).
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
                    append(position.getType()).
                    toHashCode();
        }
    }

}

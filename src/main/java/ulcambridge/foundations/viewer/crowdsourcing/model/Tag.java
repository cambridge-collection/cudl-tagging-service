package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

/**
 *
 * @author Lei
 *
 */
public class Tag extends Term {

    public Tag() {
    }

    @JsonCreator
    static Tag createTag(
        @JsonProperty("name") String name,
        @JsonProperty("raw") int raw,
        @JsonProperty("value") Optional<Double> value) {

        Tag t = new Tag();
        t.setName(name);
        t.setRaw(raw);
        t.setValue(value.orElse((double)raw));
        return t;
    }
}

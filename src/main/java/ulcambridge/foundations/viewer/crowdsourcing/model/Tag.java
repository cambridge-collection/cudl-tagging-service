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

    public Tag(String name, int raw, double value) {
        super(name, raw, value);
    }

    @JsonCreator
    static Tag createTag(
        @JsonProperty("name") String name,
        @JsonProperty("raw") int raw,
        @JsonProperty("value") Optional<Double> value) {

        return new Tag(name, raw, value.orElse((double)raw));
    }
}

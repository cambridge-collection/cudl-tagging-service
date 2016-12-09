package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.jsonwebtoken.lang.Assert;

import java.util.List;

/**
 *
 * @author Lei
 *
 */
public class Position {

    private final String type;
    private final List<Point2D> coordinates;

    @JsonCreator
    public Position(
        String type,
        @JsonProperty(required = true)
            Iterable<? extends Point2D> coordinates) {
        Assert.notNull(coordinates, "coordinates was null");

        this.type = type;
        this.coordinates = ImmutableList.copyOf(coordinates);
        this.coordinates.forEach(Assert::notNull);
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("coordinates")
    public List<Point2D> getCoordinates() {
        return coordinates;
    }

    /**
     * Format coordinates list to oa:FragmentSelector
     *
     * @see <a href="http://www.openannotation.org/spec/core/specific.html#FragmentSelector">Fragment Selector</a>
     *
     * @return
     */
    public String formatCoordinatesToFragmentSelector() {
        if (coordinates.size() < 1) {
            return "0,0,0,0";
        } else if (coordinates.size() == 1) {
            return (int) coordinates.get(0).getX() + "," + (int) coordinates.get(0).getY() + ",1,1";
        } else { // size <= 5
            int w = (int) Math.abs(coordinates.get(2).getX() - coordinates.get(1).getX());
            int h = (int) Math.abs(coordinates.get(4).getY() - coordinates.get(1).getY());
            return (int) coordinates.get(0).getX() + "," + (int) coordinates.get(0).getY() + "," + w + "," + h;
        }
    }

}

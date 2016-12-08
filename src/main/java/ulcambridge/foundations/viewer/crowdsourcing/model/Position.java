package ulcambridge.foundations.viewer.crowdsourcing.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Lei
 *
 */
public class Position {

    @JsonProperty("type")
    private String type;

    @JsonProperty("coordinates")
    private List<Point2D> coordinates = new ArrayList<Point2D>();

    public Position() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Point2D> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Point2D> coordinates) {
        this.coordinates = coordinates;
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

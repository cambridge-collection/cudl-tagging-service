package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Lei
 *
 */
public class Point2D {

    private final double x;
    private final double y;

    @JsonCreator
    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @JsonProperty("x")
    public double getX() {
        return x;
    }

    @JsonProperty("y")
    public double getY() {
        return y;
    }
}

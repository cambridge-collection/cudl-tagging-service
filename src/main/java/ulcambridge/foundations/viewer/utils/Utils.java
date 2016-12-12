package ulcambridge.foundations.viewer.utils;

import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class Utils {

    // get current date time
    public static Date getCurrentDateTime() {
        return new Date();
    }

    // format a double to 5 digit precision
    public static String formatValue(double value) {
        value = (double) Math.round(value * 100000) / 100000;
        return new DecimalFormat("#0.00000").format(value);
    }

    public static <T> Stream<T> stream(Iterable<T> i) {
        return StreamSupport.stream(i.spliterator(), false);
    }

    public static UriComponentsBuilder populateScheme(
        UriComponentsBuilder b, HttpServletRequest request) {

        return b.scheme(request.isSecure() ? "https" : "http");
    }
}

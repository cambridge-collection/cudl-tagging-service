package ulcambridge.foundations.viewer.crowdsourcing.model;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;


/**
 * The date format used in serialised JSON output.
 */
class JsonDateFormat {

    private static final ZoneId DEFAULT_TZ = ZoneId.of("UTC");

    private static final DateTimeFormatter DATE_FORMATTER =
        new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4).appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendLiteral(" ")
            .appendZoneText(TextStyle.SHORT)
            .toFormatter(Locale.UK)
            .withResolverStyle(ResolverStyle.STRICT)
            .withChronology(IsoChronology.INSTANCE);

    static String formatDate(Instant instant, ZoneId zone) {
        return DATE_FORMATTER.format(instant.atZone(zone));
    }

    static Instant parseDate(String date) {
        return DATE_FORMATTER.parse(date, Instant::from);
    }

    private JsonDateFormat() { throw new RuntimeException(); }

    public static class Serializer extends StdConverter<Instant, String> {
        @Override
        public String convert(Instant value) {
            return formatDate(value, DEFAULT_TZ);
        }
    }

    public static class Deserializer extends StdConverter<String, Instant> {
        @Override
        public Instant convert(String value) {
            return parseDate(value);
        }
    }
}

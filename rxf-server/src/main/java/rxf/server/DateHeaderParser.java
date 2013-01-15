package rxf.server;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A utility class for parsing and formatting HTTP dates as used in cookies and
 * other headers.  This class handles dates as defined by RFC 2616 section
 * 3.3.1 as well as some other common non-standard formats.
 *
 * @author Christopher Brown
 * @author Michael Becke
 * @overhauled Jim Northrup
 */
public enum DateHeaderParser {
  RFC1123("EEE, dd MMM yyyy HH:mm:ss z"),
  /**
   * Date format pattern used to parse HTTP date headers in RFC 1123 format.
   */

  /**
   * Date format pattern used to parse HTTP date headers in RFC 1036 format.
   */
  RFC1036("EEEE, dd-MMM-yy HH:mm:ss z"),

  /**
   * Date format pattern used to parse HTTP date headers in ANSI C
   * <code>asctime()</code> format.
   */
  ISO8601("yyyy-MM-dd'T'HH:mm:ssz"), ISOMS("yyyy-MM-dd'T'HH:mm:ss.SSS zzz"), SHORT(DateFormat
      .getDateInstance(DateFormat.SHORT)), MED(DateFormat.getDateInstance(DateFormat.MEDIUM)), LONG(
      DateFormat.getDateInstance(DateFormat.LONG)), FULL(DateFormat
      .getDateInstance(DateFormat.FULL)), ASCTIME("EEE MMM d HH:mm:ss yyyy"), ;
  private final DateFormat format;

  DateHeaderParser(String fmt) {

    this(new SimpleDateFormat(fmt, Locale.getDefault()));

  }

  DateHeaderParser(DateFormat dateFormat) {
    format = dateFormat;
    format.setLenient(true);
    //for unit tests we want GMT as predictable.  for other println's we want local tz
    if (BlobAntiPatternObject.isDEBUG_SENDJSON())
      format.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  /**
   * Parses the date value using the given date formats.
   *
   * @param dateValue the date value to parse
   * @return the parsed date
   */
  public static Date parseDate(CharSequence dateValue) {

    char c = dateValue.charAt(0);
    switch (c) {
      case '\'':
      case '"':

        dateValue = dateValue.subSequence(1, dateValue.length() - 1);
      default:
        break;
    }
    String source = dateValue.toString();
    for (DateHeaderParser dateHeaderParser : values()) {
      try {
        return dateHeaderParser.format.parse(source);
      } catch (ParseException e) {
        if (BlobAntiPatternObject.isDEBUG_SENDJSON()) {
          System.err.println(".--" + dateHeaderParser.name() + " failed parse: " + source);

        }
      }
    }
    return null;
  }

  public static String formatHttpHeaderDate(Date... fdate) {
    return RFC1123.format.format(fdate.length > 0 ? fdate[0] : new Date());
  }
}

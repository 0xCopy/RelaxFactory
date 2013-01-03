package rxf.server;

import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 1/2/13
 * Time: 6:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class DateHeaderParserTest {
	Map<String, String> map = new HashMap() {
		{
			put("'sun, 11-sep-01 08:49:37 EST'", "Tue Sep 11 06:49:37 PDT 2001"); // ; RFC 850, obsoleted by RFC 1036
			put("'Sun, 1 Nov 1994 08:49:37 GMT'",
					"Tue Nov 01 00:49:37 PST 1994"); //  ; RFC 822, updated by RFC 1123
			put("'Sun November  1 08:49:37 1994'",
					"Tue Nov 01 08:49:37 GMT 1994"); //   ; ANSI C's asctime() format
			put("'2007-03-01T13:00:00 PST8PDT'", "Thu Mar 01 13:00:00 PST 2007"); //iso ?
			put("'2007-03-01T13:00:00.000 UTC'", "Thu Mar 01 05:00:00 PST 2007"); //iso ?
		}
	};

	static {
		RelaxFactoryServerImpl.setDEBUG_SENDJSON(true);
	}
	@Test
	public void testIfModifiedStuff() {

		for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {
			String s = stringStringEntry.getKey();
			System.err.println("testing: " + s);
			Date date = DateHeaderParser.parseDate(s);

			assertEquals(date.toGMTString(), new Date(stringStringEntry
					.getValue()).toGMTString());
			System.err.println(" tested: " + s + " :: " + date);
		}

	}
	@Test
	public void testFmt() {
		for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {

		}
	}
}

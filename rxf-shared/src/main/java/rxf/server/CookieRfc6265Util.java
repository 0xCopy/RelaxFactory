package rxf.server;

import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.Iterator;

import static one.xio.HttpMethod.UTF8;

/**
 * This enum defines the HTTP Cookie and Set-Cookie header fields.
 * Using the Set-Cookie header field, an HTTP server can pass name/value
 * pairs and associated metadata (called cookies) to a user agent.  When
 * the user agent makes subsequent requests to the server, the user
 * agent uses the metadata and other information to determine whether to
 * return the name/value pairs in the Cookie header.
 * <p/>
 * Although simple on their surface, cookies have a number of
 * complexities.  For example, the server indicates a scope for each
 * cookie when sending it to the user agent.  The scope indicates the
 * maximum amount of time in which the user agent should return the
 * cookie, the servers to which the user agent should return the cookie,
 * and the URI schemes for which the cookie is applicable.
 * <p/>
 * For historical reasons, cookies contain a number of security and
 * privacy infelicities.  For example, a server can indicate that a
 * given cookie is intended for "secure" connections, but the Secure
 * attribute does not provide integrity in the presence of an active
 * network attacker.  Similarly, cookies for a given host are shared
 * across all the ports on that host, even though the usual "same-origin
 * policy" used by web browsers isolates content retrieved via different
 * ports.
 * <p/>
 * There are two audiences for this specification: developers of cookie-
 * generating servers and developers of cookie-consuming user agents.
 * <p/>
 * To maximize interoperability with user agents, servers SHOULD limit
 * themselves to the well-behaved profile defined in Section 4 when
 * generating cookies.
 * <p/>
 * User agents MUST implement the more liberal processing rules defined
 * in Section 5, in order to maximize interoperability with existing
 * servers that do not conform to the well-behaved profile defined in
 * Section 4.
 * <p/>
 * This document specifies the syntax and semantics of these headers as
 * they are actually used on the Internet.  In particular, this document
 * does not create new syntax or semantics beyond those in use today.
 * The recommendations for cookie generation provided in Section 4
 * represent a preferred subset of current server behavior, and even the
 * more liberal cookie processing algorithm provided in Section 5 does
 * not recommend all of the syntactic and semantic variations in use
 * today.  Where some existing software differs from the recommended
 * protocol in significant ways, the document contains a note explaining
 * the difference.
 * <p/>
 * Prior to this document, there were at least three descriptions of
 * cookies: the so-called "Netscape cookie specification" [Netscape],
 * RFC 2109 [RFC2109], and RFC 2965 [RFC2965].  However, none of these
 * documents describe how the Cookie and Set-Cookie headers are actually
 * used on the Internet (see [Kri2001] for historical context).  In
 * relation to previous IETF specifications of HTTP state management
 * mechanisms, this document requests the following actions:
 * <ol> <li> Change the status of [RFC2109] to Historic (it has already been obsoleted by [RFC2965]). </li>
 * <li> Change the status of [RFC2965] to Historic. </li>
 * <li> Indicate that [RFC2965] has been obsoleted by this document. </li>      </ol>
 * <p/>
 * In particular, in moving RFC 2965 to Historic and obsoleting it, this
 * document deprecates the use of the Cookie2 and Set-Cookie2 header
 * fields.
 */
public enum CookieRfc6265Util {
  /**
   * returns an array of bytes
   */
  Name {
    {
      token = null;
    }

    @Override
    public Serializable value(ByteBuffer input) {
      input = (ByteBuffer) input.duplicate().rewind();
      do {
        while (input.hasRemaining() && Character.isWhitespace(((ByteBuffer) input.mark()).get()));
        int begin = input.reset().position();
        while (input.hasRemaining() && '=' != ((ByteBuffer) input.mark()).get());

        return ByteBuffer.allocate(
            ((ByteBuffer) input.reset().flip().position(begin)).slice().limit()).put(input).array();
      } while (input.hasRemaining());

    }

  },
  Value {
    {
      token = null;
    }

    @Override
    public Serializable value(ByteBuffer input) {
      input = (ByteBuffer) input.duplicate().rewind();
      do {
        while (input.hasRemaining() && '=' != input.get());
        while (input.hasRemaining() && Character.isWhitespace(((ByteBuffer) input.mark()).get()));
        int begin = input.reset().position();
        while (input.hasRemaining() && ';' != ((ByteBuffer) input.mark()).get());

        return ByteBuffer.allocate(
            ((ByteBuffer) input.reset().flip().position(begin)).slice().limit()).put(input).array();
      } while (input.hasRemaining());
    }
  },
  /**
   * 5.2.1.  The Expires Attribute
   * <p/>
   * If the attribute-name case-insensitively matches the string
   * "Expires", the user agent MUST process the cookie-av as follows.
   * <p/>
   * Let the expiry-time be the result of parsing the attribute-value as
   * cookie-date (see Section 5.1.1).
   * <p/>
   * If the attribute-value failed to parse as a cookie date, ignore the
   * cookie-av.
   * <p/>
   * If the expiry-time is later than the last date the user agent can
   * represent, the user agent MAY replace the expiry-time with the last
   * representable date.
   * <p/>
   * If the expiry-time is earlier than the earliest date the user agent
   * can represent, the user agent MAY replace the expiry-time with the
   * earliest representable date.
   * <p/>
   * Append an attribute to the cookie-attribute-list with an attribute-
   * name of Expires and an attribute-value of expiry-time.
   */
  Expires {
    @Override
    public Serializable value(ByteBuffer input) {
      input = input.slice();
      while (input.hasRemaining() && Character.isWhitespace(((ByteBuffer) input.mark()).get()));
      input = ((ByteBuffer) input.reset()).slice();
      byte b;
      while (input.hasRemaining() && !Character.isWhitespace(b = ((ByteBuffer) input.mark()).get())
          && '=' != b);

      int position = input.reset().position();
      int limit = token.limit();

      if (position == limit) {

        while (input.hasRemaining() && '=' != input.get());

        CharBuffer parseme = UTF8.decode(input.slice());
        Date date = null;
        try {
          date = DateHeaderParser.parseDate(parseme.toString().trim());
        } catch (Exception e) {

        }
        return date;
      }

      return null;
    }
  },
  /**
   * 5.2.2.  The Max-Age Attribute
   * <p/>
   * If the attribute-name case-insensitively matches the string "Max-
   * Age", the user agent MUST process the cookie-av as follows.
   * <p/>
   * If the first character of the attribute-value is not a DIGIT or a "-"
   * character, ignore the cookie-av.
   * <p/>
   * If the remainder of attribute-value contains a non-DIGIT character,
   * ignore the cookie-av.
   * <p/>
   * Let delta-seconds be the attribute-value converted to an integer.
   * <p/>
   * If delta-seconds is less than or equal to zero (0), let expiry-time
   * be the earliest representable date and time.  Otherwise, let the
   * expiry-time be the current date and time plus delta-seconds seconds.
   * <p/>
   * Append an attribute to the cookie-attribute-list with an attribute-
   * name of Max-Age and an attribute-value of expiry-time.
   */
  Max$2dAge {
    @Override
    public Serializable value(ByteBuffer input) {
      input = input.slice();
      while (input.hasRemaining() && Character.isWhitespace(((ByteBuffer) input.mark()).get()));
      input = ((ByteBuffer) input.reset()).slice();
      byte b;
      while (input.hasRemaining() && !Character.isWhitespace(b = ((ByteBuffer) input.mark()).get())
          && '=' != b);

      int position = input.reset().position();
      int limit = token.limit();

      if (position == limit) {

        while (input.hasRemaining() && '=' != input.get());

        CharBuffer parseme = UTF8.decode(input.slice());
        Long l = null;
        try {
          l = Long.parseLong(parseme.toString().trim());
        } catch (NumberFormatException e) {

        }
        return l;
      }

      return null;
    }

  },
  /**
   * 5.2.3.  The Domain Attribute
   * <p/>
   * If the attribute-name case-insensitively matches the string "Domain",
   * the user agent MUST process the cookie-av as follows.
   * <p/>
   * If the attribute-value is empty, the behavior is undefined.  However,
   * the user agent SHOULD ignore the cookie-av entirely.
   * <p/>
   * If the first character of the attribute-value string is %x2E ("."):
   * <p/>
   * Let cookie-domain be the attribute-value without the leading %x2E
   * (".") character.
   * <p/>
   * Otherwise:
   * <p/>
   * Let cookie-domain be the entire attribute-value.
   * <p/>
   * Convert the cookie-domain to lower case.
   * <p/>
   * Append an attribute to the cookie-attribute-list with an attribute-
   * name of Domain and an attribute-value of cookie-domain.
   */
  Domain {
    @Override
    public Serializable value(ByteBuffer input) {
      input = input.slice();
      while (input.hasRemaining() && Character.isWhitespace(((ByteBuffer) input.mark()).get()));
      input = ((ByteBuffer) input.reset()).slice();
      byte b;
      while (input.hasRemaining() && !Character.isWhitespace(b = ((ByteBuffer) input.mark()).get())
          && '=' != b);

      int position = input.reset().position();
      int limit = token.limit();

      if (position == limit) {

        while (input.hasRemaining() && '=' != input.get());

        return ByteBuffer.allocate((input = input.slice()).limit()).put(input).array();
      }

      return null;
    }

  },

  /**
   * 5.2.4.  The Path Attribute
   * <p/>
   * If the attribute-name case-insensitively matches the string "Path",
   * the user agent MUST process the cookie-av as follows.
   * <p/>
   * If the attribute-value is empty or if the first character of the
   * attribute-value is not %x2F ("/"):
   * <p/>
   * Let cookie-path be the default-path.
   * <p/>
   * Otherwise:
   * <p/>
   * Let cookie-path be the attribute-value.
   * <p/>
   * Append an attribute to the cookie-attribute-list with an attribute-
   * name of Path and an attribute-value of cookie-path.
   */
  Path {
    @Override
    public Serializable value(ByteBuffer input) {
      input = input.slice();
      while (input.hasRemaining() && Character.isWhitespace(((ByteBuffer) input.mark()).get()));
      input = ((ByteBuffer) input.reset()).slice();
      byte b;
      while (input.hasRemaining() && !Character.isWhitespace(b = ((ByteBuffer) input.mark()).get())
          && '=' != b);

      int position = input.reset().position();
      int limit = token.limit();

      if (position == limit) {

        while (input.hasRemaining() && '=' != input.get());

        return ByteBuffer.allocate((input = input.slice()).limit()).put(input).array();
      }

      return null;
    }
  },
  /**
   * 5.2.5.  The Secure Attribute
   * <p/>
   * If the attribute-name case-insensitively matches the string "Secure",
   * the user agent MUST append an attribute to the cookie-attribute-list
   * with an attribute-name of Secure and an empty attribute-value.
   */
  Secure {
    @Override
    public Serializable value(ByteBuffer input) {
      input.rewind();
      ByteBuffer tok = token.duplicate();
      byte b;
      do {
        while (input.hasRemaining() && Character.isWhitespace(((ByteBuffer) input.mark()).get()));
        tok.rewind();
        while (tok.hasRemaining() && input.hasRemaining()
            && tok.get() == Character.toLowerCase(input.get()));
        if (!tok.hasRemaining()) {
          boolean keep = false;
          while (input.hasRemaining() && ';' != (b = ((ByteBuffer) input.mark()).get())
              && (keep = Character.isWhitespace(b)));
          if (keep)
            return Boolean.TRUE;
        }
      } while (input.hasRemaining());

      return null;
    }
  },
  /**
   * 5.2.6.  The HttpOnly Attribute
   * <p/>
   * If the attribute-name case-insensitively matches the string
   * "HttpOnly", the user agent MUST append an attribute to the cookie-
   * attribute-list with an attribute-name of HttpOnly and an empty
   * attribute-value.
   */
  HttpOnly {
    @Override
    public Serializable value(ByteBuffer input) {
      input.rewind();
      ByteBuffer tok = token.duplicate();
      byte b;
      do {
        while (input.hasRemaining() && Character.isWhitespace(((ByteBuffer) input.mark()).get()));
        tok.rewind();
        while (tok.hasRemaining() && input.hasRemaining()
            && tok.get() == Character.toLowerCase(input.get()));
        if (!tok.hasRemaining()) {
          boolean keep = false;
          while (input.hasRemaining() && ';' != (b = ((ByteBuffer) input.mark()).get())
              && (keep = Character.isWhitespace(b)));
          if (keep) {
            return Boolean.TRUE;
          }
        }
      } while (input.hasRemaining());

      return null;
    }
  };
  final String key = URLDecoder.decode(name().replace('$', '%')).toLowerCase();
  ByteBuffer token = UTF8.encode(key);

  public static EnumMap<CookieRfc6265Util, Serializable> parseSetCookie(ByteBuffer input) {
    ArrayList<ByteBuffer> a = new ArrayList<>();
    while (input.hasRemaining()) {
      int begin = input.position();
      byte b = 0;
      while (input.hasRemaining() && ';' != (b = ((ByteBuffer) input.mark()).get()));
      a.add(((ByteBuffer) (b == ';' ? input.duplicate().reset() : input.duplicate()).flip()
          .position(begin)).slice());
    }
    EnumMap<CookieRfc6265Util, Serializable> res;
    res = new EnumMap<>(CookieRfc6265Util.class);
    Iterator<ByteBuffer> iterator = a.iterator();
    ByteBuffer next = iterator.next();
    Serializable n = Name.value(next);
    res.put(Name, n);
    Serializable v = Value.value(next);
    res.put(Value, v);

    while (iterator.hasNext()) {
      ByteBuffer byteBuffer = iterator.next();
      CookieRfc6265Util[] values = values();
      for (int i = 2; i < values.length; i++) {

        CookieRfc6265Util cookieRfc6265Util = values[i];
        if (!res.containsKey(cookieRfc6265Util)) {
          Serializable value = cookieRfc6265Util.value((ByteBuffer) byteBuffer.rewind());
          if (null != value) {
            res.put(cookieRfc6265Util, value);
          }
        }
      }
    }
    return res;
  }

  /**
   * @param filter ByteBuffers as keys for cookies
   * @param input  unescaped bytes split by ';'
   * @return slist of cookies
   */
  public static Pair<Pair<ByteBuffer, ByteBuffer>, ? extends Pair> parseCookie(ByteBuffer input,
      ByteBuffer... filter) {
    Pair<?, ?> ret = null;
    ByteBuffer buf = input.duplicate().slice();

    while (buf.hasRemaining()) {
      while (buf.hasRemaining() && Character.isWhitespace(((ByteBuffer) buf.mark()).get()));
      int keyBegin = buf.reset().position();

      while (buf.hasRemaining() && '=' != ((ByteBuffer) buf.mark()).get());
      ByteBuffer ckey = ((ByteBuffer) buf.duplicate().reset().flip().position(keyBegin)).slice();
      while (buf.hasRemaining() && Character.isWhitespace(((ByteBuffer) buf.mark()).get()));
      int vBegin = buf.reset().position();

      while (buf.hasRemaining()) {
        switch (((ByteBuffer) buf.mark()).get()) {
          case ';':
          case '\r':
          case '\n':
            break;
          default:
            continue;
        }
        break;
      }
      if (filter.length > 0) {
        for (ByteBuffer filt : filter) {
          if (ckey.limit() == filt.limit()) {
            ckey.mark();
            filt.rewind();
            while (filt.hasRemaining() && ckey.hasRemaining() && filt.get() == ckey.get());
            if (!filt.hasRemaining() && !ckey.hasRemaining()) {
              ret =
                  new Pair<Pair<Object, ByteBuffer>, Pair>(new Pair(ckey.reset(), ((ByteBuffer) buf.duplicate().reset().flip()
                      .position(vBegin)).slice()), ret);
              break;
            }
          }
        }
      } else
        ret =
            new Pair<Pair<ByteBuffer, ByteBuffer>, Pair<?, ?>>(new Pair<>(ckey, ((ByteBuffer) buf.duplicate().reset().flip().position(vBegin))
                .slice()), ret);
    }
    return (Pair<Pair<ByteBuffer, ByteBuffer>, ? extends Pair>) ret;
  }

  public abstract Serializable value(ByteBuffer token);
}
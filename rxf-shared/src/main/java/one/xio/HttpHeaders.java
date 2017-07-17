package one.xio;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * iana http headers produced from shell command below.
 */
// curl http://www.iana.org/assignments/message-headers/perm-headers.csv
// http://www.iana.org/assignments/message-headers/prov-headers.csv | tr -d '\r'| grep ,http,|while read; do echo '/**'
// $(echo $REPLY|cut -f5- -d, ) '*/' $(echo $REPLY|cut -f1 -d,|sed 's,-,$2d,g') ,;echo $a;done

public enum HttpHeaders {
  /** [RFC4229] */
  A$2dIM,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 5.3.2]" */
  Accept,

  /** [RFC4229] */
  Accept$2dAdditions,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 5.3.3]" */
  Accept$2dCharset,

  /** [RFC7089] */
  Accept$2dDatetime,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 5.3.4]" */
  Accept$2dEncoding,

  /** [RFC4229] */
  Accept$2dFeatures,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 5.3.5]" */
  Accept$2dLanguage,

  /** [RFC5789] */
  Accept$2dPatch,

  /** "[RFC-ietf-httpbis-p5-range-26, Section 2.3]" */
  Accept$2dRanges,

  /** "[RFC-ietf-httpbis-p6-cache-26, Section 5.1]" */
  Age,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 7.4.1]" */
  Allow,

  /** [RFC4229] */
  Alternates,

  /** [RFC4437] */
  Apply$2dTo$2dRedirect$2dRef,

  /** [RFC4229] */
  Authentication$2dInfo,

  /** "[RFC-ietf-httpbis-p7-auth-26, Section 4.2]" */
  Authorization,

  /** [RFC4229] */
  C$2dExt,

  /** [RFC4229] */
  C$2dMan,

  /** [RFC4229] */
  C$2dOpt,

  /** [RFC4229] */
  C$2dPEP,

  /** [RFC4229] */
  C$2dPEP$2dInfo,

  /** "[RFC-ietf-httpbis-p6-cache-26, Section 5.2]" */
  Cache$2dControl,

  /** "[RFC-ietf-httpbis-p1-messaging-26, Section 8.1]" */
  Close,

  /** "[RFC-ietf-httpbis-p1-messaging-26, Section 6.1]" */
  Connection,

  /** [RFC2068][RFC2616] */
  Content$2dBase,

  /** [RFC6266] */
  Content$2dDisposition,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 3.1.2.2]" */
  Content$2dEncoding,

  /** [RFC4229] */
  Content$2dID,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 3.1.3.2]" */
  Content$2dLanguage,

  /** "[RFC-ietf-httpbis-p1-messaging-26, Section 3.3.2]" */
  Content$2dLength,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 3.1.4.2]" */
  Content$2dLocation,

  /** [RFC4229] */
  Content$2dMD5,

  /** "[RFC-ietf-httpbis-p5-range-26, Section 4.2]" */
  Content$2dRange,

  /** [RFC4229] */
  Content$2dScript$2dType,

  /** [RFC4229] */
  Content$2dStyle$2dType,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 3.1.1.5]" */
  Content$2dType,

  /** [RFC4229] */
  Content$2dVersion,

  /** [RFC6265] */
  Cookie,

  /** [RFC2965][RFC6265] */
  Cookie2,

  /** [RFC5323] */
  DASL,

  /** [RFC4918] */
  DAV,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 7.1.1.2]" */
  Date,

  /** [RFC4229] */
  Default$2dStyle,

  /** [RFC4229] */
  Delta$2dBase,

  /** [RFC4918] */
  Depth,

  /** [RFC4229] */
  Derived$2dFrom,

  /** [RFC4918] */
  Destination,

  /** [RFC4229] */
  Differential$2dID,

  /** [RFC4229] */
  Digest,

  /** "[RFC-ietf-httpbis-p4-conditional-26, Section 2.3]" */
  ETag,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 5.1.1]" */
  Expect,

  /** "[RFC-ietf-httpbis-p6-cache-26, Section 5.3]" */
  Expires,

  /** [RFC4229] */
  Ext,

  /** [draft-ietf-appsawg-http-forwarded-10] */
  Forwarded,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 5.5.1]" */
  From,

  /** [RFC4229] */
  GetProfile,

  /** "[RFC-ietf-httpbis-p1-messaging-26, Section 5.4]" */
  Host,

  /** [RFC4229] */
  IM,

  /** [RFC4918] */
  If,

  /** "[RFC-ietf-httpbis-p4-conditional-26, Section 3.1]" */
  If$2dMatch,

  /** "[RFC-ietf-httpbis-p4-conditional-26, Section 3.3]" */
  If$2dModified$2dSince,

  /** "[RFC-ietf-httpbis-p4-conditional-26, Section 3.2]" */
  If$2dNone$2dMatch,

  /** "[RFC-ietf-httpbis-p5-range-26, Section 3.2]" */
  If$2dRange,

  /** [RFC6638] */
  If$2dSchedule$2dTag$2dMatch,

  /** "[RFC-ietf-httpbis-p4-conditional-26, Section 3.4]" */
  If$2dUnmodified$2dSince,

  /** [RFC4229] */
  Keep$2dAlive,

  /** [RFC4229] */
  Label,

  /** "[RFC-ietf-httpbis-p4-conditional-26, Section 2.2]" */
  Last$2dModified,

  /** [RFC5988] */
  Link,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 7.1.2]" */
  Location,

  /** [RFC4918] */
  Lock$2dToken,

  /** [RFC4229] */
  Man,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 5.1.2]" */
  Max$2dForwards,

  /** [RFC7089] */
  Memento$2dDatetime,

  /** [RFC4229] */
  Meter,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Appendix A.1]" */
  MIME$2dVersion,

  /** [RFC4229] */
  Negotiate,

  /** [RFC4229] */
  Opt,

  /** [RFC4229] */
  Ordering$2dType,

  /** [RFC6454] */
  Origin,

  /** [RFC4918] */
  Overwrite,

  /** [RFC4229] */
  P3P,

  /** [RFC4229] */
  PEP,

  /** [RFC4229] */
  PICS$2dLabel,

  /** [RFC4229] */
  Pep$2dInfo,

  /** [RFC4229] */
  Position,

  /** "[RFC-ietf-httpbis-p6-cache-26, Section 5.4]" */
  Pragma,

  /** [draft-snell-http-prefer-18] */
  Prefer,

  /** [draft-snell-http-prefer-18] */
  Preference$2dApplied,

  /** [RFC4229] */
  ProfileObject,

  /** [RFC4229] */
  Protocol,

  /** [RFC4229] */
  Protocol$2dInfo,

  /** [RFC4229] */
  Protocol$2dQuery,

  /** [RFC4229] */
  Protocol$2dRequest,

  /** "[RFC-ietf-httpbis-p7-auth-26, Section 4.3]" */
  Proxy$2dAuthenticate,

  /** [RFC4229] */
  Proxy$2dAuthentication$2dInfo,

  /** "[RFC-ietf-httpbis-p7-auth-26, Section 4.4]" */
  Proxy$2dAuthorization,

  /** [RFC4229] */
  Proxy$2dFeatures,

  /** [RFC4229] */
  Proxy$2dInstruction,

  /** [RFC4229] */
  Public,

  /** "[RFC-ietf-httpbis-p5-range-26, Section 3.1]" */
  Range,

  /** [RFC4437] */
  Redirect$2dRef,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 5.5.2]" */
  Referer,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 7.1.3]" */
  Retry$2dAfter,

  /** [RFC4229] */
  Safe,

  /** [RFC6638] */
  Schedule$2dReply,

  /** [RFC6638] */
  Schedule$2dTag,

  /** [RFC6455] */
  Sec$2dWebSocket$2dAccept,

  /** [RFC6455] */
  Sec$2dWebSocket$2dExtensions,

  /** [RFC6455] */
  Sec$2dWebSocket$2dKey,

  /** [RFC6455] */
  Sec$2dWebSocket$2dProtocol,

  /** [RFC6455] */
  Sec$2dWebSocket$2dVersion,

  /** [RFC4229] */
  Security$2dScheme,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 7.4.2]" */
  Server,

  /** [RFC6265] */
  Set$2dCookie,

  /** [RFC2965][RFC6265] */
  Set$2dCookie2,

  /** [RFC4229] */
  SetProfile,

  /** [RFC5023] */
  SLUG,

  /** [RFC4229] */
  SoapAction,

  /** [RFC4229] */
  Status$2dURI,

  /** [RFC6797] */
  Strict$2dTransport$2dSecurity,

  /** [RFC4229] */
  Surrogate$2dCapability,

  /** [RFC4229] */
  Surrogate$2dControl,

  /** [RFC4229] */
  TCN,

  /** "[RFC-ietf-httpbis-p1-messaging-26, Section 4.3]" */
  TE,

  /** [RFC4918] */
  Timeout,

  /** "[RFC-ietf-httpbis-p1-messaging-26, Section 4.4]" */
  Trailer,

  /** "[RFC-ietf-httpbis-p1-messaging-26, Section 3.3.1]" */
  Transfer$2dEncoding,

  /** [RFC4229] */
  URI,

  /** "[RFC-ietf-httpbis-p1-messaging-26, Section 6.7]" */
  Upgrade,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 5.5.3]" */
  User$2dAgent,

  /** [RFC4229] */
  Variant$2dVary,

  /** "[RFC-ietf-httpbis-p2-semantics-26, Section 7.1.4]" */
  Vary,

  /** "[RFC-ietf-httpbis-p1-messaging-26, Section 5.7.1]" */
  Via,

  /** "[RFC-ietf-httpbis-p7-auth-26, Section 4.1]" */
  WWW$2dAuthenticate,

  /** [RFC4229] */
  Want$2dDigest,

  /** "[RFC-ietf-httpbis-p6-cache-26, Section 5.5]" */
  Warning,

  /** [RFC7034] */
  X$2dFrame$2dOptions,

  /** [W3C Web Application Formats Working Group] */
  Access$2dControl,

  /** [W3C Web Application Formats Working Group] */
  Access$2dControl$2dAllow$2dCredentials,

  /** [W3C Web Application Formats Working Group] */
  Access$2dControl$2dAllow$2dHeaders,

  /** [W3C Web Application Formats Working Group] */
  Access$2dControl$2dAllow$2dMethods,

  /** [W3C Web Application Formats Working Group] */
  Access$2dControl$2dAllow$2dOrigin,

  /** [W3C Web Application Formats Working Group] */
  Access$2dControl$2dMax$2dAge,

  /** [W3C Web Application Formats Working Group] */
  Access$2dControl$2dRequest$2dMethod,

  /** [W3C Web Application Formats Working Group] */
  Access$2dControl$2dRequest$2dHeaders,

  /** [RFC4229] */
  Compliance,

  /** [RFC4229] */
  Content$2dTransfer$2dEncoding,

  /** [RFC4229] */
  Cost,

  /** [RFC6017] */
  EDIINT$2dFeatures,

  /** [RFC4229] */
  Message$2dID,

  /** [W3C Web Application Formats Working Group] */
  Method$2dCheck,

  /** [W3C Web Application Formats Working Group] */
  Method$2dCheck$2dExpires,

  /** [RFC4229] */
  Non$2dCompliance,

  /** [RFC4229] */
  Optional,

  /** [W3C Web Application Formats Working Group] */
  Referer$2dRoot,

  /** [RFC4229] */
  Resolution$2dHint,

  /** [RFC4229] */
  Resolver$2dLocation,

  /** [RFC4229] */
  SubOK,

  /** [RFC4229] */
  Subst,

  /** [RFC4229] */
  Title,

  /** [RFC4229] */
  UA$2dColor,

  /** [RFC4229] */
  UA$2dMedia,

  /** [RFC4229] */
  UA$2dPixels,

  /** [RFC4229] */
  UA$2dResolution,

  /** [RFC4229] */
  UA$2dWindowpixels,

  /** [RFC4229] */
  Version,

  /** [W3C Mobile Web Best Practices Working Group] */
  X$2dDevice$2dAccept,

  /** [W3C Mobile Web Best Practices Working Group] */
  X$2dDevice$2dAccept$2dCharset,

  /** [W3C Mobile Web Best Practices Working Group] */
  X$2dDevice$2dAccept$2dEncoding,

  /** [W3C Mobile Web Best Practices Working Group] */
  X$2dDevice$2dAccept$2dLanguage,

  /** [W3C Mobile Web Best Practices Working Group] */
  X$2dDevice$2dUser$2dAgent,

  ;
  private final String header = URLDecoder.decode(name().replace('$', '%'));
  private final ByteBuffer token = StandardCharsets.UTF_8.encode(header);
  private int tokenLen = token.limit();
  private int sendBufferSize;

  /**
   * 
   * @param headers bytebuf rfc822
   * @return
   */
  public static Map<String, int[]> getHeaders(ByteBuffer headers) {
    headers.rewind();
    int l = headers.limit();
    Map<String, int[]> linkedHashMap = new TreeMap();
    while (headers.hasRemaining() && '\n' != headers.get());
    while (headers.hasRemaining()) {
      int p1 = headers.position();
      while (headers.hasRemaining() && ':' != headers.get());
      int p2 = headers.position();
      while (headers.hasRemaining() && '\n' != headers.get());
      int p3 = headers.position();

      String key =
          StandardCharsets.UTF_8.decode((ByteBuffer) headers.position(p1).limit(p2 - 1)).toString()
              .trim();
      if (key.length() > 0) {
        linkedHashMap.put(key, new int[] {p2, p3});
      }
      headers.limit(l).position(p3);

    }

    return linkedHashMap;
  }

  public String getHeader() {
    return header;
  }

  public ByteBuffer getToken() {
    return token;
  }

  public int getTokenLen() {
    return tokenLen;
  }

  public void setTokenLen(int tokenLen) {
    this.tokenLen = tokenLen;
  }

  /**
   * @param slice
   * @return a slice suitable for UTF8.decode
   */

  public ByteBuffer parse(ByteBuffer slice) {
    slice.position(tokenLen + 2 + slice.position());
    while (Character.isWhitespace(slice.get(slice.limit() - 1)))

      slice.limit(slice.limit() - 1);
    return slice;
  }

  public boolean recognize(ByteBuffer buffer) {

    final int i = buffer.position();
    boolean ret = false;
    if ((buffer.get(tokenLen + i) & 0xff) == ':') {

      int j = 0;
      while (j < tokenLen && token.get(j) == buffer.get(i + j)) {
        j++;
      }
      ret = tokenLen == j;
    }

    return ret;
  }

  static public int getSendBufferSize() {
    return 4 << 10;
  }
}

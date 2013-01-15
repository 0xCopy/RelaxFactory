package one.xio;

/**
 * <h1>HTTP Status Codes</h1>
 * <p/>
 * 1xx - Informational
 * <br>This class of status code indicates a provisional response, consisting only of the Status-Line and optional headers, and is terminated by an empty line. There are no required headers for this class of status code. Since HTTP/1.0 did not define any 1xx status codes, servers MUST NOT send a 1xx response to an HTTP/1.0 client except under experimental conditions.
 * <br>A client MUST be prepared to accept one or more 1xx status responses prior to a regular response, even if the client does not expect a 100 (Continue) status message. Unexpected 1xx status responses MAY be ignored by a user agent.
 * <br> Proxies MUST forward 1xx responses, unless the connection between the proxy and its client has been closed, or unless the proxy itself requested the generation of the 1xx response. (For example, if a proxy adds a"Expect: 100-continue" field when it forwards a request, then it need not forward the corresponding 100 (Continue) response(s).)
 * <p>2xx - Successful
 * <br> This class of status code indicates that the client's request was successfully received, understood, and accepted.
 * <p>3xx - Redirection
 * <br> This class of status code indicates that further action needs to be taken by the user agent in order to fulfill the request.  The action required MAY be carried out by the user agent without interaction with the user if and only if the method used in the second request is GET or HEAD. A client SHOULD detect infinite redirection loops, since such loops generate network traffic for each redirection.
 * <br> Note: previous versions of this specification recommended a maximum of five redirections. Content developers should be aware that there might be clients that implement such a fixed limitation.
 * <p>4xx - Client Error
 * <br> The 4xx class of status code is intended for cases in which the client seems to have erred. Except when responding to a HEAD request, the server SHOULD include an entity containing an explanation of the error situation, and whether it is a temporary or permanent condition. These status codes are applicable to any request method. User agents SHOULD display any included entity to the user.
 * <br> If the client is sending data, a server implementation using TCP SHOULD be careful to ensure that the client acknowledges receipt of the packet(s) containing the response, before the server closes the input connection. If the client continues sending data to the server after the close, the server's TCP stack will send a reset packet to the client, which may erase the client's unacknowledged input buffers before they can be read and interpreted by the HTTP application.
 * <p>5xx - Server Error
 * <br>Response status codes beginning with the digit"5" indicate cases in which the server is aware that it has erred or is incapable of performing the request. Except when responding to a HEAD request, the server SHOULD include an entity containing an explanation of the error situation, and whether it is a temporary or permanent condition. User agents SHOULD display any included entity to the user. These response codes are applicable to any request method.
 */
public enum HttpStatus {

	$100("Continue"), //
	$101("Switching Protocols"), //
	/**
	 * $2xx - Successful
	 * This class of status code indicates that the client's request was successfully received, understood, and accepted.
	 */
	$200("OK"), //
	$201("Created"), //
	$202("Accepted"), //
	$203("Non-Authoritative Information"), //
	$204("No Content"), //
	$205("Reset Content"), //
	$206("Partial Content"), //
	$207("Multi-Status"), //

	/**
	 * 3xx - Redirection
	 * This class of status code indicates that further action needs to be taken by the user agent in order to fulfill the request.  The action required MAY be carried out by the user agent without interaction with the user if and only if the method used in the second request is GET or HEAD. A client SHOULD detect infinite redirection loops, since such loops generate network traffic for each redirection.
	 * <p/>
	 * Note: previous versions of this specification recommended a maximum of five redirections. Content developers should be aware that there might be clients that implement such a fixed limitation.
	 */
	$300("Multiple Choices"), //
	$301("Moved Permanently"), //
	$302("Found"), //
	$303("See Other"), //
	$304("Not Modified"), //
	$305("Use Proxy"), //
	$306("(Reserved)"), //
	$307("Temporary Redirect"), //
	/**
	 * $4xx("Client Error"),
	 * The 4xx class of status code is intended for cases in which the client seems to have erred. Except when responding to a HEAD request, the server SHOULD include an entity containing an explanation of the error situation, and whether it is a temporary or permanent condition. These status codes are applicable to any request method. User agents SHOULD display any included entity to the user.
	 * If the client is sending data, a server implementation using TCP SHOULD be careful to ensure that the client acknowledges receipt of the packet(s) containing the response, before the server closes the input connection. If the client continues sending data to the server after the close, the server's TCP stack will send a reset packet to the client, which may erase the client's unacknowledged input buffers before they can be read and interpreted by the HTTP application.
	 */
	$400("Bad Request"), //
	$401("Unauthorized"), //
	$402("Payment Required"), //
	$403("Forbidden"), //
	$404("Not Found"), //
	$405("Method Not"), //
	$406("Not Acceptable"), //
	$407("Proxy Authentication"), //
	$408("Request Timeout"), //
	$409("Conflict"), //
	$410("Gone"), //
	$411("Length Required"), //
	$412("Precondition Failed"), //
	$413("Request Entity Too Large"), //
	$414("Request-URI Too Long"), //
	$415("Unsupported Media Type"), //
	$416("Requested Range Not Satisfiable"), //
	$417("Expectation Failed"), //
	$422("Unprocessable Entity"), //
	$423("Locked"), //
	$424("Failed Dependency"), //
	$5xx("Server Error"), //
	/**
	 * Response status codes beginning with the digit"5" indicate cases in which the server is aware that it has erred or is incapable of performing the request. Except when responding to a HEAD request, the server SHOULD include an entity containing an explanation of the error situation, and whether it is a temporary or permanent condition. User agents SHOULD display any included entity to the user. These response codes are applicable to any request method.
	 */
	$500("Internal Server Error"), //
	$501("Not Implemented"), //
	$502("Bad Gateway"), //
	$503("Service Unavailable"), //
	$504("Gateway Timeout"), //
	$505("HTTP Version Not Supported"), //
	$507("Insufficient Storage"); //
	public final String caption;

	HttpStatus(String caption) {
		this.caption = caption;;
	}
}

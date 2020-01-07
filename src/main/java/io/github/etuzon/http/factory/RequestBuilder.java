package io.github.etuzon.http.factory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;

import io.github.etuzon.http.enums.HeaderEnum;
import io.github.etuzon.http.enums.RequestTypeEnum;
import io.github.etuzon.http.exceptions.InvalidHttpRequestException;
import io.github.etuzon.projects.core.utils.StringUtil;

/************************************************************
 * Build HTTP requests.
 * 
 * @author Eyal Tuzon
 *
 */
public class RequestBuilder {
	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private final String baseUrl;

	private List<BasicHeader> headerList = new ArrayList<BasicHeader>();
	private RequestTypeEnum requestType;
	private String fullUrl = null;
	private String username = null;
	private String password = null;
	private String jSessionId = null;
	private StringEntity entity = new StringEntity("", UTF8_CHARSET);
	private boolean releaseConnection = true;

	/*************************************************
	 * Constructor.
	 * 
	 * @param baseUrl Base URL.
	 */
	public RequestBuilder(String baseUrl) {
		this(baseUrl, RequestTypeEnum.GET);
	}

	/*************************************************
	 * Constructor.
	 * 
	 * @param baseUrl     Base URL.
	 * @param requestType Request type.
	 */
	public RequestBuilder(String baseUrl, RequestTypeEnum requestType) {
		this.baseUrl = baseUrl;
		this.requestType = requestType;
	}

	/*************************************************
	 * Constructor.
	 * 
	 * @param baseUrl Base URL.
	 * @param port    Port.
	 */
	public RequestBuilder(String baseUrl, int port) {
		this(baseUrl, port, RequestTypeEnum.GET);
	}

	/*************************************************
	 * Constructor.
	 * 
	 * @param baseUrl     Base URL.
	 * @param port        Port.
	 * @param requestType Request type.
	 */
	public RequestBuilder(String baseUrl, int port, RequestTypeEnum requestType) {
		if ((port != 80) && (port != 443)) {
			baseUrl += ":" + port;
		}

		this.baseUrl = baseUrl;
		this.fullUrl = baseUrl;
		this.requestType = requestType;
	}

	/*************************************************
	 * Set suffix to URL.
	 * 
	 * Example http://URL/Suffix.
	 * 
	 * @param suffixUrl URL Suffix.
	 * @return RequestBuilder.
	 */
	public RequestBuilder setSuffixUrl(String suffixUrl) {
		fullUrl = getFullUrl(suffixUrl);
		return this;
	}

	/*************************************************
	 * Set credentials (Basic authentication header) in HTTP request.
	 * 
	 * @param username Username.
	 * @param password Password.
	 * @return RequestBuilder.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 */
	public RequestBuilder setCredentials(String username, String password) throws InvalidHttpRequestException {
		if (jSessionId != null) {
			throw new InvalidHttpRequestException(
					"Unable to set username and password in RequestBuilder when JSESSIONID [" + jSessionId
							+ "] already set");
		}

		this.username = username;
		this.password = password;

		return this;
	}

	/*************************************************
	 * Set JSESSIONID in HTTP request.
	 * 
	 * @param jSessionId JSESSIONID.
	 * @return RequestBuilder.
	 */
	public RequestBuilder setJSessionId(String jSessionId) {
		this.jSessionId = jSessionId;
		this.username = null;
		this.password = null;

		return this;
	}

	/*************************************************
	 * Add header to HTTP request.
	 * 
	 * @param header Header.
	 * @return RequestBuilder.
	 */
	public RequestBuilder addHeader(BasicHeader header) {
		headerList.add(header);
		return this;
	}

	/*************************************************
	 * Add headers to HTTP request.
	 * 
	 * @param headerList List of headers.
	 * @return RequestBuilder.
	 */
	public RequestBuilder addHeaders(List<BasicHeader> headerList) {
		this.headerList.addAll(headerList);
		return this;
	}

	/*************************************************
	 * Set header in HTTP request.
	 * 
	 * @param header Header.
	 * @return RequestBuilder.
	 */
	public RequestBuilder setHeader(BasicHeader header) {
		headerList = new ArrayList<BasicHeader>();
		headerList.add(header);
		return this;
	}

	/*************************************************
	 * Set headers in HTTP request.
	 * 
	 * @param headerList List of headers.
	 * @return RequestBuilder.
	 */
	public RequestBuilder setHeaders(List<BasicHeader> headerList) {
		if (headerList != null) {
			this.headerList = new ArrayList<BasicHeader>();
			this.headerList.addAll(headerList);
		}

		return this;
	}

	/*************************************************
	 * Set entity in HTTP request.
	 * 
	 * Entity (Request body) is supported only in POST and PUT requests.
	 * 
	 * @param entityStr Entity.
	 * @return RequestBuilder.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 */
	public RequestBuilder setEntity(String entityStr) throws InvalidHttpRequestException {
		if ((requestType != RequestTypeEnum.POST) && (requestType != RequestTypeEnum.PUT)) {
			throw new InvalidHttpRequestException("Request type [" + requestType + "] should not contain entity");
		}

		entity = new StringEntity(entityStr, UTF8_CHARSET);
		return this;
	}

	/*************************************************
	 * Set request type.
	 * 
	 * @param requestType Request type.
	 * @return RequestBuilder.
	 */
	public RequestBuilder setRequestType(RequestTypeEnum requestType) {
		this.requestType = requestType;
		return this;
	}

	/*************************************************
	 * Set Connection header in HTTP request.
	 * 
	 * @param releaseConnection In case value is true than Connection header is 'Close',
	 *                          else header value is keep-alive.
	 * @return RequestBuilder.
	 */
	public RequestBuilder setReleaseConnection(boolean releaseConnection) {
		this.releaseConnection = releaseConnection;
		return this;
	}

	/*************************************************
	 * Build HTTP request.
	 * 
	 * @return HTTP request object.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 */
	public HttpUriRequestBase build() throws InvalidHttpRequestException {
		if (requestType == RequestTypeEnum.GET) {
			return buildGetRequest();
		} else if (requestType == RequestTypeEnum.POST) {
			return buildPostRequest();
		} else if (requestType == RequestTypeEnum.DELETE) {
			return buildDeleteRequest();
		} else if (requestType == RequestTypeEnum.PUT) {
			return buildPutRequest();
		}

		throw new InvalidHttpRequestException("Bug: Request [" + requestType + "] builder not exists");
	}

	/*************************************************
	 * Return release connection value.
	 * 
	 * @return true in case release connection value is true,
	 *              else return false.
	 */
	public boolean isReleaseConnection() {
		return releaseConnection;
	}

	private HttpGet buildGetRequest() {
		HttpGet request = new HttpGet(fullUrl);

		request = (HttpGet) addBasicHeadersToRequest(request, releaseConnection);
		request = (HttpGet) addHeadersToRequest(request, headerList);

		return request;
	}

	private HttpPost buildPostRequest() {
		HttpPost request = new HttpPost(fullUrl);

		request = (HttpPost) addBasicHeadersToRequest(request, releaseConnection);
		request = (HttpPost) addHeadersToRequest(request, headerList);

		request.setEntity(entity);

		return request;
	}

	private HttpDelete buildDeleteRequest() {
		HttpDelete request = new HttpDelete(fullUrl);

		request = (HttpDelete) addBasicHeadersToRequest(request, releaseConnection);
		request = (HttpDelete) addHeadersToRequest(request, headerList);

		return request;
	}

	private HttpPut buildPutRequest() {
		HttpPut request = new HttpPut(fullUrl);

		request = (HttpPut) addBasicHeadersToRequest(request, releaseConnection);
		request = (HttpPut) addHeadersToRequest(request, headerList);

		request.setEntity(entity);

		return request;
	}
	
	private HttpUriRequestBase addBasicHeadersToRequest(HttpUriRequestBase request, boolean releaseConnection) {
		if (isAuthentication()) {
			request.addHeader(HeaderEnum.AUTORIZATION.getName(), getAutorizationHeaderValue());
		} else if (isJSessionId()) {
			request.addHeader(HeaderEnum.COOKIE.getName(), "JSESSIONID=" + jSessionId);
		}

		if (releaseConnection) {
			request.addHeader(HeaderEnum.CONNECTION.getName(), "close");
		}

		return request;
	}

	private String getFullUrl(String suffixUrl) {
		if (suffixUrl == null) {
			return baseUrl;
		}

		if (suffixUrl.isEmpty()) {
			return baseUrl;
		}

		if ((baseUrl.endsWith("/")) && suffixUrl.startsWith("/")) {
			return StringUtil.removeLastChar(baseUrl) + suffixUrl;
		}

		if ((baseUrl.endsWith("/")) || suffixUrl.startsWith("/")) {
			return baseUrl + suffixUrl;
		}

		return baseUrl + "/" + suffixUrl;
	}

	private HttpUriRequestBase addHeadersToRequest(HttpUriRequestBase request, List<BasicHeader> headerList) {
		if (headerList != null) {
			for (Header header : headerList) {
				request.addHeader(header);
			}
		}

		return request;
	}

	private String getAutorizationHeaderValue() {
		String encoding = Base64.getEncoder()
				.encodeToString((username + ":" + password).getBytes(Charset.forName("UTF-8")));
		return "Basic " + encoding;
	}

	private boolean isAuthentication() {
		return (username != null);
	}

	private boolean isJSessionId() {
		return (jSessionId != null);
	}
}
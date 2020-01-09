package io.github.etuzon.http.client;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.message.BasicHeader;

import io.github.etuzon.http.certificate.CertificateInstaller;
import io.github.etuzon.http.enums.RequestTypeEnum;
import io.github.etuzon.http.exceptions.HttpException;
import io.github.etuzon.http.exceptions.InvalidHttpRequestException;
import io.github.etuzon.http.factory.RequestBuilder;
import io.github.etuzon.http.objects.HttpObject;
import io.github.etuzon.projects.core.utils.StringUtil;

/*****************************************************************
 * HTTP Client.
 * 
 * @author Eyal Tuzon
 *
 */
public class HttpClient {
	public static final int HTTP_PORT = 80;
	public static final int HTTPS_PORT = 443;

	public static final boolean RELEASE_CONNECTION = true;
	public static final boolean NOT_RELEASE_CONNECTION = false;

	private final String baseUrl;
	private RequestBuilder requestBuilder = null;
	private CloseableHttpClient httpClient = null;

	/*******************************************
	 * Constructor.
	 * 
	 * @param baseUrl Basic URL not include port and URL suffix.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed to init HTTP client.
	 */
	public HttpClient(String baseUrl) throws InvalidHttpRequestException, HttpException {
		this(baseUrl, -1);
	}

	/*******************************************
	 * Constructor.
	 * 
	 * @param baseUrl Basic URL not include port and URL suffix.
	 * @param port Port.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed to init HTTP client.
	 */
	public HttpClient(String baseUrl, int port) throws InvalidHttpRequestException, HttpException {
		this(baseUrl, null, null, port);
	}

	/*******************************************
	 * Constructor.
	 * 
	 * @param baseUrl    Basic URL not include port and URL suffix.
	 * @param jSessionId JESSIONID.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed to init HTTP client.
	 */
	public HttpClient(String baseUrl, String jSessionId) throws InvalidHttpRequestException, HttpException {
		this(baseUrl, jSessionId, -1);
	}

	/*******************************************
	 * Constructor.
	 * 
	 * @param baseUrl    Basic URL not include port and URL suffix.
	 * @param jSessionId JESSIONID.
	 * @param port Port.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed to init HTTP client.
	 */
	public HttpClient(String baseUrl, String jSessionId, int port) throws InvalidHttpRequestException, HttpException {
		this(baseUrl, null, null, port);
		requestBuilder.setJSessionId(jSessionId);
	}

	/*******************************************
	 * Constructor.
	 * 
	 * @param baseUrl  Basic URL not include port and URL suffix.
	 * @param username Username.
	 * @param password Password.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed to init HTTP client.
	 */
	public HttpClient(String baseUrl, String username, String password)
			throws InvalidHttpRequestException, HttpException {
		this(baseUrl, username, password, -1);
	}

	/*******************************************
	 * Constructor.
	 * 
	 * @param baseUrl  Basic URL not include port and URL suffix.
	 * @param username Username.
	 * @param password Password.
	 * @param port     Port.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed to init HTTP client.
	 */
	public HttpClient(String baseUrl, String username, String password, int port)
			throws InvalidHttpRequestException, HttpException {
		this.baseUrl = baseUrl;

		if (port != -1) {
			requestBuilder = new RequestBuilder(baseUrl, port);
		} else {
			requestBuilder = new RequestBuilder(baseUrl);

		}
		if ((username != null) && (password != null)) {
			requestBuilder.setCredentials(username, password);
		}

		installCertificateIfHttps();

		httpClient = buildHttpClient();
	}

	/*******************************************
	 * Check if URL is HTTPS.
	 * 
	 * @return true in case URL start with 'https://'.
	 */
	public boolean isHttps() {
		return baseUrl.toLowerCase().startsWith("https://");
	}

	/*******************************************
	 * Sent POST request.
	 * 
	 * @param headerList List of headers.
	 * @return HTTP object that contain HTTP request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendPost(List<BasicHeader> headerList) throws InvalidHttpRequestException, HttpException {
		return sendPost(null, headerList, null);
	}

	/*******************************************
	 * Sent POST request.
	 * 
	 * @return HTTP object that contain HTTP request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendPost() throws InvalidHttpRequestException, HttpException {
		return sendPost(null, null, "");
	}

	/*******************************************
	 * Sent POST request.
	 * 
	 * @param suffixUrl URL suffix.
	 * @return HTTP object that contain HTTP request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendPost(String suffixUrl) throws InvalidHttpRequestException, HttpException {
		return sendPost(suffixUrl, null, "");
	}

	/*******************************************
	 * Sent POST request.
	 * 
	 * @param suffixUrl  URL suffix.
	 * @param headerList List of headers.
	 * @return HTTP object that contain HTTP request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendPost(String suffixUrl, List<BasicHeader> headerList)
			throws InvalidHttpRequestException, HttpException {
		return sendPost(suffixUrl, headerList, "");
	}

	/*******************************************
	 * Sent POST request.
	 * 
	 * @param headerList List of headers.
	 * @param entity     Body of request.
	 * @return HttpObject which contains request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendPost(List<BasicHeader> headerList, String entity)
			throws InvalidHttpRequestException, HttpException {
		return sendPost(null, headerList, entity);
	}

	/*******************************************
	 * Sent POST request.
	 * 
	 * @param suffixUrl  URL suffix.
	 * @param headerList List of headers.
	 * @param entity     Body of request.
	 * @return HttpObject which contains request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendPost(String suffixUrl, List<BasicHeader> headerList, String entity)
			throws InvalidHttpRequestException, HttpException {
		return sendPost(suffixUrl, headerList, entity, RELEASE_CONNECTION);
	}

	/*******************************************
	 * Sent POST request.
	 * 
	 * @param suffixUrl         URL suffix.
	 * @param headerList        List of headers.
	 * @param entity            Body of request.
	 * @param releaseConnection Set 'Connection' header value. In case value is true
	 *                          than 'Connection: keep-alive' else 'Connection:
	 *                          close'.
	 * @return HttpObject which contains request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendPost(String suffixUrl, List<BasicHeader> headerList, String entity, boolean releaseConnection)
			throws InvalidHttpRequestException, HttpException {
		requestBuilder.setRequestType(RequestTypeEnum.POST).setSuffixUrl(suffixUrl).setHeaders(headerList)
				.setEntity(entity).setReleaseConnection(releaseConnection);
		HttpPost request = (HttpPost) requestBuilder.build();
		return executeRequest(request);
	}

	/*******************************************
	 * Send GET request.
	 * 
	 * @return HttpObject which contains request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendGet() throws InvalidHttpRequestException, HttpException {
		return sendGet("");
	}

	/*******************************************
	 * Send GET request.
	 * 
	 * @param suffixUrl URL suffix.
	 * @return HttpObject which contains request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendGet(String suffixUrl) throws InvalidHttpRequestException, HttpException {
		return sendGet(suffixUrl, null);
	}

	/*******************************************
	 * Send GET request.
	 * 
	 * @param suffixUrl  URL suffix.
	 * @param headerList List of headers.
	 * @return which contains request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendGet(String suffixUrl, List<BasicHeader> headerList)
			throws InvalidHttpRequestException, HttpException {
		return sendGet(suffixUrl, headerList, RELEASE_CONNECTION);
	}

	/*******************************************
	 * Send GET request.
	 * 
	 * @param suffixUrl         URL suffix.
	 * @param headerList        List of headers.
	 * @param releaseConnection Set 'Connection' header value. In case value is true
	 *                          than 'Connection: keep-alive' else 'Connection:
	 *                          close'.
	 * @return HttpObject which contains request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendGet(String suffixUrl, List<BasicHeader> headerList, boolean releaseConnection)
			throws InvalidHttpRequestException, HttpException {
		requestBuilder.setRequestType(RequestTypeEnum.GET).setSuffixUrl(suffixUrl).setHeaders(headerList)
				.setReleaseConnection(releaseConnection);
		HttpGet request = (HttpGet) requestBuilder.build();
		return executeRequest(request);
	}

	/*******************************************
	 * Send DELETE request.
	 * 
	 * @param suffixUrl         URL suffix.
	 * @param releaseConnection Set 'Connection' header value. In case value is true
	 *                          than 'Connection: keep-alive' else 'Connection:
	 *                          close'.
	 * @return HttpObject which contains request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendDelete(String suffixUrl, boolean releaseConnection)
			throws InvalidHttpRequestException, HttpException {
		return sendDelete(suffixUrl, null, releaseConnection);
	}

	/*******************************************
	 * Send DELETE request.
	 * 
	 * @param suffixUrl         URL suffix.
	 * @param headerList        List of headers.
	 * @param releaseConnection Set 'Connection' header value. In case value is true
	 *                          than 'Connection: keep-alive' else 'Connection:
	 *                          close'.
	 * @return HttpObject which contains request and response.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpObject sendDelete(String suffixUrl, List<BasicHeader> headerList, boolean releaseConnection)
			throws InvalidHttpRequestException, HttpException {
		requestBuilder.setRequestType(RequestTypeEnum.DELETE).setSuffixUrl(suffixUrl).setHeaders(headerList)
				.setReleaseConnection(releaseConnection);
		HttpDelete request = (HttpDelete) requestBuilder.build();
		return executeRequest(request);
	}

	/*******************************************
	 * Sent PUT request.
	 * 
	 * @return HttpObject which contains request and response.
	 * @throws HttpException               in case failed send HTTP request.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 */
	public HttpObject sendPut() throws HttpException, InvalidHttpRequestException {
		return sendPut("");
	}

	/*******************************************
	 * Sent PUT request.
	 * 
	 * @param suffixUrl Sent PUT request.
	 * @return HttpObject which contains request and response.
	 * @throws HttpException               in case failed send HTTP request.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 */
	public HttpObject sendPut(String suffixUrl) throws HttpException, InvalidHttpRequestException {
		return sendPut(suffixUrl, "");
	}

	/*******************************************
	 * Sent PUT request.
	 * 
	 * @param suffixUrl Sent PUT request.
	 * @param entity    Body of request.
	 * @return HttpObject which contains request and response.
	 * @throws HttpException               in case failed send HTTP request.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 */
	public HttpObject sendPut(String suffixUrl, String entity) throws HttpException, InvalidHttpRequestException {
		return sendPut(suffixUrl, null, entity);
	}

	/*******************************************
	 * Sent PUT request.
	 * 
	 * @param suffixUrl  URL suffix.
	 * @param headerList List of headers.
	 * @param entity     Body of request.
	 * @return HttpObject which contains request and response.
	 * @throws HttpException               in case failed send HTTP request.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 */
	public HttpObject sendPut(String suffixUrl, List<BasicHeader> headerList, String entity)
			throws HttpException, InvalidHttpRequestException {
		return sendPut(suffixUrl, headerList, entity, RELEASE_CONNECTION);
	}

	/*******************************************
	 * Sent PUT request.
	 * 
	 * @param suffixUrl         URL suffix.
	 * @param headerList        List of headers.
	 * @param entity            Body of request.
	 * @param releaseConnection Set 'Connection' header value. In case value is true
	 *                          than 'Connection: keep-alive' else 'Connection:
	 *                          close'.
	 * @return HttpObject which contains request and response.
	 * @throws HttpException               in case failed send HTTP request.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 */
	public HttpObject sendPut(String suffixUrl, List<BasicHeader> headerList, String entity, boolean releaseConnection)
			throws HttpException, InvalidHttpRequestException {
		requestBuilder.setRequestType(RequestTypeEnum.PUT).setSuffixUrl(suffixUrl).setHeaders(headerList)
				.setEntity(entity).setReleaseConnection(releaseConnection);
		HttpPut request = (HttpPut) requestBuilder.build();
		return executeRequest(request);
	}

	/*******************************************
	 * Send a-synchronized GET request.
	 * 
	 * @return HTTP a-synchronized client object which contain session thread.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpAsyncClient sendAsyncGet() throws InvalidHttpRequestException, HttpException {
		return sendAsyncGet("");
	}

	/*******************************************
	 * Send a-synchronized GET request.
	 * 
	 * @param suffixUrl URL suffix.
	 * @return HTTP a-synchronized client object which contain session thread.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpAsyncClient sendAsyncGet(String suffixUrl) throws InvalidHttpRequestException, HttpException {
		return sendAsyncGet(suffixUrl, null);
	}

	/*******************************************
	 * Send a-synchronized GET request.
	 * 
	 * @param suffixUrl  URL suffix.
	 * @param headerList List of headers.
	 * @return HTTP a-synchronized client object which contain session thread.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpAsyncClient sendAsyncGet(String suffixUrl, List<BasicHeader> headerList)
			throws InvalidHttpRequestException, HttpException {
		return sendAsyncGet(suffixUrl, headerList, NOT_RELEASE_CONNECTION);
	}

	/*******************************************
	 * Send a-synchronized GET request.
	 * 
	 * @param suffixUrl         URL suffix.
	 * @param headerList        List of headers.
	 * @param releaseConnection Set 'Connection' header value. In case value is true
	 *                          than 'Connection: keep-alive' else 'Connection:
	 *                          close'.
	 * @return HTTP a-synchronized client object which contain session thread.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpAsyncClient sendAsyncGet(String suffixUrl, List<BasicHeader> headerList, boolean releaseConnection)
			throws InvalidHttpRequestException, HttpException {
		requestBuilder.setRequestType(RequestTypeEnum.GET).setSuffixUrl(suffixUrl).setHeaders(headerList)
				.setReleaseConnection(releaseConnection);
		HttpAsyncClient asyncThread = new HttpAsyncClient(requestBuilder);
		asyncThread.start();
		return asyncThread;
	}

	/*******************************************
	 * Send a-synchronized POST request.
	 * 
	 * @param entity Body of request.
	 * @return HTTP a-synchronized client object which contain session thread.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpAsyncClient sendAsyncPost(String entity) throws InvalidHttpRequestException, HttpException {
		return sendAsyncPost("", entity);
	}

	/*******************************************
	 * Send a-synchronized POST request.
	 * 
	 * @param suffixUrl URL suffix.
	 * @param entity    Body of request.
	 * @return HTTP a-synchronized client object which contain session thread.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpAsyncClient sendAsyncPost(String suffixUrl, String entity)
			throws InvalidHttpRequestException, HttpException {
		return sendAsyncPost(suffixUrl, null, entity);
	}

	/*******************************************
	 * Send a-synchronized POST request.
	 * 
	 * @param suffixUrl  URL suffix.
	 * @param headerList List of headers.
	 * @param entity     Body of request.
	 * @return HTTP a-synchronized client object which contain session thread.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpAsyncClient sendAsyncPost(String suffixUrl, List<BasicHeader> headerList, String entity)
			throws InvalidHttpRequestException, HttpException {
		return sendAsyncPost(suffixUrl, headerList, entity, NOT_RELEASE_CONNECTION);
	}

	/*******************************************
	 * Send a-synchronized POST request.
	 * 
	 * @param suffixUrl         URL suffix.
	 * @param headerList        List of headers.
	 * @param entity            Body of request.
	 * @param releaseConnection Set 'Connection' header value. In case value is true
	 *                          than 'Connection: keep-alive' else 'Connection:
	 *                          close'.
	 * @return HTTP a-synchronized client object which contain session thread.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed send HTTP request.
	 */
	public HttpAsyncClient sendAsyncPost(String suffixUrl, List<BasicHeader> headerList, String entity,
			boolean releaseConnection) throws InvalidHttpRequestException, HttpException {
		requestBuilder.setRequestType(RequestTypeEnum.POST).setSuffixUrl(suffixUrl).setHeaders(headerList)
				.setEntity(entity).setReleaseConnection(releaseConnection);
		HttpAsyncClient asyncThread = new HttpAsyncClient(requestBuilder);
		asyncThread.start();
		return asyncThread;
	}

	private HttpObject executeRequest(HttpUriRequestBase request) throws HttpException {
		CloseableHttpResponse response = executeHttpRequest(request);
		return new HttpObject(request, response);
	}

	private CloseableHttpResponse executeHttpRequest(HttpUriRequestBase request) throws HttpException {
		try {
			return httpClient.execute(request);
		} catch (IOException e) {
			throw new HttpException(e);
		}
	}

	private void installCertificateIfHttps() throws HttpException {
		if (isHttps()) {
			try {
				CertificateInstaller.installCertificate(baseUrl);
			} catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException | CertificateException
					| IOException e) {
				throw new HttpException(
						"Unable to install certificate of [" + baseUrl + "]\n" + StringUtil.getExceptionStacktrace(e));
			}
		}
	}

	private CloseableHttpClient buildHttpClient() {
		return HttpClientBuilder.create().build();
	}

	/*******************************************
	 * Get base HTTP/HTTPS url without port and suffix.
	 * 
	 * @param url URL.
	 * @return base HTTP/HTTPS url without port and suffix.
	 */
	public static String getBaseUrl(String url) {
		final String START_STR = "://";

		int startIndex = url.indexOf(START_STR);

		if (startIndex != -1) {
			startIndex += START_STR.length();
		} else {
			startIndex = 0;
		}

		int slashIndex = url.indexOf("/", startIndex);
		int colonIndex = url.indexOf(":", startIndex);

		if ((slashIndex == -1) && (colonIndex == -1)) {
			return url;
		}

		if (slashIndex == -1) {
			return url.substring(0, colonIndex);
		}

		if (colonIndex == -1) {
			return url.substring(0, slashIndex);
		}

		if (slashIndex < colonIndex) {
			return url.substring(0, slashIndex);
		}

		return url.substring(0, colonIndex);
	}
}
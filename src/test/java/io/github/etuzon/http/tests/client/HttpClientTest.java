package io.github.etuzon.http.tests.client;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.github.etuzon.http.client.HttpClient;
import io.github.etuzon.http.exceptions.HttpException;
import io.github.etuzon.http.exceptions.InvalidHttpRequestException;
import io.github.etuzon.http.objects.HttpObject;
import io.github.etuzon.http.objects.HttpResponse;
import io.github.etuzon.http.tests.parameters.TestParameters;
import io.github.etuzon.unit.tests.asserts.SoftAssertUnitTest;
import io.github.etuzon.unit.tests.exceptions.AutomationUnitTestException;

/************************************************
 * Used https://requestbin.com to get HTTPS response. Username eyaltuzon via
 * eyal.tuzon.dev@gmail.com.
 * 
 * @author etuzon
 *
 */
public class HttpClientTest extends HttpClientTestBase implements TestParameters {
	public static final String BASE_URL_WITHOUT_HTTP_PREFIX = "baseurl.http.test";
	public static final String HTTP_BASE_URL = "http://baseurl.http.test";
	public static final String HTTPS_BASE_URL = "https://baseurl.http.test";

	public static final String USERNAME = "usertest";
	public static final String PASSWORD = "passwordtest";

	public static final String J_SESSION_ID = "J_SESSION_ID_Value";

	public static final String EXPECTED_RESPONSE_BODY_FOR_REQUEST_WITH_JSESSION_ID = "\"Cookie\": \"JSESSIONID=J_SESSION_ID_Value\",";

	@DataProvider(name = "httpUrl")
	public Object[][] getHttpUrl() {
		return new Object[][] { { HTTP_BASE_URL }, { HTTP_BASE_URL + ":3000" }, { HTTP_BASE_URL + "/suffix" },
				{ HTTP_BASE_URL + ":3000/suffix" } };
	}

	@Test(dataProvider = "httpUrl")
	public void getBaseUrl_http_test(String fullUrl) {
		String baseUrlTest = HttpClient.getBaseUrl(fullUrl);
		SoftAssertUnitTest.assertTrueNow(
				baseUrlTest.equals(HTTP_BASE_URL), "Base URL is [" + baseUrlTest + "] and should be [" + HTTP_BASE_URL
						+ "] for full URL [" + fullUrl + "]",
				"Verify that base URL is [" + HTTP_BASE_URL + "] for full URL [" + fullUrl + "]");
	}

	@DataProvider(name = "httpsUrl")
	public Object[][] getHttpsUrl() {
		return new Object[][] { { HTTPS_BASE_URL }, { HTTPS_BASE_URL + ":3000" }, { HTTPS_BASE_URL + "/suffix" },
				{ HTTPS_BASE_URL + ":3000/suffix" } };
	}

	@Test(dataProvider = "httpsUrl")
	public void getBaseUrl_https_test(String fullUrl) {
		String baseUrlTest = HttpClient.getBaseUrl(fullUrl);
		SoftAssertUnitTest.assertTrueNow(
				baseUrlTest.equals(HTTPS_BASE_URL), "Base URL is [" + baseUrlTest + "] and should be [" + HTTPS_BASE_URL
						+ "] for full URL [" + fullUrl + "]",
				"Verify that base URL is [" + HTTPS_BASE_URL + "] for full URL [" + fullUrl + "]");
	}

	@DataProvider(name = "urlWithoutHttpPrefix")
	public Object[][] getUrlWithoutHttpPrefix() {
		return new Object[][] { { BASE_URL_WITHOUT_HTTP_PREFIX }, { BASE_URL_WITHOUT_HTTP_PREFIX + ":3000" },
				{ BASE_URL_WITHOUT_HTTP_PREFIX + "/suffix" }, { BASE_URL_WITHOUT_HTTP_PREFIX + ":3000/suffix" } };
	}

	@Test(dataProvider = "urlWithoutHttpPrefix")
	public void getBaseUrl_without_http_prefix_test(String fullUrl) {
		String baseUrlTest = HttpClient.getBaseUrl(fullUrl);
		SoftAssertUnitTest.assertTrueNow(baseUrlTest.equals(BASE_URL_WITHOUT_HTTP_PREFIX),
				"Base URL is [" + baseUrlTest + "] and should be [" + BASE_URL_WITHOUT_HTTP_PREFIX + "] for full URL ["
						+ fullUrl + "]",
				"Verify that base URL is [" + BASE_URL_WITHOUT_HTTP_PREFIX + "] for full URL [" + fullUrl + "]");
	}

	@Test
	public void send_GET_request_test() throws AutomationUnitTestException {
		HttpClient httpClient = initHttpClient(HTTP_ADDRESS, HttpClient.HTTP_PORT);

		HttpObject httpObject = null;

		try {
			httpObject = httpClient.sendGet("get");
		} catch (HttpException | InvalidHttpRequestException e) {
			throw new AutomationUnitTestException(e);
		}

		verifyHasResponse(httpObject);
		verifyStatusCode(httpObject.getResponse(), STATUS_CODE_200);
	}

	@Test
	public void send_POST_request_test() throws AutomationUnitTestException {
		HttpClient httpClient = initHttpClient(HTTP_ADDRESS, HttpClient.HTTP_PORT);

		HttpObject httpObject = null;

		try {
			httpObject = httpClient.sendPost("post");
		} catch (HttpException | InvalidHttpRequestException e) {
			throw new AutomationUnitTestException(e);
		}

		verifyHasResponse(httpObject);
		verifyStatusCode(httpObject.getResponse(), STATUS_CODE_200);
	}

	@Test
	public void send_DEL_request_test() throws AutomationUnitTestException {
		HttpClient httpClient = initHttpClient(HTTP_ADDRESS, HttpClient.HTTP_PORT);

		HttpObject httpObject = null;

		try {
			httpObject = httpClient.sendDelete("delete", false);
		} catch (HttpException | InvalidHttpRequestException e) {
			throw new AutomationUnitTestException(e);
		}

		verifyHasResponse(httpObject);
		verifyStatusCode(httpObject.getResponse(), STATUS_CODE_200);
	}

	@Test
	public void send_PUT_request_test() throws AutomationUnitTestException {
		HttpClient httpClient = initHttpClient(HTTP_ADDRESS);

		HttpObject httpObject = null;

		try {
			httpObject = httpClient.sendPut("put");
		} catch (HttpException | InvalidHttpRequestException e) {
			throw new AutomationUnitTestException(e);
		}

		verifyHasResponse(httpObject);
		verifyStatusCode(httpObject.getResponse(), STATUS_CODE_200);
	}
	
	@Test
	public void send_HTTPS_GET_request_test() throws AutomationUnitTestException {
		HttpClient httpClient = initHttpClient(HTTPS_ADDRESS);

		HttpObject httpObject = null;

		try {
			httpObject = httpClient.sendGet("get");
		} catch (HttpException | InvalidHttpRequestException e) {
			throw new AutomationUnitTestException(e);
		}

		verifyHasResponse(httpObject);
		verifyStatusCode(httpObject.getResponse(), STATUS_CODE_200);
	}

	@Test
	public void send_GET_with_basic_authentication_request_test() throws AutomationUnitTestException {
		HttpClient httpClient = initHttpClient(HTTP_ADDRESS, HttpClient.HTTP_PORT, USERNAME, PASSWORD);

		HttpObject httpObject = null;

		try {
			httpObject = httpClient.sendGet("basic-auth/" + USERNAME + "/" + PASSWORD);
		} catch (HttpException | InvalidHttpRequestException e) {
			throw new AutomationUnitTestException(e);
		}

		verifyHasResponse(httpObject);
		verifyStatusCode(httpObject.getResponse(), STATUS_CODE_200);
	}

	@Test
	public void send_GET_with_J_SESSION_ID_cookie_request_test() throws AutomationUnitTestException {
		HttpClient httpClient = initHttpClient(HTTP_ADDRESS, HttpClient.HTTP_PORT, J_SESSION_ID);

		HttpObject httpObject = null;

		try {
			httpObject = httpClient.sendGet("get");
		} catch (HttpException | InvalidHttpRequestException e) {
			throw new AutomationUnitTestException(e);
		}

		verifyHasResponse(httpObject);
		verifyStatusCode(httpObject.getResponse(), STATUS_CODE_200);
		verifyRequestSentWithJSessionId(httpObject.getResponse());
	}

	private void verifyRequestSentWithJSessionId(HttpResponse httpResponse) {
		String responseBody = httpResponse.getResponseBody();
		SoftAssertUnitTest.assertTrueNow(responseBody.contains(EXPECTED_RESPONSE_BODY_FOR_REQUEST_WITH_JSESSION_ID),
				"Response body to request that is send with JSESSIONID [" + J_SESSION_ID + "] not contains ["
						+ EXPECTED_RESPONSE_BODY_FOR_REQUEST_WITH_JSESSION_ID + "].\nResponse body:\n" + responseBody,
				"Verify that request sent with JSESSIONID");
	}

	private void verifyHasResponse(HttpObject httpObject) {
		SoftAssertUnitTest.assertNotNullNow(httpObject, "HTTP object is null");
		SoftAssertUnitTest.assertNotNullNow(httpObject.getResponse(), "HTTP object is null");
	}

	private void verifyStatusCode(HttpResponse response, int expectedStatusCode) throws AutomationUnitTestException {
		SoftAssertUnitTest.assertTrueNow(response.getStatusCode() == expectedStatusCode,
				"HTTP response status code is [" + response.getStatusCode() + "] and it should be ["
						+ expectedStatusCode + "].\nResponse body:\n" + response.getResponseBody(),
				"Verify that HTTP response status code is [" + expectedStatusCode + "]");
	}
}
package io.github.etuzon.http.tests.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.testng.annotations.Test;

import io.github.etuzon.http.client.HttpAsyncClient;
import io.github.etuzon.http.client.HttpClient;
import io.github.etuzon.http.exceptions.HttpException;
import io.github.etuzon.http.exceptions.InvalidHttpRequestException;
import io.github.etuzon.http.tests.parameters.TestParameters;
import io.github.etuzon.projects.core.base.ThreadBase;
import io.github.etuzon.projects.core.enums.ThreadStateEnum;
import io.github.etuzon.projects.core.utils.DateUtil;
import io.github.etuzon.projects.core.utils.ThreadUtil;
import io.github.etuzon.unit.tests.asserts.SoftAssertUnitTest;
import io.github.etuzon.unit.tests.exceptions.AutomationUnitTestException;

public class HttpAsyncClientTest extends HttpClientTestBase implements TestParameters {
	public static final int TCP_SERVER_PORT = 16009;

	public static final long TIMEOUT_UNTIL_HTTP_SERVER_GET_REQUEST_BODY = 15 * ThreadUtil.SECOND_1;
	public static final long TIMEOUT_UNTIL_HTTP_CLIENT_GET_RESPONSE_BODY = 5 * ThreadUtil.SECOND_1;

	public static final long SLEEP_IN_WAIT_LOOP_MS = 300;

	private static final String HTTP_SERVER_HEADER = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n"
			+ "Connection: keep-alive\r\n" + "\r\n";

	private static final String S_1234567890 = "1234567890";

	private static final String HTTP_SERVER_BODY = S_1234567890 + S_1234567890 + S_1234567890 + S_1234567890
			+ S_1234567890 + S_1234567890 + S_1234567890;

	private static final String POST_ENTITY = "Entity";

	private TcpServerTester tcpServerTester = null;

	@Test
	public void send_aSync_GET_request_test() throws AutomationUnitTestException {
		HttpClient httpClient = initHttpClient(HTTP_ADDRESS, HttpClient.HTTP_PORT);

		HttpAsyncClient asyncHttpClient = null;

		try {
			asyncHttpClient = httpClient.sendAsyncGet("get");
		} catch (HttpException | InvalidHttpRequestException e) {
			throw new AutomationUnitTestException(e);
		}

		verifyAsyncGetResponse(asyncHttpClient);
	}
	
	@Test
	public void send_aSync_POST_request_test() throws AutomationUnitTestException {
		HttpAsyncClient httpAsync = null;
		try {
			initTcpServer();

			HttpClient httpClient = initHttpClient("http://127.0.0.1", TCP_SERVER_PORT);

			httpAsync = sendAsyncHttpPostAndVerifyServerGetPostEntity(httpClient, POST_ENTITY,
					TIMEOUT_UNTIL_HTTP_SERVER_GET_REQUEST_BODY);

			verifyCharsPerSecondSegmentsIs0(httpAsync);

			tcpServerTester.sendHttpBody();

			waitUntilHttpClientGetResponseBodyFromServer(httpAsync, TIMEOUT_UNTIL_HTTP_CLIENT_GET_RESPONSE_BODY);

			verifyCharsPerSecondSegmentsIsBiggerFrom0(httpAsync);

			ThreadUtil.sleep(3 * ThreadUtil.SECOND_1);

			verifyTcpServerTesterNotHaveException();

			verifyHttpAsyncResponse(httpAsync);
		} finally {
			if (httpAsync != null) {
				httpAsync.shutdown();
			}

			if (tcpServerTester != null) {
				tcpServerTester.shutdown();
			}
		}
	}

	private void verifyAsyncGetResponse(HttpAsyncClient asyncHttpClient) throws AutomationUnitTestException {
		ThreadUtil.sleep(10 * ThreadUtil.SECOND_1);
		SoftAssertUnitTest.assertNotNullNow(asyncHttpClient.getStatusCode(), "Did not get response status code (null)");
		verifyAsyncStatusCode(asyncHttpClient, STATUS_CODE_200);
	}
	
	private String waitUntilHttpClientGetResponseBodyFromServer(HttpAsyncClient httpAsync, long timeout) {
		boolean isEqual = false;
		long startTimeout = System.currentTimeMillis();

		String httpServerResponse = "";
		String body = "";

		while ((DateUtil.isTimeout(startTimeout, timeout) == false) && (!isEqual)) {
			httpServerResponse = httpAsync.getResponseBuffer();
			int index = httpServerResponse.indexOf("\r\n\r\n");

			body = "";

			if (index != -1) {
				body = httpServerResponse.substring(index + 4);
			}

			isEqual = HTTP_SERVER_BODY.equals(body);

			ThreadUtil.sleep(SLEEP_IN_WAIT_LOOP_MS);
		}

		return body;
	}

	private String waitUntilTcpServerGetPostRequest(long timeout) {
		boolean isEqual = false;
		long startTimeout = System.currentTimeMillis();

		String tcpServerInput = "";
		String entity = "";

		while ((DateUtil.isTimeout(startTimeout, timeout) == false) && (!isEqual)) {
			tcpServerInput = tcpServerTester.getInput();
			int index = tcpServerInput.indexOf("\r\n\r\n");

			entity = "";

			if (index != -1) {
				entity = tcpServerInput.substring(index + 4);
			}

			isEqual = POST_ENTITY.equals(entity);

			ThreadUtil.sleep(SLEEP_IN_WAIT_LOOP_MS);
		}

		return entity;
	}

	private void verifyTcpServerTesterNotHaveException() {
		if (tcpServerTester.isException()) {
			SoftAssertUnitTest.failNow(
					"Exception [" + tcpServerTester.getException().getMessage() + "] happened on TCP Server Tester");
		}
	}

	private void verifyHttpAsyncResponse(HttpAsyncClient httpAsync) {
		String currentResponseBuffer = httpAsync.getResponseBuffer();

		SoftAssertUnitTest.assertTrueNow(HTTP_SERVER_BODY.equals(currentResponseBuffer),
				"HTTP async client response from server is [" + currentResponseBuffer + "] but it should be ["
						+ HTTP_SERVER_BODY + "]",
				"Verify that HTTP async client response from server [" + currentResponseBuffer + "] is ["
						+ HTTP_SERVER_BODY + "]");
	}

	private void verifyCharsPerSecondSegmentsIs0(HttpAsyncClient httpAsync) {
		verifyHttpAsyncCharsPerSecond(0, 0, "1 second", httpAsync.getCharsPerSecond_segment_1_second());
		verifyHttpAsyncCharsPerSecond(0, 0, "5 seconds", httpAsync.getCharsPerSecond_segment_5_seconds());
		verifyHttpAsyncCharsPerSecond(0, 0, "30 seconds", httpAsync.getCharsPerSecond_segment_30_seconds());
		verifyHttpAsyncCharsPerSecond(0, 0, "1 minute", httpAsync.getCharsPerSecond_segment_1_minute());
		SoftAssertUnitTest.assertAll();
	}

	private void verifyCharsPerSecondSegmentsIsBiggerFrom0(HttpAsyncClient httpAsync) {
		verifyHttpAsyncCharsPerSecond(1, 100, "1 second", httpAsync.getCharsPerSecond_segment_1_second());
		verifyHttpAsyncCharsPerSecond(1, 100, "5 seconds", httpAsync.getCharsPerSecond_segment_5_seconds());
		verifyHttpAsyncCharsPerSecond(1, 100, "30 seconds", httpAsync.getCharsPerSecond_segment_30_seconds());
		verifyHttpAsyncCharsPerSecond(1, 100, "1 minute", httpAsync.getCharsPerSecond_segment_1_minute());
		SoftAssertUnitTest.assertAll();
	}

	private void verifyHttpAsyncCharsPerSecond(int minExpected, int maxExpected, String segmentPerSecondsStr,
			int currentCharsPerSecond) {
		boolean isCurrentCharsPerSecondInBorder = currentCharsPerSecond >= minExpected
				&& currentCharsPerSecond <= maxExpected;
		SoftAssertUnitTest.assertTrue(isCurrentCharsPerSecondInBorder,
				"Chars per seconds in segment of " + segmentPerSecondsStr + " segment should be between [" + minExpected
						+ "] to [" + maxExpected + "] but it is [" + currentCharsPerSecond + "]",
				"Verify that chars per second of " + segmentPerSecondsStr + " segment [" + currentCharsPerSecond
						+ "] is between [" + minExpected + "] to [" + maxExpected + "]");
	}

	private void verifyAsyncStatusCode(HttpAsyncClient asyncHttpClient, int expectedStatusCode)
			throws AutomationUnitTestException {
		SoftAssertUnitTest.assertTrueNow(asyncHttpClient.getStatusCode() == expectedStatusCode,
				"HTTP response status code is [" + asyncHttpClient.getStatusCode() + "] and it should be ["
						+ expectedStatusCode + "].\nResponse body:\n" + asyncHttpClient.getResponseBuffer(),
				"Verify that HTTP response status code is [" + expectedStatusCode + "]");
	}

	private HttpAsyncClient sendAsyncHttpPostAndVerifyServerGetPostEntity(HttpClient httpClient, String entity,
			long timeout) throws AutomationUnitTestException {
		HttpAsyncClient httpAsync = null;

		try {
			httpAsync = httpClient.sendAsyncPost(entity);
		} catch (InvalidHttpRequestException | HttpException e) {
			throw new AutomationUnitTestException(e);
		}

		entity = waitUntilTcpServerGetPostRequest(timeout);

		SoftAssertUnitTest.assertTrueNow(POST_ENTITY.equals(entity),
				"HTTP client POST entity is [" + entity + "] but it should be [" + POST_ENTITY + "]",
				"Verify that HTTP client POST entity is [" + POST_ENTITY + "]");

		return httpAsync;
	}

	private void initTcpServer() throws AutomationUnitTestException {
		tcpServerTester = new TcpServerTester(TCP_SERVER_PORT);
		tcpServerTester.start();
	}

	private class TcpServerTester extends ThreadBase {
		public static final int BUFFER = 1024 * 1024;

		private ServerSocket tcpServer = null;
		private AutomationUnitTestException exception = null;
		private DataOutputStream output = null;
		private Socket socket = null;
		private BufferedReader input = null;

		private String inputStr = "";

		private TcpServerTester(int port) throws AutomationUnitTestException {
			try {
				tcpServer = new ServerSocket(port);
			} catch (IOException e) {
				throw new AutomationUnitTestException(
						"Failed init TCP server on port [" + TCP_SERVER_PORT + "].\n" + e.getMessage());
			}
		}

		public void run() {
			acceptConnection();
			initSocketInputAndOutput();

			char[] charBuff = new char[BUFFER];

			int charsAmount = -1;

			boolean isSentResponse = false;

			while (isRunning()) {
				try {
					charsAmount = input.read(charBuff);
				} catch (IOException e) {
					exception = new AutomationUnitTestException(e);
					shutdown();
				}

				if (getThreadState() == ThreadStateEnum.RUNNING) {
					if (charsAmount != -1) {
						synchronized (inputStr) {
							inputStr += String.copyValueOf(charBuff, 0, charsAmount);
						}
					} else {
						ThreadUtil.sleep(200);
					}
				}

				if (inputStr.length() > 3) {
					if (isSentResponse == false) {
						ThreadUtil.sleep(5 * ThreadUtil.SECOND_1);
						sendHttpHeadersAndBody();
						isSentResponse = true;
					}
				}
			}
		}

		public String getInput() {
			synchronized (inputStr) {
				return inputStr;
			}
		}

		public void sendHttpBody() throws AutomationUnitTestException {
			if (isRunning()) {
				try {
					output.writeBytes(HTTP_SERVER_HEADER);
				} catch (IOException e) {
					shutdown();
					throw new AutomationUnitTestException(
							"Unable send HTTP body [" + HTTP_SERVER_BODY + "].\n" + e.getMessage());
				}
			}
		}

		private boolean isException() {
			return exception != null;
		}

		private AutomationUnitTestException getException() {
			return exception;
		}

		private void acceptConnection() {
			try {
				socket = tcpServer.accept();
			} catch (IOException e) {
				exception = new AutomationUnitTestException(
						"TCP Server failed listen to port [" + TCP_SERVER_PORT + "].\n" + e.getMessage());
				shutdown();
			}
		}

		private void initSocketInputAndOutput() {
			if (isRunning()) {
				try {
					output = new DataOutputStream(socket.getOutputStream());
					input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				} catch (IOException e) {
					exception = new AutomationUnitTestException(
							"Unable init TCP server intout/output.\n" + e.getMessage());
					shutdown();
				}
			}
		}

		private void sendHttpHeadersAndBody() {
			if (isRunning()) {
				try {
					output.writeBytes(HTTP_SERVER_HEADER + HTTP_SERVER_BODY);
					output.flush();
				} catch (IOException e) {
					exception = new AutomationUnitTestException(
							"Unable send HTTP header [" + HTTP_SERVER_HEADER + "].\n" + e.getMessage());
					shutdown();
				}
			}

			while (isRunning()) {
				ThreadUtil.sleep(100);
			}

			closeConnection();
		}

		private void closeConnection() {
			try {
				socket.close();
				tcpServer.close();
			} catch (IOException e) {
			}

			socket = null;
			tcpServer = null;
		}
	}
}
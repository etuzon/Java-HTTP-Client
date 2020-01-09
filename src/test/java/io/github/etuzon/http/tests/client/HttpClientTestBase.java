package io.github.etuzon.http.tests.client;

import io.github.etuzon.http.client.HttpClient;
import io.github.etuzon.unit.tests.base.BaseUnitTest;
import io.github.etuzon.unit.tests.exceptions.AutomationUnitTestException;

public class HttpClientTestBase extends BaseUnitTest {
	protected HttpClient initHttpClient(String url) throws AutomationUnitTestException {
		try {
			return new HttpClient(url);
		} catch (Exception e) {
			throw new AutomationUnitTestException(e);
		}
	}
	
	protected HttpClient initHttpClient(String url, int port) throws AutomationUnitTestException {
		try {
			return new HttpClient(url, port);
		} catch (Exception e) {
			throw new AutomationUnitTestException(e);
		}
	}
	
	protected HttpClient initHttpClient(String url, int port, String username, String password) throws AutomationUnitTestException {
		try {
			return new HttpClient(url, username, password);
		} catch (Exception e) {
			throw new AutomationUnitTestException(e);
		}
	}
	
	protected HttpClient initHttpClient(String url, int port, String jSessionId) throws AutomationUnitTestException {
		try {
			return new HttpClient(url, jSessionId);
		} catch (Exception e) {
			throw new AutomationUnitTestException(e);
		}
	}
}
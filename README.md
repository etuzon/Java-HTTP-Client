HTTP Client

HTTP/HTTPS client support synced and asynced requests.

Example of synced request:

HttpClient httpClient = initHttpClient(HTTP_ADDRESS);

HttpObject httpObject = null;

try {
   httpObject = httpClient.sendGet();
} catch (HttpException | InvalidHttpRequestException e) {
	 throw new AutomationUnitTestException(e);
}

Example of asynced request:

HttpClient httpClient = initHttpClient(HTTP_ADDRESS);

HttpAsyncClient asyncHttpClient = null;

try {
	asyncHttpClient = httpClient.sendAsyncGet("get");
} catch (HttpException | InvalidHttpRequestException e) {
	throw new AutomationUnitTestException(e);
}

private HttpClient initHttpClient(String url) throws AutomationUnitTestException {
  try {
			return new HttpClient(url);
	} catch (Exception e) {
			throw new AutomationUnitTestException(e);
	}
}

package io.github.etuzon.http.tests.parameters;

public interface TestParameters {
	//Used https://requestbin.com to create HTTP server for response
	public static final String HTTPS_ADDRESS = "https://ene1qj9ifitys.x.pipedream.net";
	
	//For GET add 'get' suffix, for POST add 'post', for PUT add 'put' and for DEL add 'delete' suffix
	public static final String HTTP_ADDRESS = "http://httpbin.org";
	
	public static final int STATUS_CODE_200 = 200;
}

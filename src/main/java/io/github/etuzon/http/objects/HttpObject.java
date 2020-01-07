package io.github.etuzon.http.objects;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpMessage;

import io.github.etuzon.http.exceptions.HttpException;

/***********************************************
 * Contains HTTP request and reponse.
 * 
 * @author Eyal Tuzon
 *
 */
public class HttpObject {
	private final HttpMessage request;
	private final HttpResponse response;

	/***********************************************
	 * Constructor.
	 * 
	 * @param response Response.
	 * @throws HttpException in case failed to create HttpResponse object.
	 */
	public HttpObject(CloseableHttpResponse response) throws HttpException {
		this(null, response);
	}

	/***********************************************
	 * Constructor.
	 * 
	 * @param request Request.
	 * @param response Response.
	 * @throws HttpException in case failed to create HttpResponse object.
	 */
	public HttpObject(HttpMessage request, CloseableHttpResponse response) throws HttpException {
		this.request = request;
		try {
			this.response = new HttpResponse(response);
		} catch (Exception e) {
			throw new HttpException(e);
		}
	}

	/***********************************************
	 * Get request object.
	 * 
	 * @return request object.
	 */
	public HttpMessage getRequest() {
		return request;
	}

	/***********************************************
	 * Get response object.
	 * 
	 * @return response object.
	 */
	public HttpResponse getResponse() {
		return response;
	}
}
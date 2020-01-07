package io.github.etuzon.http.objects;

import java.io.IOException;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;

import io.github.etuzon.http.exceptions.HttpException;
import io.github.etuzon.projects.core.utils.StringUtil;

/***********************************************
 * HTTP response object.
 * 
 * @author Eyal Tuzon
 *
 */
public class HttpResponse {
	private final int statusCode;
	private final String responseBody;
	private final CloseableHttpResponse closeableHttpResponse;
	
	/***********************************************
	 * Constructor.
	 * 
	 * @param closeableHttpResponse Apache response object.
	 * @throws HttpException n case failed to create HttpResponse object.
	 */
	public HttpResponse(CloseableHttpResponse closeableHttpResponse) throws HttpException {
		this.closeableHttpResponse = closeableHttpResponse;
		this.statusCode = closeableHttpResponse.getCode();
		try {
			this.responseBody = StringUtil.readFromInputStream(closeableHttpResponse.getEntity().getContent());
		} catch (UnsupportedOperationException | IOException e) {
			throw new HttpException(e);
		}
	}

	/***********************************************
	 * Get HTTP response status code.
	 * 
	 * @return HTTP response status code.
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/***********************************************
	 * Get HTTP response body (entity).
	 * 
	 * @return HTTP response body (entity).
	 */
	public String getResponseBody() {
		return responseBody;
	}

	/***********************************************
	 * Apache response object.
	 * 
	 * @return Apache response object.
	 */
	public CloseableHttpResponse getCloseableHttpResponse() {
		return closeableHttpResponse;
	}
}
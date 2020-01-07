package io.github.etuzon.http.exceptions;

import io.github.etuzon.projects.core.expections.EtuzonExceptionBase;

public class HttpException extends EtuzonExceptionBase {
	private static final long serialVersionUID = 1L;

	/**************************************
	 * Constructor.
	 * 
	 */
	public HttpException() {
		super();
	}
	
	/**************************************
	 * Constructor.
	 * 
	 * @param message Exception message.
	 */
	public HttpException(String message) {
		super(message);
	}
	
	/**************************************
	 * Constructor.
	 * 
	 * Convert input exception as exception message and exception stacktrace.
	 * 
	 * @param e Input exception object that will be converted to exception message and stacktrace.
	 */
	public HttpException(Exception e) {
		super(e);
	}
}
package io.github.etuzon.http.exceptions;

import io.github.etuzon.projects.core.expections.EtuzonExceptionBase;

public class InvalidHttpRequestException extends EtuzonExceptionBase {

	private static final long serialVersionUID = 1L;

	/*************************************************
	 * Constructor.
	 * 
	 */
	public InvalidHttpRequestException() {
		super();
	}

	/*************************************************
	 * Constructor.
	 * 
	 * @param message Exception message.
	 */
	public InvalidHttpRequestException(String message) {
		super(message);
	}
	
	/*************************************************
	 * Constructor.
	 * 
	 * @param e Input exception.
	 */
	public InvalidHttpRequestException(Exception e) {
		super(e);
	}
}
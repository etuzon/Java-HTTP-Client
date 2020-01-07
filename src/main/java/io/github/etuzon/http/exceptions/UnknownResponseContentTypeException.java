package io.github.etuzon.http.exceptions;

import io.github.etuzon.projects.core.expections.EtuzonExceptionBase;

public class UnknownResponseContentTypeException extends EtuzonExceptionBase {

	private static final long serialVersionUID = 1L;

	/*************************************************
	 * Constructor.
	 * 
	 */
	public UnknownResponseContentTypeException() {
		super();
	}

	/*************************************************
	 * Constructor.
	 * 
	 * @param message Exception message.
	 */
	public UnknownResponseContentTypeException(String message) {
		super(message);
	}
	
	/*************************************************
	 * Constructor.
	 * 
	 * @param e Input exception.
	 */
	public UnknownResponseContentTypeException(Exception e) {
		super(e);
	}
}
package io.github.etuzon.http.enums;

public enum HeaderEnum {
	AUTORIZATION("Authorization"), CONNECTION("Connection"), CONTENT_LENGTH("Content-Length"),
	CONTENT_TYPE("Content-Type"), COOKIE("Cookie");

	private final String name;

	private HeaderEnum(String name) {
		this.name = name;
	}

	/**************************************
	 * Get header name.
	 * 
	 * @return header name.
	 */
	public String getName() {
		return name;
	}
}
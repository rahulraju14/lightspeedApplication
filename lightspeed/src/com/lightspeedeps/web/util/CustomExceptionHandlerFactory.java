/**
 * File: CustomExceptionHandlerFactory.java
 */
package com.lightspeedeps.web.util;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;

/**
 * Override the default server-error exception handling.  This class is
 * specified in the faces-config.xml file, in the < factory > section.
 */
public class CustomExceptionHandlerFactory extends ExceptionHandlerFactory {
	private ExceptionHandlerFactory parent;

	// this injection handles JSF
	public CustomExceptionHandlerFactory(ExceptionHandlerFactory parent) {
		this.parent = parent;
	}

	@Override
	public ExceptionHandler getExceptionHandler() {

		ExceptionHandler handler = new CustomExceptionHandler(parent.getExceptionHandler());

		return handler;
	}

}

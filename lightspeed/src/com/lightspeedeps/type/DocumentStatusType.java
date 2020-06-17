/**
 * File: DocumentStatusType.java
 */
package com.lightspeedeps.type;

/**
 * Status of where the document is on the approval chain.
 */
public enum DocumentStatusType {
	/** Documents are in the approved state. */
	STATUS_COMPLETE 	("C"),
	/** Documents are still awaiting some kind of action. */
	STATUS_PENDING	("P"),
	/** There was an error retrieving the status for the document */
	STATUS_ERROR	("E")
	;
	
	private String label;
	
	DocumentStatusType(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
	@Override
	public String toString() {
		return getLabel();
	}
}

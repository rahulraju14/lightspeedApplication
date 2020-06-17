package com.lightspeedeps.object;

import java.io.Serializable;

import com.lightspeedeps.model.Document;
import com.lightspeedeps.model.DocumentChain;
import com.lightspeedeps.model.Folder;

public class DocRowItem implements Serializable {

	private static final long serialVersionUID = -1976799086425486683L;

	/** Scene.sequence, which determines the order of the Scene in the Script. */
	private boolean checked = false;

	/** Scene.scriptDay - the user-defined description of when this Scene occurs. */
	private DocumentChain documentChain;

	/** Strip.sheetNumber, a sequential number assigned to Strips which is no longer important. */
	private Folder folder;
	
	private Document document;

	public DocRowItem() {
		// default constructor
	}
	
	public DocRowItem(boolean checked, DocumentChain documentChain, Folder folder, Document document) {
		super();
		this.checked = checked;
		this.documentChain = documentChain;
		this.folder = folder;
		this.document = document;
	}
	public boolean getChecked() {
		return checked;
	}
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	public DocumentChain getDocumentChain() {
		return documentChain;
	}
	public void setDocumentChain(DocumentChain documentChain) {
		this.documentChain = documentChain;
	}

	public Folder getFolder() {
		return folder;
	}
	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	public Document getDocument() {
		return document;
	}
	public void setDocument(Document document) {
		this.document = document;
	}
	
}

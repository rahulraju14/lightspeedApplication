package com.lightspeedeps.dao;

import com.lightspeedeps.model.Recording;

public class RecordingDAO extends BaseTypeDAO<Recording> {
	
	public static RecordingDAO getInstance() {
		return (RecordingDAO)getInstance("RecordingDAO");
	}

}

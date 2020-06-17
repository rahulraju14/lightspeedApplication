package com.lightspeedeps.object;

import java.io.Serializable;

/**
 * A data object to hold the scene numbers appearing in the
 * calendar display for a specific date.
 */
public class SceneItem implements Serializable {

	private static final long serialVersionUID = - 7312213084525781035L;

	private String sceneNumbers;
	private boolean renderHLink = true;

	public SceneItem() {
	}

	public SceneItem(String scenes, boolean hlink) {
		sceneNumbers = scenes;
		renderHLink = hlink;
	}

	public String getSceneNumbers() {
		return sceneNumbers;
	}
	public void setSceneNumbers(String sceneNumbers) {
		this.sceneNumbers = sceneNumbers;
	}
	public boolean isRenderHLink() {
		return renderHLink;
	}
	public void setRenderHLink(boolean renderHLink) {
		this.renderHLink = renderHLink;
	}

}

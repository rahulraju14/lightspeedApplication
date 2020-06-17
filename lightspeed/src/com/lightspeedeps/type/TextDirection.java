package com.lightspeedeps.type;

/**
 * The direction in which watermark text will be applied
 * to a generated PDF.
 */
public enum TextDirection {
	/** Text is horizontal, left to right. */
	HORIZONTAL,
	/** Text is vertical, top to bottom. */
	VERTICAL,
	/** Text is at an angle, starting at the top left
	 * of the page, down to the bottom right of the page.*/
	TOPLEFT_BOTTOMRIGHT,
	/** Text is at an angle, starting at the bottom left
	 * of the page, up to the top right of the page.*/
	BOTTOMLEFT_TOPRIGHT;

	public int getAngle() {
		return getAngle( 8.5f, 11.0f ); // 8.5(612) x 11.0(792) = 52
	}

	public int getAngle(float width, float height) {
		int angle = 0;
		switch(this) {
		case HORIZONTAL:
			break;
		case VERTICAL:
			angle = 90;
			break;
		case BOTTOMLEFT_TOPRIGHT:
			angle = (int)(180.0*Math.atan(height/width)/Math.PI);
			break;
		case TOPLEFT_BOTTOMRIGHT:
			angle = -(int)(180.0*Math.atan(height/width)/Math.PI);
			break;
		}
		return angle;
	}

}

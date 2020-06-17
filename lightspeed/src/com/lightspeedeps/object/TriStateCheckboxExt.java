/**
 * File: TriStateCheckboxExt.java
 */
package com.lightspeedeps.object;

import java.io.Serializable;

import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.SessionUtils;

/**
 * An extension of the TriStateCheckbox class that supports call-backs upon
 * clicking.
 */
public class TriStateCheckboxExt implements Serializable {
	/**  */
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(TriStateCheckboxExt.class);

	/** The string value used by the ace triStateCheckbox component to represent the 'checked' (true) state. */
	private static final String VALUE_CHECKED = "checked";

	/** The string value used by the ace triStateCheckbox component to represent the 'indeterminate'
	 * (neither true nor false) state. */
	private static final String VALUE_PARTIALLY_CHECKED = "indeterminate";

	/** The string value used by the ace triStateCheckbox component to represent the 'unchecked' (false) state. */
	private static final String VALUE_UNCHECKED = "unchecked";

	/** An identifier supplied by our holder, which is passed back when a
	 * 'clicked' callback is done.  May be any value, including null. */
	private Object id;

	/** The owner of this object.  If null, no special action occurs. If
	 * not null, it's clicked method is called when our checkBoxClicked
	 * method is called (from the framework). */
	private ControlHolder holder;

	/** A 4-state value for the status of the checkbox. This is used in our code
	 * (e.g., in PermissionBean) instead of the underlying {@link #value} String
	 * for flexibility and efficiency, particularly when doing "roll ups" of
	 * multiple values into a summary checkbox. The legal values are defined as
	 * static constants {@link #CHECK_ON}, {@link #CHECK_OFF},
	 * {@link #CHECK_MIXED}, and {@link #CHECK_NA}. The values could have been
	 * an enum, but this is (hopefully) more efficient when accessing from JSP,
	 * important when we may have over a thousand check-boxes on the page at
	 * once. */
	private byte checkValue = CHECK_OFF;

	/** The string value used by the ace triStateCheckbox component to determine its
	 * status: "checked", "unchecked", or "indeterminate".  In the Java code the constants
	 * {@link #VALUE_CHECKED}, {@link #VALUE_UNCHECKED}, and {@link #VALUE_PARTIALLY_CHECKED} are used. */
	private String value = VALUE_UNCHECKED;

	/** The disabled status of the checkbox. */
	private boolean disabled;

	/** The value of {@link #checkValue} indicating the checkbox is not checked.  */
	public static final byte CHECK_OFF = 0;

	/** The value of {@link #checkValue} indicating the checkbox is checked.  */
	public static final byte CHECK_ON = 1;

	/** The value of {@link #checkValue} indicating the checkbox is gray, indicating
	 * a mixed On/Off state.  */
	public static final byte CHECK_MIXED = 2;

	/** The values of {@link #checkValue} indicating the value of the checkbox is not applicable
	 * or unknown -- neither On, Off, nor Mixed. */
	public static final byte CHECK_NA = 3;

	/**
	 * Default constructor; checkbox will be set to un-checked.
	 */
	public TriStateCheckboxExt() {
	}

	/**
	 * This method should be called by the class using the checkbox when the checkbox
	 * is clicked, e.g., from a valueChangeListener method.
	 * @param event The ValueChangeEvent from the framework.
	 */
	public void listenChecked(ValueChangeEvent event) {
		LOG.debug(SessionUtils.getPhaseIdFmtd() + "Old value: " + event.getOldValue() + "  New value: " + event.getNewValue());
		if (event.getNewValue() != null) {
			setValue((String)event.getNewValue());
			updateCheckValue();
			if (holder != null) {
				holder.clicked(this, id);
			}
		}
	}

	/** See {@link #checkValue}. */
	public byte getCheckValue() {
		return checkValue;
	}
	/** See {@link #checkValue}. */
	public void setCheckValue(byte val) {
		switch(val) {
			case CHECK_ON:
				setChecked();
				break;
			case CHECK_OFF:
				setUnchecked();
				break;
			case CHECK_MIXED:
				setPartiallyChecked();
				break;
			case CHECK_NA:
				setPartiallyChecked();
				setDisabled(true);
				break;
		}
	}

	/**
	 * update our {@link #checkValue} field based on the
	 * current {@link #value} setting.
	 */
	private void updateCheckValue() {
		switch(value) {
			case VALUE_CHECKED:
				checkValue = CHECK_ON;
				break;
			case VALUE_UNCHECKED:
				checkValue = CHECK_OFF;
				break;
			case VALUE_PARTIALLY_CHECKED:
				checkValue = CHECK_MIXED;
				break;
		}
	}

	/** See {@link #id}. */
	public Object getId() {
		return id;
	}
	/** See {@link #id}. */
	public void setId(Object id) {
		this.id = id;
	}

	/** See {@link #holder}. */
	public ControlHolder getHolder() {
		return holder;
	}
	/** See {@link #holder}. */
	public void setHolder(ControlHolder holder) {
		this.holder = holder;
	}

	/** See {@link #value}. */
	public String getValue() {
		//LOG.debug(SessionUtils.getPhaseIdFmtd() + " value=" + value);
		return value;
	}
	/** See {@link #value}. */
	public void setValue(String val) {
		this.value = val;
		updateCheckValue();
		//LOG.debug(SessionUtils.getPhaseIdFmtd() + " value=" + value);
	}

	/** True iff the checkbox's state is un-checked (not checked
	 * or partially checked. */
	public boolean isUnchecked() {
		return VALUE_UNCHECKED.equals(value);
	}
	public void setUnchecked() {
		setValue(VALUE_UNCHECKED);
		updateCheckValue();
	}

	/** True iff the checkbox's state is checked (not un-checked
	 * or partially checked. */
	public boolean isChecked() {
		return checkValue == CHECK_ON;
	}
	public void setChecked() {
		setValue(VALUE_CHECKED);
		updateCheckValue();
	}

	/** True iff the checkbox's state is partially checked,
	 * i.e., 'mixed', not un-checked or checked. */
	public boolean isPartiallyChecked() {
		return checkValue == CHECK_MIXED;
	}
	public void setPartiallyChecked() {
		setValue(VALUE_PARTIALLY_CHECKED);
		updateCheckValue();
	}

	/** See {@link #disabled}. */
	public boolean getDisabled() {
		return disabled;
	}
	/** See {@link #disabled}. */
	public void setDisabled(boolean b) {
		disabled = b;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "value=" + (getValue()==null ? "null" : value);
		s += "]";
		return s;
	}

}

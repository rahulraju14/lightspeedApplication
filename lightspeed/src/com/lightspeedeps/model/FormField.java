package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import com.lightspeedeps.type.FormFieldType;

/** FormField entity holds the fields and their FormFieldType for a PDF form.
 * The later is used in replacing the FormFieldType values with the actual
 * user values.
 *
 * @author root
 *
 */
@Entity
@Table(name="form_field")
public class FormField extends PersistentObject<FormField> {

	private static final long serialVersionUID = -4165750417988498529L;

	/** Form Name, the field belongs to. */
	private String formName;

	/** FormField Type, type of the form field from the enum */
	private FormFieldType fieldType;

	/** Field key, unique identifier for the field key */
	private String fieldKey;

	/** Default constructor */
	public FormField() {

	}

	@Column(name = "form_name", nullable = false, length = 100)
	public String getFormName() {
		return formName;
	}
	public void setFormName(String formName) {
		this.formName = formName;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "field_type" , nullable = false, length = 30)
	public FormFieldType getFieldType() {
		return fieldType;
	}
	public void setFieldType(FormFieldType fieldType) {
		this.fieldType = fieldType;
	}

	@Column(name = "field_key", nullable = false, length = 100)
	public String getFieldKey() {
		return fieldKey;
	}
	public void setFieldKey(String fieldKey) {
		this.fieldKey = fieldKey;
	}
}

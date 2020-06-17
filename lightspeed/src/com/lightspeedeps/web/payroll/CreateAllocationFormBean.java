
package com.lightspeedeps.web.payroll;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.TaxWageAllocationFormDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.TaxWageAllocationForm;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupBean;

/**
 * Create a new contact and user record for new allocation form
 */
@ManagedBean
@ViewScoped
public class CreateAllocationFormBean extends PopupBean{
	@SuppressWarnings("unused")
	private static Log log = LogFactory.getLog(CreateAllocationFormBean.class);
	private static final long serialVersionUID = 1L;

	// Fields
	
	/* New contact record to use to create new contact */
	private Contact newFormContact;
	/** Index of the selected contact from dropdown **/
	private int selectedContactId;
	/** List of contacts in this production excluding LS Admin */
	private List<Contact> contacts;
	/** List of SelectItems representing the contacts for contact dropdown.*/
	private List<SelectItem> contactsDL;
	/** If the selected contact already has allocation forms, display error */
	private boolean contactAllocFormExists;
	/** Whether the selected contact is new */
	private boolean addNewContact;
	/** First name of the new contact to create. */
	private String firstName;
	/** Last name of the new contact to create. */
	private String lastName;
	private boolean disableAddBttn;
	
	// DAO classes
	private transient TaxWageAllocationFormDAO taxWageAllocationFormDAO;
	private transient ContactDAO contactDAO;
	
	public static CreateAllocationFormBean getInstance() {
		return (CreateAllocationFormBean)ServiceFinder.findBean("createAllocationFormBean");
	}

	public CreateAllocationFormBean() {
		contactAllocFormExists = false;
		addNewContact = true;
		disableAddBttn = true;
		selectedContactId = -1;
	}
	
	/** See {@link #newFormContact}. */
	public Contact getNewFormContact() {
		return newFormContact;
	}

	/** See {@link #newFormContact}. */
	public void setNewFormContact(Contact newFormContact) {
		this.newFormContact = newFormContact;
	}
	
	/** See {@link #selectedContact}. */
	public int getSelectedContactId() {
		return selectedContactId;
	}

	/** See {@link #selectedContact}. */
	public void setSelectedContactId(int selectedContactId) {
		this.selectedContactId = selectedContactId;
	}

	/** See {@link #contactAllocFormExists}. */
	public boolean getContactAllocFormExists() {
		return contactAllocFormExists;
	}

	/** See {@link #contactAllocFormExists}. */
	public void setContactAllocFormExists(boolean contactAllocFormExists) {
		this.contactAllocFormExists = contactAllocFormExists;
	}

	/** See {@link #addNewContact}. */
	public boolean getAddNewContact() {
		return addNewContact;
	}

	/** See {@link #addNewContact}. */
	public void setAddNewContact(boolean addNewContact) {
		this.addNewContact = addNewContact;
	}

	/** See {@link #firstName}. */
	public String getFirstName() {
		return firstName;
	}

	/** See {@link #firstName}. */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
 
	/** See {@link #lastName}. */
	public String getLastName() {
		return lastName;
	}

	/** See {@link #lastName}. */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/** See {@link #disableAddBttn}. */
	public boolean getDisableAddBttn() {
		return disableAddBttn;
	}

	/** See {@link #disableAddBttn}. */
	public void setDisableAddBttn(boolean disableAddBttn) {
		this.disableAddBttn = disableAddBttn;
	}

	/** See {@link #contacts}. */
	public List<Contact> getContacts() {
		return contacts;
	}

	/** See {@link #contacts}. */
	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}

	/** See {@link #contactsDL}. */
	public List<SelectItem> getContactsDL() {
		createContactsDL();
		
		return contactsDL;
	}

	/** See {@link #contactsDL}. */
	public void setContactsDL(List<SelectItem> contactsDL) {
		this.contactsDL = contactsDL;
	}

	private TaxWageAllocationFormDAO getTaxWageAllocationFormDAO() {
		if(taxWageAllocationFormDAO == null) {
			taxWageAllocationFormDAO = TaxWageAllocationFormDAO.getInstance();
		}
		
		return taxWageAllocationFormDAO;
	}
	
	private ContactDAO getContactDAO() {
		if(contactDAO == null) {
			contactDAO = ContactDAO.getInstance();
		}
		
		return contactDAO;
	}
	
	public String actionOk() {
		// If not creating a new contact, set the selected contact to be returned
		// to the caller for creating a new Allocation Form. Otherwise use the blank
		// contact originally passed in.
		if(selectedContactId != -1) {
			newFormContact = getContactDAO().findById(selectedContactId);
		}
		else {
			newFormContact.getUser().setFirstName(firstName);
			newFormContact.getUser().setLastName(lastName);
		}
		
		TaxWageAllocationBean bean = (TaxWageAllocationBean)getConfirmationHolder();
		bean.setNewFormContact(newFormContact);

		return super.actionOk();
	}
	
	public void listenContactChange(ValueChangeEvent event) {
		selectedContactId = (Integer)event.getNewValue();
		disableAddBttn = false;
		addNewContact = false;
		contactAllocFormExists = false;

		// See of the existing contact already has allocation forms.
		// If so, set the error flag.
		if(selectedContactId > 0) {
			Contact selectedContact = getContactDAO().findById(selectedContactId);
			List<TaxWageAllocationForm> forms = getTaxWageAllocationFormDAO().findByContact(selectedContact, null);

			if(forms != null && !forms.isEmpty()) {
				// Forms already exist;
				disableAddBttn = true;
				contactAllocFormExists = true;
			}
		}
		
		if(selectedContactId == -1) {
			// Adding new contact so show the Last and First name fields.
			addNewContact = true;
			disableAddBttn = true;

		}
	}

	/**
	 * Create a list of Contact select items from the list
	 * of contacts passed in.
	 */
	private void createContactsDL() {
		contactsDL = new ArrayList<>();

		contactsDL.add(new SelectItem(-1, "(add New Person)"));

		if(contacts != null && !contacts.isEmpty()) {
			for(Contact contact : contacts) {
				contactsDL.add(new SelectItem(contact.getId(), contact.getUser().getLastNameFirstName()));
			}
		}
	}
}

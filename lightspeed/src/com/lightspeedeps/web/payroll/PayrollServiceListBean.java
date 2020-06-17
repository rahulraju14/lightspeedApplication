package com.lightspeedeps.web.payroll;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.AddressDAO;
import com.lightspeedeps.dao.ImageDAO;
import com.lightspeedeps.dao.PayrollServiceDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.Image;
import com.lightspeedeps.model.PayrollService;
import com.lightspeedeps.model.PersistentObject;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.type.ServiceMethod;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.view.ListImageView;

/**
 * Backing bean for the Payroll Service page.
 */
@ManagedBean
@ViewScoped
public class PayrollServiceListBean extends ListImageView implements Serializable {
	/** */
	private static final long serialVersionUID = - 6190739835038434764L;

	private static final Log log = LogFactory.getLog(PayrollServiceListBean.class);

	private static final int ACT_DELETE_IMAGE = 11;

	private List<PayrollService> services;

	private PayrollService payrollService;

	private List<Image> imageList;

	/** List of productions to which the currently Payroll Service is assigned. */
	private List<Production> productionList;

	/** Set from JSP to indicate which image is being added or deleted.
	 * Values are "D":desktop, "M":mobile, or "R":report */
	private String imageType;

	/** connection method selection list */
	private static List<SelectItem> serviceMethodList = EnumList.createEnumValueSelectList(ServiceMethod.class);

	PayrollServiceDAO payrollServiceDAO;

	/**
	 * default constructor
	 */
	public PayrollServiceListBean() {
		super(PayrollService.SORTKEY_NAME, "Payroll.");
		log.debug("Init");

		refreshList();
		setup();
		checkTab();
	}

	private void setup() {
		if (services.size() > 0) {
			Integer id = SessionUtils.getInteger(Constants.ATTR_PAYROLL_LIST_ID);
			if (id != null) {
				payrollService = getPayrollServiceDAO().findById(id);
			}
			else {
				payrollService = services.get(0);
			}
			payrollService.setSelected(true);
		}
		forceLazyInit();
	}

	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);
		if (payrollService != null) {
			payrollService.setSelected(false);
		}
		imageList = null;
		productionList = null;
		if (id == null) {
			id = findDefaultId();
		}
		if (id == null) {
			SessionUtils.put(Constants.ATTR_PAYROLL_LIST_ID, null);
			payrollService = null;
			editMode = false;
			newEntry = false;
		}
		else if ( ! id.equals(NEW_ID)) {
			payrollService = getPayrollServiceDAO().findById(id);
			if (payrollService == null) {
				id = findDefaultId();
				if (id != null) {
					payrollService = getPayrollServiceDAO().findById(id);
				}
			}
			SessionUtils.put(Constants.ATTR_PAYROLL_LIST_ID, id);
		}
		else { // Add new service
			payrollService = new PayrollService();
			PayrollServiceDAO.getInstance().save(payrollService);
		}
		if (payrollService != null) {
			payrollService.setSelected(true);
			if (payrollService.getAddress() == null) {
				payrollService.setAddress(new Address());
			}
			if (payrollService.getMailingAddress() == null) {
				payrollService.setMailingAddress(new Address());
			}
			int ix = services.indexOf(payrollService);
			if (ix >= 0) { // ensure 'selectedRow' matches
				setSelectedRow(ix);
			}
			forceLazyInit();
		}
	}

	@SuppressWarnings({ "rawtypes" })
	protected Integer findDefaultId() {
		Integer id = null;
		if (getItemList().size() > 0) {
			id = ((PersistentObject)getItemList().get(0)).getId();
		}
		return id;
	}

	/**
	 *
	 */
	private void forceLazyInit() {
		if (payrollService != null) {
			payrollService.getName();
			if (payrollService.getDesktopLogo() != null) {
				payrollService.getDesktopLogo().getTitle();
			}
			if (payrollService.getMobileLogo() != null) {
				payrollService.getMobileLogo().getTitle();
			}
			if (payrollService.getReportLogo() != null) {
				payrollService.getReportLogo().getTitle();
			}
			if (payrollService.getAddress() != null) {
				payrollService.getAddress().getAddrLine1();
			}
			if (payrollService.getMailingAddress() != null) {
				payrollService.getMailingAddress().getAddrLine1();
			}
		}
	}

	/**
	 * The Action method of the "Edit" button on the List page.
	 * @return null navigation string
	 */
	@Override
	public String actionEdit() {
		log.debug("");
		try {
			super.actionEdit();
			if (payrollService.getAddress() == null) {
				payrollService.setAddress(new Address());
				log.debug("new address for user -->  " + payrollService.getAddress());
			}
			if (payrollService.getMailingAddress() == null) {
				payrollService.setMailingAddress(new Address());
				log.debug("new address for user -->  " + payrollService.getAddress());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	@Override
	public String actionSave() {
		resetImages();
		if (payrollService != null) {
			if (payrollService.getContact1Email() != null) {
				payrollService.setContact1Email(payrollService.getContact1Email().trim());
			}
			if (payrollService.getContact2Email() != null) {
				payrollService.setContact2Email(payrollService.getContact2Email().trim());
			}
			if (payrollService.getContact3Email() != null) {
				payrollService.setContact3Email(payrollService.getContact3Email().trim());
			}
			PayrollServiceDAO.getInstance().attachDirty(payrollService);
			AddressDAO.getInstance().attachDirty(payrollService.getAddress());
			AddressDAO.getInstance().attachDirty(payrollService.getMailingAddress());
			PayrollServiceDAO.resetPayrollServices();
		}
		refreshList();
		getElement().setSelected(true);
		forceLazyInit();
		return super.actionSave();
	}

	/**
	 * The Action method for Cancel button while in Edit mode. Cleans up the
	 * state of any images added or removed, and calls our superclass'
	 * actionCancel() method.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		try {
			super.actionCancel();
			imageList = null;
			if (isNewEntry() || payrollService == null) {
				refreshList();
				setup();
			}
			else {
				payrollService = PayrollServiceDAO.getInstance().refresh(payrollService);
			}
			forceLazyInit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	@Override
	public String actionDelete() {
		return super.actionDelete();
	}

	/**
	 * Delete the selected Payroll Service from Database. This is called as a result
	 * of clicking "ok" on the delete confirmation pop-up.
	 */
	@Override
	public String actionDeleteOk() {
		try {
			payrollService = getPayrollServiceDAO().findById(payrollService.getId()); // refresh
			getPayrollServiceDAO().delete(payrollService);
			SessionUtils.put(Constants.ATTR_PAYROLL_LIST_ID, null);

			services.remove(payrollService);
			payrollService = null;

			refreshList();
			setupSelectedItem(null);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	public String actionDeleteImage() {
		log.debug("delete: " + imageType);
		PopupBean.getInstance().show(
				this, ACT_DELETE_IMAGE, "Image.Delete.");
		return null;
	}

	/**
	 * Delete a particular payroll image; called via "confirmOk" when
	 * the user clicks the OK button on the confirmation dialog.
	 *
	 * @return null navigation string
	 */
	private String actionDeleteImageOk() {
		Image image = null;
		if (imageType.equals("D")) {
			image = payrollService.getDesktopLogo();
			payrollService.setDesktopLogo(null);
		}
		else if (imageType.equals("M")) {
			image = payrollService.getMobileLogo();
			payrollService.setMobileLogo(null);
		}
		else if (imageType.equals("R")) {
			image = payrollService.getReportLogo();
			payrollService.setReportLogo(null);
		}
		if (image != null) {
			getPayrollServiceDAO().attachDirty(payrollService);
			ImageDAO.getInstance().delete(image);
		}
		return null;
	}

	@Override
	public String actionOpenNewImage() {
		log.debug("new: " + imageType);
		super.actionOpenNewImage();
		getImageAddBean().setAutoCommit(true);
		return null;
	}

	/**
	 * Set default values depending on whether this is a Team payroll service
	 * @param event
	 */
	public void listenServiceMethodChange(ValueChangeEvent event) {
		ServiceMethod serviceMethod = (ServiceMethod)event.getNewValue();

		try {
			if(serviceMethod != null) {
				if(payrollService != null) {
					if(serviceMethod.isTeam()) {
						payrollService.setDisablePayBreakdownLines(true);
						payrollService.setBreakByDay(true);
						payrollService.setTeamPayroll(true);
					}
					else {
						payrollService.setTeamPayroll(false);
					}
				}
			}
		}
		catch(Exception ex) {
			EventUtils.logError("Error changing service method for payroll service. " + ex.getMessage());
		}
	}

	/**
	 * Typically implemented by subclasses to rebuild whatever list is displayed
	 * as that class' "main list".
	 */
	@Override
	protected void refreshList() {
		services = PayrollServiceDAO.getInstance().findAll();
	}

	/**
	 * Called when user clicks OK in one of our standard pop-up dialog boxes.
	 * The 'action' parameter indicates what task is supposed to be performed.
	 *
	 * @see com.lightspeedeps.web.view.ListView#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_DELETE_IMAGE:
				res = actionDeleteImageOk();
				break;
			default:
				res = super.confirmOk(action);
		}
		return res;
	}

	/**
	 * @return The "list of images" owned by the Payroll service.  This is used
	 * by the ImagePaginatorBean and ImageAddBean when adding and removing
	 * elements.  Since we only support a single logo, we just create a list
	 * and add the logo element to it.
	 */
	@Override
	public List<Image> getImageList() {
		if (imageList == null) {
			imageList = new ArrayList<>();
			if (payrollService != null && payrollService.getDesktopLogo() != null) {
				imageList.add(payrollService.getDesktopLogo());
			}
		}
		return imageList;
	}

	/**
	 * @see com.lightspeedeps.web.image.ImageHolder#updateImage(com.lightspeedeps.model.Image, java.lang.String)
	 */
	@Override
	public void updateImage(Image image, String filename) {
		log.debug("");
		try {
			if (image != null) {
				Image priorImage = null;
				getImageList().clear();
				if (imageType.equals("D")) {
					priorImage = payrollService.getDesktopLogo();
					payrollService.setDesktopLogo(image);
				}
				else if (imageType.equals("M")) {
					priorImage = payrollService.getMobileLogo();
					payrollService.setMobileLogo(image);
				}
				else if (imageType.equals("R")) {
					priorImage = payrollService.getReportLogo();
					payrollService.setReportLogo(image);
				}
				getImageList().add(image);
				if (priorImage != null) {
					getPayrollServiceDAO().attachDirty(payrollService);
					getPayrollServiceDAO().delete(priorImage);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * @see com.lightspeedeps.web.image.ImageHolder#removeImage(com.lightspeedeps.model.Image)
	 */
	@Override
	public void removeImage(Image image) {
		try {
			getImageList().clear();
			payrollService.setDesktopLogo(null);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * @see com.lightspeedeps.web.view.ImageView#getElementName()
	 */
	@Override
	public String getElementName() {
		return payrollService.getName();
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#setSelected(java.lang.Object, boolean)
	 */
	@Override
	protected void setSelected(Object item, boolean b) {
		((PayrollService)item).setSelected(b);
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#getComparator()
	 */
	@Override
	protected Comparator<PayrollService> getComparator() {
		Comparator<PayrollService> comparator = new Comparator<PayrollService>() {
			@Override
			public int compare(PayrollService c1, PayrollService c2) {
				return c1.compareTo(c2, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	/**
	 * Get a List of all the Production`s that the currently selected PayrollService is assigned to.
	 *
	 * @return A non-null, but possibly empty, List as described above.
	 */
	private List<Production> createProductionList() {
		List<Production> list = ProductionDAO.getInstance().findByProperty("payrollPref.payrollService", payrollService);
		Collections.sort(list);
		return list;
	}

	// accessors and mutators

	/**
	 * @see com.lightspeedeps.web.view.ListView#getItemList()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List getItemList() {
		return services;
	}

	public PayrollService getElement() {
		return payrollService;
	}

	/**See {@link #services}. */
	public List<PayrollService> getServices() {
		return services;
	}
	/**See {@link #services}. */
	public void setServices(List<PayrollService> services) {
		this.services = services;
	}

	/**See {@link #payrollService}. */
	public PayrollService getPayrollService() {
		return payrollService;
	}
	/**See {@link #payrollService}. */
	public void setPayrollService(PayrollService payrollService) {
		this.payrollService = payrollService;
	}

	/**See {@link #imageType}. */
	public String getImageType() {
		return imageType;
	}
	/**See {@link #imageType}. */
	public void setImageType(String imageType) {
		this.imageType = imageType;
	}

	/**See {@link #serviceMethodList}. */
	public List<SelectItem> getServiceMethodList() {
		return serviceMethodList;
	}
	/**See {@link #serviceMethodList}. */
	public void setServiceMethodList(List<SelectItem> list) {
		serviceMethodList = list;
	}

	/** See {@link #productionList}. */
	public List<Production> getProductionList() {
		if (productionList == null) {
			productionList = createProductionList();
		}
		return productionList;
	}
	/** See {@link #productionList}. */
	public void setProductionList(List<Production> productions) {
		productionList = productions;
	}

	/**
	 * @return The current instance of PayrollServiceDAO.
	 */
	private PayrollServiceDAO getPayrollServiceDAO() {
		if (payrollServiceDAO == null) {
			payrollServiceDAO = PayrollServiceDAO.getInstance();
		}
		return payrollServiceDAO;
	}

}

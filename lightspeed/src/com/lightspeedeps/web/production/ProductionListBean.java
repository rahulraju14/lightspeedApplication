package com.lightspeedeps.web.production;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import javax.faces.application.FacesMessage;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.util.IceOutputResource;

import com.lightspeedeps.dao.AddressDAO;
import com.lightspeedeps.dao.ProductDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.model.Image;
import com.lightspeedeps.model.Product;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.User;
import com.lightspeedeps.service.OnboardService;
import com.lightspeedeps.type.OrderStatus;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.ImageUtils;
import com.lightspeedeps.util.common.TimeZoneUtils;
import com.lightspeedeps.web.validator.EmailValidator;
import com.lightspeedeps.web.view.ListImageView;

/**
 * The superclass of backing beans for both the Prod Admin / Production
 * and My Production - List/View/Edit pages.
 */
public abstract class ProductionListBean extends ListImageView implements Serializable {
	/** */
	private static final long serialVersionUID = 4326782440715178256L;

	private static final Log log = LogFactory.getLog(ProductionListBean.class);

	/** The index of our "Admin" mini-tab (0). */
	protected static final int TAB_ADMIN = 0;
	/** The index of our "Detail" mini-tab (1). */
	protected static final int TAB_DETAIL = 1;

	/* Variables*/

	/** The list of elements currently displayed. */
	protected List<Production> productionList;

	/** The currently viewed item. */
	protected Production production;

	/** The filter text used to limit the list of Productions displayed.  Only
	 * available if the (un-filtered) list is long enough to cause pagination. */
	protected String filter;

	protected BigDecimal billAmount = BigDecimal.ZERO;

	/** A List of images, required by the ImageHolder interface (to ImageAddBean),
	 * which, for Production purposes, holds, at most, a single Image, the logo. */
	protected List<Image> imageList;

	private transient ProductionDAO productionDAO;

	/** List of images, as resource objects, for selected production, used for JSP presentation. */
	private transient List<IceOutputResource> imageResources;

	@Override
	protected abstract void refreshList();

	/**
	 * Normal constructor.
	 *
	 * @param sortKey The default sortkey, which determines which column in our
	 *            main list is used to sort the items for their initial display.
	 *            The available sortkey values are usually defined as static
	 *            constants in the model class that corresponds to the items
	 *            in the list.
	 * @param msgPrefix The message id prefix, used by several superclass
	 *            methods that provide standard functions (such as Delete),
	 *            often with dialog boxes. The prefix is used to create a full
	 *            message id which will be looked up in our
	 *            messageResources.properties file. By convention, the supplied
	 *            prefix should end with a period. The string usual reflects the
	 *            primary type of item or function of the page being backed,
	 *            e.g., "Project." or "Contact.".
	 */
	public ProductionListBean(String sortKey, String msgPrefix) {
		super(sortKey, msgPrefix);
		log.debug("Init");
		try {
			setImageAddedMessageId("Image.Uploaded.Logo"); // custom message for adding logo image
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Determine the Production.id value of the default Production to show as
	 * selected. This is typically the first one in the list.
	 *
	 * @return Production.id value, or null if the list of Production`s is
	 *         empty.
	 */
	@SuppressWarnings("unchecked")
	protected Integer findDefaultId() {
		Integer id = null;
		List<Production> list = getItemList();
		if (list.size() > 0) {
			id = list.get(0).getId();
		}
		return id;
	}

	/**
	 * Ensure all the fields we need for the render phase are initialized.
	 */
	protected void forceLazyInit() {
		if (getElement().getAddress() != null) {
			getElement().getAddress().getAddrLine1();
		}
//		if (getElement().getDefaultProject() != null) {
//			getElement().getDefaultProject().getSequence();
//		}
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#setSelected(java.lang.Object, boolean)
	 */
	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((Production)item).setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * The Action method of the "Edit" button on the View page. Gets a fresh
	 * copy of the Production instance and then switches to Edit mode.
	 *
	 * @return empty navigation string
	 */
	@Override
	public String actionEdit() {
		try {
			production = getProductionDAO().refresh(production);
			if (production != null) {
				forceLazyInit();
				super.actionEdit();
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for the Save button - save any changes and exit Edit mode.
	 *
	 * @return empty navigation string
	 * @see com.lightspeedeps.web.view.ListImageView#actionSave()
	 */
	@Override
	public String actionSave() {
		try {
			if (getElement().getTitle() == null || getElement().getTitle().trim().length() == 0) {
				MsgUtils.addFacesMessage("Production.BlankName", FacesMessage.SEVERITY_ERROR);
				setSelectedTab(getTabDetail());
				return null;
			}

			TimeZone tz = TimeZoneUtils.getTimeZone(production.getTimeZoneStr());
			if (tz == null) {
				MsgUtils.addFacesMessage("Production.UnknownTimeZone", FacesMessage.SEVERITY_ERROR,
						production.getTimeZoneStr());
				setSelectedTab(TAB_ADMIN);
				return null;
			}
			production.setTimeZone(tz);

			if (! validateEmailSender(production)) {
				MsgUtils.addFacesMessage("Email.InvalidPrefix", FacesMessage.SEVERITY_ERROR);
				setSelectedTab(getTabDetail());
				return null;
			}

			// Replace any characters that would pose a security risk in the title or studio name.
			String value = getElement().getTitle().trim();
			value = ApplicationUtils.fixSecurityRiskForStrings(value);
			getElement().setTitle(value);
			if (production.getType().hasPayrollByProject()) {
				// for Commercial productions, we sync studio (prod company) to title
				production.setStudio(production.getTitle());
			}
			AddressDAO.getInstance().merge(getProduction().getAddress());
			if (! newEntry) {
				if (production.getType().isTalent() && ! production.getAllowOnboarding()) {
					// if production's type is changed from other type to Canada talent, enable Onboarding.
					// else, if we create new Production with its type Canada Talent, then ProductionDAO.create()
					// which we are calling in the else check below will enable its Onboarding feature automatically.
					log.debug("Changed to Canada Talent");
					production.setAllowOnboarding(true);
					OnboardService service = new OnboardService();
					production = service.enableProductionOnboarding(production);
					//
				}
				production = getProductionDAO().merge(production);
				commitImages();
				setImageResources(null);
				if (! production.getType().getEpisodic()) {
					if (production.getProjects().size() > 0) {
						Project proj = production.getProjects().iterator().next();
						proj.setTitle(production.getTitle());
						ProjectDAO.getInstance().merge(proj);
					}
					else { // should never happen! test added for bugz #856; rev 4062.
						production = getProductionDAO().findById(production.getId()); // force refresh
						throw new IllegalArgumentException("project set has size 0");
					}
				}
				refreshList();
				getElement().setSelected(true);
				forceLazyInit();
			}
			else { // unusual: admin did "Add" on Prod Admin/Productions page
				// Note: most default values are set in Production constructor
				production.setSku("added");
				User user = SessionUtils.getCurrentUser();
				production = getProductionDAO().create(production, user);
				refreshList();
				scrollToRow();
			}
			SessionUtils.put(Constants.ATTR_PRODUCTION_ID, production.getId());
			return super.actionSave();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * The Action method for Cancel button while in Edit mode.
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		super.actionCancel();
		if (getElement() == null || isNewEntry()) {
			production = null;
			refreshList();
		}
		else {
			production = getProductionDAO().refresh(production);
		}
		return null;
	}

	/** Implemented by subclasses. */
//	public String actionDeleteOk() {
//		try {
//			production = productionDAO.findById(production.getId()); // refresh
//			productionDAO.remove(production);
//			SessionUtils.put(Constants.ATTR_PRODUCTION_ID, null);
//
//			productionList.remove(production);
//			production = null;
//			setupSelectedItem(getRowId(getSelectedRow()));
//			addClientResize();
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//		}
//		return null;
//	}

	/**
	 * Validate the "emailSender" field of the given Production.
	 *
	 * @param prod The Production to be validated.
	 * @return True iff the emailSender field was, or has been made, valid. If,
	 *         upon entry, the Production's emailSender field is null, empty, or
	 *         all blank, it is set to the default value. If it is non-blank, it
	 *         will be trimmed. If it is non-blank and invalid, no further
	 *         change is made to it and false is returned.
	 */
	public static boolean validateEmailSender(Production prod) {
		if (prod.getEmailSender() != null) {
			String prefix = prod.getEmailSender().trim();
			if (prefix.length() == 0) {
				prod.setEmailSender(Constants.DEFAULT_EMAIL_SENDER);
			}
			else {
				prod.setEmailSender(prefix); // save trimmed value
				// check for validity as email address prefix! E.g., cannot contain "@".
				if (! EmailValidator.isValidLocalName(prefix)) {
					return false;
				}
			}
		}
		else {
			prod.setEmailSender(Constants.DEFAULT_EMAIL_SENDER);
		}
		return true;
	}

	/**
	 * If no item is currently selected, find the current element in
	 * the main (left-hand) list, select it, and send a JavaScript
	 * command to scroll the list so that it is visible.
	 */
	protected void scrollToRow() {
		scrollToRow(getElement());
	}

	/**
	 * Return the id of the item that resides in the n'th row of the
	 * currently displayed list.
	 * @param row
	 * @return Returns null only if the list is empty.
	 */
	protected Integer getRowId(int row) {
		Object item;
		return ((item=getRowItem(row)) == null ? null : ((Production)item).getId());
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected Comparator getComparator() {
		Comparator<Production> comparator = new Comparator<Production>() {
			@Override
			public int compare(Production c1, Production c2) {
				return c1.compareTo(c2, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	/**
	 * @return The list of images owned by the current element.  This is used
	 * by the ImagePaginatorBean and ImageAddBean when adding and removing
	 * elements.
	 */
	@Override
	public List<Image> getImageList() {
		if (imageList == null) {
			setImageResources(null);
			imageList = new ArrayList<>();
			if (getElement() != null && getElement().getLogo() != null) {
				imageList.add(getElement().getLogo());
			}
		}
		return imageList;
	}

	/** See {@link #imageResources}. */
	@Transient
	public List<IceOutputResource> getImageResources() {
		if(imageResources == null) {
			imageResources = ImageUtils.createImageResources(getImageList());
		}
		return imageResources;
	}
	/** See {@link #imageResources}. */
	public void setImageResources(List<IceOutputResource> imageResources) {
		this.imageResources = imageResources;
	}

	/**
	 * Called by ImageAddBean when the user has successfully uploaded a new
	 * image. Since we only support a single logo for the production, we replace
	 * any contents of the imageList with the new entry.
	 *
	 * @see com.lightspeedeps.web.view.ListImageView#updateImage(com.lightspeedeps.model.Image,
	 *      java.lang.String)
	 */
	@Override
	public void updateImage(Image image, String filename) {
		log.debug("");
		try {
			setImageResources(null);
			if (image != null) {
				getImageList().clear();
				production.setLogo(image);
				getImageList().add(image);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Callback from ImagePaginatorBean.
	 */
	@Override
	public void removeImage(Image image) {
		try {
			setImageResources(null);
			getImageList().clear();
			production.setLogo(null);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Returns the current element's name, for use in the text and/or title of
	 * the Add Image dialog.
	 */
	@Override
	public String getElementName() {
		return getElement().getTitle();
	}

	/**
	 * Determine the monthly billing amount, based on the current Production's
	 * SKU, and the matching product's price.
	 */
	protected void calculateBillAmount() {
		billAmount = BigDecimal.ZERO;
		if (production != null && production.getSku() != null
				&& production.getOrderStatus() != OrderStatus.FREE) {
			Product product = ProductDAO.getInstance().findOneByProperty(ProductDAO.SKU, production.getSku());
			if (product != null && product.getPrice() != null && product.getPrice().intValue() > 0) {
				billAmount = product.getPrice();
			}
		}
	}

	/**
	 * @return the production
	 */
	public Production getProduction() {
		return production;
	}
	public void setProduction(Production production) {
		this.production = production;
	}
	public Production getElement() {
		return production;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List getItemList() {
		return productionList;
	}

	public BigDecimal getBillingAmount() {
		return billAmount;
	}

	/**See {@link #TAB_DETAIL}.  May be overridden by subclasses
	 * which have different mini-tab arrangements. */
	public int getTabDetail() {
		return TAB_DETAIL;
	}

	/** See {@link #filter}. */
	public String getFilter() {
		return filter;
	}
	/** See {@link #filter}. */
	public void setFilter(String filter) {
		this.filter = filter;
	}

	protected ProductionDAO getProductionDAO() {
		if (productionDAO == null) {
			productionDAO = ProductionDAO.getInstance();
		}
		return productionDAO;
	}
}

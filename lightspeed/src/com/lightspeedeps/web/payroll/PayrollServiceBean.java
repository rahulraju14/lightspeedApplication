package com.lightspeedeps.web.payroll;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.PayrollServiceDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.model.Image;
import com.lightspeedeps.model.PayrollService;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.image.ImageHolder;
import com.lightspeedeps.web.view.ImageView;

/**
 * Backing bean for the Payroll Service page.
 */
@ManagedBean
@ViewScoped
public class PayrollServiceBean extends ImageView implements ImageHolder, Serializable {
	/** */
	private static final long serialVersionUID = - 6190739835038434764L;

	private static final Log log = LogFactory.getLog(PayrollServiceBean.class);

//	private static final int ACT_NEW_BATCH = 15;

	private Production production;

	private PayrollService payrollService;

	private List<Image> imageList;

	/**
	 * default constructor
	 */
	public PayrollServiceBean() {
		super("Payroll.");

		log.debug("Init");

		production = SessionUtils.getProduction();
		payrollService = production.getPayrollPref().getPayrollService();

		forceLazyInit();
	}

	@Override
	public String actionSave() {
		resetImages();
		if (payrollService != null) {
			PayrollServiceDAO.getInstance().attachDirty(payrollService);
		}
		if (production != null) {
			ProductionDAO.getInstance().attachDirty(production);
		}
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
			payrollService = PayrollServiceDAO.getInstance().refresh(payrollService);
			forceLazyInit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
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
		}
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
			imageList = new ArrayList<Image>();
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
				getImageList().clear();
				payrollService.setDesktopLogo(image);
				getImageList().add(image);
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
	 * @see com.lightspeedeps.web.image.ImageHolder#addEndingJavascript()
	 */
	@Override
	public void addEndingJavascript() {
		// no extra Javascript required.
	}

	// accessors and mutators

	/**See {@link #payrollService}. */
	public PayrollService getPayrollService() {
		return payrollService;
	}
	/**See {@link #payrollService}. */
	public void setPayrollService(PayrollService payrollService) {
		this.payrollService = payrollService;
	}

	/**See {@link #production}. */
	public Production getProduction() {
		if (production == null) {
			production = SessionUtils.getProduction();
		}
		return production;
	}

	/**
	 * @see com.lightspeedeps.web.view.ImageView#getElementName()
	 */
	@Override
	public String getElementName() {
		return payrollService.getName();
	}

}

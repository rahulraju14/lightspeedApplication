package com.lightspeedeps.web.production;

import java.io.Serializable;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ProductDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.model.Product;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.MemberType;
import com.lightspeedeps.type.ProductionType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.popup.PopupBean;

/**
 * Backing bean for the "Choose a Product" page (viewed when a user
 * clicks the "Create" button from the My Productions page).
 */
@ManagedBean
@ViewScoped
public class ProductListBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 8181444623838804183L;

	private static final Log log = LogFactory.getLog(ProductListBean.class);

	/** List of feature-film products */
	private List<Product> filmProductList;

	/** List of TV-series products */
	private List<Product> tvProductList;

	/** List of TV-commercial products */
	private List<Product> commProductList;

	/** The unique database id of the Product entry selected by the user. */
	private Integer productId;

	/** Constructor */
	public ProductListBean() {
		log.debug("");
		initialize();
	}

	/**
	 * Initialization of product lists.
	 */
	private void initialize() {
		log.debug("");
		try {
			ProductDAO productDAO = ProductDAO.getInstance();
			User user = SessionUtils.getCurrentUser();
			if (user.getMemberType() == MemberType.PREMIUM) {
				// LS staff users see all Product entries ("LS Admin" on System not required)
				filmProductList = productDAO.findPremium(ProductionType.TOURS);
				filmProductList.addAll(productDAO.findPremium(ProductionType.FEATURE_FILM));
				tvProductList = productDAO.findPremium(ProductionType.TELEVISION_SERIES);
				commProductList = productDAO.findPremium(ProductionType.TV_COMMERCIALS);
				commProductList.addAll(productDAO.findPremium(ProductionType.CANADA_TALENT));
				commProductList.addAll(productDAO.findPremium(ProductionType.US_TALENT));
			}
			else { // everyone else sees all but those with '-99' in their SKU
				filmProductList = productDAO.findPublic(ProductionType.FEATURE_FILM);
				tvProductList = productDAO.findPublic(ProductionType.TELEVISION_SERIES);
				commProductList = productDAO.findPublic(ProductionType.TV_COMMERCIALS);
			}
		}
		catch (Exception e) {
			EventUtils.logError("exception: ", e);
		}
	}

	/**
	 * Action method when the user hits a Select button on one of the
	 * Product entries listed.  If the selection passes our error tests,
	 * the user is forwarded to the next page. The productId value should
	 * be set already from an f:setPropertyActionListener tag.
	 */
	public String actionSelect() {
		String nextPage = null;
		SessionUtils.put(Constants.ATTR_SELECTED_PRODUCT_ID, null);
		SessionUtils.put(Constants.ATTR_UPGRADE_PRODUCTION_ID, null);
		try {
			if (productId != null) {
				Product currentProduct = ProductDAO.getInstance().findById(productId);
				if (currentProduct != null) {
					log.debug(currentProduct.getSku());
					if (! checkFree(currentProduct)) {
						return null;
					}
					if (! checkStudent(currentProduct)) {
						return null;
					}
					SessionUtils.put(Constants.ATTR_SELECTED_PRODUCT_ID, productId);
					nextPage = "createproduction";
				}
				else {
					log.error("Invalid SKU in jsp - no matching product found!");
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("exception: ", e);
		}
		return nextPage;
	}

	/**
	 * Determine if the user already owns more free trials than allowed; if so,
	 * put up a dialog box the problem.
	 *
	 * @param currentProduct
	 * @return True iff the user is allowed to create another free production.
	 */
	public static boolean checkFree(Product currentProduct) {
		if (currentProduct == null) { // should not happen!
			return false;
		}
		if (currentProduct.getPrice().intValue() == 0 &&
				currentProduct.getSku().indexOf("-ED-") < 0) {
			User user = SessionUtils.getCurrentUser();
			if (user.getMemberType() == MemberType.FREE) {
				// check for limit of 'n' free trials per user
				int hasFree = ProductionDAO.getInstance()
						.findCountOwnedFree(user);
				if (hasFree >= Constants.MAX_FREE_PRODUCTIONS) {
					String phone = Constants.LS_SUPPORT_PHONE;
					String msg;
					if(SessionUtils.isTTCOnline()) {
						phone = Constants.TTC_ONLINE_SUPPORT_PHONE;
					}
					
					msg = MsgUtils.formatMessage("Production.NoMoreFree.Text", phone);
					PopupBean checkFreePopup = PopupBean.getInstance();
					checkFreePopup.show(null, 0,
							"Production.NoMoreFree.Title", "Confirm.OK", null);
					checkFreePopup.setMessage(msg);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Determine if the user has selected a product that is only available to
	 * students. If so, check if the User.student flag is set, and, if not, put
	 * up a dialog box explaining the problem.  Products that are only available
	 * to students should have the string "-ED-" (EDucational) somewhere in their SKU.
	 *
	 * @param currentProduct
	 * @return False iff the product is only available to students, and the
	 *         User.student flag is NOT set.
	 */
	public static boolean checkStudent(Product currentProduct) {
		if (currentProduct.getSku().indexOf("-ED-") >= 0) {
			// check for student attribute if requesting EDucational SKU
			if (! SessionUtils.getCurrentUser().getStudent()) {
				PopupBean.getInstance().show(null, 0,
						"Production.StudentOnly.Title", "Production.StudentOnly.Text",
						"Confirm.Close", null);
				return false;
			}
			else {
				int hasEd = ProductionDAO.getInstance()
						.findCountOwnedEducational(SessionUtils.getCurrentUser());
				if (hasEd >= Constants.MAX_STUDENT_PRODUCTIONS) {
					String phone = Constants.LS_SUPPORT_PHONE;
					if(SessionUtils.isTTCOnline()) {
						phone = Constants.TTC_ONLINE_SUPPORT_PHONE;
					}
					
					PopupBean conf = PopupBean.getInstance();
					conf.show(null, 0,
							"Production.NoMoreStudent.Title",
							"Confirm.Close", null);
					String msg = MsgUtils.formatMessage("Production.NoMoreStudent.Text",
							Constants.MAX_STUDENT_PRODUCTIONS, phone);
					conf.setMessage(msg);
					return false;
				}
			}
		}
		return true;
	}

	public List<Product> getFilmProductList() {
		return filmProductList;
	}
	public void setFilmProductList(List<Product> filmProductList) {
		this.filmProductList = filmProductList;
	}

	/** See {@link #tvProductList}. */
	public List<Product> getTvProductList() {
		return tvProductList;
	}
	/** See {@link #tvProductList}. */
	public void setTvProductList(List<Product> tvProductList) {
		this.tvProductList = tvProductList;
	}

	/**See {@link #commProductList}. */
	public List<Product> getCommProductList() {
		return commProductList;
	}
	/**See {@link #commProductList}. */
	public void setCommProductList(List<Product> commProductList) {
		this.commProductList = commProductList;
	}

	/** See {@link #productId}. */
	public Integer getProductId() {
		return productId;
	}
	/** See {@link #productId}. */
	public void setProductId(Integer productId) {
		this.productId = productId;
	}

}

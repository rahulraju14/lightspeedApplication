package com.lightspeedeps.web.production;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.checkout.Settings;
import com.lightspeedeps.dao.CouponDAO;
import com.lightspeedeps.dao.ProductDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.AccessStatus;
import com.lightspeedeps.type.OrderStatus;
import com.lightspeedeps.type.ProductionType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * Backing bean for the "purchase confirmation" page, where the one product
 * selected is displayed, along with some explanatory text, and possibly a
 * Checkout button which will take the user to the authorize.net payment page.
 * This page is also where the user will be requested to enter the production
 * name for a new production. The same page is used for upgrades and
 * re-subscribes, which do not require entry of a production name.
 */
@ManagedBean
@ViewScoped
public class CreateProductionBean implements Serializable {
	/** */
	private static final long serialVersionUID = 5959862887439990546L;

	private static final Log log = LogFactory.getLog(CreateProductionBean.class);

	/** The Product selected for purchase by the user. */
	private Product currentProduct;

	/** The newly-created Production. */
	private Production production;

	/** The 'list' of products, which will only have a single entry -- the
	 * one selected for purchase by the user.  We back this as a list, so we
	 * can use the same table display JSP code for the 'create production'
	 * page as for the 'select product' page. */
	private List<Product> productList;

	/** The unique database id of the Product entry selected by the user. */
	private Integer productId;

	/** The name of a new production to be created. */
	private String createProdName;

	/** The user-entered reseller code on the Pre-purchased page. */
	private String resellerCode;

	/** The user-entered discount coupon code, if any. */
	private String couponCode;

	/** The "cleaned" discount coupon code -- the user's input, minus all
	 * blanks and hyphens, and uppercase. */
	private String couponCodeOut;

	/** The coupon entity that corresponds to the user's coupon code. */
	private Coupon coupon;

	/** Error message, if any, to be displayed below the coupon entry field */
	private String couponMsg;

	/** True iff the "checkout" button should be displayed. */
	private boolean showCheckout;

	/** True iff the "upgrade" text should be displayed. */
	private boolean showUpgrade;

	/** True iff the "resubscribe" text should be displayed. */
	private boolean showResubscribe;

	/** True iff the "Continue" button should be enabled. */
	private boolean showContinue;

	/** True iff the text and input field prompting the user to enter a
	 * coupon code should be displayed. */
	private boolean showCouponPrompt;

	/** True iff a coupon discount has been successfully applied, and the
	 * corresponding page text should be displayed. */
	private boolean showCouponApplied;

	/** True iff the page being displayed is the pre-paid purchase page.
	 * This is set via getPrepaid(), called as a result of a 'rendered' tag in JSP. */
	private boolean prepaidPage;

	private String amount;

	/** A unique invoice number for this transaction, which we generate. */
	private String invoiceNumber;
	/** transaction sequence used for fingerprint generation */
	private int sequence;
	/** timestamp used for fingerprint generation */
	private long timeStamp;
	/** Generated fingerprint for transaction. */
	private String fingerprint;

	/** The URL the form uses to POST to authorize.net; this will vary between
	 * the production and test environments. */
	private String postUrl;

	/** The URL associated with the link or button to return to Lightspeed
	 * from the authorize.net receipt page. */
	private String returnUrl;

	/** The URL shown at the bottom of the Payment page, next to "Submit",
	 * allowing the user to cancel out of the payment page and return
	 * to Lightspeed.*/
	private String cancelUrl;

	/** Constructor */
	public CreateProductionBean() {
		log.debug("");
		HttpServletRequest request = SessionUtils.getHttpRequest();
		String pre = request.getParameter("pr");
		if (pre != null && pre.trim().equals("1")) {
			prepaidPage = true;
		}
		initialize();
	}

	/**
	 * Initialization.
	 */
	private void initialize() {
		log.debug("");
		try {
			postUrl = Settings.getPostUrl();
			currentProduct = null;
			productList = new ArrayList<Product>();
			if (prepaidPage) {
				currentProduct = ProductDAO.getInstance().findOneByProperty(ProductDAO.SKU, "F-PR-99");
				SessionUtils.put(Constants.ATTR_SELECTED_PRODUCT_ID, null);
			}
			else {
				Integer id = SessionUtils.getInteger(Constants.ATTR_SELECTED_PRODUCT_ID);
				if (id != null) {
					currentProduct = ProductDAO.getInstance().findById(id);
				}
			}
			if (currentProduct == null) {
				//MsgUtils.addFacesMessage("Production.InvalidProduct", FacesMessage.SEVERITY_ERROR);
			}
			else {
				productList.add(currentProduct);
				Integer upgradeId = SessionUtils.getInteger(Constants.ATTR_UPGRADE_PRODUCTION_ID);
				if (upgradeId != null) {
					production = ProductionDAO.getInstance().findById(upgradeId);
					if (production != null) {
						createProdName = production.getTitle();
						if (production.getOrderStatus()==OrderStatus.FREE) {
							showCheckout = true;
							showUpgrade = true;
						}
						else {
							if (production.getStatus() == AccessStatus.EXPIRED_ACTIVE ||
									production.getStatus() == AccessStatus.EXPIRED_ARCHIVED ) {
								showCheckout= true;
							}
							showResubscribe = true;
						}
						setupCheckout();
					}
				}
				if (! showCheckout && ! showUpgrade && ! showResubscribe) {
					if (currentProduct.getPrice().signum() > 0) {
						showCouponPrompt = true;
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("exception: ", e);
		}
	}

	/**
	 * Action method for the "Continue" button on the "create production" page.
	 * For free productions, the new production is created here, and the user is
	 * forwarded to the "My Productions" page.
	 *
	 * @return null navigation string if errors detected, or navigation string
	 *         to jump to My Productions page, if the user has created a free
	 *         production.
	 */
	public String actionCreateOk() {
		try {
			showCheckout = false;
			if (currentProduct == null) {
				// unusual case, if user directly enters URL for confirmation page
				return HeaderViewBean.MYPROD_MENU_PROD; // error - jump to My Productions page
			}
			if (createProdName == null || createProdName.trim().length() == 0) {
				MsgUtils.addFacesMessage("Production.BlankName", FacesMessage.SEVERITY_ERROR);
				return null;
			}

			createProdName = createProdName.trim();
			// Make sure there are no characters that could pose a security risk
			createProdName = ApplicationUtils.fixSecurityRiskForStrings(createProdName);

			if (! ProductListBean.checkFree(currentProduct)) {
				return null;
			}
			if (currentProduct.getPrice().signum() > 0) { // only positive prices go to checkout
				showCheckout = true;
				showCouponPrompt = false;
				setupCheckout();
				return null;
			}
			else {
				User user = SessionUtils.getCurrentUser();
				production = ProductionDAO.getInstance().create(
					user.getAccountNumber(), currentProduct, createProdName, user, null);
				DoNotification.getInstance().productionCreated(production, user);
			}
		}
		catch (Exception e) {
			MsgUtils.addGenericErrorMessage();
			EventUtils.logError("exception: ", e);
		}
		return HeaderViewBean.MYPROD_MENU_PROD; // free or error - jump to My Productions page
	}

	/**
	 * The action method for the "Activate" button on the pre-purchased
	 * page.
	 * @return null navigation string
	 */
	public String actionActivate() {
		// validate reseller code
		showContinue = false;
		String code = getResellerCode();
		if (StringUtils.isEmpty(code)) {
			MsgUtils.addFacesMessage("Production.ResellerCode.Missing", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		code = code.trim().toUpperCase();
		boolean valid = false;
		if (code.length() == 12) {
			String reseller = code.substring(0, 4);
			if (Constants.RESELLER_CODES.contains(reseller)) {
				String type = code.substring(4,8);
				if (type.equals("-EP-") || type.equals("-FE-") || type.equals("-CO-")) {
					if (type.equals("-EP-")) {
						currentProduct.setType(ProductionType.TELEVISION_SERIES);
					}
					else if (type.equals("-CO-")) {
						currentProduct.setType(ProductionType.TV_COMMERCIALS);
					}
					else {
						currentProduct.setType(ProductionType.FEATURE_FILM);
					}
					String number = code.substring(8);
					if (NumberUtils.validateChecksum(number)) {
						valid = true;
					}
				}
			}
		}
		if (! valid) {
			MsgUtils.addFacesMessage("Production.ResellerCode.Invalid", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		showContinue = true;
		return null;
	}

	/**
	 * Action method for the "Apply" button on the purchase page. We validate
	 * the user's coupon code and issue appropriate messages.
	 *
	 * @return null navigation string
	 */
	public String actionApplyCoupon() {
		couponMsg = "";
		if (couponCode == null || couponCode.trim().length() == 0) {
			couponMsg = MsgUtils.getMessage("Coupon.MissingCode");
			return null;
		}
		couponCode = couponCode.trim().toUpperCase();
		couponCodeOut = couponCode.replaceAll("[- ]", "");
		CouponDAO couponDAO = CouponDAO.getInstance();
		coupon = couponDAO.findOneByProperty(CouponDAO.CODE, couponCodeOut);
		if (coupon == null) {
			couponMsg = MsgUtils.getMessage("Coupon.UnknownCode");
			return null;
		}
		if (coupon.getUsesLeft() < 1) {
			couponMsg = MsgUtils.getMessage("Coupon.UsedCode");
			return null;
		}
		Calendar cal = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		CalendarUtils.setStartOfDay(cal); // allows using coupon on its expiration date.
		Date now = cal.getTime();
		if (coupon.getCouponType().getExpires() != null && coupon.getCouponType().getExpires().before(now)) {
			couponMsg = MsgUtils.getMessage("Coupon.ExpiredCode");
			return null;
		}
		Matcher m = Pattern.compile(coupon.getCouponType().getSkuPattern()).matcher(currentProduct.getSku());
		if (! m.matches()) {
			couponMsg = MsgUtils.getMessage("Coupon.WrongSKU");
			return null;
		}
		if (! applyDiscount(coupon, currentProduct)) {
			return null;
		}
		showCouponPrompt = true;
		showCouponApplied = true;
		return null;
	}

	/**
	 * Apply the discount specified by the given coupon to the price in the
	 * given product. The Product entry's values for maximum number of users
	 * and SMS-enabled are also set based on the CouponType values.
	 *
	 * @param coupon The Coupon defining the discount to apply.
	 * @param product The Product whose price should be updated appropriately.
	 * @return True iff the coupon contained a supported discount type.
	 */
	public static boolean applyDiscount(Coupon coupon, Product product) {
		boolean bRet = true;
		CouponType type = coupon.getCouponType();
		switch (type.getDiscountType()) {
		case AMOUNT_OFF:
			product.setPrice(product.getPrice().subtract(type.getAmount()));
			break;
		case PRICE:
			product.setPrice(type.getAmount());
			break;
		case PERCENT_OFF:
			BigDecimal discount = product.getPrice().multiply(type.getAmount());
			discount = discount.divide(new BigDecimal(100), 2, RoundingMode.UP);
			product.setPrice(product.getPrice().subtract(discount));
			break;
		case FREE_MONTHS:
		case PRE_PURCHASE:
			MsgUtils.addFacesMessage("Coupon.TypeNotSupported", FacesMessage.SEVERITY_ERROR);
			bRet = false;
			break;
		}
		if (bRet && product.getPrice().signum() < 0) { // discount made price negative!
			product.setPrice(BigDecimal.ZERO);	// set it to zero instead.
		}
		if (type.getNumberUsers() > 0) {
			product.setMaxUsers(type.getNumberUsers().intValue());
		}
		product.setSmsEnabled(type.getSmsEnabled());
		return bRet;
	}

	/**
	 * The action method for the Resubscribe button ONLY used when a production was
	 * unsubscribed recently and the "next billing date" has not yet been reached. In this
	 * case the Production was never expired.  We will remove the End Date (so it won't
	 * expire), and a new ARB (recurring billing) will need to be generated from the
	 * original purchase transaction id.
	 *
	 * @return Navigation string to jump to the My Productions page.
	 */
	public String actionResubscribe() {
		if (production != null) {
			Product product = ProductDAO.getInstance()
					.findOneByProperty(ProductDAO.SKU, production.getSku());
			ProductionDAO.getInstance().resubscribe(production, product);
			DoNotification.getInstance().productionResubscribed(production, SessionUtils.getCurrentUser());
		}
		return "myproductions";
	}

	private void setupCheckout() {
		if (currentProduct.getDuration() == 0) {
			amount = "" + currentProduct.getPrice().toPlainString();
		}
		else {
			double price = (currentProduct.getPrice().floatValue()*100/currentProduct.getDuration());
			BigDecimal dPrice = new BigDecimal(Math.floor(price));
			dPrice = dPrice.divide(Constants.DECIMAL_100).setScale(2);
			amount = "" + dPrice.toPlainString();
		}

		returnUrl = MsgUtils.createPath(Constants.CHECKOUT_PAID_LINK, false);
		cancelUrl = MsgUtils.createPath(Constants.CHECKOUT_CANCEL_LINK, false);
		// NOTE: for Authorize.net, all possible return URLs must be listed
		// in the merchant account information!

		// an invoice is generated using the date and time
		Date myDate = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
		invoiceNumber = dateFormat.format(myDate);

		sequence = new Random().nextInt(1000); // generate random sequence number
		timeStamp = System.currentTimeMillis()/1000; // get timestamp
		String inputstring = Settings.getAuthLoginId() + "^" + sequence + "^" + timeStamp + "^" + amount + "^";

		try {
			//This section uses Java Cryptography functions to generate a fingerprint
			// First, the Transaction key is converted to a "SecretKey" object
			SecretKey key = new SecretKeySpec(Settings.getAuthTransactionKey().getBytes(), "HmacMD5");
			// A MAC object is created to generate the hash using the HmacMD5 algorithm
			Mac mac = Mac.getInstance("HmacMD5");
			mac.init(key);
			byte[] result = mac.doFinal(inputstring.getBytes());
			// Convert the result from byte[] to hexadecimal format
			fingerprint = StringUtils.toHexString(result);
			// end of fingerprint generation
		}
		catch (InvalidKeyException e) {
			EventUtils.logError(e);
		}
		catch (NoSuchAlgorithmException e) {
			EventUtils.logError(e);
		}
		catch (IllegalStateException e) {
			EventUtils.logError(e);
		}
	}

//	private CheckoutRedirect createAndPostCart(Production production) {
//			CheckoutShoppingCartBuilder builder = Constants.API_CONTEXT.cartPoster().makeCart();
//
//			Item item = new Item();
//			item.setMerchantItemId(production.getProdId());
//			item.setItemName(currentProduct.getTitle());
//			item.setItemDescription(currentProduct.getDescription());
//			item.setUnitPrice(Constants.API_CONTEXT.makeMoney(currentProduct.getPrice()));
//			item.setQuantity(1);
//			builder.addItem(item);
//
//			CheckoutShoppingCart cart = builder.build();
//
//			// Add our private data to the shopping cart
//			AnyMultiple privateData = new AnyMultiple();
//
//			privateData.getContent().add(currentProduct.getSku());
//			cart.getShoppingCart().setMerchantPrivateData(privateData);
//
//			CheckoutRedirect checkoutRedirect = Constants.API_CONTEXT.cartPoster().postCart(cart);
//
//			return checkoutRedirect;
//		}

	public String getLoginId() {
		return Settings.getAuthLoginId();
	}

	/** See {@link #productId}. */
	public Integer getProductId() {
		return productId;
	}
	/** See {@link #productId}. */
	public void setProductId(Integer productId) {
		this.productId = productId;
	}

	/** See {@link #production}. */
	public Production getProduction() {
		return production;
	}

	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #currentProduct}. */
	public Product getCurrentProduct() {
		return currentProduct;
	}
	/** See {@link #currentProduct}. */
	public void setCurrentProduct(Product currentProduct) {
		this.currentProduct = currentProduct;
	}

	/** See {@link #amount}. */
	public String getAmount() {
		return amount;
	}
	/** See {@link #amount}. */
	public void setAmount(String amount) {
		this.amount = amount;
	}

	/** See {@link #createProdName}. */
	public String getCreateProdName() {
		return createProdName;
	}
	/** See {@link #createProdName}. */
	public void setCreateProdName(String createProdName) {
		this.createProdName = createProdName;
	}

	/** See {@link #productList}. */
	public List<Product> getProductList() {
		return productList;
	}
	/** See {@link #productList}. */
	public void setProductList(List<Product> productList) {
		this.productList = productList;
	}

	/** See {@link #resellerCode}. */
	public String getResellerCode() {
		return resellerCode;
	}
	/** See {@link #resellerCode}. */
	public void setResellerCode(String resellerCode) {
		this.resellerCode = resellerCode;
	}

	/** See {@link #couponCode}. */
	public String getCouponCode() {
		return couponCode;
	}
	/** See {@link #couponCode}. */
	public void setCouponCode(String discountCode) {
		couponCode = discountCode;
	}

	/** See {@link #couponCodeOut}. */
	public String getCouponCodeOut() {
		return couponCodeOut;
	}
	/** See {@link #couponCodeOut}. */
	public void setCouponCodeOut(String couponCodeOut) {
		this.couponCodeOut = couponCodeOut;
	}

	/** See {@link #coupon}. */
	public Coupon getCoupon() {
		return coupon;
	}
	/** See {@link #coupon}. */
	public void setCoupon(Coupon coupon) {
		this.coupon = coupon;
	}

	/** See {@link #couponMsg}. */
	public String getCouponMsg() {
		return couponMsg;
	}
	/** See {@link #couponMsg}. */
	public void setCouponMsg(String couponMsg) {
		this.couponMsg = couponMsg;
	}

	/** See {@link #showCouponPrompt}. */
	public boolean getShowCouponPrompt() {
		return showCouponPrompt;
	}
	/** See {@link #showCouponPrompt}. */
	public void setShowCouponPrompt(boolean showCouponPrompt) {
		this.showCouponPrompt = showCouponPrompt;
	}

	/** See {@link #showCouponApplied}. */
	public boolean getShowCouponApplied() {
		return showCouponApplied;
	}
	/** See {@link #showCouponApplied}. */
	public void setShowCouponApplied(boolean showCouponApplied) {
		this.showCouponApplied = showCouponApplied;
	}

	/** See {@link #showContinue}. */
	public boolean getShowContinue() {
		return showContinue;
	}
	/** See {@link #showContinue}. */
	public void setShowContinue(boolean showContinue) {
		this.showContinue = showContinue;
	}

	/** See {@link #showCheckout}. */
	public boolean getShowCheckout() {
		return showCheckout;
	}
	/** See {@link #showCheckout}. */
	public void setShowCheckout(boolean showCheckout) {
		this.showCheckout = showCheckout;
	}

	/** See {@link #showUpgrade}. */
	public boolean getShowUpgrade() {
		return showUpgrade;
	}
	/** See {@link #showUpgrade}. */
	public void setShowUpgrade(boolean showUpgrade) {
		this.showUpgrade = showUpgrade;
	}

	/** See {@link #showResubscribe}. */
	public boolean getShowResubscribe() {
		return showResubscribe;
	}
	/** See {@link #showResubscribe}. */
	public void setShowResubscribe(boolean showResubscribe) {
		this.showResubscribe = showResubscribe;
	}

	/** See {@link #prepaidPage}. */
	public boolean getPrepaidPage() {
		return prepaidPage;
	}
	/** See {@link #prepaidPage}. */
	public void setPrepaidPage(boolean prepaidPage) {
		this.prepaidPage = prepaidPage;
	}

	/**
	 * This is called due to a 'rendered' clause in the prepaid.jsp file.
	 * It's a way of telling this bean that the prepaid page is the one
	 * being rendered, as opposed to the purchase confirmation page.
	 * @return false
	 */
	public boolean getPrepaid() {
		if (! prepaidPage) {
			prepaidPage = true;
			initialize();
		}
		return false;
	}

	/**See {@link #postUrl}. */
	public String getPostUrl() {
		return postUrl;
	}
	/**See {@link #postUrl}. */
	public void setPostUrl(String url) {
		postUrl = url;
	}

	/** See {@link #returnUrl}. */
	public String getReturnUrl() {
		return returnUrl;
	}
	/** See {@link #returnUrl}. */
	public void setReturnUrl(String returnUrl) {
		this.returnUrl = returnUrl;
	}

	/** See {@link #cancelUrl}. */
	public String getCancelUrl() {
		return cancelUrl;
	}
	/** See {@link #cancelUrl}. */
	public void setCancelUrl(String cancelUrl) {
		this.cancelUrl = cancelUrl;
	}

	/** See {@link #invoiceNumber}. */
	public String getInvoiceNumber() {
		return invoiceNumber;
	}
	/** See {@link #invoiceNumber}. */
	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	/** See {@link #sequence}. */
	public int getSequence() {
		return sequence;
	}
	/** See {@link #sequence}. */
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	/** See {@link #timeStamp}. */
	public long getTimeStamp() {
		return timeStamp;
	}
	/** See {@link #timeStamp}. */
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	/** See {@link #fingerprint}. */
	public String getFingerprint() {
		return fingerprint;
	}
	/** See {@link #fingerprint}. */
	public void setFingerprint(String fingerprint) {
		this.fingerprint = fingerprint;
	}

}

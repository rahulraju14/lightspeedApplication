//	File Name:	MessageTestBean.java
package com.lightspeedeps.web.test;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.BaseDAO;
import com.lightspeedeps.dao.MessageDAO;
import com.lightspeedeps.dao.ScriptDAO;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.message.HttpUtils;
import com.lightspeedeps.message.SmsUtils;
import com.lightspeedeps.model.*;
import com.lightspeedeps.test.script.UpdateScriptPageHash;
import com.lightspeedeps.type.NotificationMethod;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.script.ScriptFormatter;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * The backing class for some message testing functions and also some other
 * miscellaneous test functions. Most of these are accessed from the Prod Admin
 * / Other page, on the "Other tests" mini-tab.
 */
@ManagedBean
@ViewScoped
public class MessageTestBean implements Serializable {
	/** */
	private static final long serialVersionUID = 3046618678321215447L;

	private static final Log log = LogFactory.getLog(MessageTestBean.class);

	private String input;
	private String input2;
	private String output;
	private String output2;

	/** If true, then create new timecards regardless of the status of the prior week's
	 * timecard.  If false, new timecards are NOT created when the prior week's timecard
	 * exists but is empty (unused). */
	private boolean ignorePriorWeek;

	/** The week-ending date of the timecards to be created. */
	private Date timecardDate;

	public MessageTestBean() {
		log.debug("this="+this);
		input = "";
		output = "";
	}

	public String getInput() {
		return input;
	}
	public void setInput(String title) {
		input = title;
	}

	public String getInput2() {
		return input2;
	}
	public void setInput2(String input2) {
		this.input2 = input2;
	}

	public String getOutput() {
		return output;
	}
	public void setOutput(String output) {
		this.output = output;
	}

	public String getOutput2() {
		return output2;
	}
	public void setOutput2(String output2) {
		this.output2 = output2;
	}

	/**See {@link #ignorePriorWeek}. */
	public boolean getIgnorePriorWeek() {
		return ignorePriorWeek;
	}
	/**See {@link #ignorePriorWeek}. */
	public void setIgnorePriorWeek(boolean ignorePriorWeek) {
		this.ignorePriorWeek = ignorePriorWeek;
	}


	/**See {@link #timecardDate}. */
	public Date getTimecardDate() {
		return timecardDate;
	}
	/** This sets the timecardDate to the week-ending (Saturday) date of the same
	 * week as the date entered. See {@link #timecardDate}. */
	public void setTimecardDate(Date timecardDate) {
		if (timecardDate != null) {
			timecardDate = TimecardUtils.calculateWeekEndDate(timecardDate);
		}
		this.timecardDate = timecardDate;
	}


	/**
	 * Re-queue an existing Message object to be sent.
	 *
	 * @return null navigation String.
	 */
	public String actionResendMessage() {
		String text = getInput().trim();
		Integer msgId = 0;
		setOutput("no action");
		setOutput2("");
		try {
			msgId = Integer.valueOf(text);
		}
		catch (NumberFormatException e) {
			setOutput("input 1 must be Message id number");
		}
		try {
			if (msgId > 0) {
				DoNotification notify = DoNotification.getInstance();
//				for (msgId = 54402; msgId < 54463; msgId++) { // restore loop code if many messages to re-send
					Message msg = MessageDAO.getInstance().findById(msgId);
					if (msg != null && msg.getMethod() == NotificationMethod.EMAIL) {
						Set<Message> messages = new HashSet<>();
						messages.add(msg);
						notify.executeMessages(messages);
						setOutput("Message queued");
					}
					else {
						setOutput("message not found");
					}
//				}
			}
			else {
				setOutput("invalid Message id");
			}
		}
		catch (Exception e) {
			//EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
			log.error(e);
			setOutput("** ERROR **");
		}

		return null;
	}

	public String actionGetAuth() {
		Production prod = SessionUtils.getProduction();
//		Production prod = ProductionDAO.getInstance().findById(25);
		if (prod != null) {
			PayrollService service = prod.getPayrollPref().getPayrollService();
			if (service != null) {
				String response = HttpUtils.getAuthToken(service.getAuthUrl(), service.getLoginName(), service.getPassword());
				setOutput("response=" + response);
				setInput(response);
			}
		}
		return null;
	}

	public String actionSendTimecards() {
		Production prod = SessionUtils.getProduction();
		if (input.equals("ssn")) {
//			int updated = 0;
//			WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
//			List<TimecardEntry> list = weeklyTimecardDAO.findByProduction(prod);
//			for (TimecardEntry tce : list) {
//				WeeklyTimecard wtc = tce.getWeeklyTc();
//				if (wtc.getSocialSecurity() == null || wtc.getSocialSecurity().length()==0) {
//					wtc.setSocialSecurity("123123123");
//					weeklyTimecardDAO.attachDirty(wtc);
//					updated++;
//				}
//			}
//			setOutput("SSN updated on " + updated + " timecards.");
		}
		else {
			PayrollService service = prod.getPayrollPref().getPayrollService();
			String json = "{}";
			String response = HttpUtils.sendTimecards(service.getBatchUrl(), input, json);
			setOutput("response=" + response);
		}
		return null;
	}

	/**
	 * Test the SFTP send-file function.
	 * @return null navigation string
	 */
	public String actionSendFile() {
		String filename = "0954.pdf";
		String userName = "lightspeed";
		String password = "passwordGoesHere";
		File file = new File("D:\\Dev\\MyEclipseWorkspace\\.metadata\\.me_tcat\\webapps\\lightspeed29\\report\\954.pdf");
		String domain = "moveit.indiepayroll.com";
		String directory = "/home/lightspeed";

		boolean b = HttpUtils.sendFile(file, filename, domain, null, directory, userName, password);

		setOutput(""+b);
		return null;
	}

	/**
	 * Just a simple test method for testing SMS send, via a button on
	 * the test/misc page.
	 * @return null navigation string
	 */
	public String actionSend() {
		String phone = getInput();
		String text = getInput2();
		int rc = SmsUtils.sendMsg(text,phone);
		setOutput("return code=" + rc);
		return null;
	}

	/**
	 * Just a simple test method for testing mod-10 checksum validation, via a button on
	 * the test/misc page.
	 * @return null navigation string
	 */
	public String actionChecksum() {
		String text = getInput();
		text = text.trim();
		Long l = 0l;
		setOutput2("");
		try {
			l = Long.valueOf(text);
		}
		catch (NumberFormatException e) {
			setOutput("input 1 must be numeric");
		}
		if (l>0) {
			boolean ok = NumberUtils.validateChecksum(text);
			setOutput(""+ok);
			if (!ok) {
				int n = NumberUtils.calculateCheckDigit(text);
				setOutput2("check digit = "+n);
			}
		}
		return null;
	}

	/**
	 * Test message id encoding.
	 * @return null navigation string
	 */
	public String actionEncode() {
		String text = getInput();
		text = text.trim();
		int n = -1;
		try {
			n = Integer.valueOf(text);
		}
		catch (NumberFormatException e) {
			setOutput("input must be numeric");
		}
		if (n >= 0) {
			String res = NumberUtils.createToken(n);
			setOutput(res);
			setInput2(res);
		}
		return null;
	}

	/**
	 * Test message id encoding.
	 * @return null navigation string
	 */
	public String actionDecode() {
		String text = getInput2();
		text = text.trim();
		setOutput2(""+NumberUtils.parseToken(text));
		return null;
	}

	/**
	 * Update the Page hash total for the most recently loaded Script.
	 * @return null navigation string
	 */
	public String actionUpdatePageHash() {
		ScriptDAO scriptDAO = ScriptDAO.getInstance();
		String qry = "select max(id) from Script";
		Integer max = (Integer)((BaseDAO)scriptDAO).findOne(qry, null);
		log.debug(max);

		int totalDiff = UpdateScriptPageHash.updateScripts(scriptDAO, max);

		String msg = "page update done, " + totalDiff + " pages different";
		MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_WARN, msg );
		return null;
	}

	/**
	 * Run the re-formatting process against the Script whose Script.id value is
	 * specified in the first input field.
	 *
	 * @return null navigation string
	 */
	public String actionUpdateScriptPagination() {
		String text = getInput();
		int id;
		try {
			id = Integer.parseInt(text);
		}
		catch (NumberFormatException e) {
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO, "enter script id in 'input 1' field" );
			return null;
		}
		Script script = ScriptDAO.getInstance().findById(id);
		if (script == null) {
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO, "Script not found" );
			return null;
		}
		ScriptFormatter formatter = new ScriptFormatter();
		boolean b = (formatter.format(script) != null);
		MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO, "Result: " + b );
		return null;
	}

	/**
	 * Forces a NullPointerException and has the usual catch with EventUtils logging.
	 * @return null, except it won't get there!
	 */
	@SuppressWarnings("null")
	public String actionNullPointer() {
		try {
			MessageTestBean bean = this;
			if (! bean.equals(null)) {
				bean = null;
			}
			bean.actionNullPointer();
		}
		catch (Exception e) {
			EventUtils.logError(this, e);
			MsgUtils.addGenericErrorMessage();

		}
		return null;
	}

	/**
	 * Simulate session expiration, by navigating to the URL
	 * specified in web.xml for expiration redirect.
	 * @return null navigation string
	 */
	public String actionExpire() {
		HeaderViewBean.navigateToUrl("jsp/error/expired.jsp");
		return null;
	}

}

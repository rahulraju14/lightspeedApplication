//	File Name:	DoodViewBean.java
package com.lightspeedeps.web.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dood.ElementDood;
import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.dood.ProjectDood;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * A sample class for showing use of the Day-out-of-Days (DooD) objects.
 * This bean backs the dood_test.jsp page, and generates a 10-day view
 * of DooD status for all CHARACTER script elements in the current project,
 * starting with the project schedule's current start date.
 */
@ManagedBean
@ViewScoped
public class DoodViewBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 8026694131884449072L;

	//@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DoodViewBean.class);

	private List<DoodItem> doodItemList;
	private String defaultSort = "alpha";
	Project project;
	Unit unit;

	public DoodViewBean() {
		log.debug(""+this);
		initBean();
	}

	@SuppressWarnings("unused")
	private void exampleGettingDayTypeForTimeSheet() {

		ElementDood elementDood;
		ScriptElement scriptElement = null;

		Date date = null; // date should be set to whatever date matches the time sheet.

		// probably a loop here for all cast time cards
		for ( ; ; ) {
			scriptElement = new ScriptElement();
			// ... assume scriptElement gets set here to the Character of the cast member being listed
			elementDood = scriptElement.getElementDood(unit);
			// ** Here we get the dayType based on the date **
			WorkdayType type = elementDood.getStatus(date);

		}
	}

	/*
	 * Another example of using DooD information.
	 */
	private void initBean() {
		project = SessionUtils.getCurrentProject();
		ProductionDood.markProjectDirty(project); // make sure we get fresh DooD info
		unit = project.getMainUnit();
		Date projectStartDate = unit.getProjectSchedule().getStartDate();

		ScriptElementDAO seDAO = ScriptElementDAO.getInstance();
		ContactDAO contactDAO = ContactDAO.getInstance();
		Contact contact;

/**		// *********** TEST CODE to generate Report data ************
		// DooD report
		DoodReportGen gen = new DoodReportGen();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH:mm.ss");
		gen.generate("break-"+df.format(new Date()), unit, true);
		gen.generate("nobreak-"+df.format(new Date()), unit, false);

		// Stripboard report
		if (project.getScript()!=null && project.getStripboard()!=null) {
			StripboardReportGenTest.testGenerate();
		}
		// ** end report test code **
/**/
		// Get all the Script elements for this project
		List<ScriptElement> seList = seDAO.findByProject(project);

		doodItemList = new ArrayList<DoodItem>();
		DoodItem item;
		ElementDood eDood;
		for (ScriptElement se : seList) {
			eDood = se.getElementDood(unit);
			item = new DoodItem();
			item.setName(se.getName()); // ScriptElement name is Character name
			item.setCastid(se.getElementId());
			item.setCastIdStr(se.getElementIdStr());
			item.setEDood(eDood);
			if (se.getType()==ScriptElementType.CHARACTER) {
				contact = contactDAO.findContactFromCharacter(se);
				item.setContact(contact);
			}
			//int n = item.getStatus().length;
			doodItemList.add(item);
		}

		Date date;
		int daynum;
		Calendar cal = new GregorianCalendar();
		cal.setTime(projectStartDate);

		// Cycle through the calendar until we get enough shooting days
		// TODO how far to cycle?  how much is displayed at once?
		for (daynum = 0; daynum < 10; ) {
			date = cal.getTime();
			// get the status of each script element in the project
			for (DoodItem dItem : doodItemList) {
				eDood = dItem.getEDood();
				dItem.setStatus(daynum, eDood.getStatus(date).asWorkStatus()+(char)160);
				// (non-breaking space, '160', fixes missing borders on datatable display)
			}
			cal.add(Calendar.DAY_OF_MONTH, 1); // get next day
			daynum++;
		}

		// Now remove any unscheduled items
		Iterator<DoodItem> it = doodItemList.iterator();
		DoodItem dItem;
		while( it.hasNext() ) {
			dItem = it.next();
			if (dItem.getTotal() == 0) {
				it.remove();
			}
		}

		sort(defaultSort);
	}

	public void sortTable(ValueChangeEvent e) {
		String check = e.getNewValue().toString();
		sort(check);
	}

	protected void sort(String sortorder) {
		if (sortorder.equalsIgnoreCase("alpha")) {
			alphaSort();
		}
		else {
			castidSort();
		}
	}
	protected void alphaSort() {
		Collections.sort(doodItemList, alphaComparator);
	}

	protected void castidSort() {
		Collections.sort(doodItemList, castidComparator);
	}

	public List<DoodItem> getDoodItemList() {
		return doodItemList;
	}

	public void setDoodItemList(List<DoodItem> x) {
		doodItemList = x;
	}

	public String getDefaultSort() {
		return defaultSort;
	}

	public void setDefaultSort(String defaultSort) {
		this.defaultSort = defaultSort;
	}

	public class DoodItem implements Serializable {
		/** */
		private static final long serialVersionUID = 7993788747127135061L;

		private String name;
		private Integer castid;
		private String castIdStr;
		private ElementDood eDood;
		private Contact contact;
		String[] status = new String[60];

		public int getTotal() {
			return eDood.getWorkDays() + eDood.getTravelDays()
					+ eDood.getHoldDays() + eDood.getHolidayDays();
		}

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}

		public Integer getCastid() {
			return castid;
		}
		public void setCastid(Integer castid) {
			this.castid = castid;
		}

		/** See {@link #castIdStr}. */
		public String getCastIdStr() {
			return castIdStr;
		}
		/** See {@link #castIdStr}. */
		public void setCastIdStr(String castIdStr) {
			this.castIdStr = castIdStr;
		}

		public ElementDood getEDood() {
			return eDood;
		}
		public void setEDood(ElementDood dood) {
			eDood = dood;
		}

		public Contact getContact() {
			return contact;
		}
		public void setContact(Contact contact) {
			this.contact = contact;
		}

		public String[] getStatus() {
			return status;
		}
		public void setStatus(String[] status) {
			this.status = status;
		}
		public void setStatus(int i, String status) {
			this.status[i] = status;
		}

	}

	static final Comparator<DoodItem> alphaComparator = new Comparator<DoodItem>() {
		@Override
		public int compare(DoodItem e1, DoodItem e2) {
			int ret = 0;
			if (e1.getContact() != null) {
				ret = e1.getContact().compareTo(e2.getContact());
			}
			if (ret == 0) {
				ret = e1.getName().compareTo(e2.getName());
			}
			//log.debug("Value of alpha compare >> "+ret);
			return ret;
		}
	};

	static final Comparator<DoodItem> castidComparator = new Comparator<DoodItem>() {
		@Override
		public int compare(DoodItem e1, DoodItem e2) {
			int x = 1;
			if ((e1.getCastid() != null) && (e2.getCastid() != null))
				x = e1.getCastid().compareTo(e2.getCastid());
				//log.debug("Value of castid compare >> "+x);
			return x;
		}
	};

	public Map<Integer, ProjectDood> getProdDoodMap() {
		return ProductionDood.getProductionDoodMap();
	}

	public int getProdDoodSize() {
		return getProdDoodMap().size();
	}

	public int getProdDoodElems() {
		int count = 0;
		Set<Integer> set = getProdDoodMap().keySet();
		log.debug("prod dood size (units)=" + set.size());
		for (Integer uid : set) {
			ProjectDood pd = getProdDoodMap().get(uid);
			Map<Integer, ElementDood> seMap = pd.getSeMap();
			log.debug("unit#" + uid + ",  SE count=" + seMap.size());
			count += seMap.size();
			Map<Integer, ElementDood> rwMap = pd.getRwMap();
			log.debug("unit#" + uid + ", RWE count=" + rwMap.size());
			count += rwMap.size();
//			Set<Integer> seset = seMap.keySet();
//			for (Integer seid : seset) {
//				ElementDood eld = seMap.get(seid);
//				log.debug(eld.toString());
//			}
		}
		log.debug("total elementDood's=" + count);
		return count;
	}

	public int getProdDoodStreamsize() {
		int count = 0;
		OutputStream byteStream = new ByteArrayOutputStream();
		try {
			ObjectOutputStream out;
			out = new ObjectOutputStream(byteStream);
			out.writeObject(getProdDoodMap());
			out.close();
			String s = byteStream.toString();
			count = s.length();
		}
		catch (IOException e) {
			log.error("exception: ", e);
		}
		return count;
	}

}

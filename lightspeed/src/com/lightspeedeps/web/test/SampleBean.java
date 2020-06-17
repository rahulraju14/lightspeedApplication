package com.lightspeedeps.web.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIData;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.fileentry.FileEntryEvent;
import org.icefaces.ace.event.SelectEvent;

/**
 * A bean typically used for testing coding issues and creating sample code to
 * post on user forums such as icesoft.org or myeclipseide.com.
 */
@ManagedBean
@ViewScoped
public class SampleBean implements Serializable {
	//@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SampleBean.class);
	private static final long serialVersionUID = 1L;

	private int number = 5;
	private String text = "hello";
	private Week week = new Week();
	private List<Item> list = new ArrayList<>();
	private List<String> textList = new ArrayList<>();
	private List<Integer> itemList1 = new ArrayList<>();
	private List<Integer> itemList2 = new ArrayList<>();

	private boolean z = true;
	private boolean bool = true;

	public SampleBean() {
		log.debug("");
		for (int i=0; i < 1; i++) {
			list.add(new Item(i));
		}
		week.page.list = list;
		for (int i=0; i < 5; i++) {
			textList.add("some <b>bold</b> " + i);
		}
		for (int i=1; i <= 50; i++) {
			itemList1.add(new Integer(i));
			itemList2.add(new Integer(i+1000));
		}
	}

	public void onUndo(ActionEvent event) {
		log.debug("..............action Undo");
		z = true;
		text = text + ",undo";
	}

	public boolean getZ() {
		log.debug("...............get: " + z);
		return z;
	}
	public void setZ(boolean z) {
		log.debug("...............SET: " + z);
		this.z = z;
	}

	/**See {@link #bool}. */
	public boolean getBool() {
		return bool;
	}
	/**See {@link #bool}. */
	public void setBool(boolean bool) {
		this.bool = bool;
	}

	public String actionNext() {
//		week.page.list.add(new Item(1));
		week = new Week();
		week.page.list.add(new Item(2));
		return null;
	}

	public String actionPrev() {
		week = new Week();
//		if (week.page.list.size() > 0) {
//			week.page.list.remove(0);
//		}
		return null;
	}

	public String actionSomething() {
		return null;
	}

	public void rowSelected(SelectEvent evt) {
		UIData ud = (UIData)evt.getComponent();
		Integer row = (Integer)evt.getComponent().getAttributes().get("currentId");
		log.debug("row=" + ud.getRowIndex() + ", id=" + row);
		list.get(row).selected = true;
	}

	public void listenDrop(SelectEvent evt) {
//		log.debug(evt.getIndex());
//		log.debug(evt.getOldIndex());
//		log.debug(evt.getComponent());
//		log.debug(evt.getType());
//		itemList1.set(0,itemList1.get(0) - 1);
//		itemList2.set(0,itemList2.get(0) - 2);
//		setItemList1(itemList1);
	}

	public void listenUpload(FileEntryEvent evt) {
	}

	public void changeListener(ValueChangeEvent event) {
	}

	public List<Item> getList() {
		return list;
	}
	public void setList(List<Item> list) {
		this.list = list;
	}
	public List<String> getTextList() {
		return textList;
	}
	public void setTextList(List<String> textList) {
		this.textList = textList;
	}

	public List<Integer> getItemList1() {
		return itemList1;
	}
	public void setItemList1(List<Integer> itemList) {
		itemList1 = itemList;
	}

	public List<Integer> getItemList2() {
		return itemList2;
	}
	public void setItemList2(List<Integer> itemList2) {
		this.itemList2 = itemList2;
	}

	public Integer getNumber() {
		return number;
	}
	public void setNumber(Integer number) {
		this.number = number;
	}

	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

	public Week getWeek() {
		return week;
	}
	public void setWeek(Week week) {
		this.week = week;
	}

	public class Week implements Serializable {
		private static final long serialVersionUID = 1L;
		public Page page = new Page();
		public Page getPage() {
			return page;
		}
		public void setPage(Page page) {
			this.page = page;
		}
	}

	public class Page implements Serializable {
		private static final long serialVersionUID = 1L;
		public List<Item> list = new ArrayList<>();
		public List<Item> getList() {
			return list;
		}
		public void setList(List<Item> list) {
			this.list = list;
		}
	}

	public class Item implements Serializable {
		private static final long serialVersionUID = 1L;

		String name;
		int number;
		boolean selected = false;
		public Item(int n) {
			name = "row " + n;
			number = n;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getNumber() {
			return number;
		}
		public void setNumber(int number) {
			this.number = number;
		}
		public boolean getSelected() {
			return selected;
		}
		public void setSelected(boolean selected) {
			this.selected = selected;
		}
	}

}

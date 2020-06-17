//	File Name:	FakeJavaScript.java
package com.lightspeedeps.test.util;

import junit.framework.TestCase;

public class FakeJavaScript extends TestCase {

	static class document {
		public static document getElementById(String x) {
			return new document();
		}
		public static document getElementById(var x) {
			return new document();
		}
		public String value;
		public String className;
		public boolean checked;
	}

	class var {
	}

	@SuppressWarnings("unused")
	private String eval(int x) {
		return "";
	}

/*	private boolean selectRow(int id) {
		// alert(id);
		var idPre = "j_id15:j_id227:";
		var idPost = ":j_id229";
		document.getElementById("j_id15:" + "stripStatus").value = "";
		// alert(document.getElementById("j_id15:"+"radioValue").value);
		// alert(document.getElementById("j_id15:j_id145:_1").checked);
		// if else condition to switch the row select between the different
		// tables
		if ((document.getElementById("j_id15:" + "scheduleRowSelectId").value != "first")) {
			if (document.getElementById("j_id15:j_id174:_1").checked) {// unschedule
				// select unselect the border of left to right table when
				// clicking on to the strips

				var rightIndex = document.getElementById("j_id15:" + "scheduleRightRowSelectId").value;
				// alert("rightIndex"+rightIndex);
				if (document.getElementById(rightIndex) != null) {// if record
					// is just
					// ommitted
					// so not
					// present
					// or just
					// filtered
					document.getElementById(rightIndex).className = "colorBorderBlack"; // black
					// the
					// right
					// table
				}
				document.getElementById("j_id15:" + "scheduleRightRowSelectId").value = "first"; // right
				// unschedule
				// tablle

			}
			else if (document.getElementById("j_id15:" + "selectRightScheduled") != null) {
				// select unselect the border of right scheduled table when
				// clicking on to the strips

				var rightIndex = document.getElementById("j_id15:" + "selectRightScheduled").value;
				// alert("rightIndex"+rightIndex);
				if (rightIndex != "first") {
					if (document.getElementById(rightIndex) != null) {// if
						// record
						// is
						// just
						// ommitted
						// so
						// not
						// present
						document.getElementById(rightIndex).className = "colorBorderBlack"; // black
						// the
						// right
						// table
					}
					document.getElementById("j_id15:" + "selectRightScheduled").value = "first"; // right
					// unschedule
					// tablle
				}
			}
		}

		if (document.getElementById("j_id15:" + "scheduleRowSelectId").value == "first") {
			var rightIndex = document.getElementById("j_id15:" + "selectRightScheduled");
			// alert(document.getElementById("j_id15:"+"selectRightScheduled").value);
			if (rightIndex != null) {
				if (document.getElementById("j_id15:" + "selectRightScheduled").value != "first") {
					if (!document.getElementById("j_id15:j_id174:_1").checked) {
						var rightIndex = document.getElementById("j_id15:" + "selectRightScheduled");
						// alert(rightIndex);
						// document.getElementById(rightIndex).className="colorBorderBlack";
						document.getElementById("j_id15:" + "selectRightScheduled").className = "colorBorderBlack";
					}
				}
			}
			// alert(rightIndex);
			document.getElementById("j_id15:" + "scheduleRowSelectId").value = idPre + eval(id - 1) + idPost;
			document.getElementById(idPre + eval(id - 1) + idPost).className = "colorBorder";
			return true;

		}
		else {
			var oldElem = document.getElementById("j_id15:" + "scheduleRowSelectId").value;
			if (document.getElementById(oldElem) != null) {
				document.getElementById(oldElem).className = "colorBorderBlack";
				document.getElementById("j_id15:" + "scheduleRowSelectId").value = idPre + eval(id - 1) + idPost;
				document.getElementById(idPre + eval(id - 1) + idPost).className = "colorBorder";
			}
			else {
				document.getElementById("j_id15:" + "scheduleRowSelectId").value = idPre + eval(id - 1) + idPost;
				document.getElementById(idPre + eval(id - 1) + idPost).className = "colorBorder";
			}
			return true;

		}
	}
*/}

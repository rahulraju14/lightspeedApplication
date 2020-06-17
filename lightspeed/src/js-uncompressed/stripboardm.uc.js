/* to clear the error message layer pop by manually reloading page onclick of the error layer "x" */
function reloadPage() {
  window.location.reload();
}

/* Functions for stripboard editor */

// 3/21/10 document id's used; "n x"=number of occurences; DH has additional info in "design_notes".
// "se"   	main form id
// "schRadio:_1" 6x	selectLeftRow(): "unscheduled" radio button
// "sl"			8x	idPre -- prefix for id's in scheduled list 
// "lr"			3x	omitRecord(): idPostHiddn; editBanner(): idPostHiddn; selectLeftRow() 
// "sno"		3x	idPostScreen -- suffix for id
// "eodText"	6x	editBanner(): idPost, idEndOfDay
// "ru"			3x	omitRecord(): idPostR; selectRightRow(): idPost
// "rs"    		1x  selectRightScheduled()

//var unschedCB = 'se:schRadio:_1';
var idPre='se:sl:'; 	// prefix for scheduled list ids
//var idPostScreen=':sno'; // suffix for scheduled list ids - scene number field
var idPostL=':lr';	// suffix for selecting scheduled (left) list row

var idPreR='se:us:'; 	// prefix for right-side unscheduled list ids
var idPostR=':ru';		// suffix for selecting right-side unscheduled list row

var idPreRs='se:ss:'; // prefix for right-side scheduled list ids
var idPostRs=':rs'; 	// suffix for selecting right-side scheduled list row

var SELECT_CLASS = ' st_sel'; // style class to show selected item; leading blank to separate from other classes
//var NORMAL_CLASS = 'g2'; // style class to show unselected item

var mouseX = 0;  // The x-coordinate of the last mouse-down on a strip
var mouseY = 0;  // The y-coordinate of the last mouse-down on a strip

var UNSELECTED = 'none';

/** Remember the x/y coordinates of the last mouse-down operation on a strip. */
function md(evt) {
	mouseX = evt.clientX;
	mouseY = evt.clientY;	
}

/** 
 * Called when Omit button is clicked. 
 * Returns true if no row is selected, and issues an alert.
 * Returns false otherwise.
 */
function omitRecord() {

	var msg = 'Please select a Strip to omit.';

	var parts = document.getElementById('se:selectLeftScheduled').value.split(":");
	var rowId = parts[2];
	parts = document.getElementById('se:selectRightUnscheduled').value.split(":");
	var rowIdR = parts[2];
	parts = document.getElementById('se:selectRightScheduled').value.split(":");
	var rowIdRs = parts[2];

	//alert("id=" + document.getElementById('se:selectLeftScheduled').value + ", rowId="+rowId + ", rowIdR="+rowIdR);
	var elemId = idPre + rowId + idPostL;
	var elemIdR = idPreR + rowIdR + idPostR;
	var elemIdRs = idPreRs + rowIdRs + idPostRs;
	//alert("left id=" + elemId + ", left obj=" + document.getElementById(elemId) + ", rt id=" + elemIdR + ", rt obj=" + document.getElementById(elemIdR)
	//		+ ", rts id=" + elemIdRs + ", rts obj=" + document.getElementById(elemIdRs));
	if (document.getElementById(elemId)==null ) {
		if (document.getElementById(elemIdR)==null ) {
			if (document.getElementById(elemIdRs)==null || 
					document.getElementById(elemIdRs).innerText=="first") {
				alert(msg);
				return true;
			}
		}
		else if (document.getElementById(elemIdR).innerText=="first") {
			alert(msg);
			return true;
		}
	}
	else if (document.getElementById(elemId).innerText=="first") {
		alert(msg);
		return true;
	}
	return false;
}

function restoreOmit() {
	//	we currently don't know (in js) if selected right row is Omitted or Unscheduled status
	if (document.getElementById('se:stripStatus').value=="R") {
		// normal situation if something on right is selected & right-hand is unscheduled
		return false;  // let post to server proceed
	}
	if(document.getElementById('se:stripStatus').value==""){ 
		// when the right list row item is selected to be Restored, and the Restore btn is clicked. 
		alert('Please select an Omitted Strip from the bottom of the Unscheduled Strips.');
		return true;
	}
	if(document.getElementById('se:selectRightUnscheduled').value=="first"){ 
		// may be true when nothing is selected on the right side 
		alert('Please select an Omitted Strip from the bottom of the Unscheduled Strips.');
		return true;
	}
	if(document.getElementById('se:stripStatus').value=="UNSCHEDULED") {  
		// This never seems to happen???
		alert('The selected Strip is already unscheduled.');
		return true;
	}
	// not sure when this happens, if ever...
	return false;	// let server figure it out.
}

// Returns FALSE if banner is selected to be edited.
// Returns TRUE if nothing selected, or selected row is not a Banner.
function editBanner() {
	var elemId = document.getElementById('se:selectLeftScheduled').value;
	var parts = elemId.split(":");
	//alert('parts: '+parts[0]+'/'+parts[1]+'/'+parts[2]+'/'+parts[3]);
	elemId = parts[0] + ':' + parts[1] + ':' + parts[2] + ':bnr';
	//alert(elemId);
	elem = document.getElementById(elemId);
	if (elem != null) {
		return false; // Looks like a banner ... post to server
	}
	alert("Please select a banner to edit.");
	return true;
}

/**
 * Called when a left-hand row is clicked. Save the row index and shift/ctrl key
 * status, and notify the server via a simulated button click.
 * 
 * @param id  the row number, which is generated from listStatus.index in our
 *            loop.
 * @param evt the mouse event.
 * @return
 */
function sl(id,evt) {
	// alert('select id=' + id);
	var elemId = idPre + eval(id) + idPostL;
	
	unselectAll();
	document.getElementById('se:stripStatus').value="";
	document.getElementById('se:selectedRow').value=id;
	document.getElementById('se:selectLeftScheduled').value=elemId;
	document.getElementById(elemId).className += SELECT_CLASS;
	clickStrip('se:leftButton',evt);

	return true;
}

/**
 * Called when a right-hand row is clicked and the right-hand 
 * list is Unscheduled strips.  Save the row status for the
 * Omit and Restore functions to check.
 */
function sr(id,evt) { // selectRightUnscheduled
	//if (document.getElementById('se:selectedRow').value != null) {
	//	alert(document.getElementById('se:selectedRow').value=id);
	//}
	//alert('select Right id='+id);
	var elemId = idPreR + eval(id) + idPostR;
	
	//alert(elemId);
	//alert(listType);
	//alert(document.getElementById('se:stripStatus').value);
	unselectAll();
	document.getElementById('se:stripStatus').value="R"; // Right-side row?
	document.getElementById('se:selectedRow').value=id;
	document.getElementById('se:selectRightUnscheduled').value=elemId;
	document.getElementById(elemId).className += SELECT_CLASS;
	clickStrip('se:ruButton',evt);

	return true;
}

/**
 * Called when a right-hand row is clicked and the right-hand 
 * list is Scheduled strips.
 */
function ss(id,evt) {  // TODO: test drag from sch to sch
	//if (document.getElementById('se:selectedRow').value=id != null) {
	//	alert(document.getElementById('se:selectedRow').value=id);
	//}
	//alert('select right sched id='+id);
	var elemId = idPreRs + eval(id) + idPostRs;
	
	//alert(elemId);
	//alert(document.getElementById('se:stripStatus').value);
	unselectAll();
	document.getElementById('se:stripStatus').value = "";
	document.getElementById('se:selectedRow').value=id;
	document.getElementById('se:selectRightScheduled').value=elemId;
	document.getElementById(elemId).className += SELECT_CLASS;
	clickStrip('se:rsButton',evt);
	
	return true;
}

function clickStrip(id,evt) {
	// skip the button click if the user just finished dragging the strip
	if (4 > Math.abs(evt.clientX - mouseX)) {
		if (4 > Math.abs(evt.clientY - mouseY)) {
			var shiftPressed= (evt.shiftKey ? '1' : '0');
			var ctrlPressed = (evt.ctrlKey ? '1' : '0');
			document.getElementById('se:shiftCtrl').value = shiftPressed + ctrlPressed;
			var btn = document.getElementById(id);
			if (btn != null) {
				btn.click();
			}
			else {
				alert("btn=" + btn);
			}
		}
		else {
			//alert("drag y");
		}
	}
	else {
		//alert("drag x");
	}
}

/**
 * Called when a user changes the value of a filter on the unscheduled
 * list.
 * NOTE: currently called on "mouseup".  We can't use "onchange=" because
 * that nullifies the valueChangeListener setting!
 */
function filterChange() {
	//unselectRight(); 
	document.getElementById('se:selectRightUnscheduled').value= UNSELECTED;
	//if (!leftSelected()) {
	//	setButtonStatus(false); // why would a button status matter here?
		//alert('setButtonStatus-false : filterChange()');
		// TODO :handle ButtonStatus better
	//}
}

// looks like this just gets set to false to set the state of a button?
// TODO :This needs investigation, handle ButtonStatus better
function leftSelected() {
	// should return true if any left-side row is selected.
	return false;
}


/** Select the "elemId" element, and save its id in the "saveId" location. */
function selectId(saveId, elemId) {
	//alert('selectId');
	document.getElementById(saveId).value=elemId;
	document.getElementById(elemId).className += SELECT_CLASS;
	//setButtonStatus(true); // JM what button?  This has to capture the intended event, not just randomly
	//alert('setWhatButton-True : 215;') yep this does nothing but interfere, handle btn Status more gracefully
}

// moved unselectId into the unselectAll()
/**
 * Given an element id, if it is not the literal "first", check that it is 
 * not null, and if so, set the class so that it does NOT appear selected.
 */
//function unselectId(elemId) {
//	if( elemId != UNSELECTED ) {
//		//alert("id=" + elemId );
//		//alert("elem=" + document.getElementById(elemId));
//		if (document.getElementById(elemId) != null) {
//			document.getElementById(elemId).className = NORMAL_CLASS;
//		}
//	}
//}

// moved the unselectIndirectId(savedId) function to unselectAll()
/**
 * Given the element id of a "save location" -- which will contain either "first" 
 * or the element id of interest -- look up that value and un-select the element
 * whose id is stored in the "save location".
 * Finally, set the save location value to "first" to indicate that there is no element
 * selected which is related to that "save location".
 */
//function unselectIndirectId(savedId) {  
//	unselectId(document.getElementById(savedId).value);
//	document.getElementById(savedId).value= UNSELECTED;
//}


/** unselectAll() to avoid calling excessive scripts that collide in IE */
function unselectAll() {  
	//alert('unselectAll');
	if (document.getElementById(document.getElementById('se:selectLeftScheduled').value) != null) {
//		document.getElementById(document.getElementById('se:selectLeftScheduled').value).className = NORMAL_CLASS;
		document.getElementById('se:selectLeftScheduled').value= UNSELECTED;
	}
	else if (document.getElementById(document.getElementById('se:selectRightUnscheduled').value) != null) {
//		document.getElementById(document.getElementById('se:selectRightUnscheduled').value).className = NORMAL_CLASS;
		document.getElementById('se:selectRightUnscheduled').value= UNSELECTED;
	}
	else if (document.getElementById(document.getElementById('se:selectRightScheduled').value) != null) {
//		document.getElementById(document.getElementById('se:selectRightScheduled').value).className = NORMAL_CLASS;
		document.getElementById('se:selectRightScheduled').value= UNSELECTED;
	}
}

// removing this function
///** Un-select any selected row in the left-hand (scheduled) list. */
//function unselectLeft() {
	// alert('unselectLeft');
	//unselectIndirectId('se:selectLeftScheduled');
	// unfortunately when you click on a row, the mouse up triggers this which
	// is weird because it should not unselect, when clicked?  There must be
	// another event listener triggering this... a
	// Actually, the other functions are unselecting all 
	// over the place, these should be handled by one func() and it should only unselect 
	// based on the parm info gathered onclick()
//}

// removing this function
///** Un-select any selected rows in the right-hand list, whether
// * scheduled or unscheduled.
// */
//function unselectRight() {
//	//unselectIndirectId('se:selectRightUnscheduled');  // we should detect what is selected and not just call both
//	//unselectIndirectId('se:selectRightScheduled');
//}



function setButtonStatus(selected) {
	if (selected) {
		// TODO check for not omitted:
//		enableButton('se:omitBtn', true);
		// TODO check for omitted:
//		enableButton('se:restoreBtn', true);
		// TODO check for banner:
//		enableButton('se:editBannerBtn', true);
	}
	else {
		enableButton('se:omitBtn', false);
		enableButton('se:restoreBtn', false);
		enableButton('se:editBannerBtn', false);
	}
}

function enableButton(elemId, enable) {
	if (enable) {
		// TODO figure out what elem Type & Status and pick the right btn to enable, 
		// not working right as far as I can tell so disabled for now
		//document.getElementById(elemId).disabled = false;
		//document.getElementById(elemId).className="btn";
	}
	else {
		//document.getElementById(elemId).disabled = true;
		//document.getElementById(elemId).className="btn-dis";
	}
}


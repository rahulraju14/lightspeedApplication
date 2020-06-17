/* LightSPEED JavaScript functions for desktop (not used in mobile pages).  */

// Specify Session Timeout URL relative to desktop URLs
var sess_logout_uri = '../error/timedOut.jsp';

// Specify error handling page reference relative to desktop URLs
var ice_error_url = "../error/500a.html";

// Specify if checkButton() needs to call checkShowErrors(). True for desktop, false for mobile.
var checkShowErrorsOn = true;

/************************************************************************************************************
	Editable select
	Copyright (C) September 2005  DHTMLGoodies.com, Alf Magne Kalleland
	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.
	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.
	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
	Dhtmlgoodies.com., hereby disclaims all copyright interest in this script
	written by Alf Magne Kalleland.
	Alf Magne Kalleland, 2006
	Owner of DHTMLgoodies.com
	************************************************************************************************************/
	// Path to arrow images
	var arrowImage = '../../i/select_arrow.gif';	// Regular arrow
	var arrowImageOver = '../../i/select_arrow_over.gif';	// Mouse over
	var arrowImageDown = '../../i/select_arrow_down.gif';	// Mouse down

	var selectBoxIds = 0;
	var currentlyOpenedOptionBox = false;
	var editableSelect_activeArrow = false;

	function selectBox_switchImageUrl()
	{
		if(this.src.indexOf(arrowImage)>=0){
			this.src = this.src.replace(arrowImage,arrowImageOver);
		}else{
			this.src = this.src.replace(arrowImageOver,arrowImage);
		}
	}

	function selectBox_showOptions()
	{
		if(editableSelect_activeArrow && editableSelect_activeArrow!=this){
			editableSelect_activeArrow.src = arrowImage;
		}
		editableSelect_activeArrow = this;

		var numId = this.id.replace(/[^\d]/g,'');
		var optionDiv = document.getElementById('selectBoxOptions' + numId);
		if(optionDiv.style.display=='block'){
			optionDiv.style.display='none';
			if(navigator.userAgent.indexOf('MSIE')>=0)document.getElementById('selectBoxIframe' + numId).style.display='none';
			this.src = arrowImageOver;
		}
		else{
			optionDiv.style.display='block';
			if(navigator.userAgent.indexOf('MSIE')>=0)document.getElementById('selectBoxIframe' + numId).style.display='block';
			this.src = arrowImageDown;
			if(currentlyOpenedOptionBox && currentlyOpenedOptionBox!=optionDiv)currentlyOpenedOptionBox.style.display='none';
			currentlyOpenedOptionBox= optionDiv;
		}
	}

	function selectOptionValue()
	{
		var parentNode = this.parentNode.parentNode;
		var textInput = parentNode.getElementsByTagName('INPUT')[0];
		textInput.value = this.innerHTML;
		this.parentNode.style.display='none';
		document.getElementById('arrowSelectBox' + parentNode.id.replace(/[^\d]/g,'')).src = arrowImageOver;

		if(navigator.userAgent.indexOf('MSIE')>=0)document.getElementById('selectBoxIframe' + parentNode.id.replace(/[^\d]/g,'')).style.display='none';

	}
	var activeOption;
	function highlightSelectBoxOption()
	{
		if(this.style.backgroundColor=='#316AC5'){
			this.style.backgroundColor='';
			this.style.color='';
		}else{
			this.style.backgroundColor='#316AC5';
			this.style.color='#FFF';
		}

		if(activeOption){
			activeOption.style.backgroundColor='';
			activeOption.style.color='';
		}
		activeOption = this;
	}

	function createEditableSelect(dest)
	{
		dest.className='selectBoxInput';
		var div = document.createElement('DIV');
		div.style.styleFloat = 'left';
		div.style.width = dest.offsetWidth + 16 + 'px';
		div.style.position = 'relative';
		div.id = 'selectBox' + selectBoxIds;
		var parent = dest.parentNode;
		parent.insertBefore(div,dest);
		div.appendChild(dest);
		div.className='selectBox';
		div.style.zIndex = 10000 - selectBoxIds;

		var img = document.createElement('IMG');
		img.src = arrowImage;
		img.className = 'selectBoxArrow';

		img.onmouseover = selectBox_switchImageUrl;
		img.onmouseout = selectBox_switchImageUrl;
		img.onclick = selectBox_showOptions;
		img.id = 'arrowSelectBox' + selectBoxIds;

		div.appendChild(img);

		var optionDiv = document.createElement('DIV');
		optionDiv.id = 'selectBoxOptions' + selectBoxIds;
		optionDiv.className='selectBoxOptionContainer';
		optionDiv.style.width = div.offsetWidth-2 + 'px';
		div.appendChild(optionDiv);

		if(navigator.userAgent.indexOf('MSIE')>=0){
			var iframe = document.createElement('<IFRAME src="about:blank" frameborder=0>');
			iframe.style.width = optionDiv.style.width;
			iframe.style.height = optionDiv.offsetHeight + 'px';
			iframe.style.display='none';
			iframe.id = 'selectBoxIframe' + selectBoxIds;
			div.appendChild(iframe);
		}

		if(dest.getAttribute('selectBoxOptions')){
			var options = dest.getAttribute('selectBoxOptions').split(';');
			var optionsTotalHeight = 0;
			var optionArray = new Array();
			for(var no=0;no<options.length;no++){
				var anOption = document.createElement('DIV');
				anOption.innerHTML = options[no];
				anOption.className='selectBoxAnOption';
				anOption.onclick = selectOptionValue;
				anOption.style.width = optionDiv.style.width.replace('px','') - 2 + 'px';
				anOption.onmouseover = highlightSelectBoxOption;
				optionDiv.appendChild(anOption);
				optionsTotalHeight = optionsTotalHeight + anOption.offsetHeight;
				optionArray.push(anOption);
			}
			if(optionsTotalHeight > optionDiv.offsetHeight){
				for(var no=0;no<optionArray.length;no++){
					optionArray[no].style.width = optionDiv.style.width.replace('px','') - 22 + 'px';
				}
			}
			optionDiv.style.display='none';
			optionDiv.style.visibility='visible';
		}
		selectBoxIds = selectBoxIds + 1;
	}

/** If IE7 or other browser without "console.log", provide dummy functions */
if ( ! window.console ) {

    (function() {
      var names = ["log", "debug", "info", "warn", "error",
          "assert", "dir", "dirxml", "group", "groupEnd", "time",
          "timeEnd", "count", "trace", "profile", "profileEnd"],
          i, l = names.length;

      window.console = {};

      for ( i = 0; i < l; i++ ) {
        window.console[ names[i] ] = function() {};
      }
    }());
}

var popupMsg = 'Your browser is currently set to block popups from our site. Please change your browser settings to allow popups and then click the link or button again.';

/**
 * Do all our standard page-initialization functions. If a valid object ID is
 * passed, it is assumed to be the "mail list" whose scrolling is to be monitored.
 */
function initDomLoaded(listId) {
	resize();	// Resize our div/tables etc to match window
	sessionInitTimer(); // Set up session timeout
	if (listId != null) { // "main" list specified?
		setMainScrollOn(listId); // set the 'onscroll' function
	}
	// Use ICE/jquery function to set observer on browser resize.
	ice.ace.jq( window ).resize(
		function() {
			resize();
		}
	);
}

/** to clear the error message layer pop by manually reloading page onclick */
function reloadPage() {
  window.location.reload();
}

/** empty function used here globally to allow an override for special/specific onclick functions in addition to actions.
 * It's empty so that a shared popup can use the onclick override().  This way if the parent page doesn't need the override,
 * the onclick call won't throw and error.
 * Just place the actual override() function in the parent page with the actual script you need to run */
function override() {
	return false;
}

//var formname = "elem"; // this needs to be set on each page; used by 'overTpNav' and 'findActTpNav'
var actTpIdx = -1; // index to compare hovered vs. active (of top menu tabs)
var hvrTpIdx = -2; // index to compare hovered vs. active (of top menu tabs)
var actTpNav = null; // element of the top navigation tab that is "Active"
var actSbNav = null; // element of the sub nav (sub-tab) AREA that is "Active"
var hvrTpTog = null; // flag for when a different top nav link is hovered, so we can "Toggle" the "Newly Hovered" nav links etc
var hvrSbTog = null; // flag for when a different top nav link is hovered, so we can "Toggle" the "Newly Hovered" sub nav links etc
// ID's in header nav = activeTopNav, activeSubNav, topNav, subNav

/** called when mouse hovers over any "inactive" header nav link
 * ix = index of tab, 0...7
 */
function checkTpNav(ix) {

	hvrTpNavId = formname + ":tnloop:" + ix + ":topNav"; // the hovered top nav link
	hvrTpNav = document.getElementById(hvrTpNavId);
	//console.log('hover id=' + hvrTpNavId + ', hover item=' + hvrTpNav);

	hvrSbNavId =  (formname + ":tnloop:" + ix + ":subNav"); // the sub nav links related to the hovered top nav
	hvrSbNav = document.getElementById(hvrSbNavId);
	//console.log('hover id=' + hvrSbNavId + ', hover item=' + hvrSbNav);

	if (actTpNav == null) {
		//console.log("Active Top Nav not set, either a fresh page load or user refreshed browser");
		findActTpNav();
	}
	if (timer_on != 0) {
		//timer is running so switch nav
		switchTpNav(ix);
	} else {
		// timer is not running, just show hovered nav
		showTpNav(ix);
	}
}

/** called if the mouse hover determined by checkTpNav() detects that there is a timer active
 */
function switchTpNav(ita) {

	hvrTpIdx = ita;  // set index each loop

	//if (timer_on != 0) {	// psuedo timer is running, stop timer

		stophvrtimer();

		if ((hvrTpTog != null) && (hvrTpTog != hvrTpNav)) { // if toggled and hovered nav are not null, hovered nav is not the active nav
			// another nav was hovered, change hovered top nav text the blue color, and toggled text link dark grey
			// we check if they hover over the same hovered nav so can
			// change the hovered top nav text blue or grey accordingly
			// set toggle
			// start timer
			hvrTpTog.style.color = "#1369b4";
			hvrSbTog.style.display = "none";

	  	hvrTpNav.style.color = "#606060"; // turn the top nav link text blue and
	    hvrSbNav.style.display = "block"; // display/unhide the hovered sub nav links

			hvrTpTog = hvrTpNav; // flag to re-set and show a diff top nav hovered
			hvrSbTog = hvrSbNav; // flag to re-set and show a diff sub nav hovered if applicable

	    hvrtimer();// re-start the hvrtimer timer

		} else {
			// no toggle was set yet, hide the active sub nav, show the hovered sub nav links,
			// then set toggle
			// set timer

			actSbNav.style.display = "none";

			hvrTpNav.style.color = "#1369b4";
			hvrSbNav.style.display = "block";

			hvrTpTog = hvrTpNav; // flag to re-set and show a diff top nav hovered
			hvrSbTog = hvrSbNav; // flag to re-set and show a diff sub nav hovered if applicable

			hvrtimer();
		}
}


function showTpNav(ita) {

	hvrTpIdx = ita;  // set index each loop

	//console.log('showTpNav, index=' + hvrTpIdx);
	// no timer was running
	// check if hvrTpIdx is the active nav
	// if NOT the active nav, hide active sub nav, show hovered nav, set timer,set toggle

	if (hvrTpIdx != actTpIdx) {

		//console.log('non-active top nav was hovered, hide active/hovered nav and show sub nav of hovered link if applicable, start timer, set toggle');

		try {
			//console.log('actSbNav=' + actSbNav + ', hvrTpNav=' + hvrTpNav + 'hvrSbNav=' + hvrSbNav);
			actSbNav.style.display = "none";// if actTpNav has a sub nav hide it

			hvrTpNav.style.color = "#1369b4";
			hvrSbNav.style.display = "block";

			hvrtimer(); //start the hvrtimer timerr tim/*-*/
		}
		catch (e) {
			console.log("ERROR: " + e);
		}

		hvrTpTog = hvrTpNav; // flag to re-set and show a diff top nav hovered
		hvrSbTog = hvrSbNav; // flag to re-set and show a diff sub nav hovered if applicable
	}
}


function resetTpNav(ita) {

	hvrTpIdx = ita;

	if (actTpNav == null) {
		//console.log("Active Top Nav not set, it's a fresh page load or user refreshed browser");
		findActTpNav();
	}

	//user hovered over the active link, ck if timer was running to determine if there is a toggle set

	if (timer_on != 0) {	// timer is running so stop timer

		stophvrtimer();

		if (hvrTpIdx == actTpIdx) { // ck if hovered nav is the active nav

			if (hvrTpNav != null) {
				hvrTpNav.style.color = "#606060";
				hvrSbNav.style.display = "none";
			}
			if (hvrTpTog != null) {

				hvrTpTog.style.color = "#606060";
				hvrSbTog.style.display = "none";
			}

			actSbNav.style.display = "inline-block"; //if actTpNav has a sub nav show it

			hvrTpNav = null; // flag to re-set and show a diff top nav hovered
			hvrSbNav = null; // flag to re-set and show a diff sub nav hovered if applicable

			// don't run hvrtimer because it's the active tab
			// console.log('Timer was on, and HvrTpNav = ActTpIdx:' + ita);
			// console.log('resetTpNav was triggered because the active top nav was hovered');

		} else {
			if (hvrTpNav != null) {
				hvrTpNav.style.color = "#606060";
				hvrSbNav.style.display = "none";
			}
			if (hvrTpTog != null) {

				hvrTpTog.style.color = "#606060";
				hvrSbTog.style.display = "none";
				//console.log('hvrTpTog:' + hvrTpTog);
			}
			actSbNav.style.display = "inline-block"; //if actTpNav has a sub nav show it
			//console.log('hover ne to active');
		}

	} else {
		//console.log('timer is not running, active nav was hovered do nothing');// timer is not running just leave the active nav
	}

}

/**
 * Function used by overHead to determine element id of the
 * Active top navigation tab.
 * **************** THIS FUNCTION NEEDS TO KNOW THE NUMBER OF TOP TABS !!!! ****************
 * @return
 */
function findActTpNav() {
	for (i = 0; !(i >= 13); i++) { // Currently 12 main tabs (0...11)
		id = formname + ":tnloop:" + i + ":activeTopNav";
		obj = document.getElementById(id);

		if (obj != null) {
			actTpNav = obj;

			actTpIdx = i; // set index, it's used to ck if actTpNav index is same as hvrTopNav index
			actSbId = formname + ":tnloop:" + i + ":activeSubNav";
			actSbNav = document.getElementById(actSbId);

			//console.log("active Top Nav index set to: " + i);
			//console.log("actTpNav id set to: " + id + ", elem=" + actTpNav);
			//console.log("actSbNav id set to: " + actSbId + ", elem=" + actSbNav);
			break; // break added 3/9/18 - DH
		}
	}
}

/**
 * Functions hvrtimer and stophvrtimer are used by overHead. hvrtimer is a timer
 * set to hide "Hovered" sub nav links & change color of "Hovered" top nav links back to default
 * when the timer runs out, and that allows sub nav links to remain
 * viewable long enough for user to navigate to any of the sub nav links.  The stophvrtimer
 * is used/called when hovering over "other/diff" top nav links not previously hovered, stops
 * the current timer and starts the delay process over for the "newly" "Hovered" top nav link
 */
var timeout = 2500; // time the sub nav links display after hover and set to auto hide when timer ends
var timer = 0; // if a given timer is running the timer will be greater than 0
var timer_on = 0;

function hvrtimer() {

	timer = setTimeout('expirehvrtimer()', timeout);
	//console.log('hover timer trigger');
	timer_on = 1;
}

/**
 * Used with hvrtimer()... if we don't use a separate call for the active top
 * nav (resetTpNav()) and another top nav link is hovered just as the timer
 * expires, one script can cancel the other, timer will get cancelled prematurely
 * and break the UI, best I can describe it right now. (JM)
 */
function expirehvrtimer() {
	//console.log('hover timer expired');
	stophvrtimer();
}

function stophvrtimer() {
	//console.log('hover timer cancelled');

	clearTimeout(timer); // clear timer
	timer_on = 0; // timer set to false
	timer = 0; // reset timer to 0

	if (hvrTpIdx == actTpIdx) { // hover over active main tab

		//console.log('hvrTpTog = hvrTpNav');
		hvrTpTog.style.color = "#606060";
		hvrSbTog.style.display = "none";

		hvrTpNav.style.color = "#606060";
		hvrSbNav.style.display = "none";

		actSbNav.style.display = "inline-block"; // make sub-tabs visible

		hvrTpNav = null;  //re-set top nav toggle
		hvrSbNav = null;  //re-set sub nav toggle
	}

	if (hvrTpNav != null) {

		//console.log('hvrTpNav is NOT null, hverTpIdx is NOT eq to actTpNav and the timer expired for the previously hovered nav or a diff top nav was hovered/toggled');
		hvrTpTog.style.color = "#606060";
		hvrSbTog.style.display = "none";

		hvrTpNav.style.color = "#606060";
		hvrSbNav.style.display = "none";

		actSbNav.style.display = "inline-block";

	}

	hvrTpTog = hvrTpNav; // flag to re-set and show a different top nav hovered
	hvrSbTog = hvrSbNav; // flag to re-set and show a different sub nav hovered if applicable
}


/**
 * Function used to select a row in Import Script Review and Final pages.
 * (Used to issue a "beep" if the row clicked is for an omitted scene; selecting
 * an omitted scene is now allowed so that it can be un-omitted or deleted.)
 * @param id The element id of the (hidden) edit button.  The js function 'clicks' this button.
 * @param deleted True if the row represents a deleted (omitted) scene; false otherwise.
 */
function sceneRowClicked(id,deleted) {
	var btn = document.getElementById(id);
	//console.log(id + ', ' + btn);
	if (btn != null) {
		btn.click();
	}
}

/**
*  Function called by "first", "last", "next" & "previous" buttons on the Breakdown
*  Page screen to position the current breakdown strip in
*  about the vertical middle of the list of strips.
*/
function showStrip(index) {
	showRowId(index,'bd:scenelist');
}

/**
 * Position a scrollable ice:dataTable so the "index"-th entry is positioned
 * about in the vertical middle of the container. The row height must be 26
 * pixels (typical for our left-side lists).
 *
 * @param index
 *            The index of the row to be centered (origin 0).
 * @param tableId
 *            The html id of the ice:dataTable.
 */
function showRowId(index, tableId) {
	showRowIx(index, tableId, 26);
}

/**
 * Position a scrollable ice:dataTable so the "index"-th entry is positioned
 * about in the vertical middle of the container.
 *
 * @param index
 *            The index of the row to be centered (origin 0).
 * @param tableId
 *            The html id of the ice:dataTable.
 * @param size
 *            The height of each row.
 */
function showRowIx(index, tableId, size) {
	try {
		//console.log('index=' + index + ', size=' + size)
		resize(); // do clientHeight adjustment to window, else it will be minimum when switching pages.
		var element = getScrollDiv(tableId);
		if (element != null) {
			// adjust client height for average/typical table header height
			var ht = (element.clientHeight-24) / 2;
			var scr = (index * size) - ht;
			if (scr < 0) {
				scr = 0;
			}
			element.scrollTop = scr;
			//console.log('scroll row ix=' + scr);
			//console.log("elem=" + element + ", to=" + scr + ", ht=" + element.clientHeight);
		}
		else {
			//console.log("null element for " + tableId);
		}
	} catch (e) {
		//console.log("exception: " + e);
	}
}

/**
 * Position a scrollable list so the 'index'-th entry is positioned about in the
 * vertical middle of the container. Assumes a row height of 26px, which most of
 * our left-hand lists use.
 *
 * @param element
 *            The html id of the container.
 * @param index
 *            The index of the row to be centered (origin 0).
 */
function showRow(element, index) {
	if (element != null) {
		var ht = element.clientHeight / 2;
		var scr = ((index + 1) * 26) - ht;
		if (scr < 0) {
			scr = 0;
		}
		element.scrollTop = scr;
		//console.log('scroll row=' + scr);
	}
}

/**
 * Opens a new window on a help topic. The current parameter list matches the
 * call generated in HeaderViewBean.openHelpWindow. It includes parameters for
 * both the 'chm2web' and 'doc2web' styles of help files. So we can just change
 * this js function to invoke the appropriate help page without changing the
 * Java code.
 *
 * @param topic
 *            For documentation/debugging only
 * @param mainix
 *            The 'main index tab' number -- which main tab is selected, with
 *            Home=0. This is used in chm2web to calculate the "topic number" to
 *            be opened.
 * @param subindex
 *            The 'sub index tab' number -- which sub-tab is selected within the
 *            main tab. This is also used in chm2web to calculate the "topic
 *            number" to be opened.
 * @param file
 *            For 'doc2web', the name of the topic file (without the .html
 *            suffix) to be opened.
 */
function openHelp(topic, mainix, subindex, file) {
	// chm2web style:
		//index = 5; // *********** 5 is the index of the first tab's (Home) help topic!!! *********
		//index += mainix + 1 + subindex;
		//loc = '../../../../lightspeed/jsp/help/source/_' + index + '.htm';
	// doc2web style:
		loc = '../../help/index.html?n=' + file + '.html';
	//console.log("help for " + topic + " at " + mainix + "," + subindex + ", " + file + "\n" + loc);
	reportOpen(loc,'helpWindow');
}

function reportOpen(popup_file, name_of_popup) {
	var popup = window.open(popup_file, name_of_popup);
	// Detect popup blocker
//	setTimeout(function() {
//	    if(!popup || popup.closed || popup.screenX == 0) {
//	        // Close the window that is hidden behind Chrome's popup blocker
//	        // This is useful because you can then reclaim the name of the window that is being hidden
//	        // Without this, window.open(..., name_of_popup, ...) will be blocked because it is currently hidden by Chrome
//	        //popup && popup.close();
//	        // Tell the user
//			alert(popupMsg);
//	    }
//	}, 1000);
}


function showDirections(sourceAdd, destAdd) {
	//var URL = "map.jsp?sourceAddress="+sourceAdd+ "&amp;destinationAddress="+destAdd;
	//childWindow = window.open(URL, 'imagewindow','toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=yes,resizable=1,width=750,height=650');
	var URL = "http://maps.google.com/maps?saddr="+sourceAdd+"&daddr="+destAdd;
	childWindow = window.open(URL, 'imagewindow','toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=yes,resizable=1,width=750,height=650');
}

/**
 * Designed to resize an icefaces scrollable datatable. The code "knows" the
 * structure that icefaces builds beneath an ice:datatable tag, so it can find
 * the div that needs to be resized. The structure looks like: "table tbody tr
 * td divA (divB0 divB1)..." where divB0 and divB1 are siblings; on IE, they are
 * the only child nodes of divA, but on FireFox, there is a text node in between
 * them!
 *
 * @param idx
 *            the id assigned to the ice:datatable tag.
 * @param diff
 *            The desired difference in height between the browser client window
 *            and the scrollable container.
 * @param minimum
 *            The minimum height to set for the scrollable container.
 */
function resizeScrollable(idx, diff, minimum) {
	var bdy = getScrollDiv(idx); /* the scrollable div within the datatable */
	if (bdy != null) {
		resizeElement(bdy, diff, minimum);
	}
	else {
		//console.log('element is null for id: ' + idx);
	}
}

/**
 * Resize any container relative to the browser client window size.
 *
 * @param idx
 *            the container (object, not id) to be resized.
 * @param diff
 *            The desired difference in height between the browser client window
 *            and the container.
 * @param minimum
 *            The minimum height to set for the container.
 */
function resizeElement(elem, diff, minimum) {
	//console.log('client ht=' + document.documentElement.clientHeight);
	if (elem != null) {
		maxht = (document.documentElement.clientHeight - diff);
		if ( ! (maxht > minimum)) {
			maxht = minimum;
		}
		elem.style.maxHeight = maxht + "px";
		elem.style.height = maxht + "px";
		//console.log('ht=' + maxht);
	}
	else {
		//console.log('resize called with null element');
	}
}

/**
 * Resize any container relative to HALF OF the browser client window size.
 *
 * @param idx
 *            the container (object, not id) to be resized.
 * @param diff
 *            The desired difference in height between half of the browser
 *            client window and the container to be resized.
 * @param minimum
 *            The minimum height to set for the container.
 */
function resizeElementHalf(elem, diff, minimum) {
	//console.log('client ht=' + document.documentElement.clientHeight);
	if (elem != null) {
		maxht = (document.documentElement.clientHeight - diff) / 2;
		if ( ! (maxht > minimum)) {
			maxht = minimum;
		}
		elem.style.maxHeight = maxht + "px";
		elem.style.height = maxht + "px";
		//console.log('1/2 ht=' + maxht);
	}
}


/**
 * Resize any container relative to a percentage of the available the browser
 * client window area.
 *
 * @param idx
 *            the container (object, not id) to be resized.
 * @param diff
 *            The desired difference in height between the browser client window
 *            and the total area being resized.
 * @param minimum
 *            The minimum height to set for the container.
 * @param percent
 *            The percentage of the available height to be assigned to the given
 *            element.
 */
function resizeElementPct(elem, diff, minimum, percent) {
	//console.log('client ht=' + document.documentElement.clientHeight);
	if (elem != null) {
		maxht = (document.documentElement.clientHeight - diff) * percent / 100;
		if ( ! (maxht > minimum)) {
			maxht = minimum;
		}
		elem.style.maxHeight = maxht + "px";
		elem.style.height = maxht + "px";
		//console.log('' + percent + '% ht=' + maxht);
	}
}

/**
 * Scroll function for the Stripboard View page; brings the specified row
 * of a scheduled tab into view.
 * @param index The zero-origin index of the row to be displayed.
 * @param height The height of each row in pixels.
 */
function scrollToStrip(index, height) {
	showRowIx(index,'sv:t:0:sL',height);
}

/**
 * Scroll function for the Stripboard View page; brings the specified row
 * of the unscheduled tab into view.
 * @param index The zero-origin index of the row to be displayed.
 * @param height The height of each row in pixels.
 */
function scrollToUnscheduledStrip(index, height) {
	showRowIx(index,'sv:t:0:uL',height);
}

var mainScroll = 0;
var mainScrollDiv;

// ** var mainListId should be set in each individual page that uses "main scroll" support **
// e.g., var mainListId='elem:mainlist'.  A second, alternate, list id is also supported; this
// is used when 2 minitabs have separate lists, e.g., on the Approver Dashboard. The variable
// in this case is "altListId".  If the element with the mainListId is not found -- presumably
// because it was not rendered -- then the code looks for an element matching the altListId.
/**
 * A function invoked from the server to restore the main list's scroll
 * scroll position to its saved value -- typically after it has been
 * reset to zero by some other activity.
 */
function scrollMainToPos() {
	try {
		if (mainListId != null) {
			var scr = getScrollDiv(mainListId);
			if (scr != null) {
				scrollMainToPosId(mainListId);
			} else if (altListId != null) {
				//console.log("alt: " + altListId);
				scr = getScrollDiv(altListId);
				if (scr != null) {
					scrollMainToPosId(altListId);
				} else if (alt2ListId != null) {
					//console.log("alt2: " + alt2ListId);
					scr = getScrollDiv(alt2ListId);
					if (scr != null) {
						scrollMainToPosId(alt2ListId);
					}
				}
			}
		}
	}
	catch (e) {
		console.log("exception(1): " + e);
	}
}

/**
 * A function invoked from the server to position a particular row of the main
 * list to the center (vertically), used to make sure the selected row is
 * visible.
 *
 * @param index
 *            The 0-origin index of the row to be brought into view.
 * @param height
 *            The height of each row; typically 26. The Timecard Approval page
 *            uses 24.
 */
function showMainRow(index, height) {
	try {
		//console.log('index=' + index + ', height=' + height + " mainListId=" + mainListId);
		if (mainListId != null) {
			var scr = getScrollDiv(mainListId);
			if (scr != null) {
				showRowIx(index, mainListId, height);
				mainScroll = getScrollDiv(mainListId).scrollTop;
			}
			else if (altListId != null) {
				//console.log("alt: " + altListId);
				scr = getScrollDiv(altListId);
				if (scr != null) {
					showRowIx(index, altListId, height);
					mainScroll = getScrollDiv(altListId).scrollTop;
				} else if (alt2ListId != null) {
					//console.log("alt2: " + alt2ListId);
					scr = getScrollDiv(alt2ListId);
					if (scr != null) {
						showRowIx(index, alt2ListId, height);
						mainScroll = getScrollDiv(alt2ListId).scrollTop;
					}
				}
			}
		}
	}
	catch (e) {
		console.log("exception(2): " + e);
	}
}

/**
 * Set an 'onscroll' function on the "main" list, which will save
 * the scrollTop value whenever the list is scrolled.
 * @param tableId The html id of the main list, which must be
 * an ice:dataTable with scrollable=true for this code to work!
 */
function setMainScrollOn(tableId) {
		mainScrollDiv = getScrollDiv(tableId);
		if (mainScrollDiv != null) {
			mainScrollDiv.onscroll = function() {saveMainScrollPos(this);};
		}
		//console.log("main scroll div=" + mainScrollDiv);
}

/**
 * Save the "main" list's scroll value. Typically called via
 * on onscroll function.
 * @param id The scrolling div's element id (not html id).
 */
function saveMainScrollPos(id) {
	if (id.scrollTop == 183 || id.scrollTop == 184) {
		// We are getting called to set a scroll value of '183' after a banner is
		// dropped; don't know why. So we ignore it & put the old value back.
		// (IE sometimes gives 183, sometimes 184!)
		id.scrollTop = mainScroll;
		//console.log('SET scroll=' + mainScroll);
	}
	else {
		mainScroll = id.scrollTop;
		//console.log('save scroll=' + mainScroll);
	}
}


/**
 * Scroll the "main" list to the saved position.  The scroll
 * value should be set in 'mainScroll'; this is typically
 * done via onscroll function(s) added to the main list's
 * scrolling div. (See setMainScrollOn().)
 *
 * @param tableId  The html id of the main list.
 * @return
 */
function scrollMainToPosId(tableId) {
//	if (! isPopupOpen()) {
	// 5/30/15 even if a popup is open, we sometimes want to preservce the position
	// (visible in the background) of a list. So we let this run.
		if (mainScroll != null) {
			var scrollDiv = getScrollDiv(tableId);
			if (scrollDiv != null) {
				scrollDiv.scrollTop = mainScroll;
				//console.log("set mainpos ="+mainScroll);
				if (scrollDiv != mainScrollDiv) {
					//console.log("new div");
					mainScrollDiv = scrollDiv;
					mainScrollDiv.onscroll = function() {saveMainScrollPos(this);};
				}
			}
		}
//	}
//	else {
//		console.log("popup open, no main scroll set");
//	}
}

/** holds popup's scrolling div position */
var popScrollPos = 0;

/** Save the popup's scrolling div position. */
function savePopScrollPos(pos) {
	popScrollPos = pos;
}

/** Restore the popup's scrolling div position. */
function scrollPopToPos() {
  if (popScrollPos != null) {
    document.getElementById(popScrollId).scrollTop = popScrollPos;
	//console.log('scroll pop=' + popScrollPos);
  }
}

/**
 * Find the scrolling div that is within a ice:dataTable that has
 * scrollable=true set. The code "knows" the structure that icefaces builds
 * beneath the item with that id, so it can find the div that needs to be
 * resized. It looks like:
 * <p>
 * "table tbody tr td divA (divB0 divB1)..."
 * <p>
 * where divB0 and divB1 are siblings; on IE, they are the only child nodes
 * of divA, but on FireFox, there is a text node in between them!
 * <p>
 * 'divB1' then contains a table whose rows (tr) are the actual displayed
 * rows of data.
 */
function getScrollDiv(id) {
	scrollDiv = null;
	var row = document.getElementById(id + ':0'); // id of first table row (tr)
	if (row == null) { // try "ace" component style
		row = document.getElementById(id + '_row_0'); // id of first table row (tr)
		//console.log('getscrolldiv: row item=' + row);
	}
	if (row != null) {
		// the easy way is find the great-grandparent of the first row:
		scrollDiv = row.parentNode.parentNode.parentNode;
		        //  tr -> tbody -> table -> scrollable div
		var parentDiv = row.parentNode.parentNode;
		if(parentDiv == document.getElementById(id)) {
			scrollDiv = parentDiv;
		}
	}
	else { // no rows in table; use alternate method;
		var divp =  document.getElementById(id + '_paginatortop');
		if (divp != null) { // ace dataTable with paginator on top
			scrollDiv = divp.nextSibling.nextSibling;
		}
		else {
			var body = document.getElementById(id + '_body'); /* ace:dataTable - tbody */
			if (body != null) { /* parent is 'table', grandparent is scrollable body 'div' to resize */
				//console.log("using _body grandparent");
				scrollDiv = body.parentNode.parentNode;
			}
			else {
				var tbl = document.getElementById(id); /* the table */
				if (tbl != null) {
					if (tbl.childNodes.length == 3) {
						//console.log("assuming ace:dataTable (with no rows)");
						scrollDiv = tbl.childNodes[1];
					}
					else {
						// This depends on the ice:dataTable structure described above.
						//console.log("assuming ice:dataTable structure");
						divB0 = tbl.getElementsByTagName('div')[0].firstChild;
						scrollDiv = divB0.nextSibling; // usually this is "divB1" - our target
						if (scrollDiv != null && scrollDiv.nodeType == 3) { // a text node lies between the 2 divs!
							scrollDiv = scrollDiv.nextSibling;
						}
					}
				}
			}
		}
	}
	return scrollDiv;
}

/**
 * Sets the 'onscroll' function to an empty function for the table
 * whose element is given.  The purpose of this is to stop the
 * server interaction every time the user scrolls the table.
 */
function clearOnScroll(element) {
	var scrDiv = document.getElementById(element+'_scroll'); // getScrollDiv(element);
	if (scrDiv != null) {
		scrDiv.onscroll = function() {};
	}
}

// * * * Functions & data for maintaining a scrollable div's position * * *
var scrollme = 0; // used by resize() to know if "scrollToPos()" was called before it.

function setScrollPos(pos) {
	// called via 'onscroll' tag
	document.getElementById(formname + ':scrollPos').value = pos;
	//console.log("save scroll=" + pos);
}

function scrollToPos() {
	//console.log("at scrollToPos");
	try {
		scrollme = 1; // tell "resize" to call us again in case it executes after us.
		var scrollable = document.getElementById('scrolldiv');
		if (scrollable != null) {
			var scrollposElem = document.getElementById(formname + ':scrollPos');
			pos = scrollposElem.value;
			if (pos != null) {
				scrollable.scrollTop = pos;
				//console.log("scroll to=" + pos);
			}
		}
	}
	catch(e) {
		console.log('scrollToPos exception: ' + e);
	}
}

/** Determine if one of the common pop-ups is open, based on one
 * of the "gradient" div id's existing.  Usually, if the pop-up is
 * not being displayed, it is not rendered, so the id won't exist. */
function isPopupOpen() {
	if (document.getElementById('popGrdnt') == null &&
			document.getElementById('popAddScriptElemGrdnt') == null &&
			document.getElementById('popAddElemGrdnt') == null) {
		return false;
	}
	return true;
}

var showErrorCount = 0;

/**
 * Manage display of our error box, so that if user closes it, but then
 * continues trying to update data, the partialSubmit activity will re-display
 * the error box.  This function is normally invoked as a result of being
 * registered like this:
 * 			ice.onAfterUpdate(checkShowErrors);
 *
 * @param updates
 *            Parameter from ICEfaces API -- updates that were just applied. Not
 *            used.
 * @returns
 */
var checkShowErrors = function(updates) {
	//console.log('checkShowErrors, cnt=' + showErrorCount);
	showErrorCount++;
	if (showErrorCount > 2) {
		showErrors();
	}
};

/** Show (make visible) our standard error message block. */
function showErrors() {
	showErrorCount = 0;
	displayErrors(true,'errMsgCont');
}
/** Show (make visible) our popup error message block. */
function showPopErrors() {
	displayErrors(true,'popErrMsgCont');
}

/** Hide our standard error message block. */
function hideErrors() {
	showErrorCount = 0;
	displayErrors(false,'errMsgCont');
}
/** Hide our popup error message block. */
function hidePopErrors() {
	displayErrors(false,'popErrMsgCont');
}
/** Hide our standard information message block. */
function hideInfo() {
	displayErrors(false,'infoMsgCont');
}
/** Hide our popup error message block. */
function hidePopInfo() {
	displayErrors(false,'popInfoMsgCont');
}

/** Show or hide our standard error message block. */
function displayErrors(bool, id) {
	errElement = document.getElementById(id);
	if (errElement != null) {
		//console.log('LS: element ' + id + ' set ' + bool);
		if (bool == true) {
			errElement.style.display = "block";
		}
		else {
			errElement.style.display = "none";
		}
	}
	else {
		//console.log('LS: element ' + id + ' not found.');
	}
}

/**
 * Set the focus on an element.  The element must be named following
 * our convention, which is '<prompt_type>_focus'.  The JSP page must
 * also set the 'formname' variable, but that is relatively standard
 * within the LightSPEED application.
 *
 * @param prompt_type The string that will be used as the leading
 * portion of the element id which should receive the focus.
 */
//var lastFocus = null;
function focusOn(prompt_type) {
	try {
//		lastFocus = prompt_type;
   		var id = formname + ':' + prompt_type + '_focus';
   		var p = document.getElementById(id);
		if (p == null) {
			id = formname + ':tabs:0:' + prompt_type + '_focus';
			p = document.getElementById(id);
		}
//		console.log('focusOn: ' + id + '= ' + p);
		if (p != null && p.offsetWidth != 0) { /* put cursor in field on pop-up */
//			console.log('focusOn: ' + id + '= ' + p);
			p.focus();
//			ice.applyFocus(id);
		}
	}
	catch(e) {
		console.log('focusOn exception: ' + e);
	}
}

//var checkFocus = function(updates) {
//	if (lastFocus != null) {
//		console.log('re-focusOn: form=' + formname + ', type=' + lastFocus);
//		focusOn(lastFocus);
//		lastFocus = null;
//	}
//};

/**
 * A function to slow down the processing of a commandLink click, to workaround
 * the ICEfaces 3.2 issue of the 'blur' and 'click' events colliding and causing
 * an action method to NOT be called when it should.
 * <p>
 * Example of its use:<br/>
 * < ice:commandLink value="Cancel" action="#{fullTimecardBean.actionCancel}" <br/>
 * 			onclick="return slowClick(this);" / >
 */
function slowClick(button, event) {
	var slowEvent = event;
	//console.log("in slowclick(500): " + button);
	setTimeout(function() {
			//console.log("in func: " + button);
			clickLink(button, slowEvent);
		}, 500);

	return false;
}

/* A set of functions used for enhancing the Script Import dialog box. */

/**
 * Upload a script import file.  This action replaces the "normal" submit
 * button for an inputFile tag structure.  It checks for maximum allowed
 * file size; if the file is too big, it forces an error by clicking the
 * "too big" button.  Otherwise, it submits the inputFile form.
 */
function uploadImport(event) {
	var file = getFile(importInputFileId);
	if (file != null) {
		if (file.size != null) if (file.size > (6*1024*1024)) {
			//console.log('file too big');
			var bigBtn = document.getElementById('s:btnTooBig');
			if (bigBtn) {
				bigBtn.click();
				return false;
			}
			else {
				console.log('button not found!');
			}
		}
	}
	submitUploadForm(importInputFileId, event);
	return true;
}

/**
 * Find the File object that is embedded within an ICEfaces inputFile
 * tag structure.  The File object will only exist if the user has
 * already done the "Browse" (file selection) step.
 */
function getFile(inputid) {
	input = getInputElement(inputid); // get the input tag in the ace:fileEntry
	if ( ! input ) { // IE6/7? Or the Upload dialog box is not rendered.
		return null;
	}

//    var input, file = null;
//    var inputs = frameDoc.getElementsByTagName('input');
//    for (i=0; i != inputs.length; i++) {
//    	input = inputs[i];
//    	if (input.files != null) {
//    		break;
//    	}
//    }

    if (!input.files) {
    	// IE8 got this
    	//alert("This browser doesn't seem to support the `files` property of file inputs.");
    	console.log("This browser doesn't seem to support the `files` property of file inputs.");
    }
    else if (!input.files[0]) {
    	//alert("Please select a file before clicking 'Load'");
    	console.log("Please select a file before clicking 'Load'");
    }
    else {
    	file = input.files[0];
    	//alert("File " + file.name + " is " + file.size + " bytes in size");
    	console.log("File " + file.name + " is " + file.size + " bytes in size");
    }
    return file;
}

/**
 * Hide the "upload file" button that is created as part of the
 * ICEfaces inputFile tag structure.
 */
function hideUpload(inputid) {
	var input = getInputElement(inputid); // get the document inside the inputFile iFrame.
	if ( ! input ) { // IE6/7? Or the Upload dialog box is not rendered.
		return;
	}
	if (input.type == 'submit') {
		input.style.display='none';
		//console.log('hidden');
	}
}

/**
 * Submit the "upload file" form created by an ICEfaces inputFile tag.
 */
function submitUploadForm(inputid, event) {
	var element = document.getElementById('s:import_form');
	alert('form=' + element);
	element.submit();
}

/**
 * Finds the document object that is inside an iFrame built by
 * ICEfaces for an inputFile tag.
 */
function getInputElement(inputid) {
	// ICEfaces adds div's around input tag
	var element = document.getElementById(inputid); // div generated with the id we specified
	element = element.firstChild;  // div inside of that - with no id
	element = element.firstChild;  // input tag inside of that

	//console.log('input elem=' + element);
	//console.log('input elem=' + element);
	return element;
}


/** Disable selection of text within a field. 'name' is the textual id
 * of the element where selection should be disabled.  Note that we don't
 * do anything for Firefox, as this can be handled in CSS.
 * Also note that a user can get around this by using "Select All". */
function disableSelect(name) {
	id = document.getElementById(name);
	if (id != null) {
		if (typeof id.style.MozUserSelect != "undefined") { // Firefox
			// id.style.MozUserSelect = "none"; // set in css
		}
		else if (typeof id.onselectstart != "undefined") { // IE, Safari
			id.onselectstart = function(){return false;};
		}
		else { // Opera (and others?)
			id.onmousedown = function(){return false;};
			id.style.cursor = "default";
		}
	}
	else {
		// console.log("id not found=" + name);
	}
}

//	* * * Breakdown page methods * * *

/** THESE id strings are ALSO used in the JAVA code!! (BreakdownBean.java) */
var elpref = 'bd:etbl';  // id prefix for "+" buttons next to auto-complete fields
var tabpref = 'bd:etab'; // id prefix for "tab pressed" buttons (hidden)
var elpattern = 'bd:etbl(\\d+):(\\d+):in'; // id pattern for "+" buttons

var bdLastKey = 0;
var bdLastDown = 0;
var KEY_TAB = 9;
var KEY_BACK_TAB = 16;
var KEY_ENTER = 13;
var KEY_ARROW_DOWN = 40;
var KEY_ARROW_UP = 38;
var KEY_CLICK = -1;
var STYLE_VISIBLE = 'visible';
var STYLE_HIDDEN = 'hidden';

/**
 * invoked from BreakdownBean.java to set focus to the auto-complete
 * script-element field in the section where one was just added.
 */
function bdFocus(col, row) {
	element = document.getElementById(elpref + col + ':' + row + ':in');
	//console.log(col + "," + row + "," + element);
	if (element != null) {
		bdSetPlus(element, STYLE_VISIBLE); // ensure focused field has "enabled" plus sign
		element.focus();
	}
}


/**
 * Invoked from BreakdownBean.java to scroll the script window to the top. If
 * this was not done, when we switched scenes the script window stayed scrolled
 * to its previous position.
 */
function scrollScriptTop() {
	(document.getElementById("scriptPageContB")).scrollTop = 0;
	//console.log('scroll script=0');
}

//	* * * END of Breakdown page methods * * *

/**
 * Function to issue a "beep" (alert-style) sound.
 * Calls the beepApp applet ... use this to include it on the page:
 *
 * <applet name="beepApp" codebase="../../applet" code="com.lightspeedeps.util.Beep.class"
 * 			style="height:1px;width:1px;" />
 *
 * Note that a height or width of 0px makes this NOT function in IE (at least IE6).
 * Also, it will generate a "field" of about 15px high -- that is, it will force whatever
 * div or row it is in to be at least that tall, so don't just put it at the beginning or
 * ending of the page, as it will create a blank row about 15px high.
 * @return
 */
function beep() {
	//java.awt.Toolkit.getDefaultToolkit().beep(); // only works in Firefox
	document.beepApp.beep();
}

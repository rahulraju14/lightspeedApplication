/* LightSPEED JavaScript functions for mobile (not used in desktop pages).  */

// Specify Session Timeout URL relative to desktop URLs
var sess_logout_uri = '../../jsp/error/timedOut.jsp';

// Specify error handling page reference relative to mobile URLs
var ice_error_url = "../../jsp/error/500a.html";


//Specify if checkButton() needs to call checkShowErrors(). True for desktop, false for mobile.
var checkShowErrorsOn = false;

/**
 * Do all our standard page-initialization functions.
 */
function initDomLoaded() {
	sessionInitTimer(); // Set up session timeout
	window.scrollTo(0,1);
	window.location.hash = 'default';
}

/**
 * Scroll to an anchor location, equivalent to #name in a URL.
 */
function scrollToHash(place) {
	// Going to an empty hash value first makes the scrolling reliable,
	// otherwise going to the same place we already are has unreliable effects.
	window.location.hash = '';
	window.location.hash = place;
	//console.log('scroll to: ' + place);
}


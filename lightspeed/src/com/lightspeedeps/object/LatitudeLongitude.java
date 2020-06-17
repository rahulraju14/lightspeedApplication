/**
 * LatitudeLongitude.java
 */
package com.lightspeedeps.object;

/**
 * Object to store a latitude/longitude pair.  Used in sunrise/sunset
 * calculations.
 */
public class LatitudeLongitude {
		private double latitude;
		private double longitude;

		public LatitudeLongitude() {
		}

		public LatitudeLongitude(double lat, double lon) {
			latitude = lat;
			longitude = lon;
		}

		/**See {@link #latitude}. */
		public double getLatitude() {
			return latitude;
		}
		/**See {@link #latitude}. */
		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		/**See {@link #longitude}. */
		public double getLongitude() {
			return longitude;
		}
		/**See {@link #longitude}. */
		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

}

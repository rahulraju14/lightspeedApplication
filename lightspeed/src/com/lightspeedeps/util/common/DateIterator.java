package com.lightspeedeps.util.common;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

/**
 * This class is used to iterate between two given dates and returns an Iterator object.
 * See the constructor javadoc for more information.
 */
public class DateIterator implements Iterator<Date>, Iterable<Date> {

	private final Calendar start = Calendar.getInstance();
	private final Calendar end = Calendar.getInstance();
	private Calendar current = null;

	/** if true, start is less than end, and "next()" goes forward in time;
	 * otherwise, next() goes backward in time. */
	private boolean forward = true;

	/**
	 * Create a Date iterator which can go either forward or backward in time. Both the start and
	 * end parameters are used for their date value only -- the time of day is ignored.
	 * <p>
	 * If the starting date is less than or equal to the ending date, the Date iterator will go
	 * "forward" -- each next() call advances one day later. If the starting date is greater than
	 * the endind date, then the iterator runs "backwards" -- each next() call moves one day
	 * earlier.
	 * <p>
	 * In either case, the iterator is inclusive of both the start and end dates. The first next()
	 * call returns the starting date, and the last call to next() returns the ending date. All
	 * Dates returned by next() have the time of day set to 12:00am.
	 *
	 * @param pStart The starting date.
	 * @param pEnd The ending date.
	 */
	public DateIterator(Date pStart, Date pEnd) {
		init(pStart, pEnd);
	}

	/** See {@link #DateIterator}. */
	public DateIterator(Calendar pStart, Calendar pEnd) {
		init(pStart.getTime(), pEnd.getTime());
	}

	private void init(Date pStart, Date pEnd) {
		start.setTime(pStart);
		CalendarUtils.setStartOfDay(start);

		end.setTime(pEnd);
		CalendarUtils.setStartOfDay(end);

		if (start.after(end)) {
			forward = false;
		}
	}

	@Override
	public boolean hasNext() {
		boolean ret = true;
		if (current != null) {
			if (forward) {
				ret = current.before(end);
			}
			else {
				ret = current.after(start);
			}
		}
		return ret;
	}

	@Override
	public Date next() {
		if (current == null) {
			current = Calendar.getInstance();
			current.setTime(start.getTime());
		}
		else {
			current.add(Calendar.DATE, 1);
		}
		return current.getTime();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Cannot remove");
	}

	@Override
	public Iterator<Date> iterator() {
		return this;
	}

}
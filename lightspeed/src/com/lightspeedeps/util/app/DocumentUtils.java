/**
 * File: DocumentUtils.java
 */
package com.lightspeedeps.util.app;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.object.DocumentStatusCount;

/**
 * A class for static utility functions related to Documents.
 */
public class DocumentUtils {

	private static final Log log = LogFactory.getLog(DocumentUtils.class);

	/**
	 * Method creates the array of color percentages used on the status graph.
	 * @param pEmployment Employment to be updated.
	 * @param pEmploymentList List of Employments to be updated.
	 *
	 * @return array of four percentages (totaling 100) of documents for the
	 *         given Employment record, corresponding to the four different
	 *         colored segments of the status graph: completed, open, submitted,
	 *         and pending.
	 */
	public static void createStatusGraphForEmployment(Employment pEmployment, List<Employment> pEmploymentList) {
		List<DocumentStatusCount> statusGraph = null;
		try {
			log.debug("");
			boolean found = false;
			List<Integer> empList = new ArrayList<>();
			if (pEmployment != null) {
				log.debug("");
				empList.add(pEmployment.getId());
			}
			else if (pEmploymentList != null) {
				for (Employment emp : pEmploymentList) {
					empList.add(emp.getId());
				}
			}
			statusGraph = ContactDocumentDAO.getInstance().findDocStatusGraphForSelectedEmployments(empList);
			if (pEmployment != null) {
				for (DocumentStatusCount status : statusGraph) {
					found = true;
					pEmployment.setPercentageArray(DocumentUtils.setStatusGraph(status));
				}
				if (! found) {
					pEmployment.setPercentageArray(new Integer[] {0, 0, 0, 0});
				}
			}
			else if (pEmploymentList != null) {
				for (Employment emp : pEmploymentList) {
					found = false;
					for (DocumentStatusCount status : statusGraph) {
						if (status.getEmploymentId() != null && status.getEmploymentId().equals(emp.getId())) {
							found = true;
							emp.setPercentageArray(setStatusGraph(status));
							break; // stop inner loop
						}
					}
					if (! found) {
						emp.setPercentageArray(new Integer[] {0, 0, 0, 0});
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method creates the array of color percentages for the
	 * given status count values for an Employment.
	 *
	 * @param statusGraph
	 * @return array of four percentages (totaling 100) of documents for the
	 *         given Employment record, corresponding to the four different
	 *         colored segments of the status graph: completed, open, submitted,
	 *         and pending.
	 */
	public static Integer[] setStatusGraph(DocumentStatusCount statusGraph) {
		try {
			if (statusGraph != null) {
				int greenCount =  (statusGraph.getGreenCount() == null  ? 0 : statusGraph.getGreenCount().intValue());
				int yellowCount = (statusGraph.getYellowCount() == null ? 0 : statusGraph.getYellowCount().intValue());
				int redCount =    (statusGraph.getRedCount() == null    ? 0 : statusGraph.getRedCount().intValue());
				int blackCount =  (statusGraph.getBlackCount() == null  ? 0 : statusGraph.getBlackCount().intValue());
				int allDocs = greenCount + yellowCount + redCount + blackCount;

				if (allDocs != 0) {
					greenCount = calculateColorPercentage(allDocs, greenCount);
					redCount = calculateColorPercentage(allDocs, redCount);
					yellowCount = calculateColorPercentage(allDocs, yellowCount);
					blackCount = calculateColorPercentage(allDocs, blackCount);
					int diff = 100 - (blackCount + greenCount + redCount + yellowCount);
					if (diff != 0) {
						// rounding errors can get us here; add diff to any non-zero value
						if (blackCount != 0) {
							blackCount += diff;
						}
						else if (redCount != 0) {
							redCount += diff;
						}
						else if (yellowCount != 0) {
							yellowCount += diff;
						}
					}
					Integer percentageArray[] = { greenCount, redCount, yellowCount, blackCount };
					return percentageArray;
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return new Integer[] {0, 0, 0, 0};
	}

	/** Utility method used to calculate the percentage of individual
	 * status color against total count
	 * @param allDocs count of contact documents
	 * @param greenCount count of individual documents status
	 * @return percentage of color
	 */
	private static int calculateColorPercentage(int allDocs, int greenCount) {
		int percent = 0;
		try {
			if (allDocs != 0 && greenCount != 0) {
				percent = ((greenCount*100)/allDocs);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return percent;
	}

}

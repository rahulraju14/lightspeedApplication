package com.lightspeedeps.web.onboard;

import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;

public class StudentDocumentUtil {

	public static Integer[] setStatusGraph(StudentStatusCount statusGraph) {
		try {
			if (statusGraph != null) {
				int greenCount =  (statusGraph.getGreenCount() == null  ? 0 : statusGraph.getGreenCount().intValue());
				int redCount =    (statusGraph.getRedCount() == null    ? 0 : statusGraph.getRedCount().intValue());
				int blackCount =  (statusGraph.getBlackCount() == null  ? 0 : statusGraph.getBlackCount().intValue());
				int allDocs = greenCount + redCount + blackCount;

				if (allDocs != 0) {
					greenCount = calculateColorPercentage(allDocs, greenCount);
					redCount = calculateColorPercentage(allDocs, redCount);
					blackCount = calculateColorPercentage(allDocs, blackCount);
					int diff = 100 - (blackCount + greenCount + redCount);
					if (diff != 0) {
						// rounding errors can get us here; add diff to any non-zero value
						if (blackCount != 0) {
							blackCount += diff;
						}
						else if (redCount != 0) {
							redCount += diff;
						}
						
					}
					Integer percentageArray[] = { greenCount, redCount, blackCount };
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
	
	private static int calculateColorPercentage(int allDocs, int colorCount) {
		int percent = 0;
		try {
			if (allDocs != 0 && colorCount != 0) {
				percent = ((colorCount*100)/allDocs);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return percent;
	}
	
	
	
	
	
	
}

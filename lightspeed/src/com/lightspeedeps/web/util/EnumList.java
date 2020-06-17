//	File Name:	EnumList.java
package com.lightspeedeps.web.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.Constants;

/**
 * Provides select lists (for drop-down list boxes) of various Enum classes in
 * lightSPEED.
 */
@Component("enumList")
@Scope("singleton")
public class EnumList {
	private static final Log log = LogFactory.getLog(EnumList.class);

	private static final List<SelectItem> accessStatusList 			= createEnumSelectList(AccessStatus.class);

	private static final List<SelectItem> callSheetVersionList 		= createEnumSelectList(CallSheetVersion.class);

	private static final List<SelectItem> dayNightTypeList 			= createEnumValueSelectList(DayNightType.class);

	/** Contents of the drop-down selection list for the "Day Type" of a timecard's daily entry. The
	 * value of each SelectItem entry is a {@link com.lightspeedeps.type.DayType DayType} enum, and the label
	 * is the "pretty" name from the DayType.shortLabel field. */
	private static final List<SelectItem> dayTypeList 				= createEnumValueSelectList(DayType.class);

	private static final List<SelectItem> toursDayTypeList 			= createEnumValueSelectList(DayType.class, "TSH");

	/** Selection list for ACA Employment Basis drop-down, including "none selected" heading. */
	private static final List<SelectItem> employmentBasisTypeList 	= createEnumValueSelectList(EmploymentBasisType.class);

	private static final List<SelectItem> fileAccessTypeList 		= createEnumSelectList(FileAccessType.class);

	private static final List<SelectItem> imServiceTypeList 		= createEnumSelectList(IMServiceType.class);

	/** Int/Ext selection list for JSF, uses enum values, not Strings */
	private static final List<SelectItem> intExtTypeList 			= createEnumValueSelectList(IntExtType.class);

	private static final List<SelectItem> watermarkPreferenceList 	= createEnumSelectList(WatermarkPreference.class);

	private static final List<SelectItem> orderStatusList 			= createEnumSelectList(OrderStatus.class);

	private static final List<SelectItem> pointOfInterestTypeList 	= createEnumSelectList(PointOfInterestType.class);

	private static final List<SelectItem> productionPhaseList 		= createEnumSelectList(ProductionPhase.class);

	private static final List<SelectItem> productionTypeList 		= createEnumSelectList(ProductionType.class);

	private static final List<SelectItem> realWorldElementTypeList 	= createRweSelectList();

	private static final List<SelectItem> realLinkStatusList 		= createEnumSelectList(RealLinkStatus.class);

	private static final List<SelectItem> reportFrequencyList 		= createEnumSelectList(ReportFrequency.class);

	private static final List<SelectItem> scriptElementTypeList 	= createEnumSelectList(ScriptElementType.class);

	private static final List<SelectItem> scriptElementTypeListNoSet = createElementTypesNoSet();

	private static final List<SelectItem> userStatusList 			= createEnumSelectList(UserStatus.class);

	private static final List<SelectItem> weeklyStatusFilterList 	= createEnumSelectListStopNA(ApprovableStatusFilter.class);

	private static final List<SelectItem> documentFlowTypeList		= createEnumSelectList(DocumentFlowType.class);

	private static final List<SelectItem> documentActionTypeList    = createEnumSelectList(DocumentAction.class);

	private static final List<SelectItem> actionTypeList    = createEnumSelectList(ActionType.class);

	/** Full list of gender selections. LS-2502 */
	private static final List<SelectItem> genderFullList    = createEnumSelectList(GenderType.class);

	/** Short list of gender selections: M/F only. LS-2502 */
	private static final List<SelectItem> genderShortList    = new ArrayList<>();

	/** Contents of the drop-down selection list for a timecard's "Work Zone".
	 * The value of each SelectItem entry is a
	 * {@link com.lightspeedeps.type.WorkZone WorkZone} enum, and the label is
	 * the "pretty" name from the WorkZone.shortLabel field. */
	private static final List<SelectItem> workZoneList 				= createEnumValueSelectList(WorkZone.class);

	/** Contents of the Tax Wage Allocation Frequency dropdown
	 * {@link com.lightspeedeps.type.WorkZone WorkZone} enum, and the label is
	 * the "pretty" name from the WorkZone.shortLabel field. */
	private static final List<SelectItem> taxWageAllocationFrequencyList	= createEnumValueSelectList(TaxWageAllocationFrequencyType.class);
	//	public static final List<SelectItem> contractStatusList 		= createEnumSelectListNA(ContractStateStatus.class);

	/** List of how the employee will be paid, either as an Individual or Loan-out. LS-2562 */
	private static final List<SelectItem> paidAsTypeList    = createEnumSelectList(PaidAsType.class);

	static {
		// Update lists that have "heading" entries:
		employmentBasisTypeList.add(0, new SelectItem(null, Constants.SELECT_HEAD_BASIS));
		dayTypeList.add(0, Constants.EMPTY_SELECT_ITEM);
		workZoneList.add(0, Constants.EMPTY_SELECT_ITEM);
		genderFullList.add(0, Constants.EMPTY_SELECT_ITEM); // LS-2502
		paidAsTypeList.add(0, Constants.EMPTY_SELECT_ITEM); // LS-2562

		// Create short gender list, just M/F. LS-2502
		genderShortList.add(Constants.EMPTY_SELECT_ITEM);
		genderShortList.add(new SelectItem(GenderType.M.name(), GenderType.M.getLabel()));
		genderShortList.add(new SelectItem(GenderType.F.name(), GenderType.F.getLabel()));
	}

	public EnumList() {
		log.debug("");
	}

	/**
	 * Create a SelectItem list (for a drop-down or radio buttons) where the
	 * value field is the name() of the Enum object. If the specified Enum class
	 * has a custom toString() method that does NOT return the name() of the
	 * entry, and if this List is used in a drop-down (not for radio buttons),
	 * then a JSF converter class must exist to handle the input.  See, for example,
	 * AccessStatusConverter. Note that values with a name of "N_A" are ignored
	 * (not included in the SelectItem list).
	 *
	 * @param c The Enum class whose values will be used to create the list.
	 * @return A non-empty List of SelectItem's.
	 */
	public static <T extends Enum<T>> List<SelectItem> createEnumSelectList(Class<T> c) {
		List<SelectItem> list = new ArrayList<>();
		SelectItem selectitem;
		T[] enums = c.getEnumConstants();
		if (enums != null) {
			for (T p : enums) {
				if (!"N_A".equals(p.name())) {
					selectitem = new SelectItem(p.name(), p.toString());
					list.add(selectitem);
				}
			}
		}
		return list;
	}

	/**
	 * Create a SelectItem list (for a drop-down or radio buttons) where the
	 * value field is the name() of the Enum object. If the specified Enum class
	 * has a custom toString() method that does NOT return the name() of the
	 * entry, and if this List is used in a drop-down (not for radio buttons),
	 * then a JSF converter class must exist to handle the input. See, for
	 * example, AccessStatusConverter.
	 * <p>
	 * NOTE: Only entries up to, and not including, an entry named "N_A" will be
	 * included in the resulting list. This is used by some Enum classes to
	 * distinguish between Enum values available to the user for selection,
	 * versus those that are only used internally.
	 *
	 * @param c The Enum class whose values will be used to create the list.
	 * @return A non-empty List of SelectItem's.
	 */
	public static <T extends Enum<T>> List<SelectItem> createEnumSelectListStopNA(Class<T> c) {
		List<SelectItem> list = new ArrayList<>();
		SelectItem selectitem;
		T[] enums = c.getEnumConstants();
		if (enums != null) {
			for (T p : enums) {
				if (p.name().equals("N_A")) {
					break;
				}
				selectitem = new SelectItem(p.name(), p.toString());
				list.add(selectitem);
			}
		}
		return list;
	}

	/**
	 * Create a SelectItem list (for a drop-down or radio buttons) where the
	 * value field is the actual Enum object, not just the name() of it.
	 * <p>
	 * NOTE: Only entries up to, and not including, an entry named "N_A" will be
	 * included in the resulting list. This is used by some Enum classes to
	 * distinguish between Enum values available to the user for selection,
	 * versus those that are only used internally. (See
	 * {@link com.lightspeedeps.type.DayType DayType} for example.)
	 *
	 * @param c The Enum class whose values will be used to create the list.
	 * @return A non-empty List of SelectItem's.
	 */
	public static <T extends Enum<T>> List<SelectItem> createEnumValueSelectList(Class<T> c) {
		List<SelectItem> list = new ArrayList<>();
		SelectItem selectitem;
		T[] enums = c.getEnumConstants();
		if (enums != null) {
			for (T p : enums) {
				if (p.name().equals("N_A")) {
					break;
				}
				selectitem = new SelectItem(p, p.toString());
				list.add(selectitem);
			}
		}
		return list;
	}

	/**
	 * Create a SelectItem list (for a drop-down or radio buttons) where the
	 * value field is the actual Enum object, not just the name() of it.
	 * <p>
	 * NOTE: Only entries including and following the entry with name matching
	 * the 'start' parameter will be included in the resulting list. This is
	 * currently only used to create the list of "Tours" Day Types from the
	 * {@link com.lightspeedeps.type.DayType DayType} enumeration.
	 *
	 * @param c The Enum class whose values will be used to create the list.
	 * @param start The name of the Enum which will be the first entry added to
	 *            the list; all Enum values after that will also be added. None
	 *            of the values of the Enum prior to the one matching 'start'
	 *            will be included.
	 * @return A non-empty List of SelectItem's.
	 */
	public static <T extends Enum<T>> List<SelectItem> createEnumValueSelectList(Class<T> c, String start) {
		List<SelectItem> list = new ArrayList<>();
		boolean include = false;
		T[] enums = c.getEnumConstants();
		if (enums != null) {
			for (T p : enums) {
				if (p.name().equals(start)) {
					include = true;
				}
				if (include ) {
					list.add(new SelectItem(p, p.toString()));
				}
			}
		}
		return list;
	}

	/**
	 * A special build for the RealWorld element types -- uses ScriptElementType, except
	 * a special toRwString() method, as a couple of the types are displayed differently
	 * for Real World elements vs Script elements.
	 *
	 * @return A List of SelectItem's generated from the ScriptElementType enum.
	 */
	private static List<SelectItem> createRweSelectList() {
		List<SelectItem> list = new ArrayList<>();
		SelectItem selectitem;
		for (ScriptElementType p : ScriptElementType.values()) {
			if (!"N_A".equals(p.name())) {
				selectitem = new SelectItem(p.name(), p.toRwString()); // get the Real World version
				list.add(selectitem);
			}
		}
		return list;
	}

	/**
	 * Same as createEnumSelectList, except that it includes "N_A" values.
	 */
//	@SuppressWarnings("unchecked")
//	private static List<SelectItem> createEnumSelectListNA(Class c) {
//		List<SelectItem> list = new ArrayList<SelectItem>();
//		SelectItem selectitem;
//		Object[] enums = c.getEnumConstants();
//		if (enums != null) {
//			for (Object p : enums) {
//				selectitem = new SelectItem(p.toString(), p.toString());
//				list.add(selectitem);
//			}
//		}
//		return list;
//	}

	/**
	 * Build the SelectItem list of ScriptElementType`s that excludes the
	 * LOCATION type.  Used on the Breakdown and Script Element pages.
	 */
	private static List<SelectItem> createElementTypesNoSet() {
		List<SelectItem> list = createEnumSelectList(ScriptElementType.class);
		for (Iterator<SelectItem> iter = list.iterator(); iter.hasNext(); ) {
			SelectItem si = iter.next();
			if (si.getValue().toString().equals(ScriptElementType.LOCATION.name())) {
				iter.remove();
				break;
			}
		}
		return list;
	}

	public static List<SelectItem> getScriptElementTypeList() {
		return scriptElementTypeList;
	}
	/** Non-static version for use in JSP. */
	public List<SelectItem> getScriptElementTypes() {
		return scriptElementTypeList;
	}

	public static List<SelectItem> getScriptElementTypeListNoSet() {
		return scriptElementTypeListNoSet;
	}
	/** Non-static version for use in JSP. */
	public List<SelectItem> getScriptElementTypesNoSet() {
		return scriptElementTypeListNoSet;
	}

	public static List<SelectItem> getRealWorldElementTypeList() {
		return realWorldElementTypeList;
	}
	/** Non-static version for use in JSP. */
	public List<SelectItem> getRealWorldElementTypes() {
		return realWorldElementTypeList;
	}

	public static List<SelectItem> getRealLinkStatusListS() {
		return realLinkStatusList;
	}
	/** Non-static version for use in JSP. */
	public List<SelectItem> getRealLinkStatusList() {
		return realLinkStatusList;
	}

	public static List<SelectItem> getFileAccessTypeList() {
		return fileAccessTypeList;
	}

	public static List<SelectItem> getImServiceTypeList() {
		return imServiceTypeList;
	}
	public List<SelectItem> getImServiceTypes() {
		return imServiceTypeList;
	}

	public static List<SelectItem> getReportFrequencyList() {
		return reportFrequencyList;
	}

	public static List<SelectItem> getAccessStatusList() {
		return accessStatusList;
	}
	public List<SelectItem> getAccessStatusTypes() {
		return accessStatusList;
	}

	public static List<SelectItem> getCallSheetVersionList() {
		return callSheetVersionList;
	}

	public static List<SelectItem> getDayTypeList() {
		return dayTypeList;
	}

	public static List<SelectItem> getToursDayTypeList() {
		return toursDayTypeList;
	}

	/** non-static version for use in JSP files */
	public List<SelectItem> getDayNightList() {
		return dayNightTypeList;
	}

	/** See {@link #employmentBasisTypeList}. */
	public static List<SelectItem> getEmploymentBasisTypeList() {
		return employmentBasisTypeList;
	}

	/** non-static version for use in JSP files */
	public List<SelectItem> getIntExtList() {
		return intExtTypeList; // ice4: use the select list with enum's, not Strings
	}

	/** List of WatermarkPreference enumeration. */
	public static List<SelectItem> getWatermarkPreferenceList() {
		return watermarkPreferenceList;
	}
	/** List of WatermarkPreference enumeration for use in JSP. */
	public List<SelectItem> getWatermarkPreferenceTypes() {
		return watermarkPreferenceList;
	}

	/** List of OrderStatus enumeration. */
	public static List<SelectItem> getOrderStatusList() {
		return orderStatusList;
	}
	/** List of OrderStatus enumeration for use in JSP. */
	public List<SelectItem> getOrderStatusTypes() {
		return orderStatusList;
	}

	/** List of PointOfInterestType enumeration. */
	public static List<SelectItem> getPointOfInterestTypeList() {
		return pointOfInterestTypeList;
	}
	/** non-static version for use in JSP files */
	public List<SelectItem> getPointOfInterestTypes() {
		return pointOfInterestTypeList;
	}

	/** See {@link #productionPhaseList}. */
	public static List<SelectItem> getProductionPhaseList() {
		return productionPhaseList;
	}

	/** List of ProductionType enumeration. */
	public static List<SelectItem> getProductionTypeList() {
		return productionTypeList;
	}
	/** List of ProductionType enumeration for use in JSP. */
	public List<SelectItem> getProductionTypes() {
		return productionTypeList;
	}

	/** List of UserStatus enumeration. */
	public static List<SelectItem> getUserStatusList() {
		return userStatusList;
	}
	/** List of UserStatus enumeration - non-static, for JSP. */
	public List<SelectItem> getUserStatusTypes() {
		return userStatusList;
	}

	/** See {@link #weeklyStatusFilterList}. */
	public static List<SelectItem> getWeeklyStatusFilterList() {
		return weeklyStatusFilterList;
	}

	/** See {@link #workZoneList}. */
	public static List<SelectItem> getWorkZoneList() {
		return workZoneList;
	}

	/** See {@link #documentFlowTypeList}. */
	public static List<SelectItem> getDocumentflowtypelist() {
		return documentFlowTypeList;
	}

	/** See {@link #actionTypeList}. */
	public static List<SelectItem> getDocumentActionTypeList() {
		return documentActionTypeList;
	}

	/** See {@link #documentActionTypeList}. */
	public static List<SelectItem> getActionTypeList() {
		return actionTypeList;
	}

	/** See {@link #taxWageAllocationFrequencyList}. */
	public static List<SelectItem> getTaxWageAllocationFrequencyList() {
		return taxWageAllocationFrequencyList;
	}

	/** See {@link #genderFullList}. non-static, for JSP.*/
	public List<SelectItem> getGenderFullList() {
		return genderFullList;
	}

	/** See {@link #genderShortList}. non-static, for JSP.*/
	public List<SelectItem> getGenderShortList() {
		return genderShortList;
	}

	/** See {@link #genderShortList}. for JSP.*/
	public static List<SelectItem> getPaidAsList() {
		return paidAsTypeList;
	}
}

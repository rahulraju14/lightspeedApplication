package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.text.*;
import java.time.LocalDate;
import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.lightspeedeps.dao.*;
import com.lightspeedeps.type.FormFieldType;
import com.lightspeedeps.util.app.Constants;

/**
	Entity for Form_model_release_print table.  Each one is represents contract
	information between a producer (employer) and a "print model", the employee.
*/
@Entity
//@Table(name = "start_form")
public class FormModelRelease extends Form<FormModelRelease> {

	private static final long serialVersionUID = 4744980758186414931L;

	public static Byte MODEL_RELEASE_VERSION_2019 = 19; // last 2 digit of the years
	public static final Byte DEFAULT_MR_VERSION = MODEL_RELEASE_VERSION_2019;

	public FormModelRelease() {
		super();
		setVersion(DEFAULT_MR_VERSION);
	}

	private String ttcClient;

	private String modelName;

	private String modelSSN;

	private boolean modelAgencyYes = false;

	private String modelAgencyText;

	private boolean modelAgencyNo = false;

	private boolean modelCorporationYes = false;

	private String modelCorporationText;

	private boolean modelCorporationNo = false;

	private boolean corporationFederalYes = false;

	private String corporationFederalId;

	private boolean corporationFederalNo = false;

	private String projectName;

	private String job;

	private String jobPo;

	private String shootDates;

	private String product;

	private String advertiser;

	private String adAgency;

	private String producerName;

	private String photographer;

	private boolean usageYes = false;

	private boolean usageNo = false;

	private boolean unlimited = false;

	private boolean unlimitedExceptOutdoor = false;

	private boolean electricMedia = false;

	private boolean broll = false;

	private boolean directMail = false;

	private boolean collateral = false;

	private boolean ism = false;

	private boolean pr = false;

	private boolean print = false;

	private boolean circular = false;

	private boolean industrial = false;

	private boolean mediaOther = false;

	private String mediaOtherText;

	private boolean term = false;

	//LS-3147 change type and length
	private String termMonth;

	@JsonDeserialize(using = LocalDateDeserializer.class)
	@JsonSerialize(using = LocalDateSerializer.class)
	private LocalDate termDate;

	@JsonDeserialize(using = LocalDateDeserializer.class)
	@JsonSerialize(using = LocalDateSerializer.class)
	private LocalDate termYear;

	//LS-3208 Add column to save term other text.
	private String termOtherText;

	private boolean unlimitedTime = false;

	private boolean termOther = false;

	private boolean northAmerica = false;

	private boolean europeanUnion = false;

	private boolean southAmerica = false;

	private boolean asia = false;

	private boolean worldWide = false;

	private boolean worldWideElectronicMedia = false;

	private boolean territoryOther = false;

	private String territoryOtherText;

	private boolean optionalReuseYes = false;

	private boolean optionalReuseNo = false;

	private String reuseMedia;

	private String reuseTerm;

	private String reuseTerritory;

	private String reuseFee;

	private boolean compensationYes = false;

	private boolean compensationNo = false;

	private BigDecimal rateForService;

	private boolean perDay = false;

	private boolean perHour = false;

	private BigDecimal serviceHours;

	private boolean rightTerm = false;

	private boolean notrightTerm = false;

	private BigDecimal usage;

	private boolean addlYes = false;

	private boolean addlNo = false;

	private boolean addlOverTime = false;

	private BigDecimal addlPerHour;

	private String addlHours;

	private boolean shootDay = false;

	private BigDecimal shootPerDay;

	private Integer shootPerHours;

	private boolean prepDay = false;

	private BigDecimal prepPerDay;

	private Integer prepPerHours;

	private boolean weatherDay = false;

	private BigDecimal weatherPerDay;

	private Integer weatherHours;

	private boolean intimatesDay = false;

	private BigDecimal intimatesPerDay;

	private Integer intimatesPerHours;

	private boolean travelDayRate = false;

	private BigDecimal travelDayPerDay;

	private Integer travelDayPerHours;

	private boolean nonTaxable = false;

	private boolean taxable = false;

	private String additionalProvisions;

	private String project;

	private String jobModel;

	private String age;

	private String underAge;

	private String guidance;

	private boolean model = false;

	private boolean modelAgency = false;

	private boolean producer = false;

	private boolean guardian = false;

	private String modelPrintName;

	private String producerPrintName;

	private String company;

	private String modelEmail;

	private String producerEmail;

	private String modelPhone;

	private String producerPhone;

	private String modelAgencyName;

	private String modelAgencyStreet;

	private String modelAgencyPhone;

	private String modelAgencyCompany;

	private Address producerAddress;

	private Address modelAddress;

	/** Rate of per diem to be paid per day **/
	private BigDecimal perdiemAmount;

	/** Number of days per diem will be paid */
	private Integer perdiemDays;

	private boolean lodging;

	private boolean mealsIncidentals;

	/** Total of taxable reimbursements LS-3183 */
	private BigDecimal taxableTotal;

	/** Total of non-taxable reimbursements LS-3183 */
	private BigDecimal nonTaxableTotal;

	@Transient
	private Date termRefDate;

	@Transient
	private Date termRefYear;

	private boolean acknowledged = false;

	/** New Field: Title Section > Shoot/Use_Use Only Fields LS-4467 */
	private boolean shootUse = true;

	/** LS-4461 Job location remove, state drop down value */
	private String jobState;

	/** LS-4461 Job location remove, city drop down value */
	private String jobCity;

	/** LS-4460 Agency Percentage -- percentage paid to agency, 0-99% */
	private Integer agentPercentage;

	private boolean employerAttachDoc = false;

	/** LS-4602 Other city name used when Other is selected as city from city drop down*/
	private String otherCityName;

	/** The associated Payroll Start (StartForm) for a production that is using
	 * Model Release forms in place of payroll starts.  LS-*/
	@Transient
	private StartForm startForm;

	/** Model's occupation. */
	private String occupation;

	public Date getTermRefDate() {
		return termRefDate;
	}
	public void setTermRefDate(Date termRefDate) {
		this.termRefDate = termRefDate;
	}

	public Date getTermRefYear() {
		return termRefYear;
	}
	public void setTermRefYear(Date termRefYear) {
		this.termRefYear = termRefYear;
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DateFormat dateFmt = new SimpleDateFormat(Constants.START_FORM_DATE_FORMAT);
		cd = ContactDocumentDAO.getInstance().refresh(cd);
		fieldValues.put(FormFieldType.MR_TTC_CLIENT.name(),getTtcClient());
		fieldValues.put(FormFieldType.MR_MODEL_NAME.name(), getModelName());
		String viewSSNNo="";
		if(getModelSSN() != null) {
			if (getModelSSN().length() >= 4) {
				viewSSNNo = "###-##-" + getModelSSN().substring(getModelSSN().length()-4);
			}
			else {
				viewSSNNo = getModelSSN();
			}
		}
		fieldValues.put(FormFieldType.MR_MODEL_SSN.name(), viewSSNNo);
		fieldValues.put(FormFieldType.MR_MODEL_AGENCY_YES.name(), (getModelAgencyYes() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_MODEL_AGENCY_TEXT.name(), getModelAgencyText());
		fieldValues.put(FormFieldType.MR_MODEL_AGENCY_NO.name(), (getModelAgencyNo() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_MODEL_CORPORATION_YES.name(), (getModelCorporationYes() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_MODEL_CORPORATION_TEXT.name(), getModelCorporationText());
		fieldValues.put(FormFieldType.MR_MODEL_CORPORATION_NO.name(), (getModelCorporationNo() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_MODEL_CORPORATION_FEDERAL.name(), (getCorporationFederalYes() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_CORPORATION_FEDERAL_ID.name(), getCorporationFederalId());
		fieldValues.put(FormFieldType.MR_CORPORATION_FEDERAL_NO.name(), (getCorporationFederalNo() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_PRJECT_NAME.name(), getProjectName());
		fieldValues.put(FormFieldType.MR_JOB.name(), getJob());
		fieldValues.put(FormFieldType.MR_JB_PO.name(), getJobPo());
		fieldValues.put(FormFieldType.MR_SHOOT_DATES.name(), getShootDates());
		fieldValues.put(FormFieldType.MR_PRODUCT.name(), getProduct());
		fieldValues.put(FormFieldType.MR_ADVERTISER.name(), getAdvertiser());
		fieldValues.put(FormFieldType.MR_AD_AGENCY.name(), getAdAgency());
		fieldValues.put(FormFieldType.MR_PRODUCER_NAME.name(), getProducerName());
		fieldValues.put(FormFieldType.MR_PHOTOGRAPHER.name(), getPhotographer());
		if (getJobCity() != null && getJobState() != null) {
			String cityState = getJobCity() + ", " + getJobState();
			if ("Other".equalsIgnoreCase(getJobCity())) {
				cityState += ": Other City Name: " + getOtherCityName();
			}
			fieldValues.put(FormFieldType.MR_JB_LOCATION.name(), cityState);
		}
		fieldValues.put(FormFieldType.MR_USGE_YES.name(), (getUsageYes() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_USGE_NO.name(), (getUsageNo() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_UNLIMITED.name(), (getUnlimited() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_UNLMTD_EXCEPT_OUTDOOR.name(), (getUnlimitedExceptOutdoor() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_ELECTRIC_MEDIA.name(), (getElectricMedia() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_BROLL.name(), (getBroll() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_DIRECT_MAIL.name(), (getDirectMail() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_COLLATERAL.name(), (getCollateral() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_ISM.name(), (getIsm() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_PR_DATA.name(), (getPr() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_PRINT.name(), (getPrint() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_CIRCULAR.name(), (getCircular() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_INDUSTRIAL.name(), (getIndustrial() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_MEDIA_OTHER.name(), (getMediaOther() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_MEDIA_OTR_TEXT.name(), getMediaOtherText());
		fieldValues.put(FormFieldType.MR_TERM.name(), (getTerm() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_TRM_MONTH.name(), getTermMonth());
		fieldValues.put(FormFieldType.MR_TRM_DATE.name(), dateFormat(dateFmt,getTermDate()));
		fieldValues.put(FormFieldType.MR_TRM_YEAR.name(), dateFormat(dateFmt,getTermDate()));
		fieldValues.put(FormFieldType.MR_UNLMTD_TIME.name(), (getUnlimitedTime() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_TRM_OHER.name(), (getTermOther() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_NORTH_AMERICA.name(), (getNorthAmerica() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_EUROPEAN_UNION.name(), (getEuropeanUnion() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_SOUTH_AMERICA.name(), (getSouthAmerica() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_ASIA.name(), (getAsia() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_WORLD_WIDE.name(), (getWorldWide() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_WRLD_WIDE_ELECTRONIC_MEDIA.name(), (getWorldWideElectronicMedia() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_TERRITORY_OTHER.name(), (getTerritoryOther() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_TERRITRY_OTHER_TEXT.name(), getTerritoryOtherText());
		fieldValues.put(FormFieldType.MR_OPTIONAL_REUSE_YES.name(), (getOptionalReuseYes() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_OPTIONAL_REUSE_NO.name(), (getOptionalReuseNo() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_REUSE_MEDIA.name(), getReuseMedia());
		fieldValues.put(FormFieldType.MR_REUSE_TERM.name(), getReuseTerm());
		fieldValues.put(FormFieldType.MR_REUSE_TERRITORY.name(), getReuseTerritory());
		fieldValues.put(FormFieldType.MR_REUSE_FEE.name(), getReuseFee());
		fieldValues.put(FormFieldType.MR_REUSE_TERRITORY.name(), getReuseTerritory());
		fieldValues.put(FormFieldType.MR_COMPENSATION_YES.name(), (getCompensationYes() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_COMPENSATION_NO.name(), (getCompensationNo() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_RATE_FOR_SERVICE.name(), bigDecimalFormat(getRateForService()));
		fieldValues.put(FormFieldType.MR_PER_DAY.name(), (getPerDay() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_PER_HOUR.name(), (getPerHour() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_SERVICE_HOURS.name(), bigDecimalFormat(getServiceHours()));
		fieldValues.put(FormFieldType.MR_RIGHT_TERM.name(), (getRightTerm() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_NOT_RIGHT_TERM.name(), (getNotrightTerm() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_USAGE.name(), bigDecimalFormat(getUsage()));
		fieldValues.put(FormFieldType.MR_ADDL_YES.name(), (getAddlYes() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_ADDL_NO.name(), (getAddlNo() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_ADDL_OVER_TIME.name(), (getAddlOverTime() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_ADDL_PER_HOUR.name(), bigDecimalFormat(getAddlPerHour()));
		fieldValues.put(FormFieldType.MR_ADDL_HOURS.name(), getAddlHours());
		fieldValues.put(FormFieldType.MR_SHOOT_DAY.name(), (getShootDay()? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_SHOOT_PER_DAY.name(), bigDecimalFormat(getShootPerDay()));
		fieldValues.put(FormFieldType.MR_SHOOT_PER_HOURS.name(), intFormat(getShootPerHours()));
		fieldValues.put(FormFieldType.MR_PREP_DAY.name(), (getPrepDay() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_PREP_PER_DAY.name(), bigDecimalFormat(getPrepPerDay()));
		fieldValues.put(FormFieldType.MR_PREP_PER_HOURS.name(), intFormat(getPrepPerHours()));
		fieldValues.put(FormFieldType.MR_WEATHER_DAY.name(), (getWeatherDay() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_WEATHER_PER_DAY.name(), bigDecimalFormat(getWeatherPerDay()));
		fieldValues.put(FormFieldType.MR_WEATHER_HOURS.name(), intFormat(getWeatherHours()));
		fieldValues.put(FormFieldType.MR_INITIMATES_DAY.name(), (getIntimatesDay() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_INITIMATE_PER_DAY.name(), bigDecimalFormat(getIntimatesPerDay()));
		fieldValues.put(FormFieldType.MR_INTIMATE_PER_HOURS.name(), intFormat(getIntimatesPerHours()));
		fieldValues.put(FormFieldType.MR_TRAVEL_DAY_RATE.name(), (getTravelDayRate() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_TRAVEL_DAY_PER_DAY.name(), bigDecimalFormat(getTravelDayPerDay()));
		fieldValues.put(FormFieldType.MR_TRAVEL_DAY_PER_HOURS.name(), intFormat(getTravelDayPerHours()));
		fieldValues.put(FormFieldType.MR_NON_TAXABLE.name(), (getNonTaxable() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_TAXABLE.name(), (getTaxable() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_ADDITIONAL_PROVISIONS.name(), getAdditionalProvisions());
		fieldValues.put(FormFieldType.MR_PROJECT.name(), getProject());
		fieldValues.put(FormFieldType.MR_JB_MODEL.name(), getJobModel());
		fieldValues.put(FormFieldType.MR_AGE.name(), getAge());
		fieldValues.put(FormFieldType.MR_UNDER_AGE.name(), getUnderAge());
		fieldValues.put(FormFieldType.MR_GUIDANCE.name(), getGuidance());
		fieldValues.put(FormFieldType.MR_MDEL.name(), (getModel() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_MODEL_AGNCY.name(), (getModelAgency() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_PRODCER.name(), (getProducer() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_GUARDIAN.name(), (getGuardian() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_MODEL_PRINT_NAME.name(), getModelPrintName());
		fieldValues.put(FormFieldType.MR_PRODUCER_PRINT_NAME.name(), getProducerPrintName());
		//fieldValues.put(FormFieldType.MR_MODEL_DATE.name(), dateFormat(dateFmt,getModelDate()));
		//fieldValues.put(FormFieldType.MR_PRODUCER_DATE.name(), dateFormat(dateFmt,getProducerDate()));
		fieldValues.put(FormFieldType.MR_COMPANY.name(), getCompany());
		fieldValues.put(FormFieldType.MR_PRODUCER_EMAIL.name(), getProducerEmail());
		fieldValues.put(FormFieldType.MR_MODEL_EMAIL.name(), getModelEmail());
		fieldValues.put(FormFieldType.MR_MODEL_PHONE.name(), getModelPhone());
		fieldValues.put(FormFieldType.MR_PRODUCER_PHONE.name(), getProducerPhone());
		fieldValues.put(FormFieldType.MR_MODEL_AGENCY_NAME.name(), getModelAgencyText());
		fieldValues.put(FormFieldType.MR_MODEL_AGENCY_STREET.name(), getModelAgencyStreet());
		fieldValues.put(FormFieldType.MR_MODEL_AGENCY_PHONE.name(), getModelAgencyPhone());
		fieldValues.put(FormFieldType.MR_MODEL_AGENCY_COMPANY.name(), getModelAgencyCompany());
		fieldValues.put(FormFieldType.MR_MODEL_ADDRESS.name(), getModelAddress().getAddrLine1());
		fieldValues.put(FormFieldType.MR_PRODUCER_ADDRESS.name(), getProducerAddress().getAddrLine1());
		fieldValues.put(FormFieldType.MR_PRODUCER_COMPANY.name(), getModelAgencyCompany());
		// LS-3223 new fields added to pdf.
		fieldValues.put(FormFieldType.MR_NON_TAXABLE_TOTAL.name(), bigDecimalFormat(getNonTaxableTotal()));
		fieldValues.put(FormFieldType.MR_TAX_TOTAL.name(), bigDecimalFormat(getTaxableTotal()));
		fieldValues.put(FormFieldType.MR_MEALS_INCIDENTALS.name(), getMealsIncidentals() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.MR_LODGING.name(), getLodging() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.MR_PER_DIEM_RATE.name(), bigDecimalFormat(getPerdiemAmount()));
		fieldValues.put(FormFieldType.MR_PER_DIEM_DAYS.name(), intFormat(getPerdiemDays()));
		fieldValues.put(FormFieldType.MR_TRM_OHR_TEXT.name(), getTermOtherText());
		fieldValues.put(FormFieldType.MR_ACKNOWLEDGED.name(), (isAcknowledged() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MR_AGENT_PERCENTAGE.name(), intFormat(getAgentPercentage()));
		fieldValues.put(FormFieldType.MR_OCCUPATION.name(), getOccupation());
		if (isShootUse()) {
			fieldValues.put(FormFieldType.MR_SHOOTUSE.name(), (isShootUse() ? "Yes" : "Off"));
		}
		else {
			fieldValues.put(FormFieldType.MR_USEONLY.name(), (isShootUse() ? "Yes" : "Off"));
		}

		if (cd.getEmpSignature() != null) {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
					cd.getEmpSignature().getSignedBy() + "\n" + cd.getEmpSignature().getUuid());
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(),
					dateFormat(dateFmt, cd.getEmpSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), "");
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), "");
		}

		if (cd.getEmployerSignature() != null) {
			fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
					cd.getEmployerSignature().getSignedBy() + "\n" + cd.getEmployerSignature().getUuid());
			fieldValues.put(FormFieldType.EMP_SIGNATURE_DATE.name(),
					dateFormat(dateFmt, cd.getEmployerSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), "");
			fieldValues.put(FormFieldType.EMP_SIGNATURE_DATE.name(), "");
		}
	}

	/** See {@link #ttcClient}. */
	@Column(name = "TTC_Client",length = 30)
	public String getTtcClient() {
		return ttcClient;
	}
	/** See {@link #modelSSN}. */
	public void setTtcClient(String ttcClient) {
		this.ttcClient = ttcClient;
	}

	/** See {@link #modelName}. */
	@Column(name = "Model_Name", length = 30)
	public String getModelName() {
		return modelName;
	}
	/** See {@link #modelName}. */
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	/** See {@link #modelSSN}. */
	@Column(name = "Model_SSN", nullable = true)
	public String getModelSSN() {
		return modelSSN;
	}
	/** See {@link #modelSSN}. */
	public void setModelSSN(String modelSSN) {
		this.modelSSN = modelSSN;
	}

	/** See {@link #modelAgencyYes}. */
	@Column(name = "Model_Agency_Yes", nullable = true)
	public boolean getModelAgencyYes() {
		return modelAgencyYes;
	}
	/** See {@link #modelAgencyYes}. */
	public void setModelAgencyYes(boolean modelAgencyYes) {
		this.modelAgencyYes = modelAgencyYes;
	}

	/** See {@link #modelAgencyText}. */
	@Column(name = "Model_Agency_Text", length = 30)
	public String getModelAgencyText() {
		return modelAgencyText;
	}
	/** See {@link #modelAgencyText}. */
	public void setModelAgencyText(String modelAgencyText) {
		this.modelAgencyText = modelAgencyText;
	}

	/** See {@link #modelAgencyNo}. */
	@Column(name = "Model_Agency_No", nullable = true)
	public boolean getModelAgencyNo() {
		return modelAgencyNo;
	}
	/** See {@link #modelAgencyNo}. */
	public void setModelAgencyNo(boolean modelAgencyNo) {
		this.modelAgencyNo = modelAgencyNo;
	}

	/** See {@link #modelCorporationYes}. */
	@Column(name = "Model_Corporation_Yes", nullable = true)
	public boolean getModelCorporationYes() {
		return modelCorporationYes;
	}
	/** See {@link #modelCorporationYes}. */
	public void setModelCorporationYes(boolean modelCorporationYes) {
		this.modelCorporationYes = modelCorporationYes;
	}

	/** See {@link #modelCorporationText}. */
	@Column(name = "Model_Corporation_Text", length = 30)
	public String getModelCorporationText() {
		return modelCorporationText;
	}
	/** See {@link #modelCorporationText}. */
	public void setModelCorporationText(String modelCorporationText) {
		this.modelCorporationText = modelCorporationText;
	}

	/** See {@link #modelCorporationNo}. */
	@Column(name = "Model_Corporation_No", nullable = true)
	public boolean getModelCorporationNo() {
		return modelCorporationNo;
	}
	/** See {@link #modelCorporationNo}. */
	public void setModelCorporationNo(boolean modelCorporationNo) {
		this.modelCorporationNo = modelCorporationNo;
	}

	/** See {@link #corporationFederalYes}. */
	@Column(name = "Corporation_Federal_Yes", nullable = true)
	public boolean getCorporationFederalYes() {
		return corporationFederalYes;
	}
	/** See {@link #corporationFederalYes}. */
	public void setCorporationFederalYes(boolean corporationFederalYes) {
		this.corporationFederalYes = corporationFederalYes;
	}

	/** See {@link #corporationFederalId}. */
	@Column(name = "Corporation_Federal_Id", nullable = true)
	public String getCorporationFederalId() {
		return corporationFederalId;
	}
	/** See {@link #corporationfederalId}. */
	public void setCorporationFederalId(String corporationFederalId) {
		this.corporationFederalId = corporationFederalId;
	}

	/** See {@link #corporationFederalNo}. */
	@Column(name = "Corporation_Federal_No", nullable = true)
	public boolean getCorporationFederalNo() {
		return corporationFederalNo;
	}
	/** See {@link #corporationFederalNo}. */
	public void setCorporationFederalNo(boolean corporationFederalNo) {
		this.corporationFederalNo = corporationFederalNo;
	}

	/** See {@link #projectName}. */
	@Column(name = "Project_Name", length = 20)
	public String getProjectName() {
		return projectName;
	}
	/** See {@link #projectName}. */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	/** See {@link #job}. */
	@Column(name = "Job", length = 15)
	public String getJob() {
		return job;
	}
	/** See {@link #job}. */
	public void setJob(String job) {
		this.job = job;
	}

	/** See {@link #jobPo}. */
	@Column(name = "JobPO", length = 60)
	public String getJobPo() {
		return jobPo;
	}
	/** See {@link #po}. */
	public void setJobPo(String jobPo) {
		this.jobPo = jobPo;
	}

	/** See {@link #shootDates}. */
	@Column(name = "Shoot_Date", length = 60)
	public String getShootDates() {
		return shootDates;
	}
	/** See {@link #shootDates}. */
	public void setShootDates(String shootDates) {
		this.shootDates = shootDates;
	}

	/** See {@link #product}. */
	@Column(name = "Product", length = 8)
	public String getProduct() {
		return product;
	}
	/** See {@link #product}. */
	public void setProduct(String product) {
		this.product = product;
	}

	/** See {@link #advertiser}. */
	@Column(name = "Advertiser", length = 30)
	public String getAdvertiser() {
		return advertiser;
	}
	/** See {@link #advertiser}. */
	public void setAdvertiser(String advertiser) {
		this.advertiser = advertiser;
	}

	/** See {@link #adAgency}. */
	@Column(name = "Ad_Agency", length = 30)
	public String getAdAgency() {
		return adAgency;
	}
	/** See {@link #adAgency}. */
	public void setAdAgency(String adAgency) {
		this.adAgency = adAgency;
	}

	/** See {@link #producerName}. */
	@Column(name = "Producer_Name", length = 30)
	public String getProducerName() {
		return producerName;
	}
	/** See {@link #producerName}. */
	public void setProducerName(String producerName) {
		this.producerName = producerName;
	}

	/** See {@link #photographer}. */
	@Column(name = "Photographer", length = 16)
	public String getPhotographer() {
		return photographer;
	}
	/** See {@link #photographer}. */
	public void setPhotographer(String photographer) {
		this.photographer = photographer;
	}

	/** See {@link #usageYes}. */
	@Column(name = "Usage_Yes", nullable = true)
	public boolean getUsageYes() {
		return usageYes;
	}
	/** See {@link #usageYes}. */
	public void setUsageYes(boolean usageYes) {
		this.usageYes = usageYes;
	}

	/** See {@link #usageNo}. */
	@Column(name = "Usage_No", nullable = true)
	public boolean getUsageNo() {
		return usageNo;
	}
	/** See {@link #usageNo}. */
	public void setUsageNo(boolean usageNo) {
		this.usageNo = usageNo;
	}

	/** See {@link #unlimited}. */
	@Column(name = "Unlimited", nullable = true)
	public boolean getUnlimited() {
		return unlimited;
	}
	/** See {@link #unlimited}. */
	public void setUnlimited(boolean unlimited) {
		this.unlimited = unlimited;
	}

	/** See {@link #unlimitedExceptOutdoor}. */
	@Column(name = "Unlimited_Except_Outdoor", nullable = true)
	public boolean getUnlimitedExceptOutdoor() {
		return unlimitedExceptOutdoor;
	}
	/** See {@link #unlimitedExceptOutdoor}. */
	public void setUnlimitedExceptOutdoor(boolean unlimitedExceptOutdoor) {
		this.unlimitedExceptOutdoor = unlimitedExceptOutdoor;
	}

	/** See {@link #electricMedia}. */
	@Column(name = "Electric_Media", nullable = true)
	public boolean getElectricMedia() {
		return electricMedia;
	}
	/** See {@link #electricMedia}. */
	public void setElectricMedia(boolean electricMedia) {
		this.electricMedia = electricMedia;
	}

	/** See {@link #broll}. */
	@Column(name = "B_Roll", nullable = true)
	public boolean getBroll() {
		return broll;
	}
	/** See {@link #broll}. */
	public void setBroll(boolean broll) {
		this.broll = broll;
	}

	/** See {@link #directMail}. */
	@Column(name = "Direct_Mail", nullable = true)
	public boolean getDirectMail() {
		return directMail;
	}
	/** See {@link #directMail}. */
	public void setDirectMail(boolean directMail) {
		this.directMail = directMail;
	}

	/** See {@link #collateral}. */
	@Column(name = "Collateral", nullable = true)
	public boolean getCollateral() {
		return collateral;
	}
	/** See {@link #collateral}. */
	public void setCollateral(boolean collateral) {
		this.collateral = collateral;
	}

	/** See {@link #ism}. */
	@Column(name = "ISM", nullable = true)
	public boolean getIsm() {
		return ism;
	}
	/** See {@link #ism}. */
	public void setIsm(boolean ism) {
		this.ism = ism;
	}

	/** See {@link #pr}. */
	@Column(name = "PR", nullable = true)
	public boolean getPr() {
		return pr;
	}
	/** See {@link #pr}. */
	public void setPr(boolean pr) {
		this.pr = pr;
	}

	/** See {@link #print}. */
	@Column(name = "Print", nullable = true)
	public boolean getPrint() {
		return print;
	}
	/** See {@link #print}. */
	public void setPrint(boolean print) {
		this.print = print;
	}

	/** See {@link #circular}. */
	@Column(name = "Circular", nullable = true)
	public boolean getCircular() {
		return circular;
	}
	/** See {@link #circular}. */
	public void setCircular(boolean circular) {
		this.circular = circular;
	}

	/** See {@link #industrial}. */
	@Column(name = "Industrial", nullable = true)
	public boolean getIndustrial() {
		return industrial;
	}
	/** See {@link #industrial}. */
	public void setIndustrial(boolean industrial) {
		this.industrial = industrial;
	}

	/** See {@link #mediaOther}. */
	@Column(name = "Media_Other", nullable = true)
	public boolean getMediaOther() {
		return mediaOther;
	}
	/** See {@link #mediaOther}. */
	public void setMediaOther(boolean mediaOther) {
		this.mediaOther = mediaOther;
	}

	/** See {@link #mediaOtherText}. */
	@Column(name = "Media_Other_Text", length = 20)
	public String getMediaOtherText() {
		return mediaOtherText;
	}
	/** See {@link #mediaOtherText}. */
	public void setMediaOtherText(String mediaOtherText) {
		this.mediaOtherText = mediaOtherText;
	}

	/** See {@link #term}. */
	@Column(name = "Term", nullable = true)
	public boolean getTerm() {
		return term;
	}
	/** See {@link #term}. */
	public void setTerm(boolean term) {
		this.term = term;
	}

	/** See {@link #termMonth}. */
	@Column(name = "Term_Month", length = 16)
	public String getTermMonth() {
		return termMonth;
	}
	/** See {@link #termMonth}. */
	public void setTermMonth(String termMonth) {
		this.termMonth = termMonth;
	}

	/** See {@link #termDate}. */
//	@Temporal(TemporalType.DATE)
	@Column(name = "Term_Date", nullable = true, length = 0)
	public LocalDate getTermDate() {
		return termDate;
	}
	/** See {@link #termDate}. */
	public void setTermDate(LocalDate termDate) {
		this.termDate = termDate;
	}

	/** See {@link #termYear}. */
	@Column(name = "Term_Year", nullable = true)
	public LocalDate getTermYear() {
		return termYear;
	}
	/** See {@link #termYear}. */
	public void setTermYear(LocalDate termYear) {
		this.termYear = termYear;
	}

	/** See {@link #termOtherText}. */
	@Column(name = "Term_Other_Text", length = 16)
	public String getTermOtherText() {
		return termOtherText;
	}
	/** See {@link #termOtherText}. */
	public void setTermOtherText(String termOtherText) {
		this.termOtherText = termOtherText;
	}

	/** See {@link #unlimitedTime}. */
	@Column(name = "Unlimited_Time", nullable = true)
	public boolean getUnlimitedTime() {
		return unlimitedTime;
	}
	/** See {@link #unlimitedTime}. */
	public void setUnlimitedTime(boolean unlimitedTime) {
		this.unlimitedTime = unlimitedTime;
	}

	/** See {@link #termOther}. */
	@Column(name = "Term_Other", nullable = true)
	public boolean getTermOther() {
		return termOther;
	}
	/** See {@link #termOther}. */
	public void setTermOther(boolean termOther) {
		this.termOther = termOther;
	}


	/** See {@link #northAmerica}. */
	@Column(name = "North_America", nullable = true)
	public boolean getNorthAmerica() {
		return northAmerica;
	}
	/** See {@link #northAmerica}. */
	public void setNorthAmerica(boolean northAmerica) {
		this.northAmerica = northAmerica;
	}

	/** See {@link #europeanUnion}. */
	@Column(name = "European_Union", nullable = true)
	public boolean getEuropeanUnion() {
		return europeanUnion;
	}
	/** See {@link #europeanUnion}. */
	public void setEuropeanUnion(boolean europeanUnion) {
		this.europeanUnion = europeanUnion;
	}

	/** See {@link #southAmerica}. */
	@Column(name = "South_America", nullable = true)
	public boolean getSouthAmerica() {
		return southAmerica;
	}
	/** See {@link #southAmerica}. */
	public void setSouthAmerica(boolean southAmerica) {
		this.southAmerica = southAmerica;
	}

	/** See {@link #asia}. */
	@Column(name = "Asia", nullable = true)
	public boolean getAsia() {
		return asia;
	}
	/** See {@link #asia}. */
	public void setAsia(boolean asia) {
		this.asia = asia;
	}

	/** See {@link #worldWide}. */
	@Column(name = "World_Wide", nullable = true)
	public boolean getWorldWide() {
		return worldWide;
	}
	/** See {@link #worldWide}. */
	public void setWorldWide(boolean worldWide) {
		this.worldWide = worldWide;
	}

	/** See {@link #worldWideElectronicMedia}. */
	@Column(name = "World_Wide_Electronic_Media", nullable = true)
	public boolean getWorldWideElectronicMedia() {
		return worldWideElectronicMedia;
	}
	/** See {@link #worldWideElectronicMedia}. */
	public void setWorldWideElectronicMedia(boolean worldWideElectronicMedia) {
		this.worldWideElectronicMedia = worldWideElectronicMedia;
	}

	/** See {@link #territoryOther}. */
	@Column(name = "Territory_Other", nullable = true)
	public boolean getTerritoryOther() {
		return territoryOther;
	}
	/** See {@link #territoryOther}. */
	public void setTerritoryOther(boolean territoryOther) {
		this.territoryOther = territoryOther;
	}

	/** See {@link #territoryOtherText}. */
	@Column(name = "Territory_Other_Text", nullable = true, length = 16)
	public String getTerritoryOtherText() {
		return territoryOtherText;
	}
	/** See {@link #territoryOtherText}. */
	public void setTerritoryOtherText(String territoryOtherText) {
		this.territoryOtherText = territoryOtherText;
	}

	/** See {@link #optionalReuseYes}. */
	@Column(name = "Optional_Reuse_Yes", nullable = true)
	public boolean getOptionalReuseYes() {
		return optionalReuseYes;
	}

	public void setOptionalReuseYes(boolean optionalReuseYes) {
		this.optionalReuseYes = optionalReuseYes;
	}

	/** See {@link #optionalReuseNo}. */
	@Column(name = "Optional_Reuse_No", nullable = true)
	public boolean getOptionalReuseNo() {
		return optionalReuseNo;
	}
	/** See {@link #optionalReuseNo}. */
	public void setOptionalReuseNo(boolean optionalReuseNo) {
		this.optionalReuseNo = optionalReuseNo;
	}

	/** See {@link #reuseMedia}. */
	@Column(name = "Reuse_Media", nullable = true, length = 20)
	public String getReuseMedia() {
		return reuseMedia;
	}
	/** See {@link #reuseMedia}. */
	public void setReuseMedia(String reuseMedia) {
		this.reuseMedia = reuseMedia;
	}

	/** See {@link #reuseTerm}. */
	@Column(name = "Reuse_Term", nullable = true, length = 16)
	public String getReuseTerm() {
		return reuseTerm;
	}
	/** See {@link #reuseTerm}. */
	public void setReuseTerm(String reuseTerm) {
		this.reuseTerm = reuseTerm;
	}

	/** See {@link #reuseTerritory}. */
	@Column(name = "Rate_Use_Territory", nullable = true, length = 16)
	public String getReuseTerritory() {
		return reuseTerritory;
	}
	/** See {@link #reuseTerritory}. */
	public void setReuseTerritory(String reuseTerritory) {
		this.reuseTerritory = reuseTerritory;
	}

	/** See {@link #reuseFee}. */
	@Column(name = "Rate_Use_Fee")
	public String getReuseFee() {
		return reuseFee;
	}
	/** See {@link #reuseFee}. */
	public void setReuseFee(String reuseFee) {
		this.reuseFee = reuseFee;
	}

	/** See {@link #compensationYes}. */
	@Column(name = "Compensation_Yes", nullable = true)
	public boolean getCompensationYes() {
		return compensationYes;
	}
	/** See {@link #compensationYes}. */
	public void setCompensationYes(boolean compensationYes) {
		this.compensationYes = compensationYes;
	}

	/** See {@link #compensationNo}. */
	@Column(name = "Compensation_No", nullable = true)
	public boolean getCompensationNo() {
		return compensationNo;
	}
	/** See {@link #compensationNo}. */
	public void setCompensationNo(boolean compensationNo) {
		this.compensationNo = compensationNo;
	}

	/** See {@link #rateForService}. */
	@Column(name = "Rate_For_Service")
	public BigDecimal getRateForService() {
		return rateForService;
	}
	/** See {@link #rateForService}. */
	public void setRateForService(BigDecimal rateForService) {
		this.rateForService = rateForService;
	}

	/** See {@link #perDay}. */
	@Column(name = "Per_Day", nullable = true)
	public boolean getPerDay() {
		return perDay;
	}
	/** See {@link #perDay}. */
	public void setPerDay(boolean perDay) {
		this.perDay = perDay;
	}

	/** See {@link #perHour}. */
	@Column(name = "Per_Hour", nullable = true)
	public boolean getPerHour() {
		return perHour;
	}
	/** See {@link #perHour}. */
	public void setPerHour(boolean perHour) {
		this.perHour = perHour;
	}

	/** See {@link #serviceHour}. */
	@Column(name = "Service_Hour")
	public BigDecimal getServiceHours() {
		return serviceHours;
	}
	/** See {@link #serviceHour}. */
	public void setServiceHours(BigDecimal serviceHours) {
		this.serviceHours = serviceHours;
	}

	/** See {@link #rightTerm}. */
	@Column(name = "Right_Term", nullable = true)
	public boolean getRightTerm() {
		return rightTerm;
	}
	/** See {@link #rightTerm}. */
	public void setRightTerm(boolean rightTerm) {
		this.rightTerm = rightTerm;
	}

	/** See {@link #notrightTerm}. */
	@Column(name = "Not_Right_Term", nullable = true)
	public boolean getNotrightTerm() {
		return notrightTerm;
	}
	/** See {@link #notrightTerm}. */
	public void setNotrightTerm(boolean notrightTerm) {
		this.notrightTerm = notrightTerm;
	}

	/** See {@link #usage}. */
	@Column(name = "Usage_30days")
	public BigDecimal getUsage() {
		return usage;
	}
	/** See {@link #usage}. */
	public void setUsage(BigDecimal usage) {
		this.usage = usage;
	}

	/** See {@link #addlYes}. */
	@Column(name = "Addl_Yes", nullable = true)
	public boolean getAddlYes() {
		return addlYes;
	}
	/** See {@link #addlYes}. */
	public void setAddlYes(boolean addlYes) {
		this.addlYes = addlYes;
	}

	/** See {@link #addlNo}. */
	@Column(name = "Addl_No", nullable = true)
	public boolean getAddlNo() {
		return addlNo;
	}
	/** See {@link #addlNo}. */
	public void setAddlNo(boolean addlNo) {
		this.addlNo = addlNo;
	}

	/** See {@link #addlOverTime}. */
	@Column(name = "Addl_Over_Time", nullable = true)
	public boolean getAddlOverTime() {
		return addlOverTime;
	}
	/** See {@link #addlOverTime}. */
	public void setAddlOverTime(boolean addlOverTime) {
		this.addlOverTime = addlOverTime;
	}

	/** See {@link #addlPerHour}. */
	@Column(name = "Addl_Per_Hour", precision = 13, scale = 5)
	public BigDecimal getAddlPerHour() {
		return addlPerHour;
	}
	/** See {@link #addlPerHour}. */
	public void setAddlPerHour(BigDecimal addlPerHour) {
		this.addlPerHour = addlPerHour;
	}

	/** See {@link #addlHours}. */
	@Column(name = "Addl_Hour", nullable = true, length = 2)
	public String getAddlHours() {
		return addlHours;
	}
	/** See {@link #addlHours}. */
	public void setAddlHours(String addlHours) {
		this.addlHours = addlHours;
	}

	/** See {@link #shootDay}. */
	@Column(name = "Shoot_Day", nullable = true)
	public boolean getShootDay() {
		return shootDay;
	}
	/** See {@link #shootDay}. */
	public void setShootDay(boolean shootDay) {
		this.shootDay = shootDay;
	}

	/** See {@link #shootPerDay}. */
	@Column(name = "Shoot_Per_Day")
	public BigDecimal getShootPerDay() {
		return shootPerDay;
	}
	/** See {@link #shootPerDay}. */
	public void setShootPerDay(BigDecimal shootPerDay) {
		this.shootPerDay = shootPerDay;
	}

	/** See {@link #shootPerHours}. */
	@Column(name = "Shoot_Per_Hour", nullable = true, length = 2)
	public Integer getShootPerHours() {
		return shootPerHours;
	}
	/** See {@link #shootPerHours}. */
	public void setShootPerHours(Integer shootPerHours) {
		this.shootPerHours = shootPerHours;
	}

	/** See {@link #prepDay}. */
	@Column(name = "Prep_Day", nullable = true)
	public boolean getPrepDay() {
		return prepDay;
	}
	/** See {@link #prepDay}. */
	public void setPrepDay(boolean prepDay) {
		this.prepDay = prepDay;
	}

	/** See {@link #prepPerDay}. */
	@Column(name = "Prep_Per_Day")
	public BigDecimal getPrepPerDay() {
		return prepPerDay;
	}
	/** See {@link #prepPerDay}. */
	public void setPrepPerDay(BigDecimal prepPerDay) {
		this.prepPerDay = prepPerDay;
	}

	/** See {@link #prepPerHours}. */
	@Column(name = "Prep_Per_Hour", nullable = true, length = 2)
	public Integer getPrepPerHours() {
		return prepPerHours;
	}
	/** See {@link #prepPerHours}. */
	public void setPrepPerHours(Integer prepPerHours) {
		this.prepPerHours = prepPerHours;
	}

	/** See {@link #weatherDay}. */
	@Column(name = "Weather_Day", nullable = true)
	public boolean getWeatherDay() {
		return weatherDay;
	}
	/** See {@link #weatherDay}. */
	public void setWeatherDay(boolean weatherDay) {
		this.weatherDay = weatherDay;
	}

	/** See {@link #weatherPerDay}. */
	@Column(name = "Weather_Per_Day")
	public BigDecimal getWeatherPerDay() {
		return weatherPerDay;
	}
	/** See {@link #weatherPerDay}. */
	public void setWeatherPerDay(BigDecimal weatherPerDay) {
		this.weatherPerDay = weatherPerDay;
	}

	/** See {@link #weatherHours}. */
	@Column(name = "Weather_Hour", nullable = true, length = 2)
	public Integer getWeatherHours() {
		return weatherHours;
	}
	/** See {@link #weatherHours}. */
	public void setWeatherHours(Integer weatherHours) {
		this.weatherHours = weatherHours;
	}

	/** See {@link #initimatesDay}. */
	@Column(name = "Intimates_Day", nullable = true)
	public boolean getIntimatesDay() {
		return intimatesDay;
	}
	/** See {@link #intimatesDay}. */
	public void setIntimatesDay(boolean intimatesDay) {
		this.intimatesDay = intimatesDay;
	}

	/** See {@link #intimatePerDay}. */
	@Column(name = "Intimates_Per_Day")
	public BigDecimal getIntimatesPerDay() {
		return intimatesPerDay;
	}
	/** See {@link #intimatesPerDay}. */
	public void setIntimatesPerDay(BigDecimal intimatesPerDay) {
		this.intimatesPerDay = intimatesPerDay;
	}

	/** See {@link #intimatePerHours}. */
	@Column(name = "Intimates_Per_Hour", nullable = true, length = 2)
	public Integer getIntimatesPerHours() {
		return intimatesPerHours;
	}
	/** See {@link #intimatesPerHours}. */
	public void setIntimatesPerHours(Integer initimatesPerHours) {
		intimatesPerHours = initimatesPerHours;
	}

	/** See {@link #travelDayRate}. */
	@Column(name = "Travel_Day_Rate", nullable = true)
	public boolean getTravelDayRate() {
		return travelDayRate;
	}
	/** See {@link #travelDayRate}. */
	public void setTravelDayRate(boolean travelDayRate) {
		this.travelDayRate = travelDayRate;
	}

	/** See {@link #travelDayPerDay}. */
	@Column(name = "Travel_Day_Per_Day")
	public BigDecimal getTravelDayPerDay() {
		return travelDayPerDay;
	}
	/** See {@link #travelDayPerDay}. */
	public void setTravelDayPerDay(BigDecimal travelDayPerDay) {
		this.travelDayPerDay = travelDayPerDay;
	}

	/** See {@link #travelDayPerHours}. */
	@Column(name = "Travel_Day_Per_Hour", nullable = true, length = 2)
	public Integer getTravelDayPerHours() {
		return travelDayPerHours;
	}
	/** See {@link #travelDayPerHours}. */
	public void setTravelDayPerHours(Integer travelDayPerHours) {
		this.travelDayPerHours = travelDayPerHours;
	}

	/** See {@link #nonTaxable}. */
	@Column(name = "Non_Taxable", nullable = true)
	public boolean getNonTaxable() {
		return nonTaxable;
	}
	/** See {@link #nonTaxable}. */
	public void setNonTaxable(boolean nonTaxable) {
		this.nonTaxable = nonTaxable;
	}

	/** See {@link #taxable}. */
	@Column(name = "Taxable", nullable = true)
	public boolean getTaxable() {
		return taxable;
	}
	/** See {@link #taxable}. */
	public void setTaxable(boolean taxable) {
		this.taxable = taxable;
	}

	/** See {@link #additionalProvisions}. */
	@Column(name = "Additional_Provision", nullable = true, length = 300)
	public String getAdditionalProvisions() {
		return additionalProvisions;
	}
	/** See {@link #additionalProvisions}. */
	public void setAdditionalProvisions(String additionalProvisions) {
		this.additionalProvisions = additionalProvisions;
	}

	/** See {@link #project}. */
	@Column(name = "Project", nullable = true, length = 30)
	public String getProject() {
		return project;
	}
	/** See {@link #project}. */
	public void setProject(String project) {
		this.project = project;
	}

	/** See {@link #jobModel}. */
	@Column(name = "Job_Model", nullable = true, length = 60)
	public String getJobModel() {
		return jobModel;
	}
	/** See {@link #jobModel}. */
	public void setJobModel(String jobModel) {
		this.jobModel = jobModel;
	}

	/** See {@link #age}. */
	@Column(name = "Age", nullable = true, length = 3)
	public String getAge() {
		return age;
	}
	/** See {@link #age}. */
	public void setAge(String age) {
		this.age = age;
	}

	/** See {@link #underAge}. */
	@Column(name = "Under_Age", nullable = true, length = 3)
	public String getUnderAge() {
		return underAge;
	}
	/** See {@link #underAge}. */
	public void setUnderAge(String underAge) {
		this.underAge = underAge;
	}

	/** See {@link #guidance}. */
	@Column(name = "Guidance", nullable = true, length = 60)
	public String getGuidance() {
		return guidance;
	}
	/** See {@link #guidance}. */
	public void setGuidance(String guidance) {
		this.guidance = guidance;
	}

	/** See {@link #model}. */
	@Column(name = "Model", nullable = true)
	public boolean getModel() {
		return model;
	}
	/** See {@link #model}. */
	public void setModel(boolean model) {
		this.model = model;
	}

	/** See {@link #modelAgency}. */
	@Column(name = "Model_Agency", nullable = true)
	public boolean getModelAgency() {
		return modelAgency;
	}
	/** See {@link #modelAgency}. */
	public void setModelAgency(boolean modelAgency) {
		this.modelAgency = modelAgency;
	}

	/** See {@link #producer}. */
	@Column(name = "Producer", nullable = true)
	public boolean getProducer() {
		return producer;
	}
	/** See {@link #producer}. */
	public void setProducer(boolean producer) {
		this.producer = producer;
	}

	/** See {@link #guardian}. */
	@Column(name = "Guardian", nullable = true)
	public boolean getGuardian() {
		return guardian;
	}
	/** See {@link #guardian}. */
	public void setGuardian(boolean guardian) {
		this.guardian = guardian;
	}

	/** See {@link #modelPrintName}. */
	@Column(name = "Model_Print_Name", nullable = true, length = 30)
	public String getModelPrintName() {
		return modelPrintName;
	}
	/** See {@link #modelPrintName}. */
	public void setModelPrintName(String modelPrintName) {
		this.modelPrintName = modelPrintName;
	}

	/** See {@link #producerPrintName}. */
	@Column(name = "Producer_Print_Name", nullable = true, length = 30)
	public String getProducerPrintName() {
		return producerPrintName;
	}
	/** See {@link #producerPrintName}. */
	public void setProducerPrintName(String producerPrintName) {
		this.producerPrintName = producerPrintName;
	}

	/** See {@link #company}. */
	@Column(name = "Company", nullable = true, length = 30)
	public String getCompany() {
		return company;
	}
	/** See {@link #company}. */
	public void setCompany(String company) {
		this.company = company;
	}

	/** See {@link #modelEmail}. */
	@Column(name = "Model_Email", nullable = true, length = 60)
	public String getModelEmail() {
		return modelEmail;
	}
	/** See {@link #modelEmail}. */
	public void setModelEmail(String modelEmail) {
		this.modelEmail = modelEmail;
	}

	/** See {@link #producerEmail}. */
	@Column(name = "Producer_Email", nullable = true, length = 60)
	public String getProducerEmail() {
		return producerEmail;
	}
	/** See {@link #producerEmail}. */
	public void setProducerEmail(String producerEmail) {
		this.producerEmail = producerEmail;
	}

	/** See {@link #modelPhone}. */
	@Column(name = "Model_Phone", nullable = true, length = 13)
	public String getModelPhone() {
		return modelPhone;
	}
	/** See {@link #modelPhone}. */
	public void setModelPhone(String modelPhone) {
		this.modelPhone = modelPhone;
	}

	/** See {@link #producerPhone}. */
	@Column(name = "Producer_Phone", nullable = true, length = 13)
	public String getProducerPhone() {
		return producerPhone;
	}
	/** See {@link #producerPhone}. */
	public void setProducerPhone(String producerPhone) {
		this.producerPhone = producerPhone;
	}

	/** See {@link #modelAgencyName}. */
	@Column(name = "Model_Agency_Name", nullable = true, length = 30)
	public String getModelAgencyName() {
		return modelAgencyName;
	}
	/** See {@link #modelAgencyName}. */
	public void setModelAgencyName(String modelAgencyName) {
		this.modelAgencyName = modelAgencyName;
	}

	/** See {@link #modelAgencyStreet}. */
	@Column(name = "Model_Agency_Street", nullable = true, length = 60)
	public String getModelAgencyStreet() {
		return modelAgencyStreet;
	}
	/** See {@link #modelAgencyStreet}. */
	public void setModelAgencyStreet(String modelAgencyStreet) {
		this.modelAgencyStreet = modelAgencyStreet;
	}

	/** See {@link #modelAgencyPhone}. */
	@Column(name = "Model_Agency_Phone", nullable = true, length = 13)
	public String getModelAgencyPhone() {
		return modelAgencyPhone;
	}
	/** See {@link #modelAgencyPhone}. */
	public void setModelAgencyPhone(String modelAgencyPhone) {
		this.modelAgencyPhone = modelAgencyPhone;
	}

	/** See {@link #modelAgencyCompany}. */
	@Column(name = "Model_Agency_Company", nullable = true, length = 30)
	public String getModelAgencyCompany() {
		return modelAgencyCompany;
	}
	/** See {@link #modelAgencyCompany}. */
	public void setModelAgencyCompany(String modelAgencyCompany) {
		this.modelAgencyCompany = modelAgencyCompany;
	}

	/** See {@link #address}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Producer_Address_Id")
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	public Address getProducerAddress() {
		return producerAddress;
	}

	/** See {@link #address}. */
	public void setProducerAddress(Address producerAddress) {
		this.producerAddress = producerAddress;
	}

	/** See {@link #address}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Model_Address_Id")
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	public Address getModelAddress() {
		return modelAddress;
	}

	/** See {@link #address}. */
	public void setModelAddress(Address modelAddress) {
		this.modelAddress = modelAddress;
	}

	//LS-3126 Added Fields per diem section

	/** See {@link #perdiemAmount}. */
	@Column(name = "Per_Deim_Amount")
	public BigDecimal getPerdiemAmount() {
		return perdiemAmount;
	}
	/** See {@link #perdiemAmount}. */
	public void setPerdiemAmount(BigDecimal perdiemAmount) {
		this.perdiemAmount = perdiemAmount;
	}

	/** See {@link #perdiemDays}. */
	@Column(name = "Per_Diem_Days")
	public Integer getPerdiemDays() {
		return perdiemDays;
	}
	/** See {@link #perdiemDays}. */
	public void setPerdiemDays(Integer perdiemDays) {
		this.perdiemDays = perdiemDays;
	}

	/** See {@link #lodging}. */
	@Column(name = "lodging")
	public boolean getLodging() {
		return lodging;
	}
	/** See {@link #lodging}. */
	public void setLodging(boolean lodging) {
		this.lodging = lodging;
	}

	/** See {@link #mealsIncidentals}. */
	@Column(name = "Meals_Incidentals")
	public boolean getMealsIncidentals() {
		return mealsIncidentals;
	}
	/** See {@link #mealsIncidentals}. */
	public void setMealsIncidentals(boolean mealsIncidentals) {
		this.mealsIncidentals = mealsIncidentals;
	}

    // LS-3125 Added Fields per reimbursements section

	/** See {@link #taxableTotal}. */
	@Column(name = "Taxable_Total")
	public BigDecimal getTaxableTotal() {
		return taxableTotal;
	}
	/** See {@link #taxableTotal}. */
	public void setTaxableTotal(BigDecimal taxableTotal) {
		this.taxableTotal = taxableTotal;
	}
	/** See {@link #nonTaxableTotal}. */
	@Column(name = "Non_Taxable_Total")
	public BigDecimal getNonTaxableTotal() {
		return nonTaxableTotal;
	}
	/** See {@link #nonTaxableTotal}. */
	public void setNonTaxableTotal(BigDecimal nonTaxableTotal) {
		this.nonTaxableTotal = nonTaxableTotal;
	}

	/** See {@link #acknowledged}. */
	@Column(name = "Acknowledged")
	public boolean isAcknowledged() {
		return acknowledged;
	}
	/** See {@link #acknowledged}. */
	public void setAcknowledged(boolean acknowledged) {
		this.acknowledged = acknowledged;
	}

	/** See {@link #shootUse}. */
	@Column(name = "shootUse")
	public boolean isShootUse() {
		return shootUse;
	}

	/** See {@link #shootUse}. */
	public void setShootUse(boolean shootUse) {
		this.shootUse = shootUse;
	}

	/** See {@link #agentPercentage}. */
	@Column(name = "Agent_Percentage")
	public Integer getAgentPercentage() {
		return agentPercentage;
	}
	/** See {@link #agentPercentage}. */
	public void setAgentPercentage(Integer agentPercentage) {
		this.agentPercentage = agentPercentage;
	}

	/** See {@link #jobState}. */
	@Column(name = "Job_State")
	public String getJobState() {
		return jobState;
	}
	/** See {@link #jobState}. */
	public void setJobState(String jobState) {
		this.jobState = jobState;
	}

	/** See {@link #jobCity}. */
	@Column(name = "Job_City")
	public String getJobCity() {
		return jobCity;
	}
	/** See {@link #jobCity}. */
	public void setJobCity(String jobCity) {
		this.jobCity = jobCity;
	}

	/** See {@link #employerAttachDoc}. */
	@Column(name = "Employer_Attach_Doc")
	public boolean isEmployerAttachDoc() {
		return employerAttachDoc;
	}
	/** See {@link #employerAttachDoc}. */
	public void setEmployerAttachDoc(boolean employerAttachDoc) {
		this.employerAttachDoc = employerAttachDoc;
	}

	/** See {@link #occupation}. */
	@Column(name = "Occupation")
	public String getOccupation() {
		return occupation;
	}
	/** See {@link #occupation}. */
	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	/** See {@link #otherCityName}. */
	public String getOtherCityName() {
		return otherCityName;
	}
	/** See {@link #otherCityName}. */
	public void setOtherCityName(String otherCityName) {
		this.otherCityName = otherCityName;
	}

	/** See {@link #startForm}. */
	@JsonIgnore
	@Transient
	public StartForm getStartForm() {
		if (startForm == null && getId() != null) {
			Map<String, Object> values = new HashMap<>();
			values.put("form", this);
			startForm = StartFormDAO.getInstance().findOneByNamedQuery(StartForm.GET_START_FORM_BY_MODEL_RELEASE, values);
		}
		return startForm;
	}
	/** See {@link #startForm}. */
	public void setStartForm(StartForm startForm) {
		this.startForm = startForm;
	}

	@Override
	public String toString() {
		String output = super.toString();
		output += "[";
		output += "modelName:" + modelName + " modlSSN:" + modelSSN + " modelAgencyYes:" + modelAgencyYes;
		output += " modelAgencyNo:" + modelAgencyNo + " + modelAgencyText:" + modelAgencyText + " modelCorporationYes:" + modelCorporationYes  + System.lineSeparator();
		output += " modelCorporationNo:" + modelCorporationNo + " modelCorporationText:" + modelCorporationText + " corporationFederalYes:" + corporationFederalYes;
		output += " corporationFederalNo:" + corporationFederalNo + "corporationFederalId:" + corporationFederalId + " projectName: " + projectName + System.lineSeparator();
		output += " job:" + job + " jobPo:" + jobPo + " shootDates:" + shootDates + " product:" + product + " advertiser:" + advertiser;
		output += " adAgency:" + adAgency + " producerName:" + producerName + " photographer:" + photographer + " job_state:" + jobState + " job_city:" + jobCity;
		output += " usageYes:" + usageYes + " usageNo:" + usageNo + " unlimited:" + unlimited + " unlimitedExceptOutdoor:" + unlimitedExceptOutdoor + System.lineSeparator();
		output += " electricMedia:" + electricMedia + " broll:" + broll + " directMail:" + directMail + " collateral:" + collateral + "ism:" + ism;
		output += " pr:" + pr + " print:" + print + " circular:" + circular + " industrial:" + industrial + " mediaOther:" + mediaOther + " mediaOtherText:" + mediaOtherText;
		output += " term:" + term + " termMonth:" + termMonth + " termDate:" + termDate + " termYear:" + termYear + " unlimitedTime:" + unlimitedTime + System.lineSeparator();
		output += " northAmerica:" + northAmerica + " europeanUnion:" + europeanUnion + " southAmerica:" + southAmerica + " aisa:" + asia;
		output += " worldWide:" + worldWide + " worldWideElectronicMedia:" + worldWideElectronicMedia + " territoryOther:" + territoryOther + " territoryOtherText:" + territoryOtherText;
		output += " optionalReuseYes:" + optionalReuseYes + " optionalReuseNo:" + optionalReuseNo + " reuseMedia:" + reuseMedia + "reuseTerm:" + reuseTerm + System.lineSeparator();
		output += " reuseTerritory:" + reuseTerritory + " compensationnYes:" + compensationYes + " compensationNo:" + compensationNo + " rateForService:" + rateForService;
		output += " perDay:" + perDay + " serviceHours:" + serviceHours + " rightTerm:" + rightTerm + " notrightTerm:" + notrightTerm + " usage:" + usage;
		output += " addlYes:" + addlYes + " addlNo:" + addlNo + " addlOverTime:" + addlOverTime + "addlPerHour:" + addlPerHour + " addlHours:" + addlHours + System.lineSeparator();
		output += " shootDay:" + shootDay + " shootPerDay:" + shootPerDay + " shootPerHours:" + shootPerHours + " prepDay:" + prepDay + " prepPerDay:" + prepPerDay;
		output += " prepPerHours:" + prepPerHours + " weatherDay:" + weatherDay + " weatherPerDay:" + weatherPerDay + " weatherHours:" + weatherHours + " ";
		output += " travelDayRate:" + travelDayRate + "travelDayPerDay:" + travelDayPerDay + "travelDayPerHours:" + travelDayPerHours + " nonTaxable:" + nonTaxable;
		output += " taxable:" + taxable + " additionalProvisions:" + additionalProvisions + " project:" + project + " jobModel:" + jobModel + System.lineSeparator();
		output += " age:" + age + " guidance:" + guidance + " model:" + model + " modelAgency:" + modelAgency + " producer:" + producer + " guardian" + guardian;
		output += " modelPrintName:" + modelPrintName + " producerPrintName:" + producerPrintName;
		output += " company:" + company + " modelEmail:" + modelEmail + " producerEmail:" + producerEmail + " modelPhone:" + modelPhone + " producerPhone:" + producerPhone;
		output += " modelAgencyName:" + modelAgencyName + " modelAgencyStreet:" + modelAgencyStreet + " modelAgencyPhone:" + modelAgencyPhone;
		output += " modelAgencyCompany:" + modelAgencyCompany + " producerAddress:" + producerAddress + " shootUse:" + shootUse;
		output += " SF: " + startForm;
		output += "]";

		return output;
	}

}

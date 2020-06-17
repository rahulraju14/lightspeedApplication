/**
 * File: FormService.java
 */
package com.lightspeedeps.service;

import java.io.*;
import java.net.URI;

import javax.ws.rs.core.*;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.sun.jersey.api.client.*;


/**
 * Singleton class for performing form related services.
 */
public class FormService extends BaseService {
	private static Log LOG = LogFactory.getLog(FormService.class);

	private static final String API_URL =  ApplicationUtils.getInitParameterString("ONBOARDING_API_URL", false);

	private static FormService formService;

	private FormService() {}

	public static FormService getInstance() {
		if(formService == null ) {
			formService = new FormService();
		}

		return formService;
	}

	/**
	 * A utility method to find the Form that is related to the given
	 * ContactDocument. If such a form does not exist, create a new instance of
	 * the appropriate form class, based on the PayrollFormType set in the
	 * ContactDocument.
	 *
	 * @param cd The ContactDocument of interest.
	 * @return The form specified by the cd's relatedFormId value, or, if that
	 *         is null or not found in the database, then a new instance of the
	 *         appropriate Form sub-class.
	 */
	@SuppressWarnings("rawtypes")
	public static Form findRelatedOrBlankForm(ContactDocument cd) {
		Form prtForm = null;
		Integer id = cd.getRelatedFormId();
		if (id != null) {
			if (cd.getFormType().getUseOnboardingApi()) { // newer forms use the TTCO onboarding API. LS-3576
				if (cd.getFormType().isFormStateW4Type()) {
					prtForm = FormService.getInstance().findById(cd.getRelatedFormId(), PayrollFormType.CA_W4.getApiFindUrl(), FormStateW4.class);
				}
				else if (FF4JUtils.useFeature(FeatureFlagType.TTCO_MODEL_RELEASE_FORM) && cd.getFormType() == PayrollFormType.MODEL_RELEASE) {
					// LS-3351 For Model Release Form we should be using the micro-service to retrieve the form.
					prtForm = FormService.getInstance().findById(cd.getRelatedFormId(), PayrollFormType.MODEL_RELEASE.getApiFindUrl(), FormModelRelease.class);
				}
			}
			else {
				// ** NOTE ** This is calling the BaseDAO findById method that allows specification
				// of the class of the instance being read. This bypasses the usual BaseTypeDAO method
				// that would (otherwise) require us to have a switch to get the appropriate Form DAO.
				prtForm = (Form)ContactDocumentDAO.getInstance().findById(cd.getFormType().getClassName(),id);
			}
		}
		if (prtForm == null) {
			if (cd.getFormType().isFormStateW4Type()) { // LS-3576
				prtForm = new FormStateW4(cd.getFormType());
			}
			else {
				switch (cd.getFormType()) {
					case CA_WTPA:
					case NY_WTPA:
						prtForm = new FormWTPA();
						break;
					case I9:
						prtForm = new FormI9();
						break;
					case START:
						prtForm = new StartForm();
						break;
					case W4:
						prtForm = new FormW4();
						break;
					case DEPOSIT:
						prtForm = new FormDeposit();
						break;
					case W9:
						prtForm = new FormW9();
						break;
					case MTA:
						prtForm = new FormMTA();
						break;
					case INDEM:
						prtForm = new FormIndem();
						break;
					case GA_W4:
						prtForm = new FormG4();
						break;
					case AZ_W4:
						prtForm = new FormA4();
						break;
					case LA_W4:
						prtForm = new FormL4();
						break;
					case IL_W4:
						prtForm = new FormILW4();
						break;
					case ACTRA_CONTRACT:
						prtForm = new FormActraContract();
						break;
					case ACTRA_PERMIT:
						prtForm = new FormActraWorkPermit();
						break;
					case ACTRA_INTENT:
						prtForm = new FormActraIntent();
						break;
					case UDA_INM:
						prtForm = new FormUDAContract();
						break;
					case MODEL_RELEASE:
						prtForm = new FormModelRelease();
						break;
					case OTHER:
						prtForm = new CustomForm();
						break;
					case SUMMARY:
						prtForm = new Employment();
						break;
					default:
						// no applicable form; should not be called in this case.
						throw new IllegalArgumentException();
				}
			}
		}
		return prtForm;
	}

	/**
	 * Retrieve a form based on the id and URL passed.
	 * This will call TTC-Online Onboarding api to retrieve the form.
	 *
	 * LS-3060
	 * @param id - id of form to retrieve.
	 * @param apiUrl - URL to use to connect to the onboarding api service
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Form findById(Integer id, String apiUrl, Class<? extends Form> clas) {
		ClientResponse clientResponse = null;
		try {
			Form form;

			final URI uri = UriBuilder.fromPath(API_URL  + apiUrl).path(id.toString()).build(); // will encode blanks and special characters
			final WebResource webResource = ApiUtils.buildWebResource(uri);

			clientResponse = webResource.get(ClientResponse.class);
			form = clientResponse.getEntity(clas); // LS-3576

			return form;
		}
		catch(Exception e) {
			String msgText = "Error in onboarding api response.";
			if (clientResponse != null) {
				msgText = clientResponse.toString();
			}
			EventUtils.logError(msgText, e);
			throw new LoggedException(e);
		}
	}

	/**
	 * LS-4354
	 *
	 * Encrypts entity fields that have the correct encryption annotations.
	 * See {@link #User.getSocialSecurty} for an example.
	 *
	 * @param apiUrl Url used to call micro-service api in appropriate environment
	 * @since version 20.10.0
	 */
	public boolean encryptFields(String apiUrl) {
		ClientResponse clientResponse = null;
		// Value return by micro-service on success of the operation.
		// This is return to the caller of this method
		boolean success = false;

		try {
			final URI uri = UriBuilder.fromPath(apiUrl).build(); // will encode blanks and special characters
			final WebResource webResource = ApiUtils.buildWebResource(uri);

			clientResponse = webResource.get(ClientResponse.class);
			success = clientResponse.getEntity(Boolean.class);
		}
		catch(Exception e) {
			String msgText = "Error in onboarding api response.";
			if (clientResponse != null) {
				msgText = clientResponse.toString();
			}
			EventUtils.logError(msgText, e);
			throw new LoggedException(e);
		}

		return success;
	}

	/**
	 * Persist the given form in the database.
	 * This will call TTC-Online Onboarding api to persist the form.
	 *
	 * LS-3105
	 * @param form - form to persist
	 * @param apiUrl - URL to use to connect to the onboarding api service
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Form persistForm(Form form, String apiUrl, Class<? extends Form> clas) {
		try {
			final URI uri = UriBuilder.fromPath(API_URL + apiUrl).build(); // will encode blanks and special characters
			final WebResource webResource = ApiUtils.buildWebResource(uri);
			final ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, form);
			form = clientResponse.getEntity(clas); // LS-3576

			return form;
		}
		catch(Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Update an existing form. This will call TTC-Online Onboarding api to update the form
	 *
	 * LS-3108
	 *
	 * @param form - form to update
	 * @param apiUrl - URL to use to connect to the onboarding api service
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Form update(Form form, String apiUrl, Class<? extends Form> clas) {
		try {
			final URI uri = UriBuilder.fromPath(API_URL + apiUrl).build(); // will encode blanks and special characters
			final WebResource webResource = ApiUtils.buildWebResource(uri);
			final ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, form);
			form = clientResponse.getEntity(clas); // LS-3576

			return form;
		}
		catch(Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Delete the form associated with the id parameter
	 *
	 * @param id - id of the form to delete
	 * @param apiUrl - url to use to connect to the onboarding api service
	 */
	public void delete(Integer id, String apiUrl) {
		try {
			final URI uri = UriBuilder.fromPath(API_URL  + apiUrl).path(id.toString()).build(); // will encode blanks and special characters
			final WebResource webResource = ApiUtils.buildWebResource(uri);
			webResource.type(MediaType.APPLICATION_JSON_TYPE).delete();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Stream pdf content to a brower tab.
	 *
	 * @param id - id of the ContactDocument describing the document whose PDF is to be streamed.
	 * @param apiUrl - url to use to connect to the onboarding API service
	 * @param output - ByteArrayOutputStream to write the contents of the PDF to.
	 */
	public void print(String id, String apiUrl, ByteArrayOutputStream output) {
		try {
			final URI uri = UriBuilder.fromPath(API_URL  + apiUrl).path(id).build(); // will encode blanks and special characters
			final WebResource webResource = ApiUtils.buildWebResource(uri);
			final ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
			LOG.debug(clientResponse.getLength());

			byte [] bytes = new byte[1024]; // buffer for reads
			int length;
			InputStream inputStream = clientResponse.getEntityInputStream();
			while ((length = inputStream.read(bytes)) > -1) {
				output.write(bytes, 0, length); // LS-3295 only write 'length' bytes
			}
			output.flush();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

}

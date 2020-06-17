//	File Name:	LoadPostalFileImpl.java
package com.lightspeedeps.web.admin;

//============================================================================
//Imports
//============================================================================
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.context.annotation.Scope;

import com.lightspeedeps.dao.PostalLocationDAO;
import com.lightspeedeps.model.PostalLocation;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * The implementation of the function to load a postal-code information
 * file into our PostalLocation database table.
 * See doc/postalZipInfo.txt for information about the source file.
 */
@Component("loadPostalFileImpl")
@Scope("request")
/* package-private */ class LoadPostalFileImpl implements LoadPostalFile {
	private static final Log log = LogFactory.getLog(LoadPostalFileImpl.class);

	private static final int BUFFER_SIZE = 1000;

	// Saved input parameters
	protected String m_file = null;

	// Processing counters, etc.
	protected int locationCnt = 0;

	List<PostalLocation> buffer = new ArrayList<PostalLocation>(BUFFER_SIZE);

	public LoadPostalFileImpl() {
	}

	public static LoadPostalFile getInstance() {
		return (LoadPostalFile)ServiceFinder.findBean("loadPostalFileImpl");
	}

	/**
	 * Import the specified file, assigning the script the given description,
	 * and using the given processing options.
	 * All the public "importFile" methods eventually get here.  The instance
	 * variable 'm_project' must already have been set.
	 * @return True iff the import was successful.
	 */
	@Override
	@Transactional
	public boolean loadFile(String file) {
		m_file = file;

		init();
		boolean bRet = doImport();

		log.debug("LOAD FINISHED, return=" + bRet + ", count=" + locationCnt);

		userInfoMessage("LoadPostal.Locations", locationCnt);

		return bRet;
	}

	protected boolean doImport() {
		log.debug("");
		boolean bRet = false;
		BufferedReader in = null;
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(m_file);
		}
		catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return false;
		}

		in = new BufferedReader(new InputStreamReader(inputStream));

		if (in != null) {
			String line = null;
			try {
				bRet = true;
				while ((line = in.readLine()) != null && bRet) {
					bRet = process(line);
				}
				flush();
				bRet &= close();
			}
			catch (IOException e) {
				bRet = false;
				e.printStackTrace();
			}
			catch (RuntimeException e) {
				bRet = false;
				e.printStackTrace();
			}
		}
		try {
			in.close();
		}
		catch (IOException e) {
		}

		return bRet;
	}

	private boolean process(String str) {
		boolean bRet = true;
		String parts[] = str.split("\\t");
		if (parts.length < 11) {
			EventUtils.logError("record error, # of fields should be at least 11, but is " + parts.length );
			log.error("text: `" + str + "`");
			userErrorMessage("LoadPostal.FileFormatError");
			return false;
		}
		String country = parts[0].trim();
		String zip = parts[1].trim();
		double lat;
		double lng;
		try {
			lat = Double.valueOf(parts[9]);
			lng = Double.valueOf(parts[10]);
		}
		catch (NumberFormatException e) {
			EventUtils.logError("number format exception, str="+str, e);
			return true;
		}
		//log.debug("cn=" + country + ", zip=" + zip + ", lat/long=" + lat + " " + lng);

		PostalLocation pl = new PostalLocation(country, zip, lat, lng);
		//postalLocationDAO.save(pl);
		buffer.add(pl);
		if (buffer.size() >= BUFFER_SIZE) {
			flush();
		}
		locationCnt++;

		return bRet;
	}

	/**
	 * Send any buffered PostalLocation records to the database.
	 */
	private void flush() {
		if (buffer != null) {
			PostalLocationDAO.getInstance().save(buffer);
			buffer.clear();
		}
	}

	private boolean close() {
		boolean bRet = true;
		log.debug("");
		return bRet;
	}

	private void init() {
		locationCnt = 0;
	}

	private void userInfoMessage(String msgid, Object... args) {
		log.info(msgid);
		MsgUtils.addFacesMessage(msgid, FacesMessage.SEVERITY_INFO, args);
	}

	private void userErrorMessage(String msgid) {
		log.warn(msgid);
		MsgUtils.addFacesMessage(msgid, FacesMessage.SEVERITY_ERROR);
	}

}

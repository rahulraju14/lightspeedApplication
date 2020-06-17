package com.lightspeedeps.test.util;

import java.io.File;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;


public class SmsUtility {

	public static void main(String[] args) throws Exception {

		//String strURL = "http://localhost:8080/lightspeed/servlet/SubmitResponse";
		String strURL = "http://test.lightspeedeps.com/hotdog/servlet.test";
		String strXMLFilename = "d://Dev/Studio/shortestSample.xml";
		File input = new File(strXMLFilename);
		PostMethod post = new PostMethod(strURL);
		RequestEntity entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
		post.setRequestEntity(entity);
		HttpClient httpclient = new HttpClient();
		try {
			int result = httpclient.executeMethod(post);
			System.out.println("Response status code: " + result);
			System.out.println("Response body: ");
			System.out.println(post.getResponseBodyAsString());
		} finally {
			post.releaseConnection();
		}
	}

}

package ch.exense.step.examples.selenium.helper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.AbstractKeyword;

/**
 * Central class containing some helper Keywords used to manage STEP custom transactions in an efficient way.
 * Please have a look at the <a href="https://step.exense.ch/knowledgebase/3.12/devdocs/keywordAPI/#measurements">Exense documentation</a> to learn how to use custom transactions.
 * @author rubieroj
 *
 */
public class TransactionalKeyword extends AbstractKeyword {
	
	/**
	 * Simple helper to set a Keyword success status to true
	 */
	protected void setSuccess() {
		output.add("success", true);
	}
	
	/**
	 * Simple helper to set a Keyword success status to false
	 */
	protected void setFailure() {
		output.add("success", false);
	}
	
	/**
	 * Helper method to build the transaction name base on the user inputs
	 * @param transactionName the transaction name to be updated if necessary
	 * @return the potentially updated transaction name
	 */
	protected String getFullTransactionName(String transactionName) {
		String result = transactionName;
		if (input.containsKey("customTransactionName")) {
			result = input.getString("customTransactionName");
		} else {
			if (input.containsKey("transactionPrefix")) {		
				result = input.getString("transactionPrefix") + result;
			}
			if (input.containsKey("transactionSuffix")) {
				result = result + input.getString("transactionSuffix");
			}
		}
		return result;
	}
	
	/**
	 * Helper method used to start a Keyword custom transaction
	 * @param transactionName the name of the custom transaction to start
	 */
	protected void startTransaction(String transactionNameTmp){
		String transactionName = getFullTransactionName(transactionNameTmp);
		if(isHarCaptureEnabled()) {
			getProxy().newHar(transactionName);
		}
		output.startMeasure(transactionName);
	}
		
	/**
	 * Helper method used to stop a Keyword custom transaction. An optional map of measurements data can be passed to add details on the custom transaction.
	 * @param transactionName the name of the custom transaction to stop
	 * @param additionnalMeasurementData the optional map of measurements data to insert into the custom transaction
	 */
	protected void stopTransaction(String transactionNameTmp, Map<String, Object> additionnalMeasurementData) {
		String transactionName = getFullTransactionName(transactionNameTmp);
		Map<String, Object> data = new HashMap<>();
		//data.put("type", "e2e");
		if(additionnalMeasurementData != null && !additionnalMeasurementData.isEmpty()) data.putAll(additionnalMeasurementData);
		output.stopMeasure(data);		
		if(isHarCaptureEnabled()) insertHarMeasures(getProxy().getHar(), transactionName, input.getBoolean("attachHarFile", false));
	}
	
	/**
	 * Helper method used to stop a Keyword custom transaction. Optional key and value data can be passed to add details on the custom transaction.
	 * @param transactionName the name of the custom transaction to stop
	 * @param additionalMeasurementPropertyKey the name of the additional data key to insert to the custom transaction
	 * @param additionalMeasurementPropertyValue the value of the additional data to insert to the custom transaction
	 * @see #stopTransaction(String, Map)
	 */
	protected void stopTransaction(String transactionName, String additionalMeasurementPropertyKey, String additionalMeasurementPropertyValue) {
		Map<String, Object> additionalMeasurementData = new HashMap<>();
		additionalMeasurementData.put(additionalMeasurementPropertyKey, additionalMeasurementPropertyValue);
		stopTransaction(transactionName, additionalMeasurementData);	
	}
	
	/**
	 * Helper method used to stop a Keyword custom transaction.
	 * @param transactionName the name of the transaction to stop
	 * @see #stopTransaction(String, Map)
	 */
	protected void stopTransaction(String transactionName) {
		stopTransaction(transactionName, null);
	}
	
	/**
	 * Helper method used to insert the HTTP measurement details captured by an instance of the BrowserMobProxy (if enabled)
	 * @param har the Har object containing the HTTP measurement details
	 * @param transactionName the transaction to insert the HTTP measurments to
	 * @param attachHarFile define if the Har object should be streamed to a file and attached to the Keyword output
	 */
	protected void insertHarMeasures(Har har, String transactionName, boolean attachHarFile) {
		List<HarEntry> harEntries = har.getLog().getEntries();
		harEntries.stream()
			.forEach(e -> {
				Map<String, Object> measurementData = new HashMap<>();
				measurementData.put("type", "http");
				measurementData.put("request_url", e.getRequest().getUrl());
				measurementData.put("request_method", e.getRequest().getMethod());
				measurementData.put("response_status",e.getResponse().getStatus() + " - " + e.getResponse().getStatusText());
				measurementData.put("response_content_size", e.getResponse().getContent().getSize());
				measurementData.put("response_content_type", e.getResponse().getContent().getMimeType());
				output.addMeasure(transactionName, e.getTime(), measurementData);
				System.out.println("Inserting har measurment recorded at " + e.getStartedDateTime());
			});
		if(attachHarFile) {
			StringWriter sw = new StringWriter();
			try {
				har.writeTo(sw);
			} catch (IOException e) {
				AttachmentHelper.generateAttachmentForException(e);
			}
			output.addAttachment(AttachmentHelper.generateAttachmentFromByteArray(sw.toString().getBytes(), transactionName + ".har"));
		}
	}
	
	/**
	 * Helper method to get a BrowserMobProxy instance from a STEP session
	 * @return the BrowserMobProxy instance from a STEP session
	 */
	protected BrowserMobProxy getProxy() {
		return ((ProxyWrapper) session.get("ProxyWrapper")).getProxy();
	}
	
	/**
	 * Helper method to check if the Har capture is enabled
	 * @return true if enabled, otherwise false
	 */
	protected boolean isHarCaptureEnabled() {
		return (boolean) session.get("enableHarCapture");
	}
}
package ch.exense.step.library.kw.system;

import step.handlers.javahandler.Keyword;

public class ProcessKeywords extends AbstractProcessKeyword {

	protected static final String COMMAND = "Command";
	protected static final String TIMEOUT_MS = "Timeout_ms";
	protected static final String MAX_OUTPUT_ATTACHMENT_SIZE = "Max_Output_Attachment_Size";
	protected static final String MAX_OUTPUT_PAYLOAD_SIZE = "Max_Output_Payload_Size";
	
	protected String command;
	protected int timeoutInMillis;
	protected OutputConfiguration outputConfiguration;
	
	@Keyword(name = "Execute", schema = "{\"properties\":{\"" + TIMEOUT_MS + "\":{\"type\":\"string\"},"
			+ "\"" + MAX_OUTPUT_PAYLOAD_SIZE + "\":{\"type\":\"string\"},\"" + MAX_OUTPUT_ATTACHMENT_SIZE + "\":{\"type\":\"string\"},"
					+ "\"" + COMMAND + "\":{\"type\":\"string\"}},\"required\":[\"" + COMMAND + "\"]}")
	public void executeSystemCommand() throws Exception {
		readInputs();
		executeManagedCommand(command, timeoutInMillis, outputConfiguration);
	}

	protected void readInputs() {
		command = input.getString(COMMAND,"");
		timeoutInMillis = Integer.parseInt(input.getString(TIMEOUT_MS, "10000"));
		outputConfiguration = readOutputConfiguration();
	}

	protected OutputConfiguration readOutputConfiguration() {
		int maxOutputPayloadSize = Integer.parseInt(input.getString(MAX_OUTPUT_PAYLOAD_SIZE, "1000"));
		int maxOutputAttachmentSize = Integer.parseInt(input.getString(MAX_OUTPUT_ATTACHMENT_SIZE, "100000"));
		return new OutputConfiguration(true, maxOutputPayloadSize, maxOutputAttachmentSize);
	}
}

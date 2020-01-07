package ch.exense.step.library.kw.system;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.TimeoutException;

import ch.exense.commons.processes.ManagedProcess;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

public class ProcessKeywords extends AbstractKeyword {

	protected static final String COMMAND = "command";
	protected static final String TIMEOUT_MS = "timeoutMs";
	protected static final String MAX_OUTPUT_ATTACHMENT_SIZE = "maxOutputAttachmentSize";
	protected static final String MAX_OUTPUT_PAYLOAD_SIZE = "maxOutputPayloadSize";
	
	protected int timeoutInMillis;
	protected int maxOutputPayloadSize;
	protected int maxOutputAttachmentSize;
	protected String command;

	@Keyword(name = "Execute", schema = "{\"properties\":{\"" + TIMEOUT_MS + "\":{\"type\":\"string\"},"
			+ "\"" + MAX_OUTPUT_PAYLOAD_SIZE + "\":{\"type\":\"string\"},\"" + MAX_OUTPUT_ATTACHMENT_SIZE + "\":{\"type\":\"string\"},"
					+ "\"" + COMMAND + "\":{\"type\":\"string\"}},\"required\":[\"" + COMMAND + "\"]}")
	public void execute() throws Exception {
		readInputs();
		executeManagedCommand(command);
	}

	protected void readInputs() {
		command = input.getString(COMMAND,"");
		timeoutInMillis = Integer.parseInt(input.getString(TIMEOUT_MS, "10000"));
		maxOutputPayloadSize = Integer.parseInt(input.getString(MAX_OUTPUT_PAYLOAD_SIZE, "256"));
		maxOutputAttachmentSize = Integer.parseInt(input.getString(MAX_OUTPUT_ATTACHMENT_SIZE, "100000"));
	}

	protected void executeManagedCommand(String cmd) throws Exception {
		ManagedProcess process = new ManagedProcess("ProcessKeywords_Execute", cmd);
		try {
			process.start();
			int exitCode;
			try {
				exitCode = process.waitFor(timeoutInMillis);
				if (exitCode != 0) {
					output.setBusinessError("Process exited with code " + exitCode);
				}
			} catch (TimeoutException e) {
				output.setBusinessError("Process didn't exit within the defined timeout of "+timeoutInMillis+"ms");
			}

			attachOutput("stdout", process.getProcessOutputLog());
			attachOutput("stderr", process.getProcessErrorLog());
		} finally {
			process.close();
		}
	}

	protected void attachOutput(String outputName, File file) throws IOException {
		StringBuilder processOutputBuilder = new StringBuilder();
		Files.readAllLines(file.toPath(), Charset.defaultCharset()).forEach(l -> processOutputBuilder.append(l).append("\n"));

		String processOutput = processOutputBuilder.toString();

		output.add(outputName, processOutput.substring(0, Math.min(processOutput.length(), maxOutputPayloadSize)));

		if(processOutput.length() > maxOutputPayloadSize) {
			Attachment attachment = AttachmentHelper.generateAttachmentFromByteArray(
					processOutput.substring(0, Math.min(processOutput.length(), maxOutputAttachmentSize)).getBytes(), outputName + ".log");
			output.addAttachment(attachment);
			
			if (file.length() > maxOutputAttachmentSize) {
				output.add("technicalWarning",
						outputName + " size exceeded. " + outputName + " has been attached and truncated.");
			}
		}
	}
}

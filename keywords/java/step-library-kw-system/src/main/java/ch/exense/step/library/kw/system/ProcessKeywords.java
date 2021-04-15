/*******************************************************************************
 * Copyright 2021 exense GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package ch.exense.step.library.kw.system;

import java.util.ArrayList;

import ch.exense.step.library.commons.AbstractProcessKeyword;
import step.handlers.javahandler.Keyword;

public class ProcessKeywords extends AbstractProcessKeyword {

	protected static final String COMMAND = "Command";
	protected static final String TIMEOUT_MS = "Timeout_ms";
	protected static final String MAX_OUTPUT_ATTACHMENT_SIZE = "Max_Output_Attachment_Size";
	protected static final String MAX_OUTPUT_PAYLOAD_SIZE = "Max_Output_Payload_Size";
	protected static final String CHECK_EXIT_CODE = "Check_Exit_Code";
	
	protected String command;
	protected int timeoutInMillis;
	protected OutputConfiguration outputConfiguration;
	
	@Keyword(name = "Execute", schema = "{\"properties\":{\"" + TIMEOUT_MS + "\":{\"type\":\"string\"},"
			+ "\"" + MAX_OUTPUT_PAYLOAD_SIZE + "\":{\"type\":\"string\"},\""
			+ MAX_OUTPUT_ATTACHMENT_SIZE + "\":{\"type\":\"string\"},\""
			+ CHECK_EXIT_CODE + "\":{\"type\":\"boolean\"},"
			+ "\"" + COMMAND + "\":{\"type\":\"string\"}},\"required\":[\"" + COMMAND + "\"]}")
	public void executeSystemCommand() throws Exception {
		readInputs();
		executeManagedCommand(command, timeoutInMillis, outputConfiguration);
	}
	
	@Keyword(name = "ExecuteBash", schema = "{\"properties\":{\"" + TIMEOUT_MS + "\":{\"type\":\"string\"},"
			+ "\"" + MAX_OUTPUT_PAYLOAD_SIZE + "\":{\"type\":\"string\"},\""
			+ MAX_OUTPUT_ATTACHMENT_SIZE + "\":{\"type\":\"string\"},\""
			+ CHECK_EXIT_CODE + "\":{\"type\":\"boolean\"},"
			+ "\"" + COMMAND + "\":{\"type\":\"string\"}},\"required\":[\"" + COMMAND + "\"]}")
	public void executeBashCommand() throws Exception {
		readInputs();
		
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add("bash");
		cmd.add("-c");
		cmd.add(command);
		executeManagedCommand(cmd, timeoutInMillis, outputConfiguration, null);
	}
	

	@Keyword(name = "ExecuteCmd", schema = "{\"properties\":{\"" + TIMEOUT_MS + "\":{\"type\":\"string\"},"
			+ "\"" + MAX_OUTPUT_PAYLOAD_SIZE + "\":{\"type\":\"string\"},\""
			+ MAX_OUTPUT_ATTACHMENT_SIZE + "\":{\"type\":\"string\"},\""
			+ CHECK_EXIT_CODE + "\":{\"type\":\"boolean\"},"
			+ "\"" + COMMAND + "\":{\"type\":\"string\"}},\"required\":[\"" + COMMAND + "\"]}")
	public void executeCmdCommand() throws Exception {
		readInputs();
		
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add("cmd");
		cmd.add("/C");
		cmd.add(command);
		executeManagedCommand(cmd, timeoutInMillis, outputConfiguration, null);
	}

	protected void readInputs() {
		command = input.getString(COMMAND,"");
		timeoutInMillis = Integer.parseInt(input.getString(TIMEOUT_MS, "10000"));
		outputConfiguration = readOutputConfiguration();
	}

	protected OutputConfiguration readOutputConfiguration() {
		int maxOutputPayloadSize = Integer.parseInt(input.getString(MAX_OUTPUT_PAYLOAD_SIZE, "1000"));
		int maxOutputAttachmentSize = Integer.parseInt(input.getString(MAX_OUTPUT_ATTACHMENT_SIZE, "100000"));
		boolean checkExitCode = input.getBoolean(CHECK_EXIT_CODE, true);
		return new OutputConfiguration(true, maxOutputPayloadSize, maxOutputAttachmentSize, true, checkExitCode);
	}
}

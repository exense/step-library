/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
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

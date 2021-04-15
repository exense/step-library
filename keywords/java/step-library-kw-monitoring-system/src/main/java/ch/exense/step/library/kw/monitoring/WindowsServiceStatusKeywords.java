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
package ch.exense.step.library.kw.monitoring;

import java.io.File;
import java.nio.file.Files;

import ch.exense.step.library.commons.AbstractProcessKeyword;
import step.handlers.javahandler.Keyword;

public class WindowsServiceStatusKeywords extends AbstractProcessKeyword {

	@Keyword(name = "Windows_Service_Status", schema = "{\"properties\":{\"Service_Display_Name\":{\"type\":\"string\"}},\"required\":[\"Service_Display_Name\"]}")
	public void getWindowsServiceStatus() throws Exception {
		String cmd = buildCommandLine();
		executeManagedCommand(cmd, PROCESS_TIMEOUT, new OutputConfiguration(false, 1000, 10000, true, true), p->{
			try {
				executionPostProcess(p.getProcessOutputLog());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	protected String buildCommandLine() throws Exception {
		String serviceDisplayName = input.getString("Service_Display_Name");
		return "powershell Get-Service -DisplayName '" + serviceDisplayName + "' | Format-List Status";
	}

	protected void executionPostProcess(File file) throws Exception {
		String processOutput = new String(Files.readAllBytes(file.toPath())).replace("\n", "").replace("\r", "");
		String status = processOutput.substring(processOutput.lastIndexOf(":") + 1).toLowerCase().trim();
		output.add("Status", status);
	}
}

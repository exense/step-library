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
package ch.exense.step.library.kw.monitoring;

import java.io.File;
import java.nio.file.Files;

import ch.exense.step.library.kw.system.AbstractProcessKeyword;
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

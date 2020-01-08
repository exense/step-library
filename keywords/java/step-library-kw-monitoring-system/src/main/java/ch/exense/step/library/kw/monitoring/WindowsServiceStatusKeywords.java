package ch.exense.step.library.kw.monitoring;

import java.io.File;
import java.nio.file.Files;

import ch.exense.step.library.kw.system.AbstractProcessKeyword;
import step.handlers.javahandler.Keyword;

public class WindowsServiceStatusKeywords extends AbstractProcessKeyword {

	@Keyword(name = "WindowsServiceStatus", schema = "{\"properties\":{\"serviceDisplayName\":{\"type\":\"string\"}},\"required\":[\"serviceDisplayName\"]}")
	public void getWindowsServiceStatus() throws Exception {
		String cmd = buildCommandLine();
		executeManagedCommand(cmd, 1000, new OutputConfiguration(false, 1000, 10000), p->{
			try {
				executionPostProcess(p.getProcessOutputLog());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	protected String buildCommandLine() throws Exception {
		String serviceDisplayName = input.getString("serviceDisplayName");
		return "powershell Get-Service -DisplayName '" + serviceDisplayName + "' | Format-List Status";
	}

	protected void executionPostProcess(File file) throws Exception {
		String processOutput = new String(Files.readAllBytes(file.toPath())).replace("\n", "").replace("\r", "");
		String status = processOutput.substring(processOutput.lastIndexOf(":") + 1).toLowerCase().trim();
		output.add("status", status);
	}
}
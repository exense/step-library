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

import ch.exense.commons.processes.ManagedProcess;
import ch.exense.step.library.commons.AbstractProcessKeyword;
import org.apache.commons.io.filefilter.PathMatcherFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import step.handlers.javahandler.Keyword;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ProcessKeywords extends AbstractProcessKeyword {

	protected static final String COMMAND = "Command";
	protected static final String PROPERTIES_AS_ENVIRONMENT_VARIABLES = "Pass_Properties_As_Env_Variables";
	protected static final String MAX_OUTPUT_ATTACHMENT_SIZE = "Max_Output_Attachment_Size";
	protected static final String MAX_OUTPUT_PAYLOAD_SIZE = "Max_Output_Payload_Size";
	protected static final String CHECK_EXIT_CODE = "Check_Exit_Code";
	public static final String ARTIFACTS = "Artifacts";
	public static final String SCHEMA_ARRAY_STRING = "{\n" +
			"      \"type\": \"array\",\n" +
			"      \"items\": {\n" +
			"        \"type\": \"string\"\n" +
			"      }\n" +
			"    }";

	protected String command;
	protected Map<String,String> environments;
	protected int timeoutInMillis;
	protected OutputConfiguration outputConfiguration;

	@Keyword(name = "Execute", schema = "{\"properties\":{\"" + TIMEOUT_MS + "\":{\"type\":\"string\"},"
			+ "\"" + MAX_OUTPUT_PAYLOAD_SIZE + "\":{\"type\":\"string\"},\""
			+ MAX_OUTPUT_ATTACHMENT_SIZE + "\":{\"type\":\"string\"},\""
			+ CHECK_EXIT_CODE + "\":{\"type\":\"boolean\"},\""
			+ PROPERTIES_AS_ENVIRONMENT_VARIABLES + "\":{\"type\":\"boolean\"},"
			+ "\"" + COMMAND + "\":{\"type\":\"string\"}},\"required\":[\"" + COMMAND + "\"]}",
			timeout = 1800000,
			description="Keyword used to start a generic process.")
	public void executeSystemCommand() throws Exception {
		readInputs();
		executeManagedCommand(command, environments, timeoutInMillis, outputConfiguration);
	}

	@Keyword(name = "ExecuteBash", schema = "{\"properties\":{\"" + TIMEOUT_MS + "\":{\"type\":\"string\"},"
			+ "\"" + MAX_OUTPUT_PAYLOAD_SIZE + "\":{\"type\":\"string\"},\""
			+ MAX_OUTPUT_ATTACHMENT_SIZE + "\":{\"type\":\"string\"},\""
			+ CHECK_EXIT_CODE + "\":{\"type\":\"boolean\"},\""
			+ PROPERTIES_AS_ENVIRONMENT_VARIABLES + "\":{\"type\":\"boolean\"},"
			+ "\"" + COMMAND + "\":{\"type\":\"string\"}, \"" + ARTIFACTS + "\": " + SCHEMA_ARRAY_STRING + "},\"required\":[\"" + COMMAND + "\"]}",
			timeout = 1800000,
			description = "Keyword used to run a bash command.")
	public void executeBashCommand() throws Exception {
		readInputs();

		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add("bash");
		cmd.add("-c");
		cmd.add(command);

		Consumer<ManagedProcess> managedProcessConsumer = getManagedProcessConsumer();
		executeManagedCommand(cmd, environments, timeoutInMillis, outputConfiguration, managedProcessConsumer);
	}

	@Keyword(name = "ExecuteCmd", schema = "{\"properties\":{\"" + TIMEOUT_MS + "\":{\"type\":\"string\"},"
			+ "\"" + MAX_OUTPUT_PAYLOAD_SIZE + "\":{\"type\":\"string\"},\""
			+ MAX_OUTPUT_ATTACHMENT_SIZE + "\":{\"type\":\"string\"},\""
			+ CHECK_EXIT_CODE + "\":{\"type\":\"boolean\"},\""
			+ PROPERTIES_AS_ENVIRONMENT_VARIABLES + "\":{\"type\":\"boolean\"},"
			+ "\"" + COMMAND + "\":{\"type\":\"string\"}, \"" + ARTIFACTS + "\": " + SCHEMA_ARRAY_STRING + "},\"required\":[\"" + COMMAND + "\"]}",
			timeout = 1800000,
			description="Keyword used to run a windows cmd command.")
	public void executeCmdCommand() throws Exception {
		readInputs();

		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add("cmd");
		cmd.add("/C");
		cmd.add(command);

		Consumer<ManagedProcess> managedProcessConsumer = getManagedProcessConsumer();
		executeManagedCommand(cmd, environments, timeoutInMillis, outputConfiguration, managedProcessConsumer);
	}

	private Consumer<ManagedProcess> getManagedProcessConsumer() {
		Consumer<ManagedProcess> managedProcessConsumer;
		if (input.containsKey(ARTIFACTS)) {
			managedProcessConsumer = managedProcess -> {
				File executionDirectory = managedProcess.getExecutionDirectory();
				List<String> outputArtifactsToAttach = Arrays.stream(input.getJsonArray(ARTIFACTS).toArray()).map(Object::toString).collect(Collectors.toList());
				outputArtifactsToAttach.forEach(artifact -> {
					if (isPathAbsolute(artifact)) {
						attachFile(Paths.get(artifact).toFile());
					} else {
						File[] array = executionDirectory.listFiles((FilenameFilter) new PathMatcherFileFilter(new RegexFileFilter(artifact)));
						if (array != null && array.length > 0) {
							Arrays.stream(array).forEach(this::attachFile);
						}
					}
				});
			};
		} else {
			managedProcessConsumer = null;
		}
		return managedProcessConsumer;
	}

	private boolean isPathAbsolute(String artifact) {
		try {
			Path path = Paths.get(artifact);
			if (path.isAbsolute()) {
				return true;
			}
		} catch (InvalidPathException e) {
			// If the path is a regex, it cannot be parsed as Path. We ignore this error and consider the path as a regex
		}
		return false;
	}

	// the list of properties to remove from the env. variables
	private static final List<String> LIST_JAVA_PROPERTIES = List.of("currentReport", "controllerSettings",
			"report", "currentArtefact");

	protected void readInputs() {
		command = input.getString(COMMAND, "");

		if (input.getBoolean(PROPERTIES_AS_ENVIRONMENT_VARIABLES, false)) {
			environments = properties.entrySet().stream()
					.filter(entry ->
							!entry.getKey().startsWith("##") &&
									!entry.getKey().startsWith("$") &&
									!entry.getKey().startsWith("plugins.") &&
									!LIST_JAVA_PROPERTIES.contains(entry.getKey()))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		} else {
			environments = new HashMap<>();
		}

		timeoutInMillis = Integer.parseInt(input.getString(TIMEOUT_MS, Integer.toString(DEFAULT_PROCESS_TIMEOUT)));
		outputConfiguration = readOutputConfiguration();
	}

	protected OutputConfiguration readOutputConfiguration() {
		int maxOutputPayloadSize = Integer.parseInt(input.getString(MAX_OUTPUT_PAYLOAD_SIZE, "1000"));
		int maxOutputAttachmentSize = Integer.parseInt(input.getString(MAX_OUTPUT_ATTACHMENT_SIZE, "100000"));
		boolean checkExitCode = input.getBoolean(CHECK_EXIT_CODE, true);
		return new OutputConfiguration(true, maxOutputPayloadSize, maxOutputAttachmentSize, true, checkExitCode);
	}
}

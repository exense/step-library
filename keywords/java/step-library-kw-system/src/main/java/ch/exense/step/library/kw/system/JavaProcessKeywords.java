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

import step.handlers.javahandler.Keyword;

public class JavaProcessKeywords extends ProcessKeywords {

	private static final String JAVA_EXE = "Java_exe";
	private static final String VM_ARGS = "VM_args";
	private static final String PROGRAM_ARGS = "Program_args";
	private static final String MAIN_CLASS_OR_JAR = "Mainclass_or_Jar";
	private static final String CLASS_PATH = "Classpath";

	@Keyword(name = "Java", schema = "{\"properties\":{\"" + MAIN_CLASS_OR_JAR + "\":{\"type\":\"string\"}," + "\""
			+ VM_ARGS + "\":{\"type\":\"string\"},\"" + CLASS_PATH + "\":{\"type\":\"string\"}," + "\"" + JAVA_EXE
			+ "\":{\"type\":\"string\"},\"" + TIMEOUT_MS + "\":{\"type\":\"string\"}," + "\""
			+ MAX_OUTPUT_ATTACHMENT_SIZE + "\":{\"type\":\"string\"},\"" + MAX_OUTPUT_PAYLOAD_SIZE
			+ "\":{\"type\":\"string\"}}," + "\"required\":[\"" + MAIN_CLASS_OR_JAR + "\"]}",
			timeout = 1800000,
			description="Keyword used to start a Java process.")
	public void executeJavaProcess() throws Exception {
		readInputs();
		String command = buildCommandLine();
		executeManagedCommand(command, timeoutInMillis, outputConfiguration);
	}

	protected String buildCommandLine() {
		String javaExe = input.getString(JAVA_EXE, properties.getOrDefault("java.exe", "java"));

		StringBuilder sb = new StringBuilder().append(javaExe);

		if (input.containsKey(VM_ARGS)) {
			sb.append(" ").append(input.getString(VM_ARGS));
		}

		if (input.containsKey(CLASS_PATH)) {
			sb.append(" -cp ").append(input.getString(CLASS_PATH));
		}

		String mainClassOrJar = input.getString(MAIN_CLASS_OR_JAR);
		if (mainClassOrJar.endsWith(".jar")) {
			sb.append(" -jar ").append(mainClassOrJar);
		} else {
			sb.append(" ").append(mainClassOrJar);
		}

		if (input.containsKey(PROGRAM_ARGS)) {
			sb.append(" ").append(input.getString(PROGRAM_ARGS));
		}

		return sb.toString();
	}
}

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
			+ "\":{\"type\":\"string\"}}," + "\"required\":[\"" + MAIN_CLASS_OR_JAR + "\"]}")
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

package ch.exense.step.library.kw.system;

import step.handlers.javahandler.Keyword;

public class JavaProcessKeywords extends ProcessKeywords {

	private static final String JAVA_EXE = "javaExe";
	private static final String PROGRAM_ARGS = "programArgs";
	private static final String MAIN_CLASS_OR_JAR = "mainClassOrJar";
	private static final String CLASS_PATH = "classPath";
	private static final String VM_ARGS = "vmArgs";

	@Keyword(name = "Java", schema = "{\"properties\":{\"" + MAIN_CLASS_OR_JAR + "\":{\"type\":\"string\"}," + "\""
			+ VM_ARGS + "\":{\"type\":\"string\"},\"" + CLASS_PATH + "\":{\"type\":\"string\"}," + "\"" + JAVA_EXE
			+ "\":{\"type\":\"string\"},\"" + TIMEOUT_MS + "\":{\"type\":\"string\"}," + "\""
			+ MAX_OUTPUT_ATTACHMENT_SIZE + "\":{\"type\":\"string\"},\"" + MAX_OUTPUT_PAYLOAD_SIZE
			+ "\":{\"type\":\"string\"}}," + "\"required\":[\"" + MAIN_CLASS_OR_JAR + "\"]}")
	public void JavaManagedProcessKeyword() throws Exception {
		readInputs();
		String cmd = buildCommandLine();
		executeManagedCommand(cmd);
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

package ch.exense.step.library.kw.system;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.regex.Pattern;

import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

public class FileSystemKeywords extends AbstractKeyword {

	@Keyword(schema = "{\"properties\":{\"Folder\":{\"type\":\"string\"}},\"required\":[\"Folder\"]}")
	public void Rmdir() throws Exception {
		String folderName = input.getString("Folder");

		File folder = new File(folderName);

		if (!folder.isDirectory()) {
			output.setBusinessError("\"" + folderName + "\" is not a folder.");
		}

		try {
			if (!recursiveDelete(folder)) {
				output.setBusinessError("Folder \"" + folderName + "\" could not be deleted.");
			}
		} catch (SecurityException e) {
			output.setBusinessError("Security error when deleting folder \"" + folderName + "\". Message was: \""
					+ e.getMessage() + "\"");
		}
	}

	private boolean recursiveDelete(File folder) {
		File[] files = folder.listFiles();
		if (files != null) {
			for (File file : files) {
				recursiveDelete(file);
			}
		}
		return folder.delete();
	}

	@Keyword(schema = "{\"properties\":{\"Folder\":{\"type\":\"string\"}},\"required\":[\"Folder\"]}")
	public void Mkdir() throws Exception {
		String folderName = input.getString("Folder");

		File folder = new File(folderName);

		try {
			if (!folder.mkdirs()) {
				output.setBusinessError("Folder \"" + folderName + "\" could not be created.");
			}
		} catch (SecurityException e) {
			output.setBusinessError("Security error when creating folder \"" + folderName + "\". Message was: \""
					+ e.getMessage() + "\"");
		}
	}

	@Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"}},\"required\":[\"File\"]}")
	public void Read_file() throws Exception {
		String fileName = input.getString("File");

		File file = new File(fileName);

		if (!file.exists()) {
			output.setBusinessError("File \"" + fileName + "\" do not exist.");
		}
		if (!file.canRead()) {
			output.setBusinessError("File \"" + fileName + "\" is not readable.");
		}

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {

			String result = "";
			String tmp;
			while ((tmp = br.readLine()) != null) {
				result += tmp;
			}
			output.add("Content", result);
		} catch (Exception e) {
			output.setBusinessError(
					"Exception when reading file \"" + fileName + "\". Message was: \"" + e.getMessage() + "\"");
		}
	}

	@Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"},\"Regex\":{\"type\":\"string\"},\"Replacement\":{\"type\":\"string\"}},\"required\":[\"File\",\"Regex\",\"Replacement\"]}")
	public void Sed_file() throws Exception {
		String fileName = input.getString("File");

		String regex = input.getString("Regex");
		String replacement = input.getString("Replacement");

		File file = new File(fileName);

		if (!file.exists()) {
			output.setBusinessError("File \"" + fileName + "\" do not exist.");
			return;
		}
		if (!file.canRead()) {
			output.setBusinessError("File \"" + fileName + "\" is not readable.");
			return;
		}
		if (!file.canWrite()) {
			output.setBusinessError("File \"" + fileName + "\" is not writable.");
			return;
		}
		try {
			Pattern.compile(regex);
		} catch (Exception e) {
			output.setBusinessError("Regex \"" + regex + "\" is invalid. Error is \""+e.getMessage()+"\"");
			return;
		}
		
		File tmpFile = File.createTempFile(fileName, ".tmp");

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile))) {

			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.replaceAll(regex, replacement);
					writer.write(line+"\n");
				}
			} catch (Exception e) {
				output.setBusinessError(
						"Exception when reading file \"" + fileName + "\". Message was: \"" + e.getMessage() + "\"");
				return;
			}
			file.delete();
		} catch (Exception e) {
			output.setBusinessError(
					"Exception when writing file \"" + fileName + "\". Message was: \"" + e.getMessage() + "\"");
			return;
		}
		
		if (Files.move(tmpFile.toPath(),file.toPath())==null) {
			output.setBusinessError(
					"Could not move to file \"" + fileName + "\"");
		}
	}
}

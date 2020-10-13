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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import ch.exense.commons.io.FileHelper;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

public class FileSystemKeywords extends AbstractKeyword {

	@Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"}},\"required\":[\"File\"]}")
	public void Exist() {
		String zipName = input.getString("File");

		File file = new File(zipName);

		if (!file.exists()) {
			output.add("exists", "false");
			return;
		}
		output.add("exists", "true");
		if (!file.canRead()) {
			output.add("canRead", "false");
		}
		if (file.isFile()) {
			output.add("isDirectory", "false");
		} else {
			output.add("isDirectory", "true");
		}
	}

	@Keyword(schema = "{\"properties\":{\"Source\":{\"type\":\"string\"},\"Destination\":{\"type\":\"string\"},\"ToFile\":{\"type\":\"string\"}},"
				+ "\"required\":[\"Source\",\"Destination\"]}")
	public void Copy() throws Exception {
		String source = input.getString("Source");
		String destination = input.getString("Destination");
		
		boolean toFile = Boolean.parseBoolean(input.getString("ToFile","false"));

		File fileSource = new File(source);
		File fileDestination = new File(destination);

		if (!fileSource.exists()) {
			output.setBusinessError("\"" + source + "\" does not exist.");
			return;
		}
		if (!fileDestination.exists() && !toFile) {
			output.setBusinessError("\"" + destination + "\" does not exist.");
			return;
		}
		if (!fileDestination.isDirectory() && !toFile) {
			output.setBusinessError("\"" + destination + "\" is not a directory.");
			return;
		}
		if (fileSource.isDirectory() && toFile) {
			output.setBusinessError("\"" + source + "\" should not be a directory if \"ToFile\" is set to true.");
			return;
		}

		try {
			if (toFile) {
				FileUtils.copyFile(fileSource, fileDestination);
			} else if (fileSource.isDirectory()) {
				FileUtils.copyDirectory(fileSource, fileDestination);
			} else {
				FileUtils.copyFileToDirectory(fileSource, fileDestination);
			}
		} catch (SecurityException e) {
			output.setBusinessError(
					"Security error when copying folder \"" + source + "\". Message was: \"" + e.getMessage() + "\"");
		}
	}

	@Keyword(schema = "{\"properties\":{\"Folder\":{\"type\":\"string\"},\"Fail_if_dont_exist\":{\"type\":\"string\"}},\"required\":[\"Folder\"]}")
	public void Rmdir() {
		String folderName = input.getString("Folder");
		boolean failIfDontExist = Boolean.parseBoolean(input.getString("Fail_if_dont_exist", "false"));

		File folder = new File(folderName);
		
		if (!folder.isDirectory() && failIfDontExist) {
			output.setBusinessError("\"" + folderName + "\" is not a folder.");
		}

		if (!folder.exists()) {
			if (failIfDontExist) {
				output.setBusinessError("\"" + folderName + "\" do not exist.");
			} else {
				output.add("Exist","false");
			}
		} else {
			try {
				if (!recursiveDelete(folder)) {
					output.setBusinessError("Folder \"" + folderName + "\" could not be deleted.");
				}
			} catch (SecurityException e) {
				output.setBusinessError("Security error when deleting folder \"" + folderName + "\". Message was: \""
						+ e.getMessage() + "\"");
			}
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

	@Keyword(schema = "{\"properties\":{\"Folder\":{\"type\":\"string\"},\"Fail_if_exist\":{\"type\":\"string\"}},\"required\":[\"Folder\"]}")
	public void Mkdir() {
		String folderName = input.getString("Folder");
		boolean failIfExist = Boolean.getBoolean(input.getString("Fail_if_exist", "false"));

		File folder = new File(folderName);

		if (folder.exists()) {
			if (failIfExist) {
				output.setBusinessError("Folder \"" + folderName + "\" already exist.");
			}
			return;
		}

		try {
			if (!folder.mkdirs()) {
				output.setBusinessError("Folder \"" + folderName + "\" could not be created.");
			}
		} catch (SecurityException e) {
			output.setBusinessError("Security error when creating folder \"" + folderName + "\". Message was: \""
					+ e.getMessage() + "\"");
		}
	}

	@Keyword(schema = "{\"properties\":{\"Folder\":{\"type\":\"string\"},\"Destination\":{\"type\":\"string\"}},\"required\":[\"Folder\"]}")
	public void Zip_file() {
		String folderName = input.getString("Folder");
		String zip = input.getString("Destination", folderName + ".zip");

		File folder = new File(folderName);
		File zipFile = new File(zip);

		if (!folder.exists()) {
			output.setBusinessError("Folder \"" + folderName + "\" do not exist.");
			return;
		}
		if (!folder.canRead()) {
			output.setBusinessError("Folder \"" + folderName + "\" is not readable.");
			return;
		}
		if (!folder.isDirectory()) {
			output.setBusinessError("\"" + folderName + "\" is not a folder.");
			return;
		}

		try {
			FileHelper.zip(folder, zipFile);
		} catch (Exception e) {
			output.setBusinessError(
					"Exception when zipping \"" + folderName + "\". Error message was: \"" + e.getMessage() + "\"");
		}
	}

	@Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"},\"Destination\":{\"type\":\"string\"}},\"required\":[\"File\"]}")
	public void Unzip_file() {
		String zipName = input.getString("File");
		String dest = input.getString("Destination", ".");

		File file = new File(zipName);
		File folder = new File(dest);

		if (!file.exists()) {
			output.setBusinessError("File \"" + zipName + "\" do not exist.");
			return;
		}
		if (!file.canRead()) {
			output.setBusinessError("File \"" + zipName + "\" is not readable.");
			return;
		}
		if (!file.isFile()) {
			output.setBusinessError("\"" + zipName + "\" is not a file.");
			return;
		}
		if (!folder.isDirectory()) {
			output.setBusinessError("\"" + dest + "\" is not a folder.");
			return;
		}

		try {
			FileHelper.unzip(file, folder);
		} catch (Exception e) {
			output.setBusinessError(
					"Exception when unzipping \"" + zipName + "\". Error message was: \"" + e.getMessage() + "\"");
		}
	}

	@Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"}},\"required\":[\"File\"]}")
	public void Read_file() {
		String fileName = input.getString("File");

		File file = new File(fileName);

		if (!file.exists()) {
			output.setBusinessError("File \"" + fileName + "\" do not exist.");
			return;
		}
		if (!file.canRead()) {
			output.setBusinessError("File \"" + fileName + "\" is not readable.");
			return;
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
			output.setBusinessError("Regex \"" + regex + "\" is invalid. Error is \"" + e.getMessage() + "\"");
			return;
		}

		File tmpFile = File.createTempFile(fileName, ".tmp");

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile))) {

			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.replaceAll(regex, replacement);
					writer.write(line + "\n");
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

		Files.move(tmpFile.toPath(), file.toPath());
	}
}

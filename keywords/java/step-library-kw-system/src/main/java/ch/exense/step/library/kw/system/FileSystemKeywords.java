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

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
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

    @Keyword(schema = "{\"properties\":{\"Source\":{\"type\":\"string\"},\"Destination\":{\"type\":\"string\"},\"ToFile\":{\"type\":\"string\"},{\"Move\":{\"type\":\"boolean\"}},"
            + "\"required\":[\"Source\",\"Destination\"]}")
    public void Copy() throws Exception {
        String source = input.getString("Source");
        String destination = input.getString("Destination");
        boolean move = input.getBoolean("Move",false);

        boolean toFile = Boolean.parseBoolean(input.getString("ToFile", "false"));

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
        if (move) {
            FileUtils.deleteDirectory(fileDestination);
        }
    }

    @Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"},\"Fail_if_dont_exist\":{\"type\":\"string\"}},\"required\":[\"File\"]}")
    public void Rmfile() {
        String fileName = input.getString("File");
        boolean failIfDontExist = Boolean.parseBoolean(input.getString("Fail_if_dont_exist", "false"));

        File file = new File(fileName);

        if (file.isDirectory()) {
            output.setBusinessError("\"" + fileName + "\" is a folder.");
        }

        if (!file.exists()) {
            if (failIfDontExist) {
                output.setBusinessError("\"" + file + "\" do not exist.");
            } else {
                output.add("Exist", "false");
            }
        } else {
            try {
                FileUtils.forceDelete(file);
            } catch (Exception e) {
                output.setBusinessError("Error when deleting file \"" + fileName + "\". Message was: \""
                        + e.getMessage() + "\"");
            }
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
                output.add("Exist", "false");
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

    @Keyword(schema = "{\"properties\":{\"Folder\":{\"type\":\"string\"}},\"required\":[\"Folder\"]}")
    public void Ls() {
        String folderName = input.getString("Folder");

        File folder = new File(folderName);

        if (!folder.exists()) {
            output.setBusinessError("Folder \"" + folderName + "\" do not exist.");
            return;
        }
        if (!folder.isDirectory()) {
            output.setBusinessError("Object \"" + folderName + "\" is not a folder.");
            return;
        }
        try {
            List<String> directories = new ArrayList<>();
            List<String> files = new ArrayList<>();
            Files.list(folder.toPath()).forEach(f -> {
                if (f.toFile().isDirectory()) {
                    directories.add(formatFileOutput(f.toFile()));
                } else {
                    files.add(formatFileOutput(f.toFile()));
                }});
            output.add("Files",files.toString());
            output.add("Directories",directories.toString());
        } catch (IOException e) {
            output.setBusinessError("I/O error when creating folder \"" + folderName + "\". Message was: \""
                    + e.getMessage() + "\"");
        }
    }

    private String formatFileOutput(File file) {
        String sizeInfo = "";
        if (file.canRead()) {
            try {
                sizeInfo = "\"size\":" + Files.size(file.toPath()) + ",";
            } catch (Exception e) {}
        }
        return "{\"name\":\""+file.getName()+"\",\"path\":\""+file.getPath()+"\",\"lastModified\":"+ file.lastModified()+"," +
                "\"isDirectory\":"+ file.isDirectory()+"," + sizeInfo +
                "\"canRead\":"+file.canRead()+",\"canWrite\":"+file.canWrite()+",\"canExecute\":"+file.canExecute()+"}";
    }

    @Keyword(schema = "{\"properties\":{\"Folder\":{\"type\":\"string\"},\"Regex\":{\"type\":\"string\"}},\"required\":[\"Folder\",\"Regex\"]}")
    public void Find_file() {
        String folderName = input.getString("Folder");
        String regex = input.getString("Regex");

        File folder = new File(folderName);

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
            Pattern.compile(regex);
        } catch (Exception e) {
            output.setBusinessError("Regex \"" + regex + "\" is invalid. Error is \"" + e.getMessage() + "\"");
            return;
        }

        try {
            List<String> files = new ArrayList<>();
            recursiveSearch(folder, regex).forEach(f -> { files.add(formatFileOutput(f));} );
            output.add("Files",files.toString());
        } catch (Exception e) {
            output.setBusinessError(
                    "Exception when zipping \"" + folderName + "\". Error message was: \"" + e.getMessage() + "\"");
        }
    }

    private List<File> recursiveSearch(File folder, String regex) {
        List<File> result = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getPath().matches(regex)) {
                    result.add(file);
                }
                result.addAll(recursiveSearch(file,regex));
            }
        }
        return result;
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

    @Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"},\"Regex\":{\"type\":\"string\"}," +
            "\"Replacement\":{\"type\":\"string\"}},\"required\":[\"File\",\"Regex\",\"Replacement\"]}")
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

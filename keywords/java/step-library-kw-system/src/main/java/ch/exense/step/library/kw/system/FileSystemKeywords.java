package ch.exense.step.library.kw.system;

import java.io.File;

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
			output.setBusinessError("Security error when deleting folder \"" + folderName + "\". Message was: \""+e.getMessage()+"\"");
		}
	}
	
	private boolean recursiveDelete(File folder) {
	    File[] files = folder.listFiles();
	    if (files!=null) {
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
			output.setBusinessError("Security error when creating folder \"" + folderName + "\". Message was: \""+e.getMessage()+"\"");
		}
	}
}

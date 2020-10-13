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

import ch.exense.commons.io.FileHelper;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class FileCompareKeywords extends AbstractKeyword {

	@Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"}},\"required\":[\"File\"]}")
	public void Compare_XML() throws Exception {
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

		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc;
		try {
			doc = builder.parse(file);
		} catch (IOException io) {
			output.setError("IOException when trying to parse the File '" + file.getName() + "': " + io.getMessage(), io);
			return;
		} catch (SAXException sax) {
			output.setError("Invalid XML found when parsing the File '" + file.getName() + "': " + sax.getMessage(), sax);
			return;
		}

		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xPath = xpathFactory.newXPath();

		for (Object xpathObj : input.keySet().stream().filter(key -> !key.equals("File")).toArray()) {
			String xpathString = (String) xpathObj;
			String expected = input.getString(xpathString);

			try {
				XPathExpression expr = xPath.compile(xpathString);

				if (((NodeList) expr.evaluate(doc, XPathConstants.NODESET)).getLength()==0) {
					output.setBusinessError("Xpath '" + xpathString + "' not found in the document");
					return;
				}

				String result = expr.evaluate(doc);

				if (!result.equals(expected)) {
					output.setBusinessError("Error when comparing xpath '" + xpathString + "': value was expected to be: '" + expected + "' but was '" + result + "'");
				}
			} catch (XPathExpressionException expr) {
				output.setError("Invalid xpath found'" + xpathString + "': " + expr.getMessage(), expr);
				return;
			}
		}
	}
}

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

import ch.exense.step.library.commons.AbstractEnhancedKeyword;
import org.apache.commons.io.FileUtils;
import step.core.accessors.Attribute;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Attribute(key = "category",value = "Operating system")
public class SystemKeywords extends AbstractEnhancedKeyword {

	private static final String DEFAULT_FORMAT = "jpg";
	private static final String DEFAULT_FILENAME = "screenshot.jpg";

	@Keyword(description="Keyword used to take a screenshot of the screen.")
	public void TakeScreenshot() throws AWTException, IOException {
		Robot robot = new Robot();

		Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		BufferedImage screenFullImage = robot.createScreenCapture(screenRect);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(screenFullImage, DEFAULT_FORMAT, outputStream);

		Attachment attachment = AttachmentHelper.generateAttachmentFromByteArray(outputStream.toByteArray(),
				DEFAULT_FILENAME);
		output.addAttachment(attachment);
	}

	@Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"}},\"required\":[\"File\"]}",
			description="Keyword used to add a file as an attachment.")
    public void AttachFileToLog() {
		String zipName = input.getString("File");

		File file = new File(zipName);
		try {
			byte[] bytes = FileUtils.readFileToByteArray(file);
			Attachment attachment = AttachmentHelper.generateAttachmentFromByteArray(bytes, file.getName());
			output.addAttachment(attachment);
		} catch (Exception ex) {
			output.appendError("Unable to upload file");
		}
	}
}

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

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

public class SystemKeywords extends AbstractKeyword {

	private static final String DEFAULT_FORMAT = "jpg";
	private static final String DEFAULT_FILENAME = "screenshot.jpg";

	@Keyword
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
}

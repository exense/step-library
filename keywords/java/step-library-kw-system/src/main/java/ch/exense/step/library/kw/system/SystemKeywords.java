/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
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
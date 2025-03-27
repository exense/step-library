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
package ch.exense.step.library.selenium;

import ch.exense.step.library.commons.BusinessException;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.CapabilityType;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

public class AbstractEdgeDriverKeyword extends AbstractSeleniumKeyword {

    final String edgeDriverProperty = "webdriver.edge.driver";

    final List<String> defaultOptions = Arrays.asList("disable-infobars","ignore-certificate-errors", "no-zygote", "disable-extensions");
    final List<String> headlessOptions = Arrays.asList("headless", "disable-gpu", "disable-software-rasterizer", "no-sandbox");

    /**
     * <p>Keyword used to create a selenium chrome driver and start a corresponding chrome instance.
     * The driver is stored in the current STEP session and is automatically closed once the session ends.</p>
     *
     * Required properties:
     * <ul>
     * 	<li>chromedriver: path to the chrome driver (usually set in the agent properties)</li>
     * </ul>
     *
     * Inputs (default values):
     * <ul>
     * <li>headless (false): boolean toggle for the headless mode (headless required on server/kubernetes)
     * </ul>
     */
    protected void openEdge() {
        if (properties.containsKey("Edge_Driver")) {
            File edgeDriverBin = new File(properties.get("Edge_Driver"));
            if (edgeDriverBin.exists()) {
                System.setProperty(edgeDriverProperty, edgeDriverBin.getAbsolutePath());
            } else {
                throw new BusinessException("Could not find path to the edge driver executable specified in 'Edge_Driver', value was '"+edgeDriverBin.getPath()+"'");
            }
        }

        EdgeOptions options = new EdgeOptions();
        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        
        final WebDriver driver = new EdgeDriver(options);

        driver.manage().timeouts().implicitlyWait(Duration.of(input.getInt("Implicitly_Wait", 10), ChronoUnit.SECONDS));
        driver.manage().timeouts().pageLoadTimeout(Duration.of(input.getInt("PageLoad_Timeout", 10), ChronoUnit.SECONDS));
        driver.manage().window().setSize(new Dimension(1920, 1080));

        if (input.getBoolean("Maximize", false)) {
            driver.manage().window().maximize();
        }
        setDriver(driver);
    }

    protected void closeDriver() {
        WebDriver driver = getDriver();
        startTransaction();
        driver.quit();
        boolean debug = Boolean.parseBoolean(properties.getOrDefault("debug_selenium", "false"));
        if (debug) {
            properties.put("debug_selenium", "false");
        }
        stopTransaction();
        if (debug) {
            properties.put("debug_selenium", "true");
        }
    }

    /**
     * Helper method to check if the Har capture is enabled
     * @return true if enabled, otherwise false
     */
    protected boolean isHarCaptureEnabled() {
        return (boolean) session.get("enableHarCapture");
    }
}

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
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

public class AbstractChromeDriverKeyword extends AbstractSeleniumKeyword {

    final String chromeDriverProperty = "webdriver.chrome.driver";

    final List<String> defaultOptions = Arrays.asList("disable-infobars","ignore-certificate-errors", "disable-extensions");
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
    protected void openChrome() {
        if (properties.containsKey("Chrome_Driver")) {
            File chromeDriverBin = new File(properties.get("Chrome_Driver"));
            if (chromeDriverBin.exists()) {
                System.setProperty(chromeDriverProperty, chromeDriverBin.getAbsolutePath());
            } else {
                throw new BusinessException("Could not find path to the chrome driver executable specified in 'Chrome_Driver', value was '"+chromeDriverBin.getPath()+"'");
            }
        }

        ChromeOptions options = new ChromeOptions();
        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);

        if (properties.containsKey("Chrome_Path")) {
            File chromeBin = new File(properties.get("Chrome_Path"));
            if (chromeBin.exists()) {
                options.setBinary(chromeBin);
            } else {
                throw new BusinessException("Could not find path to the chrome executable specified in 'Chrome_Path', value was '"+chromeBin.getPath()+"'");
            }
        }

        options.addArguments(defaultOptions);

        if (input.getBoolean("Headless", false)) {
            options.addArguments(headlessOptions);
        }

        if (input.getBoolean("Disable_Shm",false)) {
            options.addArguments("disable-dev-shm-usage");
        }

        if (input.containsKey("Proxy_Host") && input.containsKey("Proxy_Port")) {
            Proxy proxy = new Proxy();
            proxy.setHttpProxy(input.getString("Proxy_Host")+":"+input.getInt("Proxy_Port"));
            proxy.setSslProxy(input.getString("Proxy_Host")+":"+input.getInt("Proxy_Port"));
            proxy.setNoProxy(input.getString("No_Proxy",""));
            options.setCapability(CapabilityType.PROXY, proxy);
        }

        // Custom profile settings
        if(input.containsKey("User_Data_Dir")) {
            options.addArguments("user-data-dir=" + input.getString("User_Data_Dir"));
            options.addArguments("--profile-directory=MyProfile");
        }

        if(input.containsKey("Additional_Options")) {
            options.addArguments(Arrays.asList(input.getString("Additional_Options").split(",")));
        }

        String transactionName = "Open_chrome";
        //startTransaction(transactionName);

        final WebDriver driver = new ChromeDriver(options);

        driver.manage().timeouts().implicitlyWait(Duration.of(input.getInt("Implicitly_Wait", 10), ChronoUnit.SECONDS));
        driver.manage().timeouts().pageLoadTimeout(Duration.of(input.getInt("PageLoad_Timeout", 10), ChronoUnit.SECONDS));
        driver.manage().window().setSize(new Dimension(1920, 1080));

        if (input.getBoolean("Maximize", false)) {
            driver.manage().window().maximize();
        }
        
        setDriver(driver);
        //stopTransaction(transactionName);
    }
}

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
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.CaptureType;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import step.grid.io.AttachmentHelper;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        boolean enableHarCapture = input.getBoolean("Enable_Har_Capture", false);
        session.put("enableHarCapture", enableHarCapture);

        options.addArguments(defaultOptions);

        if (input.getBoolean("Headless", false)) {
            options.addArguments(headlessOptions);
        }

        if (input.getBoolean("Disable_Shm",false)) {
            options.addArguments("disable-dev-shm-usage");
        }

        long readBytesPerSecond = input.getInt("Read_Bytes_Per_Second", 0);
        long writeBytesPerSecond = input.getInt("Write_Bytes_Per_Second", 0);

        if(enableHarCapture || readBytesPerSecond > 0 || writeBytesPerSecond > 0) {
            BrowserMobProxy browserProxy = new BrowserMobProxyServer();
            browserProxy.setTrustAllServers(true);
            if (input.containsKey("Proxy_Host") && input.containsKey("Proxy_Port")) {
                browserProxy.setChainedProxy(
                        new InetSocketAddress(
                                input.getString("Proxy_Host"), input.getInt("Proxy_Port")));
                if(input.containsKey("No_Proxy")) {
                    String noProxy = input.getString("No_Proxy").replaceAll(",", "|");
                    System.setProperty("http.nonProxyHosts", noProxy);
                    System.setProperty("https.nonProxyHosts", noProxy);
                }
            }

            if (readBytesPerSecond > 0) {
                browserProxy.setReadBandwidthLimit(readBytesPerSecond);
                output.add("Read_Bytes_Per_Second", readBytesPerSecond);
            }
            if (writeBytesPerSecond > 0) {
                browserProxy.setWriteBandwidthLimit(writeBytesPerSecond);
                output.add("Read_Bytes_Per_Second", writeBytesPerSecond);
            }
            if (!enableHarCapture) {
                browserProxy.disableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
            }

            browserProxy.start(input.getInt("Browser_Proxy_Port", 0));

            Proxy seleniumProxy = ClientUtil.createSeleniumProxy(browserProxy);
            DesiredCapabilities seleniumCapabilities = new DesiredCapabilities();
            seleniumCapabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
            options.merge(seleniumCapabilities);
            setProxy(browserProxy);
        } else if (input.containsKey("Proxy_Host") && input.containsKey("Proxy_Port")) {
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

    /**
     * Helper method to check if the Har capture is enabled
     * @return true if enabled, otherwise false
     */
    protected boolean isHarCaptureEnabled() {
        return (boolean) session.get("enableHarCapture");
    }
    /**
     * Helper method used to insert the HTTP measurement details captured by an instance of the BrowserMobProxy (if enabled)
     * @param har the Har object containing the HTTP measurement details
     * @param transactionName the transaction to insert the HTTP measurments to
     * @param attachHarFile define if the Har object should be streamed to a file and attached to the Keyword output
     */
    protected void insertHarMeasures(Har har, String transactionName, boolean attachHarFile) {
        List<HarEntry> harEntries = har.getLog().getEntries();
        harEntries.forEach(e -> {
                    Map<String, Object> measurementData = new HashMap<>();
                    measurementData.put("type", "http");
                    measurementData.put("request_url", e.getRequest().getUrl());
                    measurementData.put("request_method", e.getRequest().getMethod());
                    measurementData.put("response_status",e.getResponse().getStatus() + " - " + e.getResponse().getStatusText());
                    measurementData.put("response_content_size", e.getResponse().getContent().getSize());
                    measurementData.put("response_content_type", e.getResponse().getContent().getMimeType());
                    output.addMeasure(transactionName, e.getTime(), measurementData);
                    System.out.println("Inserting har measurement recorded at " + e.getStartedDateTime());
                });
        if(attachHarFile) {
            StringWriter sw = new StringWriter();
            try {
                har.writeTo(sw);
            } catch (IOException e) {
                AttachmentHelper.generateAttachmentForException(e);
            }
            output.addAttachment(AttachmentHelper.generateAttachmentFromByteArray(sw.toString().getBytes(), transactionName + ".har"));
        }
    }
}

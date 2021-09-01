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
package ch.exense.step.examples.selenium.keyword;

import ch.exense.step.examples.selenium.helper.ProxyWrapper;
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
import step.core.accessors.Attribute;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.Keyword;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Attribute(key = "category",value = "Selenium")
public class ChromeDriverKeyword extends AbstractSeleniumKeyword {

    final String chromeDriverProperty = "webdriver.chrome.driver";

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
    @Keyword(schema = "{ \"properties\": { "
            + "\"Headless\": {  \"type\": \"boolean\"}"
            + "}, \"required\" : []}", properties = { "" })
    public void Open_Chrome() {
       Open_Chrome_Advanced();
    }

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
     * <li>disableShm (false): boolean toggle for Shm partition usage (disable on unix/kubernetes)
     * <li>proxyHost: define chrome proxy host if set
     * <li>proxyPort: define chrome proxy port if set
     * <li>enableHarCapture (false): boolean to enable the capture of HTTP requests and create custom measurements
     * <li>browserProxyPort (0): set the browser proxy port to be used (0 means automatically selected by the JVM/system)
     * <li>readBytesPerSecond: if set, limit the rate of read bytes by second
     * <li>writeBytesPerSecond: if set, limit the rate of write bytes by second
     * <li>user-data-dir: if set, define a specific chrome data folder to be used (by default a new temporary folder is used when starting chrome)
     * <li>additionalOptions: if set, this list of options is added to ChromeOptions
     * <li>implicitlyWait (10): timeout in seconds to load a page
     * <li>pageLoadTimeout (10): timeout in seconds when waiting for a DOM element
     * <li>maximize (false): toggle to maximum the chrome windows
     * </ul>
     */
    @Keyword(schema = "{ \"properties\": { "
            + "\"Chrome_Driver\": {\"type\": \"string\"},"
            + "\"Headless\": {  \"type\": \"boolean\"},"
            + "\"Disable_Shm\": {\"type\": \"boolean\"},"
            + "\"Proxy_Host\": {\"type\": \"string\"},"
            + "\"Proxy_Port\": {\"type\": \"integer\"},"
            + "\"No_Proxy\": {\"type\": \"string\"},"
            + "\"Enable_Har_Capture\": {\"type\": \"boolean\"},"
            + "\"Browser_Proxy_Port\": {\"type\": \"integer\"},"
            + "\"Read_Bytes_Per_Second\": {\"type\": \"integer\"},"
            + "\"Write_Bytes_Per_Second\": {\"type\": \"integer\"},"
            + "\"User_Data_Dir\": {  \"type\": \"string\"},"
            + "\"Additional_Options\": {  \"type\": \"string\"},"
            + "\"Implicitly_Wait\": {  \"type\": \"integer\"},"
            + "\"PageLoad_Timeout\": {  \"type\": \"integer\"},"
            + "\"Maximize\": {  \"type\": \"boolean\"}"
            + "}, \"required\" : []}", properties = { "" })
    public void Open_Chrome_Advanced() {
        if (properties.containsKey("Chrome_Driver")) {
            File chromeDriverBin = new File(properties.get("Chrome_Driver"));
            if (chromeDriverBin.exists()) {
                System.setProperty(chromeDriverProperty, chromeDriverBin.getAbsolutePath());
            }
        }

        boolean enableHarCapture = input.getBoolean("Enable_Har_Capture", false);
        session.put("enableHarCapture", enableHarCapture);

        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);

        options.addArguments(defaultOptions);
        options.setExperimentalOption("w3c", false);

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

        driver.manage().timeouts().implicitlyWait(input.getInt("Implicitly_Wait", 10), TimeUnit.SECONDS);
        driver.manage().timeouts().pageLoadTimeout(input.getInt("PageLoad_Timeout", 10), TimeUnit.SECONDS);
        driver.manage().window().setSize(new Dimension(1920, 1080));

        if (input.getBoolean("Maximize", false)) {
            driver.manage().window().maximize();
        }
        
        setDriver(driver);
        //stopTransaction(transactionName);
    }

    /**
     * <p>Keyword used to explicitly close the current windows. The driver and browser automatically close when the step session ends.</p>
     */
    @Keyword (schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT
            + "}, \"required\" : []}", properties = { "" })
    public void Close_Driver() {
        WebDriver driver = getDriver();
        startTransaction();
        driver.quit();
        Boolean debug = Boolean.parseBoolean(properties.getOrDefault("debug_selenium", "false"));
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
    /**
     * Helper method used to insert the HTTP measurement details captured by an instance of the BrowserMobProxy (if enabled)
     * @param har the Har object containing the HTTP measurement details
     * @param transactionName the transaction to insert the HTTP measurments to
     * @param attachHarFile define if the Har object should be streamed to a file and attached to the Keyword output
     */
    protected void insertHarMeasures(Har har, String transactionName, boolean attachHarFile) {
        List<HarEntry> harEntries = har.getLog().getEntries();
        harEntries.stream()
                .forEach(e -> {
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

    /**
     * Helper method to get a BrowserMobProxy instance from a STEP session
     * @return the BrowserMobProxy instance from a STEP session
     */
    protected BrowserMobProxy getProxy() {
        return session.get(ProxyWrapper.class).getProxy();
    }

    /**
     * <p>Helper method to put a BrowserMobProxy instance into a STEP session</p>
     * @param proxy the BrowserMobProxy instance to put in session
     */
    protected void setProxy(BrowserMobProxy proxy) {
        session.put(new ProxyWrapper(proxy));
    }
}

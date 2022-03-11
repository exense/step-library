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
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.KeyStoreFileCertificateSource;
import net.lightbody.bmp.mitm.PemFileCertificateSource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import net.lightbody.bmp.proxy.CaptureType;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import step.grid.io.AttachmentHelper;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
                throw new BusinessException("Could not find path to the edgedriver executable specified in 'Edge_Driver', value was '"+edgeDriverBin.getPath()+"'");
            }
        }

        EdgeOptions options = new EdgeOptions();
        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);


        if(input.getBoolean("Login_With_Client_Certificate" , false)) {
            // Get necessary inputs
            int proxyPort = input.getInt("Browser_Proxy_Port", 0);
            String clientCertificatePath = input.getString("Browser_Proxy_Client_Certificate_Path");
            String clientCertificatePassword = input.getString("Browser_Proxy_Client_Certificate_Password");
            SeleniumSslProxy seleniumSslProxy = new SeleniumSslProxy(new File(clientCertificatePath), clientCertificatePassword);
            seleniumSslProxy.start(proxyPort);
            options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
            options.setProxy(seleniumSslProxy);
            setProxy(seleniumSslProxy.getBrowserMobProxy());
        }
        final WebDriver driver = new EdgeDriver(options);

        driver.manage().timeouts().implicitlyWait(input.getInt("Implicitly_Wait", 10), TimeUnit.SECONDS);
        driver.manage().timeouts().pageLoadTimeout(input.getInt("PageLoad_Timeout", 10), TimeUnit.SECONDS);
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

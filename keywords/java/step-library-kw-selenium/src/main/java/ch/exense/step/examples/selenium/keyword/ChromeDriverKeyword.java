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

import ch.exense.step.library.selenium.AbstractChromeDriverKeyword;
import step.handlers.javahandler.Keyword;

/**
 * Class containing selenium keywords to open a Chrome browser
 */
public class ChromeDriverKeyword extends AbstractChromeDriverKeyword {

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
            + "}, \"required\" : []}", properties = { "" },
            description="Keyword used to create a simple selenium chrome driver.")
    public void Open_Chrome() throws Exception {
        openChrome();
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
     * <li>user-data-dir: if set, define a specific chrome data folder to be used (by default a new temporary folder is used when starting chrome)
     * <li>additionalOptions: if set, this list of options is added to ChromeOptions
     * <li>implicitlyWait (10): timeout in seconds to load a page
     * <li>pageLoadTimeout (10): timeout in seconds when waiting for a DOM element
     * <li>maximize (false): toggle to maximum the chrome windows
     * </ul>
     */
    @Keyword(schema = "{ \"properties\": { "
            + "\"Chrome_Driver\": {\"type\": \"string\"},"
            + "\"Chrome_Path\": {\"type\": \"string\"},"
            + "\"Headless\": {  \"type\": \"boolean\"},"
            + "\"Disable_Shm\": {\"type\": \"boolean\"},"
            + "\"Proxy_Host\": {\"type\": \"string\"},"
            + "\"Proxy_Port\": {\"type\": \"integer\"},"
            + "\"No_Proxy\": {\"type\": \"string\"},"
            + "\"User_Data_Dir\": {  \"type\": \"string\"},"
            + "\"Additional_Options\": {  \"type\": \"string\"},"
            + "\"Implicitly_Wait\": {  \"type\": \"integer\"},"
            + "\"PageLoad_Timeout\": {  \"type\": \"integer\"},"
            + "\"Maximize\": {  \"type\": \"boolean\"}"
            + "}, \"required\" : []}", properties = { "" },
            description="Keyword used to create a chrome driver with multiple options.")
    public void Open_Chrome_Advanced() {
        openChrome();
    }
}

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

import ch.exense.step.library.selenium.AbstractEdgeDriverKeyword;
import step.handlers.javahandler.Keyword;

public class EdgeDriverKeyword extends AbstractEdgeDriverKeyword {

    /**
     * <p>Keyword used to create a selenium edge driver and start a corresponding chrome instance.
     * The driver is stored in the current STEP session and is automatically closed once the session ends.</p>
     *
     * Required properties:
     * <ul>
     * 	<li>Edge_Driver: path to the edge driver (usually set in the agent properties)</li>
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
            description="Keyword used to create a simple edge driver.")
    public void Open_Edge() {
        openEdge();
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
     * <li>proxyHost: define chrome proxy host if set
     * <li>proxyPort: define chrome proxy port if set
     * <li>browserProxyPort (0): set the browser proxy port to be used (0 means automatically selected by the JVM/system)
     * <li>implicitlyWait (10): timeout in seconds to load a page
     * <li>pageLoadTimeout (10): timeout in seconds when waiting for a DOM element
     * <li>maximize (false): toggle to maximum the chrome windows
     * </ul>
     */
    @Keyword(schema = "{ \"properties\": { "
            + "\"Chrome_Driver\": {\"type\": \"string\"},"
            + "\"Chrome_Path\": {\"type\": \"string\"},"
            + "\"Headless\": {  \"type\": \"boolean\"},"
            + "\"Proxy_Host\": {\"type\": \"string\"},"
            + "\"Proxy_Port\": {\"type\": \"integer\"},"
            + "\"No_Proxy\": {\"type\": \"string\"},"
            + "\"Login_With_Client_Certificate\": {\"type\": \"boolean\"},"
            + "\"Browser_Proxy_Port\": {\"type\": \"integer\"},"
            + "\"Browser_Proxy_Client_Certificate_Path\": {\"type\": \"string\"},"
            + "\"Browser_Proxy_Client_Certificate_Password\": {\"type\": \"string\"},"
            + "\"Implicitly_Wait\": {  \"type\": \"integer\"},"
            + "\"PageLoad_Timeout\": {  \"type\": \"integer\"},"
            + "\"Maximize\": {  \"type\": \"boolean\"}"
            + "}, \"required\" : []}", properties = { "" },
            description="Keyword used to create a edge driver with multiple options.")
    public void Open_Edge_Advanced() {
        openEdge();
    }
}

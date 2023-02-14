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
package ch.exense.step.examples.selenium;

import ch.exense.step.examples.selenium.keyword.ChromeDriverKeyword;
import ch.exense.step.examples.selenium.keyword.GenericSeleniumKeyword;
import ch.exense.step.examples.selenium.keyword.ShadowSeleniumKeyword;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;

import javax.json.JsonObject;
import javax.json.Json;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SeleniumLibTest {
	private static final String TEST_FOLDER = "src\\test\\resources\\";

	private KeywordRunner.ExecutionContext ctx;
	private Output<JsonObject> output;
	private String inputs;

	@Before
	public void setUp() throws Exception {
		Map<String, String> properties = new HashMap<>();
		ctx = KeywordRunner.getExecutionContext(properties, ShadowSeleniumKeyword.class, GenericSeleniumKeyword.class, ChromeDriverKeyword.class);

		inputs = Json.createObjectBuilder().add("Headless", true).build().toString();
		output = ctx.run("Open_Chrome", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());
	}

	@Test
	public void TestNavigateTo() throws Exception{
	    File testFile = new File(TEST_FOLDER+"test.html");
		inputs = Json.createObjectBuilder()
				.add("Url", "file:///" + testFile.getAbsolutePath())
				.build().toString();
		output = ctx.run("Navigate_To", inputs);
		System.out.println(output.getError());
		assertNull(output.getError());
	}

	@Test
	public void TestSendKeys() throws Exception{
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/input[1]")
				.add("Keys", "Test item")
				.build().toString();
		output = ctx.run("Send_Keys", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());
	}

	@Test
	public void TestShadowSendKeys() throws Exception{
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#shadow, #shadow_fname")
				.add("Keys", "Test item")
				.build().toString();
		output = ctx.run("Shadow_Send_Keys", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());
	}

	@Test
	public void TestClick() throws Exception{
	    TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/p[1]/button")
				.build().toString();
		output = ctx.run("Click", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());

		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/p[1]/button")
				.build().toString();
		output = ctx.run("Double_Click", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());

		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/div[3]" )
				.build().toString();
		output = ctx.run("Get_Text", inputs);
		assertEquals("3", output.getPayload().getString("Text"));
	}

	@Test
	public void TestShadowClick() throws Exception{
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#shadow, p > button")
				.build().toString();
		output = ctx.run("Shadow_Click", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());

		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#shadow, p > button")
				.build().toString();
		output = ctx.run("Shadow_Double_Click", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());

		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/div[3]" )
				.build().toString();
		output = ctx.run("Get_Text", inputs);
		assertEquals("3", output.getPayload().getString("Text"));
	}

	@Test
	public void TestHover() throws Exception {
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/div[2]")
				.add("Optional", true)
				.build().toString();
		output = ctx.run("Is_Displayed", inputs);
		assertNull(output.getError());
		assertFalse(output.getPayload().getBoolean("Exists"));

		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/div[1]")
				.build().toString();
		output = ctx.run("Hover", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());

		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/div[2]")
				.add("Optional", true)
				.build().toString();
		output = ctx.run("Is_Displayed", inputs);
		assertNull(output.getError());
		assertTrue(output.getPayload().getBoolean("Exists"));
	}

	@Test
	public void TestShadowHover() throws Exception {
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#shadow, div.hiddenText")
				.add("Timeout", 5)
				.add("Optional", true)
				.build().toString();
		output = ctx.run("Shadow_Is_Displayed", inputs);
		assertNull(output.getError());
		assertFalse(output.getPayload().getBoolean("Exists"));

		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#shadow, div.hoverDiv")
				.build().toString();
		output = ctx.run("Shadow_Hover", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());

		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#shadow, div.hiddenText")
				.add("Optional", true)
				.build().toString();
		output = ctx.run("Shadow_Is_Displayed", inputs);
		assertNull(output.getError());
		assertTrue(output.getPayload().getBoolean("Exists"));
	}

	/**
	 * This method is just used to check if we can execute Javascript correclty via Selenium.
	 * Since changing how Selenium executes Javascript is out of our control.
	 * It assumes that Selenium can properly execute it and just checks if it can access it.
	 * @throws Exception
	 */
	@Test
	public void TestExecuteJavascript() throws Exception{
		inputs = Json.createObjectBuilder()
				.add("Javascript_To_Execute", "alert(5);")
				.build().toString();
		output = ctx.run("Execute_Javascript", inputs);
		assertNull(output.getError());
	}

	@Test
	public void TestGetText() throws Exception{
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/p[5]")
				.build().toString();
		output = ctx.run("Get_Text", inputs);
		assertNull(output.getError());
		assertEquals("This is a p element.", output.getPayload().getString("Text"));
	}

	@Test
	public void TestShadowGetText() throws Exception{
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#container, #inside")
				.build().toString();
		output = ctx.run("Shadow_Get_Text", inputs);
		assertNull(output.getError());
		assertEquals("Inside Shadow DOM\nH2", output.getPayload().getString("Text"));
	}

	@Test
	public void TestIsVisible() throws Exception{
	    TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/div[1]")
				.add("Timeout", 1)
				.add("Optional", true)
				.build().toString();
		output = ctx.run("Is_Displayed", inputs);
		assertTrue(output.getPayload().getBoolean("Exists"));
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/div[2]")
				.add("Timeout", 1)
				.add("Optional", true)
				.build().toString();
		output = ctx.run("Is_Displayed", inputs);
		assertNull(output.getError());
		assertFalse(output.getPayload().getBoolean("Exists"));
	}

	@Test
	public void TestShadowIsVisible() throws Exception{
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#shadow, div.hoverDiv")
				.add("Timeout", 1)
				.add("Optional", true)
				.build().toString();
		output = ctx.run("Shadow_Is_Displayed", inputs);
		assertTrue(output.getPayload().getBoolean("Exists"));
		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#shadow, div.hiddenText")
				.add("Timeout", 1)
				.add("Optional", true)
				.build().toString();
		output = ctx.run("Shadow_Is_Displayed", inputs);
		assertNull(output.getError());
		assertFalse(output.getPayload().getBoolean("Exists"));
	}

	@Test
	public void TestIFrame() throws Exception{
		TestNavigateTo();
	    inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/iframe")
				.build().toString();
	    output = ctx.run("Enter_Iframe", inputs);
		assertNull(output.getError());
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/p")
				.build().toString();
		output = ctx.run("Get_Text", inputs);
		assertNull(output.getError());
		assertEquals("Le Iframe text", output.getPayload().getString("Text"));
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/iframe")
				.build().toString();
		output = ctx.run("Exit_Iframe", inputs);
		assertNull(output.getError());
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/p")
				.build().toString();
		output = ctx.run("Get_Text", inputs);
		assertNull(output.getError());
	}

	@Test
	public void TestShadowIFrame() throws Exception{
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#shadow, iframe")
				.build().toString();
		output = ctx.run("Shadow_Enter_Iframe", inputs);
		assertNull(output.getError());
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/p")
				.build().toString();
		output = ctx.run("Get_Text", inputs);
		assertNull(output.getError());
		assertEquals("Le Iframe text", output.getPayload().getString("Text"));
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/iframe")
				.build().toString();
		output = ctx.run("Exit_Iframe", inputs);
		assertNull(output.getError());
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/p")
				.build().toString();
		output = ctx.run("Get_Text", inputs);
		assertNull(output.getError());
	}

	@Test
	public void TestSelectOption()throws Exception{
	    TestNavigateTo();
	    inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/select")
				.add("Index", 1)
				.build().toString();
	    ctx.run("Select_Option", inputs);
	    assertNull(output.getError());
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/select")
				.add("Text", "Opel")
				.build().toString();
		ctx.run("Select_Option", inputs);
		assertNull(output.getError());
	}

	@Test
	public void TestShadowSelectOption()throws Exception{
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#shadow, #shadow_cars")
				.add("Index", 1)
				.build().toString();
		ctx.run("Shadow_Select_Option", inputs);
		assertNull(output.getError());
		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#shadow, #shadow_cars")
				.add("Text", "Opel")
				.build().toString();
		ctx.run("Shadow_Select_Option", inputs);
		assertNull(output.getError());
	}

	@Test
	public void TestSelectWindow() throws Exception{
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/button[2]")
				.build().toString();
		output = ctx.run("Click", inputs);
		inputs = Json.createObjectBuilder().build().toString();
		output = ctx.run("Get_Window_Handles", inputs);
		String newWindow = output.getPayload().getString("Popups");
		inputs = Json.createObjectBuilder()
				.add("Handle", newWindow)
				.build().toString();
		TestNavigateTo();
	}

	@Test
	public void TestGetWindowHandles() throws Exception{
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html/body/button[2]")
				.build().toString();
		output = ctx.run("Click", inputs);
		inputs = Json.createObjectBuilder().build().toString();
		output = ctx.run("Get_Window_Handles", inputs);
	}

	@Test
	public void TestSetScrollTop()throws Exception{
        TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Xpath", "/html")
				.add("ScrollTop", "300")
				.build().toString();
		output = ctx.run("Set_ScrollTop", inputs);
	}

	@Test
	public void TestShadowSetScrollIntoView()throws Exception{
		TestNavigateTo();
		inputs = Json.createObjectBuilder()
				.add("Shadow_Selectors", "#shadow, p > button")
				.build().toString();
		output = ctx.run("Shadow_Set_ScrollIntoView", inputs);
	}

	@After
	public void destroy() throws Exception{
		output = ctx.run("Close_Driver", "{}");
		System.out.println(output.getPayload());
		assertNull(output.getError());
	}
}

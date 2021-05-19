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
package ch.exense.step.library.ldap.keywords;

import ch.exense.step.library.kw.ldap.LdapKeywords;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNull;

public class LdapKeywordsTest {

	private KeywordRunner.ExecutionContext ctx;
	private Output<JsonObject> output;
	private String inputs;

	@Before
	public void setUp() throws Exception{
		Map<String, String> properties = new HashMap<>();
		ctx = KeywordRunner.getExecutionContext(properties, LdapKeywords.class);
	}

	@After
	public void tearDown(){

	}

	@Test
	public void simpleLdapSearch() throws Exception {
	 inputs = Json.createObjectBuilder()
			.add("LdapUrl", "ldaps://ldap.exense.ch")
			.add("BindingUser", "cn=Test User,ou=users,dc=exense,dc=ch")
			.add("BindingPassword", "100%Test")
			.build().toString();
	output = ctx.run("InitLdapClient", inputs);
		System.out.println(output.getPayload());
	assertNull(output.getError());


	inputs = Json.createObjectBuilder()
			.add("BaseDN", "dc=exense,dc=ch")
				.add("UserFilter", "cn={user}")
				.add("User", "Gianluca Notaro")
				.build().toString();
	output = ctx.run("LdapSimpleSearch", inputs);
		System.out.println(output.getPayload());
	assertNull(output.getError());
	}
}


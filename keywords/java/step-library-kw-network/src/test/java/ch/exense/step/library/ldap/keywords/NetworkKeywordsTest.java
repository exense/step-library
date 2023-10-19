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
import ch.exense.step.library.kw.ssl.SSLKeywords;
import ch.exense.step.library.tests.LocalOnly;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@Category(LocalOnly.class)
public class NetworkKeywordsTest {

    private KeywordRunner.ExecutionContext ctx =
            KeywordRunner.getExecutionContext(Map.of("cn=test,ou=users,dc=exense,dc=ch_Password",
                    "The_password_goes_here"), LdapKeywords.class, SSLKeywords.class);
    private Output<JsonObject> output;
    private String inputs;

	@Test
	public void simpleCertificateChecks() throws Exception {
        inputs = Json.createObjectBuilder()
                .add("Url", "ldap.exense.ch")
                .add("Port", 636)
                .build().toString();
        output = ctx.run("Extract_SSL_Info", inputs);
        System.out.println(output.getPayload());
        assertNull(output.getError());

        inputs = Json.createObjectBuilder()
                .add("Url", "test.stepcloud.ch")
                .add("ExtractAll", true)
                .build().toString();
        output = ctx.run("Extract_SSL_Info", inputs);
        System.out.println(output.getPayload());
        assertNull(output.getError());
	}

    @Test
    public void simpleLdapSearch() throws Exception {
        inputs = Json.createObjectBuilder()
                .add("LdapUrl", "ldaps://ldap.exense.ch")
                .add("BindingUser", "cn=test,ou=users,dc=exense,dc=ch")
                .build().toString();
        output = ctx.run("Init_Ldap_Client", inputs);
        System.out.println(output.getPayload());
        assertNull(output.getError());


        inputs = Json.createObjectBuilder()
                .add("BaseDN", "dc=exense,dc=ch")
                .add("UserFilter", "cn={user}")
                .add("User", "Jerome Brongniart")
                .build().toString();
        output = ctx.run("Ldap_Simple_Search", inputs);
        System.out.println(output.getPayload());
        assertNull(output.getError());
        assertEquals(output.getPayload().getString("Result"),"cn=Jerome Brongniart,ou=users,dc=exense,dc=ch");
    }
}


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
package ch.exense.step.library.kw.ldap;

import ch.exense.step.library.commons.AbstractEnhancedKeyword;
import ch.exense.step.library.commons.BusinessException;
import org.ldaptive.*;
import org.ldaptive.auth.*;
import step.handlers.javahandler.Keyword;

public class LdapKeywords extends AbstractEnhancedKeyword {

    /**
     * step Keyword to init an Ldap client to be placed in the
     * current step session
     *
     * Keyword inputs BasicAuthUser:
     *
     */
    @Keyword(schema = "{\"properties\":{"
            + "\"LdapUrl\":{\"type\":\"string\"},"
            + "\"LdapPort\":{\"type\":\"string\"},"
            + "\"BindingUser\":{\"type\":\"string\"},"
            + "\"BindingPassword\":{\"type\":\"string\"},"
            + "\"UseTls\":{\"type\":\"boolean\"},"
            + "\"BasicAuthPort\":{\"type\":\"string\"},"
            + "\"KeyStorePath\":{\"type\":\"string\"},"
            + "\"KeyStorePassword\":{\"type\":\"string\"},"
            + "\"TimeoutInMs\":{\"type\":\"string\"},"
            + "\"CustomDnsResolverTargetIP\":{\"type\":\"string\"},"
            + "\"CustomDnsResolverHostWithCustomDns\":{\"type\":\"string\"}"
            + "},\"required\":[\"LdapUrl\"]}", properties = { "" })
    public void Init_Ldap_Client() {

        String ldapUrl = input.getString("LdapUrl");
        boolean useTls = input.getBoolean("BindingPassword",false);

        ConnectionConfig.Builder builder = ConnectionConfig.builder()
                .url(ldapUrl).useStartTLS(useTls);

        String ldapUser = input.getString("BindingUser",null);
        String ldapPwd = input.getString("BindingPassword",null);

        if (ldapUser!=null) {
            builder = builder.connectionInitializers(new BindConnectionInitializer(ldapUser, new Credential(ldapPwd)));
        }

        // Ldap certificate
//        String pathTbJks =  input.getString("KeyStorePath");
//        String jksPassword = input.getString("KeyStorePassword");

        ConnectionConfig connConfig = builder.build();

        getSession().put(connConfig);
    }

    /**
     * step Keyword to init an Ldap client to be placed in the
     * current step session
     *
     */
    @Keyword(schema = "{\"properties\":{"
            + "\"BaseDN\":{\"type\":\"string\"},"
            + "\"UserFilter\":{\"type\":\"string\"},"
            + "\"SubtreeSearch\":{\"type\":\"boolean\"},"
            + "\"AllowMultipleDns\":{\"type\":\"boolean\"},"
            + "\"User\":{\"type\":\"string\"}"
            + "},\"required\":[\"BaseDN\",\"UserFilter\",\"User\"]}", properties = { "" })
    public void Ldap_Simple_Search() {

        String baseDN = input.getString("BaseDN");
        String userFilter = input.getString("UserFilter");
        String user = input.getString("User");

        boolean subtreeSearch = input.getBoolean("SubtreeSearch",true);
        boolean allowMultipleDns = input.getBoolean("AllowMultipleDns",true);

        SearchDnResolver dnResolver = getSearchDnResolverFromSession();

        dnResolver.setBaseDn(baseDN);
        dnResolver.setUserFilter(userFilter);
        dnResolver.setSubtreeSearch(subtreeSearch);
        dnResolver.setAllowMultipleDns(allowMultipleDns);

        try {
            String res = dnResolver.resolve(new User(user));
            output.add("Result",res!=null?res:"null");

        } catch (LdapException e) {
            output.setError("Error when executing the search",e);
        }
    }

    private SearchDnResolver getSearchDnResolverFromSession() {
        SearchDnResolver dnResolver = getSession().get(SearchDnResolver.class);
        if (dnResolver==null) {
            ConnectionConfig connConfig = getSession().get(ConnectionConfig.class);
            if (connConfig==null) {
                throw new BusinessException("No connection detail available. Please call 'InitLdapClient' before calling any other ldap keywords");
            }
            dnResolver = SearchDnResolver.builder()
                    .factory(new DefaultConnectionFactory(connConfig)).build();
            getSession().put(dnResolver);
        }
        return dnResolver;
    }
}

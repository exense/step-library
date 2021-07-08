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
package ch.exense.step.library.kw.ssl;

import ch.exense.step.library.commons.AbstractEnhancedKeyword;
import ch.exense.step.library.commons.BusinessException;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.Keyword;

import javax.net.ssl.*;
import java.net.ConnectException;
import java.security.cert.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class SSLKeywords extends AbstractEnhancedKeyword {

    /**
     * Extract the basic properties of the certificate of a given ssl connection
     * <p>
     * Keyword inputs:
     */
    @Keyword(schema = "{\"properties\":{"
            + "\"Url\":{\"type\":\"string\"},"
            + "\"Port\":{\"type\":\"integer\"},"
            + "\"ExtractAll\":{\"type\":\"boolean\"}"
            + "},\"required\":[\"Url\"]}", properties = {""})
    public void Extract_SSL_Info() throws Exception {

        String url = input.getString("Url");
        int port = input.getInt("Port", 443);
        boolean all = input.getBoolean("ExtractAll", false);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        X509TrustManager passThroughTrustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        sslContext.init(null, new TrustManager[]{passThroughTrustManager}, null);

        try (SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket(url, port)) {

            Certificate[] listCert = socket.getSession().getPeerCertificates();

            if (listCert.length == 0) {
                throw new BusinessException("No certificate(s) found for host '" + url + "' on port '" + port + "'");
            } else if (all) {
                output.add("NbCerts", listCert.length);
            }
            int suffix = 0;
            String stringSuffix = "";

            DateFormat dateFormat = new SimpleDateFormat("dd.MM.YYYY HH.mm.ss");

            for (Certificate cert : listCert) {
                if (all) {
                    stringSuffix = "_" + suffix;
                }
                if (cert.getType().equals("X.509")) {
                    X509Certificate X509cert = (X509Certificate) cert;
                    output.add("NotBefore" + stringSuffix, dateFormat.format(X509cert.getNotBefore()));
                    output.add("NotAfter" + stringSuffix, dateFormat.format(X509cert.getNotAfter()));

                    output.add("SubjectDN" + stringSuffix, X509cert.getSubjectDN().getName());
                    output.add("IssuerDN" + stringSuffix, X509cert.getIssuerDN().getName());

                    output.add("Algorithm" + stringSuffix, X509cert.getSigAlgName());
                } else {
                    throw new Exception("Unknown certificate type '" + cert.getType() + "'");
                }
                if (!all && suffix == 0) {
                    break;
                } else {
                    suffix++;
                }
            }
        } catch (ConnectException e) {
            output.addAttachment(AttachmentHelper.generateAttachmentForException(e));
            throw new BusinessException("Connection error when trying to connect to the host '" + url + "' on port '" + port + "'");
        } catch (SSLPeerUnverifiedException e) {
            output.addAttachment(AttachmentHelper.generateAttachmentForException(e));
            throw new BusinessException("The host '" + url + "' does not seems to provide a server certificate on port '" + port + "'");
        }
    }
}

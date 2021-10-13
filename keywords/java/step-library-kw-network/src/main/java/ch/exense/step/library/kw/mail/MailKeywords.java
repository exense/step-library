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
package ch.exense.step.library.kw.mail;

import ch.exense.step.library.commons.AbstractEnhancedKeyword;
import ch.exense.step.library.commons.BusinessException;
import org.ldaptive.auth.SearchDnResolver;
import step.handlers.javahandler.Keyword;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Properties;
import java.io.Closeable;

public class MailKeywords extends AbstractEnhancedKeyword {

    private class MailWrapper implements Closeable {
        private final Store store;
        private final Session mailSession;

        public MailWrapper(Session mailSession, Store store) {
            this.mailSession = mailSession;
            this.store = store;
        }

        @Override
        public void close() throws IOException {
            try {
                store.close();
            } catch (MessagingException e) {
                throw new IOException(e);
            }
        }
    }

    /**
     * step Keyword to init an Mail client to be placed in the
     * current step session
     *
     * Keyword inputs BasicAuthUser:
     *
     */
    @Keyword(schema = "{\"properties\":{"
            + "\"SmtpHost\":{\"type\":\"string\"},"
            + "\"SmtpPort\":{\"type\":\"integer\"},"
            + "\"Username\":{\"type\":\"string\"},"
            + "\"Password\":{\"type\":\"string\"},"
            + "\"StoreProtocol\":{\"type\":\"string\"},"
            + "\"StorePort\":{\"type\":\"integer\"}"
            + "},\"required\":[\"SmtpHost\",\"Username\",\"Password\"]}", properties = { "" })
    public void Init_Mail_Client() throws MessagingException {

        String host = input.getString("SmtpHost");
        int port = input.getInt("SmtpPort",587);

        String username = input.getString("Username");
        String password =  input.getString("Password");

        String storeProtocol =  input.getString("StoreProtocol","pop3");
        int storePort = input.getInt("StorePort",-1);

        Properties props = new Properties();
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ssl.trust", host);

        props.put("mail.imap.auth", true);
        props.put("mail.imap.starttls.enable", "true");
        props.put("mail.imap.host", host);
        props.put("mail.imap.port", storePort);
        props.put("mail.imap.ssl.trust", host);

        Session mailSession = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username,password);
            }
        });

        Store store = mailSession.getStore(storeProtocol);
        if (storePort==-1) {
            store.connect(host, username, password);
        } else {
            store.connect(host, storePort, username, password);
        }

        MailWrapper wrapper = new MailWrapper(mailSession,store);
        getSession().put(wrapper);
    }

    /**
     *
     */
    @Keyword(schema = "{\"properties\":{"
            + "\"EmailFrom\":{\"type\":\"string\"},"
            + "\"EmailTo\":{\"type\":\"string\"},"
            + "\"Subject\":{\"type\":\"string\"},"
            + "\"Text\":{\"type\":\"string\"}"
            + "},\"required\":[\"EmailFrom\",\"EmailTo\",\"Subject\",\"Text\"]}", properties = { "" })
    public void Mail_Send() throws MessagingException {
        MailWrapper wrapper = getSession().get(MailWrapper.class);

        String emailFrom = input.getString("EmailFrom");
        String emailTo = input.getString("EmailTo");

        String subject = input.getString("Subject");
        String text = input.getString("Text");

        MimeMessage msg = new MimeMessage(wrapper.mailSession);
        msg.setFrom(emailFrom);
        msg.setRecipients(Message.RecipientType.TO,emailTo);
        msg.setSubject(subject);
        msg.setSentDate(new Date());
        msg.setText(text);
        Transport.send(msg);
    }

    /**
     *
     */
    @Keyword(schema = "{\"properties\":{},\"required\":[]}", properties = { "" })
    public void Mail_List_Folders() throws MessagingException, IOException {
        MailWrapper wrapper = getSession().get(MailWrapper.class);

        for (Folder f : wrapper.store.getPersonalNamespaces()) {
            output.add(f.getName(),f.getMessageCount());
        }
    }

    /**
     *
     */
    @Keyword(schema = "{\"properties\":{"
            + "\"Folder\":{\"type\":\"string\"}"
            + "},\"required\":[]}", properties = { "" })
    public void Mail_Read_Folder() throws MessagingException, IOException {
        MailWrapper wrapper = getSession().get(MailWrapper.class);

        String folder = input.getString("Folder","INBOX");

        Folder emailFolder = wrapper.store.getFolder(folder);

        emailFolder.open(Folder.READ_ONLY);
        Message message = emailFolder.getMessage(emailFolder.getMessageCount());
        output.add("subject",message.getSubject());
        output.add("content",message.getContent().toString());
    }
}

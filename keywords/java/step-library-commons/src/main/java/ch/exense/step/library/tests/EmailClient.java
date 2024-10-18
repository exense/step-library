package ch.exense.step.library.tests;

import jakarta.mail.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmailClient {
    private final String smtpHost;
    private final String emailAddress;
    private final String emailPassword;

    public EmailClient(String smtpHost, String emailAddress, String emailPassword) {
        this.smtpHost = smtpHost;
        this.emailAddress = emailAddress;
        this.emailPassword = emailPassword;
    }


    public List<Message> getMessagesByRecipient(String recipientEmail, Folder emailFolder) throws MessagingException {
        List<Message> result = new ArrayList<>();
        Message[] messages = emailFolder.getMessages();

        for (Message message : messages) {
            Address[] recipients = message.getRecipients(Message.RecipientType.TO);
            if (recipients != null) {
                for (Address address : recipients) {
                    if (address.toString().equalsIgnoreCase(recipientEmail)) {
                        result.add(message);
                        break;
                    }
                }
            }
        }

        return result;
    }

    public Message getEmail(String recipientEmail, String emailSubject) throws MessagingException, InterruptedException {
        Store store = null;
        Folder emailFolder = null;
        try {
            store = connectToStore();
            emailFolder = openInbox(store, Folder.READ_ONLY);
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 60000) {
                List<Message> messages = getMessagesByRecipient(recipientEmail, emailFolder);

                for (Message message : messages) {
                    if (message.getSubject().contains(emailSubject)) {
                        return message;
                    }
                }
                Thread.sleep(3000); // Wait for 3 seconds before checking again
            }
            return null;
        } finally {
            if (emailFolder != null && emailFolder.isOpen()) {
                emailFolder.close(false);
            }
            if (store != null) {
                store.close();
            }
        }
    }

    public String getContentFromEmail(Message email) throws MessagingException, IOException {
        Object content = email.getContent();
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof Multipart) {
            return this.getTextFromMimeMultipart((Multipart) content);
        }
        return "";
    }

    private Store connectToStore() throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.imap.host", smtpHost);
        Session emailSession = Session.getDefaultInstance(properties);
        Store store = emailSession.getStore("imaps");
        store.connect(smtpHost, emailAddress, emailPassword);
        return store;
    }

    private Folder openInbox(Store store, int mode) throws MessagingException {
        Folder emailFolder = store.getFolder("INBOX");
        emailFolder.open(mode);
        return emailFolder;
    }

    private String getTextFromMimeMultipart(Multipart multipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = multipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent().toString());
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result.append(html);
            } else if (bodyPart.getContent() instanceof Multipart) {
                result.append(getTextFromMimeMultipart((Multipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }}

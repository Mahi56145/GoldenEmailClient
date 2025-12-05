import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeBodyPart;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmailReceiver {
    private String userEmail;
    private String userPassword;
    private Store store;

    public EmailReceiver(String email, String password) {
        this.userEmail = email;
        this.userPassword = password;
    }

    // --- NEW: VALIDATION METHOD ---
    // Returns true if login works, throws Exception if it fails
    public boolean validateLogin() throws Exception {
        getStore();
        return store.isConnected();
    }
    // ------------------------------

    private Store getStore() throws Exception {
        if (store == null || !store.isConnected()) {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");

            String domain = userEmail.toLowerCase();
            String host = "imap.gmail.com";

            if (domain.contains("yahoo")) host = "imap.mail.yahoo.com";
            else if (domain.contains("outlook") || domain.contains("hotmail")) host = "outlook.office365.com";

            props.put("mail.imaps.host", host);
            props.put("mail.imaps.port", "993");

            Session session = Session.getInstance(props, null);
            store = session.getStore("imaps");
            store.connect(host, userEmail, userPassword); // This validates the password
        }
        return store;
    }

    // ... (Keep all your existing methods below: getFolderList, readSpecificEmail, etc.) ...

    public static class EmailContent { String htmlBody; List<String> attachmentNames = new ArrayList<>(); }

    public List<String> getFolderList() {
        List<String> folderNames = new ArrayList<>();
        try {
            Store currentStore = getStore();
            Folder[] folders = currentStore.getDefaultFolder().list("*");
            for (Folder folder : folders) {
                if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) folderNames.add(folder.getFullName());
            }
        } catch (Exception e) { e.printStackTrace(); }
        return folderNames;
    }

    public List<String> getEmailSubjects(String folderName) {
        List<String> subjects = new ArrayList<>();
        try {
            Store currentStore = getStore();
            Folder folder = currentStore.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            Message[] messages = folder.getMessages();
            int start = Math.max(0, messages.length - 10);
            for (int i = messages.length - 1; i >= start; i--) {
                subjects.add(messages[i].getSubject() + " (From: " + messages[i].getFrom()[0] + ")");
            }
            folder.close(false);
        } catch (Exception e) { e.printStackTrace(); }
        return subjects;
    }

    public EmailContent readSpecificEmail(String folderName, int indexFromTop) {
        EmailContent content = new EmailContent();
        try {
            Store currentStore = getStore();
            Folder folder = currentStore.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            Message[] messages = folder.getMessages();
            int actualIndex = messages.length - 1 - indexFromTop;
            if (actualIndex >= 0 && actualIndex < messages.length) {
                Message msg = messages[actualIndex];
                content.htmlBody = getTextAndExtractFilenames(msg, content.attachmentNames);
                String header = "<h3>Subject: " + msg.getSubject() + "</h3>" +
                        "<p><b>From:</b> " + msg.getFrom()[0] + "</p><hr>";
                content.htmlBody = header + content.htmlBody;
            }
            folder.close(false);
        } catch (Exception e) { e.printStackTrace(); content.htmlBody = "Error: " + e.getMessage(); }
        return content;
    }

    public String downloadAttachment(String folderName, int indexFromTop, String filenameToDownload) {
        try {
            Store currentStore = getStore();
            Folder folder = currentStore.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            Message[] messages = folder.getMessages();
            int actualIndex = messages.length - 1 - indexFromTop;
            Message msg = messages[actualIndex];
            String userHome = System.getProperty("user.home");
            String downloadPath = userHome + File.separator + "Downloads" + File.separator + filenameToDownload;
            boolean found = saveSpecificPart(msg, filenameToDownload, downloadPath);
            folder.close(false);
            if (found) return "Saved to Downloads: " + filenameToDownload;
            else return "Error: Could not find attachment.";
        } catch (Exception e) { return "Error downloading: " + e.getMessage(); }
    }

    private boolean saveSpecificPart(Part part, String targetName, String destPath) throws Exception {
        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                if (saveSpecificPart(mp.getBodyPart(i), targetName, destPath)) return true;
            }
        } else if (part.getFileName() != null && part.getFileName().equalsIgnoreCase(targetName)) {
            ((MimeBodyPart) part).saveFile(destPath);
            return true;
        }
        return false;
    }

    private String getTextAndExtractFilenames(Part part, List<String> attachments) throws Exception {
        if (part.isMimeType("text/plain")) return part.getContent().toString();
        else if (part.isMimeType("text/html")) return part.getContent().toString();
        else if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < mp.getCount(); i++) result.append(getTextAndExtractFilenames(mp.getBodyPart(i), attachments));
            return result.toString();
        } else if (part.getFileName() != null) {
            attachments.add(part.getFileName());
        }
        return "";
    }
}
package io.github.ckofa.filewatcher.model;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A class for sending email notifications via the SMTP protocol. It supports both synchronous and asynchronous sending
 * for public email services (e.g., Gmail, Yahoo) and intra-corporate mail (without domain authentication and SSL).
 * Resources are managed via a customizable thread pool.
 */
public final class EmailNotificationSender {

    /**
     * Callback interface for handling the result of asynchronous email sending operations.
     */
    public interface EmailCallback {

        /**
         * Called when the email is sent successfully.
         * @param result A message indicating the success of the operation.
         */
        void onSuccess(String result);

        /**
         * Called when the email sending fails.
         * @param error A message describing the failure reason.
         */
        void onFailure(String error);
    }

    private final String SENDERS_EMAIL;
    private final Session session;
    private final ExecutorService executor = Executors.newFixedThreadPool(5); // Optimized for typical email sending workloads

    /**
     * Private constructor to enforce the use of factory methods.
     *
     * @param sendersEmail      The sender's email address (e.g., user@example.com).
     * @param sendersName       The sender's username (full email for public services, username only for corporate).
     * @param sendersPassword   The sender's email password or authentication token.
     * @param mailSmtpHost      The SMTP server hostname (e.g., smtp.gmail.com).
     * @param smtpPort          The SMTP server port (e.g., 587 for TLS, 25 for non-SSL).
     * @param useSSL            True to enable STARTTLS encryption, false otherwise.
     */
    private EmailNotificationSender(String sendersEmail, String sendersName, String sendersPassword, String mailSmtpHost, int smtpPort, boolean useSSL) {
        this.SENDERS_EMAIL = sendersEmail;
        Properties mailProps = new Properties();
        mailProps.put("mail.transport.protocol", "smtp"); //The message transfer protocol used
        mailProps.put("mail.smtp.host", mailSmtpHost);
        mailProps.put("mail.smtp.port", smtpPort);
        mailProps.put("mail.smtp.starttls.enable", Boolean.toString(useSSL));
        mailProps.put("mail.smtp.auth", "true"); //Enabling authentication
        //Creating a Session object with authentication
        session = Session.getInstance(mailProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sendersName, sendersPassword);
            }
        });
    }

    /**
     * Factory method for creating a public email sender (e.g., Gmail, Yahoo).
     * Uses full email as the login and enables SSL.
     *
     * @param sendersEmail    The sender's email address.
     * @param sendersPassword The sender's email password.
     * @param mailSmtpHost    The SMTP server address.
     * @param smtpPort        The SMTP server port.
     * @return A configured instance of EmailNotificationSender.
     * @throws IllegalArgumentException if sender email or password is invalid.
     */
    public static EmailNotificationSender createPublicEmailNotificationSender(String sendersEmail, String sendersPassword, String mailSmtpHost, int smtpPort) {
        validateEmailAuthentication(sendersEmail,sendersPassword);
        return new EmailNotificationSender(sendersEmail, sendersEmail, sendersPassword, mailSmtpHost, smtpPort, true);
    }

    /**
     * Factory method for creating a corporate email sender.
     * Uses only the username (without domain) for authentication and disables SSL.
     *
     * @param sendersEmail    The sender's corporate email address.
     * @param sendersPassword The sender's email password.
     * @param mailSmtpHost    The SMTP server address.
     * @param smtpPort        The SMTP server port.
     * @return A configured instance of EmailNotificationSender.
     * @throws IllegalArgumentException if sender email or password is invalid.
     */
    public static EmailNotificationSender createCorporateEmailNotificationSender(String sendersEmail, String sendersPassword, String mailSmtpHost, int smtpPort) {
        validateEmailAuthentication(sendersEmail,sendersPassword);
        String sendersName = sendersEmail.split("@")[0]; //Getting the sender name (only login is used for authorization without specifying the domain)
        return new EmailNotificationSender(sendersEmail, sendersName, sendersPassword, mailSmtpHost, smtpPort, false);
    }

    /**
     * Sends an email message synchronously via SMTP.
     *
     * @param textMessage       The body text of the email.
     * @param textSubject       The subject line of the email.
     * @param recipientEmail    The recipient's email address (e.g., recipient@example.com).
     * @throws MessagingException If the email sending fails due to SMTP errors.
     * @throws IllegalArgumentException If any parameter is null, blank, or malformed.
     */
    public void sendMessage(String textMessage, String textSubject, String recipientEmail) throws MessagingException {
        validateMessageParameters(textMessage, textSubject, recipientEmail);
        MimeMessage emailMessage = new MimeMessage(session);
        emailMessage.setFrom(new InternetAddress(SENDERS_EMAIL));
        emailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
        emailMessage.setSubject(textSubject);
        emailMessage.setText(textMessage);
        Transport.send(emailMessage);
    }

    /**
     * Sends an email message asynchronously via SMTP without blocking the calling thread.
     *
     * @param textMessage       The body text of the email.
     * @param textSubject       The subject line of the email.
     * @param recipientEmail    The recipient's email address (e.g., recipient@example.com).
     * @return A CompletableFuture that completes when the operation finishes or fails.
     *         If parameters are invalid, the returned CompletableFuture is completed exceptionally with an IllegalArgumentException.
     */
    public CompletableFuture<Void> sendMessageAsync(String textMessage, String textSubject, String recipientEmail) {
        try {
            validateMessageParameters(textMessage, textSubject, recipientEmail);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                sendMessage(textMessage, textSubject, recipientEmail);
            } catch (MessagingException e) {
                throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
            }
        }, executor).whenComplete((result, ex) -> {
            if (ex != null) {
                System.err.println("Failed to send email (async): " + ex.getCause().getMessage()); // ex here will be a CompletionException, its cause is our RuntimeException
            }
            // result will be null if successful, ex will be not null if there was an error
            // whenComplete does not change the result or status of a Future exception
        });
    }

    /**
     * Sends an email message asynchronously via SMTP and notifies the result via a callback.
     *
     * @param textMessage       The body text of the email.
     * @param textSubject       The subject line of the email.
     * @param recipientEmail    The recipient's email address (e.g., recipient@example.com).
     * @param callback          The callback to receive success or failure notification, or null if no callback is needed.
     * @return A CompletableFuture that completes when the operation finishes or fails.
     *         If parameters are invalid, the returned CompletableFuture is completed exceptionally with an IllegalArgumentException.
     */
    public CompletableFuture<Void> sendMessageAsync(String textMessage, String textSubject, String recipientEmail, EmailCallback callback) {
        try {
            validateMessageParameters(textMessage, textSubject, recipientEmail);
        } catch (IllegalArgumentException e) {
            if (callback != null) {
                callback.onFailure("Invalid parameters: " + e.getMessage());
            }
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                sendMessage(textMessage, textSubject, recipientEmail);
            } catch (MessagingException e) {
                throw new RuntimeException("Failed to send email (async): " + e.getMessage(), e);
            }
        }, executor).whenComplete((result, ex) -> {
            if (callback != null) {
                if (ex != null) {
                    callback.onFailure(ex.getCause().getMessage());
                    System.err.println(ex.getCause().getMessage());
                } else {
                    callback.onSuccess("Email sent successfully");
                }
            }
        });
    }

    /**
     * Shuts down the thread pool used for asynchronous email sending.
     * Waits up to 10 seconds for tasks to complete before forcing termination.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    /**
     * Validates sender email and password.
     *
     * @param sendersEmail      The sender's email address.
     * @param sendersPassword   The sender's email password.
     * @throws IllegalArgumentException if any parameter is invalid.
     */
    private static void validateEmailAuthentication(String sendersEmail, String sendersPassword) {
        if (sendersEmail == null || !sendersEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid sender email: " + sendersEmail);
        }
        if (sendersPassword == null || sendersPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
    }

    /**
     * Validates message parameters before sending an email.
     *
     * @param textMessage    The content of the email.
     * @param textSubject    The subject of the email.
     * @param recipientEmail The recipient's email address.
     * @throws IllegalArgumentException if any parameter is invalid.
     */
    private static void validateMessageParameters(String textMessage, String textSubject, String recipientEmail) {
        if (textMessage == null || textMessage.isBlank()) {
            throw new IllegalArgumentException("Message content must not be null or blank.");
        }
        if (textSubject == null || textSubject.isBlank()) {
            throw new IllegalArgumentException("Message subject must not be null or blank.");
        }
        if (recipientEmail == null || !recipientEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid recipient email: " + recipientEmail);
        }
    }
}

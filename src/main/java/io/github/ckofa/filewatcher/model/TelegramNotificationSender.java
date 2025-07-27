package io.github.ckofa.filewatcher.model;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * A utility class for sending notifications to Telegram chats using the Telegram Bot API.
 * Instances should be created using the provided static factory methods like {@code create(...)}.
 * Supports direct internet access or connection through an HTTP proxy with optional authentication.
 */
public final class TelegramNotificationSender {

    private final TelegramBot bot;

    /**
     * Private constructor to enforce the use of factory methods.
     * It takes a fully configured TelegramBot instance.
     *
     * @param bot The configured TelegramBot instance.
     */
    private TelegramNotificationSender(TelegramBot bot) {
        this.bot = bot;
    }

    /**
     * Creates a TelegramNotificationSender for direct internet access.
     *
     * @param token the Telegram bot token, must not be null or empty.
     * @return A new configured instance of TelegramNotificationSender.
     * @throws IllegalArgumentException if the token is null or empty.
     */
    public static TelegramNotificationSender create(String token) {
        validateToken(token);

        TelegramBot bot = new TelegramBot(token);
        return new TelegramNotificationSender(bot);
    }

    /**
     * Creates a TelegramNotificationSender that connects through an HTTP proxy.
     *
     * @param token        the Telegram bot token, must not be null or empty.
     * @param proxyAddress the proxy server address, must not be null or empty.
     * @param port         the proxy server port, must be between 1 and 65535.
     * @return A new configured instance of TelegramNotificationSender.
     * @throws IllegalArgumentException if any parameter is invalid.
     */
    public static TelegramNotificationSender createWithProxy(String token, String proxyAddress, int port) {
        validateToken(token);
        validateProxyConfig(proxyAddress, port);

        Proxy proxy = new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxyAddress, port));
        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .build();
        TelegramBot bot = new TelegramBot.Builder(token).okHttpClient(client).build();
        return new TelegramNotificationSender(bot);
    }

    /**
     * Creates a TelegramNotificationSender that connects through an HTTP proxy with authentication.
     *
     * @param token         the Telegram bot token, must not be null or empty.
     * @param proxyAddress  the proxy server address, must not be null or empty.
     * @param port          the proxy server port, must be between 1 and 65535.
     * @param proxyLogin    the login for proxy authentication, must not be null.
     * @param proxyPassword the password for proxy authentication, must not be null.
     * @return A new configured instance of TelegramNotificationSender.
     * @throws IllegalArgumentException if any parameter is invalid.
     */
    public static TelegramNotificationSender createWithAuthenticatedProxy(String token, String proxyAddress, int port, String proxyLogin, String proxyPassword) {
        validateToken(token);
        validateProxyConfig(proxyAddress, port);
        if (proxyLogin == null || proxyPassword == null) {
            throw new IllegalArgumentException("Proxy login and password must not be null");
        }

        Proxy proxy = new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxyAddress, port));
        Authenticator proxyAuthenticator = (route, response) -> {
            String credential = Credentials.basic(proxyLogin, proxyPassword);
            return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
        };
        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .proxyAuthenticator(proxyAuthenticator)
                .build();
        TelegramBot bot = new TelegramBot.Builder(token).okHttpClient(client).build();
        return new TelegramNotificationSender(bot);
    }

    /**
     * Synchronously sends a message to a specified Telegram chat.
     *
     * @param chatId  the ID of the Telegram chat to send the message to.
     * @param message the text message to send, must not be null or empty.
     * @throws IllegalArgumentException if the message is null or empty.
     * @throws RuntimeException         if the message cannot be sent due to network or API issues.
     */
    public void sendMessage(long chatId, String message) {
        validateMessage(message);
        bot.execute(new SendMessage(chatId, message));
    }


    /**
     * Asynchronously sends a message to a specified Telegram chat with a callback for result handling.
     *
     * @param chatId   the ID of the Telegram chat to send the message to.
     * @param message  the text message to send, must not be null or empty.
     * @param callback the callback to handle the result of the send operation.
     * @throws IllegalArgumentException if the message or callback is null or empty.
     */
    public void sendMessageAsync(long chatId, String message, Callback<SendMessage, SendResponse> callback) {
        validateMessage(message);
        if (callback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        }
        bot.execute(new SendMessage(chatId, message), callback);
    }

    /**
     * Asynchronously sends a message to a specified Telegram chat without waiting for or handling the result.
     * This is a "fire-and-forget" operation, meaning no feedback is provided on success or failure.
     *
     * @param chatId  the ID of the Telegram chat to send the message to.
     * @param message the text message to send, must not be null or empty.
     * @throws IllegalArgumentException if the message is null or empty.
     */
    public void sendMessageAsync(long chatId, String message) {
        validateMessage(message);
        bot.execute(new SendMessage(chatId, message), new Callback<SendMessage, SendResponse>() {
            @Override
            public void onResponse(SendMessage sendMessage, SendResponse sendResponse) {
                //Ignoring success
            }

            @Override
            public void onFailure(SendMessage sendMessage, IOException e) {
                System.err.println("Failed to send message to chat " + chatId + ": " + e.getMessage());
            }
        });
    }

    /**
     * Validates that the message is not null or empty.
     *
     * @param message the message to validate.
     * @throws IllegalArgumentException if the message is null or empty.
     */
    private void validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be null or empty");
        }
    }

    /**
     * Validates that the token is not null or empty.
     *
     * @param token the Telegram bot token to validate.
     * @throws IllegalArgumentException if the token is null or empty.
     */
    private static void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be null or empty");
        }
    }

    /**
     * Validates the configuration parameters for Telegram proxy setup.
     *
     * @param proxyAddress the proxy server address to validate.
     * @param port         the proxy server port to validate.
     * @throws IllegalArgumentException if any parameter is invalid.
     */
    private static void validateProxyConfig(String proxyAddress, int port) {
        if (proxyAddress == null || proxyAddress.isBlank()) {
            throw new IllegalArgumentException("Proxy address must not be null or empty");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
    }
}

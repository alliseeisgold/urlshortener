package com.urlshortener.bot;

import com.urlshortener.dto.UrlDto;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.service.UrlService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
public class TelegramBotService extends TelegramLongPollingBot {

    private final UrlService urlService;
    private final String botUsername;

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            log.info("Telegram bot registered: {}", botUsername);
        } catch (TelegramApiException e) {
            log.error("Failed to register Telegram bot", e);
        }
    }

    public TelegramBotService(
            UrlService urlService,
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername) {
        super(botToken);
        this.urlService = urlService;
        this.botUsername = botUsername;
        log.info("TelegramBotService created for: {}", botUsername);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Message message = update.getMessage();
        String text = message.getText().trim();
        long chatId = message.getChatId();

        log.debug("Received message from {}: {}", chatId, text);

        String response = processCommand(text);
        sendReply(chatId, response);
    }

    private String processCommand(String text) {
        if (text.startsWith("/shorten ")) {
            return handleShorten(text.substring(9).trim());
        } else if (text.startsWith("/info ")) {
            return handleInfo(text.substring(6).trim());
        } else if (text.startsWith("/stats ")) {
            return handleStats(text.substring(7).trim());
        } else if (text.equals("/start") || text.equals("/help")) {
            return helpMessage();
        } else if (text.startsWith("http://") || text.startsWith("https://")) {
            return handleShorten(text);
        } else {
            return "Unknown command. Send /help for usage.";
        }
    }

    private String handleShorten(String url) {
        if (url.isEmpty()) {
            return "Please provide a URL. Usage: /shorten https://example.com";
        }
        try {
            UrlDto.CreateRequest request = UrlDto.CreateRequest.builder()
                    .originalUrl(url)
                    .build();
            UrlDto.Response response = urlService.createShortUrl(request);
            return String.format("""
                            *Short URL created!*
                            
                            Short URL: `%s`
                            Original: %s
                            Expires: %s
                            """,
                    response.getShortUrl(),
                    response.getOriginalUrl(),
                    response.getExpiresAt() != null ? response.getExpiresAt().toLocalDate() : "Never");
        } catch (Exception e) {
            log.error("Failed to shorten URL: {}", url, e);
            return "Failed to shorten URL: " + e.getMessage();
        }
    }

    private String handleInfo(String shortCode) {
        if (shortCode.isEmpty()) {
            return "Please provide a short code. Usage: /info aB3xY9z";
        }
        try {
            UrlDto.Response r = urlService.getByShortCode(shortCode);
            return String.format("""
                            *URL Info*
                            
                            Code: `%s`
                            Short URL: %s
                            Original: %s
                            Clicks: %d
                            Active: %s
                            Expires: %s
                            """,
                    r.getShortCode(), r.getShortUrl(), r.getOriginalUrl(),
                    r.getClickCount(), r.getActive() ? "Yes" : "No",
                    r.getExpiresAt() != null ? r.getExpiresAt().toLocalDate() : "Never");
        } catch (UrlNotFoundException e) {
            return "Short URL not found: " + shortCode;
        } catch (Exception e) {
            log.error("Failed to get URL info: {}", shortCode, e);
            return "Failed to get URL info: " + e.getMessage();
        }
    }

    private String handleStats(String shortCode) {
        if (shortCode.isEmpty()) {
            return "Please provide a short code. Usage: /stats aB3xY9z";
        }
        try {
            UrlDto.StatsResponse stats = urlService.getStats(shortCode);
            return String.format("""
                            *Click Statistics for* `%s`
                            
                            Total clicks:    %d
                            Last 24h:        %d
                            Last 7 days:     %d
                            Active: %s
                            """,
                    stats.getShortCode(), stats.getTotalClicks(),
                    stats.getClicksLast24h(), stats.getClicksLast7d(),
                    stats.getActive() ? "Yes" : "No");
        } catch (UrlNotFoundException e) {
            return "Short URL not found: " + shortCode;
        } catch (Exception e) {
            log.error("Failed to get stats: {}", shortCode, e);
            return "Failed to get stats: " + e.getMessage();
        }
    }

    private String helpMessage() {
        return """
                *URL Shortener Bot*
                *Commands:*
                /shorten <url>    — Shorten a URL
                /info <code>      — Get URL details
                /stats <code>     — View click statistics
                /help             — Show this help
                
                *Tip:* You can also just paste a URL directly!
                """;
    }

    private void sendReply(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .build();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message to {}: {}", chatId, e.getMessage());
        }
    }
}

package com.notionbot;

import com.notionbot.bot.CommandManager;
import com.notionbot.bot.GTDListener;
import com.notionbot.config.Config;
import com.notionbot.gtd.model.GTDCaptureSession;
import com.notionbot.services.CacheService;
import com.notionbot.services.GeminiService;
import com.notionbot.services.GitHubService;
import com.notionbot.services.NotionService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static JDA jda;
    private static NotionService notionService;
    private static GeminiService geminiService;
    private static GitHubService gitHubService;
    private static CacheService<GTDCaptureSession> sessionCache;

    public static void main(String[] args) {
        logger.info("Initializing Notion GTD Assistant...");

        try {
            // Load Services
            notionService = new NotionService();
            geminiService = new GeminiService();
            gitHubService = new GitHubService();
            sessionCache = new CacheService<>(30); // 30 min duration for sessions

            // Discord Init
            String token = Config.getRequired("DISCORD_TOKEN");
            jda = JDABuilder.createDefault(token)
                    .setActivity(Activity.playing("GTD Workflow"))
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(new GTDListener(sessionCache))
                    .build();

            jda.awaitReady();

            // Register Commands
            CommandManager.registerCommands(jda);

            logger.info("Notion GTD Bot is online and services are ready!");

        } catch (Exception e) {
            logger.error("Failed to start the bot: {}", e.getMessage());
            System.exit(1);
        }
    }

    public static NotionService getNotionService() {
        return notionService;
    }

    public static GeminiService getGeminiService() {
        return geminiService;
    }

    public static GitHubService getGitHubService() {
        return gitHubService;
    }
}

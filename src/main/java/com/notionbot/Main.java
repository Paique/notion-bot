package com.notionbot;

import com.notionbot.config.Config;
import com.notionbot.services.GeminiService;
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

    public static void main(String[] args) {
        logger.info("Initializing Notion GTD Assistant...");

        try {
            // Load Services
            notionService = new NotionService();
            geminiService = new GeminiService();

            // Discord Init
            String token = Config.getRequired("DISCORD_TOKEN");
            jda = JDABuilder.createDefault(token)
                    .setActivity(Activity.playing("GTD Workflow"))
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .build();

            // Testing services (Log only)
            String inboxId = Config.get("DATABASE_INBOX_ID");
            if (inboxId != null) {
                notionService.testConnection(inboxId);
            }

            String refined = geminiService.refineText("comprar leite");
            logger.info("Gemini Test Result: {}", refined);

            // Register Listeners (Phase 2 & 3)
            // jda.addEventListener(new GTDListener());

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
}

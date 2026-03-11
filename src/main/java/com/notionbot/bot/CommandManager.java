package com.notionbot.bot;

import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);

    public static void registerCommands(JDA jda) {
        LOGGER.info("Registering slash and context commands...");

        SlashCommandData addCommand = Commands.slash("add", "Adiciona um item ao seu GTD")
                .addOptions(new OptionData(OptionType.STRING, "item", "O que você quer capturar?")
                        .setRequired(true))

                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM,
                        InteractionContextType.PRIVATE_CHANNEL);

        CommandData contextCommand = Commands.context(Command.Type.MESSAGE, "Capturar GTD")
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM,
                        InteractionContextType.PRIVATE_CHANNEL);

        jda.updateCommands().addCommands(
                addCommand,
                contextCommand).queue(
                success -> LOGGER.info("Successfully registered commands!"),
                error -> LOGGER.error("Failed to register commands", error));
    }
}

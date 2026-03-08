package com.notionbot.bot;

import com.notionbot.Main;
import com.notionbot.config.Config;
import com.notionbot.gtd.model.GTDCaptureSession;
import com.notionbot.services.CacheService;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import java.awt.Color;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GTDListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GTDListener.class);
    private final CacheService<GTDCaptureSession> sessionCache;

    public GTDListener(CacheService<GTDCaptureSession> sessionCache) {
        this.sessionCache = sessionCache;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("add"))
            return;

        String item = event.getOption("item").getAsString();
        handleCapture(event.getUser().getId(), item, event.getHook(), event);
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        if (!event.getName().equals("Capturar GTD"))
            return;

        String content = event.getTarget().getContentRaw();
        handleCapture(event.getUser().getId(), content, event.getHook(), event);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith("gtd_"))
            return;

        String userId = event.getUser().getId();
        GTDCaptureSession session = sessionCache.get(userId);

        if (session == null) {
            event.reply("Sessão expirada ou não encontrada. Tente capturar novamente.").setEphemeral(true).queue();
            return;
        }

        switch (componentId) {
            case "gtd_accept":
                session.setCurrentStep(GTDCaptureSession.FlowStep.DECIDING_ACTIONABLE);
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("✅ Item Aceito")
                        .setDescription("**Processed:** " + session.getRefinedText())
                        .addField("Próximo Passo", "Este item é **acionável**? (Requer ação física?)", false)
                        .setColor(Color.CYAN)
                        .build())
                        .setComponents(
                                ActionRow.of(
                                        Button.primary("gtd_actionable_yes", "Sim"),
                                        Button.secondary("gtd_actionable_no", "Não")))
                        .queue();
                break;

            case "gtd_actionable_yes":
                session.setCurrentStep(GTDCaptureSession.FlowStep.DECIDING_MULTISTEP);
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("🚀 Planejamento")
                        .setDescription("Este item requer **múltiplos passos** (Projeto) ou apenas um?")
                        .setColor(Color.ORANGE)
                        .build())
                        .setComponents(
                                ActionRow.of(
                                        Button.primary("gtd_multistep_yes", "Projeto (Múltiplos)"),
                                        Button.secondary("gtd_multistep_no", "Ação Única")))
                        .queue();
                break;

            case "gtd_actionable_no":
                session.setCurrentStep(GTDCaptureSession.FlowStep.PROCESSING_NOTION);
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("📂 Arquivamento")
                        .setDescription("Para onde deve ir este item **não acionável**?")
                        .setColor(Color.LIGHT_GRAY)
                        .build())
                        .setComponents(
                                ActionRow.of(
                                        Button.danger("gtd_trash", "Lixo"),
                                        Button.primary("gtd_someday", "Talvez/Um dia"),
                                        Button.secondary("gtd_reference", "Referência")))
                        .queue();
                break;

            case "gtd_multistep_yes":
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("🏗️ Novo Projeto")
                        .setDescription("Sincronizando projeto: **" + session.getRefinedText() + "**...")
                        .setColor(Color.BLUE)
                        .build())
                        .setComponents().queue();
                try {
                    Main.getNotionService().createProject(session.getRefinedText(), session.getDescription());
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setTitle("✅ Projeto Criado")
                            .setDescription("O projeto **" + session.getRefinedText() + "** foi enviado para o Notion.")
                            .setColor(Color.GREEN)
                            .build()).queue();
                } catch (Exception e) {
                    LOGGER.error("Failed to create project", e);
                    event.getHook().editOriginal("❌ Erro ao criar projeto no Notion.").queue();
                }
                sessionCache.remove(userId);
                break;

            case "gtd_multistep_no":
                session.setCurrentStep(GTDCaptureSession.FlowStep.DECIDING_2MIN);
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("⏳ Regra dos 2 Minutos")
                        .setDescription("Leva menos de **2 minutos** para fazer?")
                        .setColor(Color.YELLOW)
                        .build())
                        .setComponents(
                                ActionRow.of(
                                        Button.primary("gtd_2min_yes", "Sim (Fazer agora)"),
                                        Button.secondary("gtd_2min_no", "Não (Adiar/Delegar)")))
                        .queue();
                break;

            case "gtd_2min_yes":
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("⚡ Reação Rápida")
                        .setDescription("**Faça agora!** Assim que terminar, clique abaixo para concluir.")
                        .setColor(Color.GREEN)
                        .build())
                        .setComponents(ActionRow.of(Button.success("gtd_done_now", "Concluído!")))
                        .queue();
                break;

            case "gtd_done_now":
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("✅ Finalizado")
                        .setDescription("Que bom que resolveu logo! Registrado no Notion.")
                        .setColor(Color.GREEN)
                        .build())
                        .setComponents().queue();
                Main.getNotionService().addToActions(session.getRefinedText() + " (Concluído)",
                        session.getDescription());
                sessionCache.remove(userId);
                break;

            case "gtd_2min_no":
                session.setCurrentStep(GTDCaptureSession.FlowStep.DECIDING_DELEGATE_DEFER);
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("🤔 Próximo Passo")
                        .setDescription("Deseja **delegar** para alguém ou **adiar** para você?")
                        .setColor(Color.MAGENTA)
                        .build())
                        .setComponents(
                                ActionRow.of(
                                        Button.primary("gtd_delegate", "Delegar"),
                                        Button.secondary("gtd_defer", "Adiar")))
                        .queue();
                break;

            case "gtd_delegate":
                TextInput delegator = TextInput.create("gtd_delegate_who", TextInputStyle.SHORT)
                        .setPlaceholder("Nome da pessoa ou departamento")
                        .setRequired(true)
                        .build();
                event.replyModal(Modal.create("gtd_modal_delegate", "Delegar Tarefa")
                        .addComponents(Label.of("Para quem?", delegator))
                        .build()).queue();
                break;

            case "gtd_defer":
                TextInput deferWhen = TextInput.create("gtd_defer_when", TextInputStyle.SHORT)
                        .setPlaceholder("Ex: Próxima semana, Amanhã, 25/12")
                        .setRequired(false)
                        .build();
                event.replyModal(Modal.create("gtd_modal_defer", "Adiar Tarefa")
                        .addComponents(Label.of("Quando?", deferWhen))
                        .build()).queue();
                break;

            case "gtd_trash":
                sessionCache.remove(userId);
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("🗑️ Descartado")
                        .setDescription("Item removido do fluxo (Lixo).")
                        .setColor(Color.RED)
                        .build())
                        .setComponents().queue();
                break;

            case "gtd_someday":
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("💡 Talvez / Um Dia")
                        .setDescription("Sincronizando com o Notion...")
                        .setColor(Color.BLUE)
                        .build())
                        .setComponents().queue();
                try {
                    Main.getNotionService().addToSomeday(session.getRefinedText(), session.getDescription());
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setTitle("✅ Salvo")
                            .setDescription("Item adicionado à sua lista **Talvez / Um Dia**.")
                            .setColor(Color.GREEN)
                            .build()).queue();
                } catch (Exception e) {
                    LOGGER.error("Notion Someday error", e);
                    event.getHook().editOriginal("Erro ao salvar no Notion.").queue();
                }
                sessionCache.remove(userId);
                break;

            case "gtd_reference":
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("📚 Referência")
                        .setDescription("Sincronizando com o Notion...")
                        .setColor(Color.BLUE)
                        .build())
                        .setComponents().queue();
                try {
                    Main.getNotionService().addToReference(session.getRefinedText(), session.getDescription());
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setTitle("✅ Salvo")
                            .setDescription("Item adicionado ao seu banco de **Referência**.")
                            .setColor(Color.GREEN)
                            .build()).queue();
                } catch (Exception e) {
                    LOGGER.error("Notion Reference error", e);
                    event.getHook().editOriginal("Erro ao salvar no Notion.").queue();
                }
                sessionCache.remove(userId);
                break;

            case "gtd_discard":
                sessionCache.remove(userId);
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("🗑️ Captura Descartada")
                        .setDescription("O item foi removido do cache e não foi salvo.")
                        .setColor(Color.DARK_GRAY)
                        .build())
                        .setComponents().queue();
                break;

            case "gtd_edit":
                TextInput editTitle = TextInput.create("gtd_edit_text", TextInputStyle.SHORT)
                        .setValue(session.getRefinedText())
                        .setRequired(true)
                        .build();
                TextInput editDesc = TextInput
                        .create("gtd_edit_desc", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("Adicione mais informações sobre esta tarefa...")
                        .setValue(session.getDescription())
                        .setRequired(false)
                        .build();
                event.replyModal(Modal.create("gtd_modal_edit", "Editar Sugestão & Detalhes")
                        .addComponents(Label.of("Título:", editTitle), Label.of("Descrição (opcional):", editDesc))
                        .build()).queue();
                break;
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();
        String userId = event.getUser().getId();
        GTDCaptureSession session = sessionCache.get(userId);

        if (session == null) {
            event.reply("Sessão expirada.").setEphemeral(true).queue();
            return;
        }

        if (modalId.equals("gtd_modal_delegate")) {
            String who = event.getValue("gtd_delegate_who").getAsString();
            event.deferReply(true).queue();
            Main.getNotionService().createPage(Config.getRequired("DATABASE_WAITING_ID"),
                    session.getRefinedText() + " (Aguardando: " + who + ")", session.getDescription());
            event.getHook().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("🤝 Delegado com Sucesso")
                    .setDescription("Item: **" + session.getRefinedText() + "**")
                    .addField("Responsável", who, true)
                    .addField("Status", "Enviado para 'Aguardando' no Notion", true)
                    .setColor(Color.GREEN)
                    .build()).setEphemeral(true).queue();
            sessionCache.remove(userId);
            return;
        }

        if (modalId.equals("gtd_modal_defer")) {
            String when = event.getValue("gtd_defer_when").getAsString();
            String prefix = (when != null && !when.isEmpty()) ? "[" + when + "] " : "";
            event.deferReply(true).queue();
            Main.getNotionService().addToActions(prefix + session.getRefinedText(), session.getDescription());
            event.getHook().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("📅 Adiado com Sucesso")
                    .setDescription("Item: **" + session.getRefinedText() + "**")
                    .addField("Quando", (when == null || when.isEmpty()) ? "Não definido" : when, true)
                    .addField("Status", "Adicionado às 'Próximas Ações'", true)
                    .setColor(Color.GREEN)
                    .build()).setEphemeral(true).queue();
            sessionCache.remove(userId);
            return;
        }

        if (modalId.equals("gtd_modal_edit")) {
            String newText = event.getValue("gtd_edit_text").getAsString();
            session.setRefinedText(newText);

            if (event.getValue("gtd_edit_desc") != null) {
                session.setDescription(event.getValue("gtd_edit_desc").getAsString());
            }

            session.setCurrentStep(GTDCaptureSession.FlowStep.REFINED);

            event.editMessage("✨ **Título Atualizado:**\n> " + newText + "\n\nO que deseja fazer?")
                    .setComponents(
                            ActionRow.of(
                                    Button.primary("gtd_accept", "Aceitar"),
                                    Button.secondary("gtd_edit", "Editar"),
                                    Button.danger("gtd_discard", "Descartar")))
                    .queue();
            return;
        }
        event.reply("Modal inválido, tente novamente.").setEphemeral(true).queue();
    }

    private void handleCapture(String userId, String content, InteractionHook hook, Object event) {
        if (event instanceof IReplyCallback replyCallback) {
            replyCallback.deferReply(true).queue();
        }

        LOGGER.info("User {} is capturing: {}", userId, content);

        GTDCaptureSession session = new GTDCaptureSession(userId, content);
        sessionCache.put(userId, session);

        startRefinement(session, hook);
    }

    private void startRefinement(GTDCaptureSession session, InteractionHook hook) {
        session.setCurrentStep(GTDCaptureSession.FlowStep.REFINING);

        String githubCtx = Main.getGitHubService().extractRepoContext(session.getOriginalText());
        if (githubCtx != null) {
            session.setGithubContext(githubCtx);
        }

        Main.getGeminiService().refineTextAsync(session.getOriginalText(), githubCtx)
                .thenAccept(refined -> {
                    session.setRefinedText(refined);
                    session.setCurrentStep(GTDCaptureSession.FlowStep.REFINED);

                    hook.editOriginal("✨ **Sugestão do Gemini:**\n> " + refined + "\n\nO que deseja fazer?")
                            .setComponents(
                                    ActionRow.of(
                                            Button.primary("gtd_accept", "Aceitar"),
                                            Button.secondary("gtd_edit", "Editar"),
                                            Button.danger("gtd_discard", "Descartar")))
                            .queue();
                })
                .exceptionally(ex -> {
                    LOGGER.error("Gemini refinement failed", ex);
                    hook.editOriginal(
                            "Falha ao refinar o texto com IA. Usando original: \n> " + session.getOriginalText())
                            .queue();
                    return null;
                });
    }
}

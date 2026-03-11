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

import com.notionbot.services.GeminiService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main listener for GTD workflow interactions in Discord.
 */
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

        handleButton(event, componentId, session, userId);
    }

    private void handleButton(ButtonInteractionEvent event, String componentId, GTDCaptureSession session,
            String userId) {
        switch (componentId) {
            case "gtd_accept" -> handleAccept(event, session);
            case "gtd_actionable_yes" -> handleActionableYes(event, session);
            case "gtd_actionable_no" -> handleActionableNo(event, session);
            case "gtd_multistep_yes" -> handleMultistepYes(event, session, userId);
            case "gtd_multistep_no" -> handleMultistepNo(event, session);
            case "gtd_2min_yes" -> handle2MinYes(event, session);
            case "gtd_2min_no" -> handle2MinNo(event, session);
            case "gtd_done_now" -> handleDoneNow(event, session, userId);
            case "gtd_delegate" -> handleDelegateButton(event);
            case "gtd_defer" -> handleDeferButton(event);
            case "gtd_trash" -> handleTrash(event, userId);
            case "gtd_someday" -> handleSomeday(event, session, userId);
            case "gtd_reference" -> handleReference(event, session, userId);
            case "gtd_discard" -> handleDiscard(event, userId);
            case "gtd_edit" -> handleEditButton(event, session);
        }
    }

    private void handleAccept(ButtonInteractionEvent event, GTDCaptureSession session) {
        session.setCurrentStep(GTDCaptureSession.FlowStep.DECIDING_ACTIONABLE);
        String desc = "**Processed:** " + session.getRefinedText();
        if (session.getDueDate() != null)
            desc += "\n📅 **Data:** " + session.getDueDate();

        event.editMessageEmbeds(new EmbedBuilder()
                .setTitle("✅ Item Aceito")
                .setDescription(desc)
                .addField("Próximo Passo", "Este item é **acionável**? (Requer ação física?)", false)
                .setColor(Color.CYAN).build())
                .setComponents(ActionRow.of(Button.primary("gtd_actionable_yes", "Sim"),
                        Button.secondary("gtd_actionable_no", "Não")))
                .queue();
    }

    private void handleActionableYes(ButtonInteractionEvent event, GTDCaptureSession session) {
        session.setCurrentStep(GTDCaptureSession.FlowStep.DECIDING_MULTISTEP);
        event.editMessageEmbeds(new EmbedBuilder()
                .setTitle("🚀 Planejamento")
                .setDescription("Este item requer **múltiplos passos** (Projeto) ou apenas um?")
                .setColor(Color.ORANGE).build())
                .setComponents(ActionRow.of(Button.primary("gtd_multistep_yes", "Projeto (Múltiplos)"),
                        Button.secondary("gtd_multistep_no", "Ação Única")))
                .queue();
    }

    private void handleActionableNo(ButtonInteractionEvent event, GTDCaptureSession session) {
        session.setCurrentStep(GTDCaptureSession.FlowStep.PROCESSING_NOTION);
        event.editMessageEmbeds(new EmbedBuilder()
                .setTitle("📂 Arquivamento")
                .setDescription("Para onde deve ir este item **não acionável**?")
                .setColor(Color.LIGHT_GRAY).build())
                .setComponents(
                        ActionRow.of(Button.danger("gtd_trash", "Lixo"), Button.primary("gtd_someday", "Talvez/Um dia"),
                                Button.secondary("gtd_reference", "Referência")))
                .queue();
    }

    private void handleMultistepYes(ButtonInteractionEvent event, GTDCaptureSession session, String userId) {
        event.editMessageEmbeds(new EmbedBuilder()
                .setTitle("🏗️ Novo Projeto")
                .setDescription("Sincronizando projeto: **" + session.getRefinedText() + "**...")
                .setColor(Color.BLUE).build()).setComponents().queue();
        try {
            Main.getNotionService().createProject(session.getRefinedText(), session.getDescription(),
                    session.getDueDate());
            event.getHook().editOriginalEmbeds(new EmbedBuilder()
                    .setTitle("✅ Projeto Criado")
                    .setDescription("O projeto **" + session.getRefinedText() + "** foi enviado para o Notion.")
                    .setColor(Color.GREEN).build()).queue();
        } catch (Exception e) {
            LOGGER.error("Failed to create project", e);
            event.getHook().editOriginal("❌ Erro ao criar projeto no Notion.").queue();
        }
        sessionCache.remove(userId);
    }

    private void handleMultistepNo(ButtonInteractionEvent event, GTDCaptureSession session) {
        session.setCurrentStep(GTDCaptureSession.FlowStep.DECIDING_2MIN);
        event.editMessageEmbeds(new EmbedBuilder()
                .setTitle("⏳ Regra dos 2 Minutos")
                .setDescription("Leva menos de **2 minutos** para fazer?")
                .setColor(Color.YELLOW).build())
                .setComponents(ActionRow.of(Button.primary("gtd_2min_yes", "Sim (Fazer agora)"),
                        Button.secondary("gtd_2min_no", "Não (Adiar/Delegar)")))
                .queue();
    }

    private void handle2MinYes(ButtonInteractionEvent event, GTDCaptureSession session) {
        event.editMessageEmbeds(new EmbedBuilder()
                .setTitle("⚡ Reação Rápida")
                .setDescription("**Faça agora!** Assim que terminar, clique abaixo para concluir.")
                .setColor(Color.GREEN).build())
                .setComponents(ActionRow.of(Button.success("gtd_done_now", "Concluído!")))
                .queue();
    }

    private void handleDoneNow(ButtonInteractionEvent event, GTDCaptureSession session, String userId) {
        event.editMessageEmbeds(new EmbedBuilder()
                .setTitle("✅ Finalizado")
                .setDescription("Que bom que resolveu logo! Registrado no Notion.")
                .setColor(Color.GREEN).build()).setComponents().queue();
        Main.getNotionService().addToActions(session.getRefinedText() + " (Concluído)", session.getDescription(),
                session.getDueDate());
        sessionCache.remove(userId);
    }

    private void handle2MinNo(ButtonInteractionEvent event, GTDCaptureSession session) {
        session.setCurrentStep(GTDCaptureSession.FlowStep.DECIDING_DELEGATE_DEFER);
        event.editMessageEmbeds(new EmbedBuilder()
                .setTitle("🤔 Próximo Passo")
                .setDescription("Deseja **delegar** para alguém ou **adiar** para você?")
                .setColor(Color.MAGENTA).build())
                .setComponents(
                        ActionRow.of(Button.primary("gtd_delegate", "Delegar"), Button.secondary("gtd_defer", "Adiar")))
                .queue();
    }

    private void handleDelegateButton(ButtonInteractionEvent event) {
        TextInput delegator = TextInput.create("gtd_delegate_who", TextInputStyle.SHORT).setPlaceholder("Pessoa/Depto")
                .setRequired(true).build();
        event.replyModal(Modal.create("gtd_modal_delegate", "Delegar Tarefa")
                .addComponents(Label.of("Para quem?", delegator)).build()).queue();
    }

    private void handleDeferButton(ButtonInteractionEvent event) {
        TextInput deferWhen = TextInput.create("gtd_defer_when", TextInputStyle.SHORT)
                .setPlaceholder("Ex: Amanhã, 25/12").setRequired(false).build();
        event.replyModal(
                Modal.create("gtd_modal_defer", "Adiar Tarefa").addComponents(Label.of("Quando?", deferWhen)).build())
                .queue();
    }

    private void handleTrash(ButtonInteractionEvent event, String userId) {
        sessionCache.remove(userId);
        event.editMessageEmbeds(new EmbedBuilder().setTitle("🗑️ Descartado").setDescription("Item removido do fluxo.")
                .setColor(Color.RED).build()).setComponents().queue();
    }

    private void handleSomeday(ButtonInteractionEvent event, GTDCaptureSession session, String userId) {
        event.editMessageEmbeds(new EmbedBuilder().setTitle("💡 Talvez / Um Dia").setDescription("Sincronizando...")
                .setColor(Color.BLUE).build()).setComponents().queue();
        try {
            Main.getNotionService().addToSomeday(session.getRefinedText(), session.getDescription(),
                    session.getDueDate());
            event.getHook()
                    .editOriginalEmbeds(new EmbedBuilder().setTitle("✅ Salvo")
                            .setDescription("Item adicionado a 'Talvez / Um Dia'.").setColor(Color.GREEN).build())
                    .queue();
        } catch (Exception e) {
            LOGGER.error("Notion Someday error", e);
            event.getHook().editOriginal("Erro ao salvar no Notion.").queue();
        }
        sessionCache.remove(userId);
    }

    private void handleReference(ButtonInteractionEvent event, GTDCaptureSession session, String userId) {
        event.editMessageEmbeds(new EmbedBuilder().setTitle("📚 Referência").setDescription("Sincronizando...")
                .setColor(Color.BLUE).build()).setComponents().queue();
        try {
            Main.getNotionService().addToReference(session.getRefinedText(), session.getDescription(),
                    session.getDueDate());
            event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("✅ Salvo")
                    .setDescription("Item adicionado a 'Referência'.").setColor(Color.GREEN).build()).queue();
        } catch (Exception e) {
            LOGGER.error("Notion Reference error", e);
            event.getHook().editOriginal("Erro ao salvar no Notion.").queue();
        }
        sessionCache.remove(userId);
    }

    private void handleDiscard(ButtonInteractionEvent event, String userId) {
        sessionCache.remove(userId);
        event.editMessageEmbeds(new EmbedBuilder().setTitle("🗑️ Captura Descartada")
                .setDescription("Item removido do cache.").setColor(Color.DARK_GRAY).build()).setComponents().queue();
    }

    private void handleEditButton(ButtonInteractionEvent event, GTDCaptureSession session) {
        TextInput editTitle = TextInput.create("gtd_edit_text", TextInputStyle.SHORT).setValue(session.getRefinedText())
                .setRequired(true).build();
        TextInput editDesc = TextInput.create("gtd_edit_desc", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Adicione detalhes...").setValue(session.getDescription()).setRequired(false).build();
        event.replyModal(Modal.create("gtd_modal_edit", "Editar Tarefa")
                .addComponents(Label.of("Título:", editTitle), Label.of("Descrição:", editDesc)).build()).queue();
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

        switch (modalId) {
            case "gtd_modal_delegate" -> handleModalDelegate(event, session, userId);
            case "gtd_modal_defer" -> handleModalDefer(event, session, userId);
            case "gtd_modal_edit" -> handleModalEdit(event, session);
            default -> event.reply("Modal inválido.").setEphemeral(true).queue();
        }
    }

    private void handleModalDelegate(ModalInteractionEvent event, GTDCaptureSession session, String userId) {
        String who = event.getValue("gtd_delegate_who").getAsString();
        event.deferReply(true).queue();
        Main.getNotionService().createPage(Config.getRequired("DATABASE_WAITING_ID"),
                session.getRefinedText() + " (Aguardando: " + who + ")", session.getDescription(),
                session.getDueDate());

        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("🤝 Delegado com Sucesso")
                .setDescription("Item: **" + session.getRefinedText() + "**")
                .addField("Responsável", who, true)
                .addField("Status", "Enviado para 'Aguardando'", true)
                .setColor(Color.GREEN).build()).setEphemeral(true).queue();
        sessionCache.remove(userId);
    }

    private void handleModalDefer(ModalInteractionEvent event, GTDCaptureSession session, String userId) {
        String when = event.getValue("gtd_defer_when") != null ? event.getValue("gtd_defer_when").getAsString() : "";
        event.deferReply(true).queue();

        if (!when.isEmpty()) {
            Main.getGeminiService().parseDateAsync(when).thenAccept(parsedDate -> {
                session.setDueDate(parsedDate);
                Main.getNotionService().addToActions("[" + when + "] " + session.getRefinedText(),
                        session.getDescription(), session.getDueDate());
                sendFinalDeferEmbed(event.getHook(), session, when);
                sessionCache.remove(userId);
            }).exceptionally(ex -> {
                Main.getNotionService().addToActions("[" + when + "] " + session.getRefinedText(),
                        session.getDescription(), session.getDueDate());
                sendFinalDeferEmbed(event.getHook(), session, when + " (Extração falhou)");
                sessionCache.remove(userId);
                return null;
            });
            return;
        }
        Main.getNotionService().addToActions(session.getRefinedText(), session.getDescription(), session.getDueDate());
        sendFinalDeferEmbed(event.getHook(), session, "Não definida");
        sessionCache.remove(userId);
    }

    private void sendFinalDeferEmbed(InteractionHook hook, GTDCaptureSession session, String dateInfo) {
        hook.sendMessageEmbeds(new EmbedBuilder()
                .setTitle("📅 Adiado com Sucesso")
                .setDescription("Item: **" + session.getRefinedText() + "**")
                .addField("Quando/Data", session.getDueDate() != null ? session.getDueDate() : dateInfo, true)
                .addField("Status", "Adicionado às 'Próximas Ações'", true)
                .setColor(Color.GREEN).build()).setEphemeral(true).queue();
    }

    private void handleModalEdit(ModalInteractionEvent event, GTDCaptureSession session) {
        session.setRefinedText(event.getValue("gtd_edit_text").getAsString());
        if (event.getValue("gtd_edit_desc") != null)
            session.setDescription(event.getValue("gtd_edit_desc").getAsString());

        session.setCurrentStep(GTDCaptureSession.FlowStep.REFINED);

        ActionRow row = ActionRow.of(Button.primary("gtd_accept", "Aceitar"), Button.secondary("gtd_edit", "Editar"),
                Button.danger("gtd_discard", "Descartar"));

        event.editMessage("✨ **Título Atualizado:**\n> " + session.getRefinedText() + "\n\nO que deseja fazer?")
                .setComponents(row)
                .queue();
    }

    private void handleCapture(String userId, String content, InteractionHook hook, Object event) {
        if (event instanceof IReplyCallback replyCallback)
            replyCallback.deferReply(true).queue();
        LOGGER.info("User {} is capturing: {}", userId, content);

        GTDCaptureSession session = new GTDCaptureSession(userId, content);
        sessionCache.put(userId, session);
        startRefinement(session, hook);
    }

    private void startRefinement(GTDCaptureSession session, InteractionHook hook) {
        session.setCurrentStep(GTDCaptureSession.FlowStep.REFINING);
        String githubCtx = Main.getGitHubService().extractRepoContext(session.getOriginalText());
        if (githubCtx != null)
            session.setGithubContext(githubCtx);

        Main.getGeminiService().refineTextAsync(session.getOriginalText(), githubCtx)
                .thenAccept(refined -> {
                    session.setRefinedText(refined.title());
                    session.setDueDate(refined.dueDate());
                    if (refined.description() != null && !refined.description().isEmpty())
                        session.setDescription(refined.description());

                    session.setCurrentStep(GTDCaptureSession.FlowStep.REFINED);

                    ActionRow row = ActionRow.of(Button.primary("gtd_accept", "Aceitar"),
                            Button.secondary("gtd_edit", "Editar"), Button.danger("gtd_discard", "Descartar"));
                    hook.editOriginal(buildRefinementMessage(refined)).setComponents(row).queue();
                })
                .exceptionally(ex -> {
                    LOGGER.error("Gemini refinement failed", ex);
                    hook.editOriginal("Falha ao refinar com IA. Original: \n> " + session.getOriginalText()).queue();
                    return null;
                });
    }

    private String buildRefinementMessage(GeminiService.GeminiResult refined) {
        StringBuilder sb = new StringBuilder("✨ **Sugestão do Gemini:**\n> ").append(refined.title());
        if (refined.dueDate() != null) {
            sb.append("\n📅 **ETA Calculado:** ").append(refined.dueDate());
        }

        if (refined.description() != null && !refined.description().isEmpty()) {
            sb.append("\n📝 **Descrição Gerada:** ").append(refined.description());
        }

        return sb.append("\n\nO que deseja fazer?").toString();
    }
}

package com.notionbot.bot;

import com.notionbot.Main;
import com.notionbot.config.Config;
import com.notionbot.gtd.model.GTDCaptureSession;
import com.notionbot.services.CacheService;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
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
                event.editMessage("✅ **Item aceito:** " + session.getRefinedText()
                        + "\n\nEste item é **acionável**? (Requer alguma ação física sua?)")
                        .setComponents(
                                ActionRow.of(
                                        Button.primary("gtd_actionable_yes", "Sim"),
                                        Button.secondary("gtd_actionable_no", "Não")))
                        .queue();
                break;

            case "gtd_actionable_yes":
                session.setCurrentStep(GTDCaptureSession.FlowStep.DECIDING_MULTISTEP);
                event.editMessage("🚀 Este item requer **múltiplos passos** (é um Projeto) ou apenas um passo?")
                        .setComponents(
                                ActionRow.of(
                                        Button.primary("gtd_multistep_yes", "Vários passos (Projeto)"),
                                        Button.secondary("gtd_multistep_no", "Apenas um passo")))
                        .queue();
                break;

            case "gtd_actionable_no":
                session.setCurrentStep(GTDCaptureSession.FlowStep.PROCESSING_NOTION);
                event.editMessage("📂 Para onde deve ir este item **não acionável**?")
                        .setComponents(
                                ActionRow.of(
                                        Button.danger("gtd_trash", "Lixo"),
                                        Button.primary("gtd_someday", "Talvez/Um dia"),
                                        Button.secondary("gtd_reference", "Referência")))
                        .queue();
                break;

            case "gtd_multistep_yes":
                event.editMessage("🏗️ Criando projeto no Notion: **" + session.getRefinedText() + "**...")
                        .setComponents().queue();
                Main.getNotionService().createProject(session.getRefinedText());
                sessionCache.remove(userId);
                break;

            case "gtd_multistep_no":
                session.setCurrentStep(GTDCaptureSession.FlowStep.DECIDING_2MIN);
                event.editMessage("⏳ Leva menos de **2 minutos** para fazer?")
                        .setComponents(
                                ActionRow.of(
                                        Button.primary("gtd_2min_yes", "Sim (Vou fazer agora)"),
                                        Button.secondary("gtd_2min_no", "Não (Leva mais tempo)")))
                        .queue();
                break;

            case "gtd_2min_yes":
                event.editMessage("⚡ **Faça agora!** Assim que terminar, clique no botão abaixo para concluir.")
                        .setComponents(ActionRow.of(Button.success("gtd_done_now", "Concluído!")))
                        .queue();
                break;

            case "gtd_done_now":
                event.editMessage("✅ Que bom que resolveu logo! Registrado como feito no Notion.")
                        .setComponents().queue();
                Main.getNotionService().addToActions(session.getRefinedText() + " (Concluído)");
                sessionCache.remove(userId);
                break;

            case "gtd_2min_no":
                session.setCurrentStep(GTDCaptureSession.FlowStep.DECIDING_DELEGATE_DEFER);
                event.editMessage("🤔 Deseja **delegar** para alguém ou **adiar** para fazer você mesmo depois?")
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
                event.editMessage("🗑️ Item descartado (Lixo).").setComponents().queue();
                break;

            case "gtd_someday":
                event.editMessage("💡 Adicionando a **Talvez/Um Dia** no Notion...").setComponents().queue();
                Main.getNotionService().addToSomeday(session.getRefinedText());
                sessionCache.remove(userId);
                break;

            case "gtd_reference":
                event.editMessage("📚 Adicionando a **Referência** no Notion...").setComponents().queue();
                Main.getNotionService().addToReference(session.getRefinedText());
                sessionCache.remove(userId);
                break;

            case "gtd_discard":
                sessionCache.remove(userId);
                event.editMessage("🗑️ Captura descartada.").setComponents().queue();
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
            event.reply("🤝 Delegado para: **" + who + "**. Registrado em 'Aguardando' no Notion.")
                    .setEphemeral(true).queue();
            Main.getNotionService().createPage(Config.getRequired("DATABASE_WAITING_ID"),
                    session.getRefinedText() + " (Aguardando: " + who + ")");
            sessionCache.remove(userId);
        } else if (modalId.equals("gtd_modal_defer")) {
            String when = event.getValue("gtd_defer_when").getAsString();
            String prefix = (when != null && !when.isEmpty()) ? "[" + when + "] " : "";
            event.reply("📅 Adiado! Item adicionado às 'Próximas Ações' no Notion.")
                    .setEphemeral(true).queue();
            Main.getNotionService().addToActions(prefix + session.getRefinedText());
            sessionCache.remove(userId);
        }
    }

    private void handleCapture(String userId, String content, InteractionHook hook, Object event) {
        // Acknowledge immediately (ephemeral)
        if (event instanceof SlashCommandInteractionEvent) {
            ((SlashCommandInteractionEvent) event).deferReply(true).queue();
        } else if (event instanceof MessageContextInteractionEvent) {
            ((MessageContextInteractionEvent) event).deferReply(true).queue();
        }

        LOGGER.info("User {} is capturing: {}", userId, content);

        GTDCaptureSession session = new GTDCaptureSession(userId, content);
        sessionCache.put(userId, session);

        // Next step: AI Refinement
        startRefinement(session, hook);
    }

    private void startRefinement(GTDCaptureSession session, InteractionHook hook) {
        session.setCurrentStep(GTDCaptureSession.FlowStep.REFINING);

        Main.getGeminiService().refineTextAsync(session.getOriginalText())
                .thenAccept(refined -> {
                    session.setRefinedText(refined);
                    session.setCurrentStep(GTDCaptureSession.FlowStep.REFINED);

                    hook.editOriginal("✨ **Sugestão da IA:**\n> " + refined + "\n\nO que deseja fazer?")
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
                            "❌ Falha ao refinar o texto com IA. Usando original: \n> " + session.getOriginalText())
                            .queue();
                    return null;
                });
    }
}

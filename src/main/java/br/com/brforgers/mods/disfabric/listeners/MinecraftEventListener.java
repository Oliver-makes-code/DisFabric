package br.com.brforgers.mods.disfabric.listeners;

import br.com.brforgers.mods.disfabric.DisFabric;
import br.com.brforgers.mods.disfabric.events.PlayerAdvancementCallback;
import br.com.brforgers.mods.disfabric.events.PlayerDeathCallback;
import br.com.brforgers.mods.disfabric.events.ServerChatCallback;
import br.com.brforgers.mods.disfabric.utils.Utils;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class MinecraftEventListener {
    private static final Identifier DISFABRIC_CHAT = Identifier.of("disfabric", "decorator");

    public void init() {
        if (!DisFabric.config.commandsOnly) {
            ServerMessageDecoratorEvent.EVENT.addPhaseOrdering(DISFABRIC_CHAT, Event.DEFAULT_PHASE);
            ServerMessageDecoratorEvent.EVENT.register(DISFABRIC_CHAT, (sender, message) ->
                    CompletableFuture.completedFuture(DisFabric.stop ? message : Text.of(Utils.convertMentionsFromNames(message.getString()))));
            ServerChatCallback.EVENT.register((playerEntity, rawMessage) -> {
                if (!DisFabric.stop) {
                    String convertedString = Utils.convertMentionsFromNames(rawMessage);
                    if (DisFabric.config.isWebhookEnabled) {
                        JSONObject body = new JSONObject();
                        body.put("username", playerEntity.getEntityName());
                        body.put("avatar_url", Utils.playerAvatarUrl(playerEntity));
                        JSONObject allowed_mentions = new JSONObject();
                        allowed_mentions.put("parse", new String[]{"users"});
                        body.put("allowed_mentions", allowed_mentions);
                        body.put("content", convertedString);
                        try {
                            Unirest.post(DisFabric.config.webhookURL).header("Content-Type", "application/json").body(body).asJsonAsync();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        DisFabric.bridgeChannel.sendMessage(DisFabric.config.texts.playerMessage.replace("%playername%", MarkdownSanitizer.escape(playerEntity.getEntityName())).replace("%playermessage%", convertedString)).queue();
                    }
                }
            });

            PlayerAdvancementCallback.EVENT.register((playerEntity, advancement) -> {
                if (DisFabric.config.announceAdvancements && advancement.getDisplay() != null && advancement.getDisplay().shouldAnnounceToChat() && playerEntity.getAdvancementTracker().getProgress(advancement).isDone() && !DisFabric.stop) {
                    switch (advancement.getDisplay().getFrame()) {
                        case GOAL -> DisFabric.bridgeChannel.sendMessage(DisFabric.config.texts.advancementGoal.replace("%playername%", MarkdownSanitizer.escape(playerEntity.getEntityName())).replace("%advancement%", MarkdownSanitizer.escape(advancement.getDisplay().getTitle().getString()))).queue();
                        case TASK -> DisFabric.bridgeChannel.sendMessage(DisFabric.config.texts.advancementTask.replace("%playername%", MarkdownSanitizer.escape(playerEntity.getEntityName())).replace("%advancement%", MarkdownSanitizer.escape(advancement.getDisplay().getTitle().getString()))).queue();
                        case CHALLENGE -> DisFabric.bridgeChannel.sendMessage(DisFabric.config.texts.advancementChallenge.replace("%playername%", MarkdownSanitizer.escape(playerEntity.getEntityName())).replace("%advancement%", MarkdownSanitizer.escape(advancement.getDisplay().getTitle().getString()))).queue();
                    }
                }
            });

            PlayerDeathCallback.EVENT.register((playerEntity, damageSource) -> {
                if (DisFabric.config.announceDeaths && !DisFabric.stop) {
                    DisFabric.bridgeChannel.sendMessage(DisFabric.config.texts.deathMessage.replace("%deathmessage%", MarkdownSanitizer.escape(damageSource.getDeathMessage(playerEntity).getString())).replace("%playername%", MarkdownSanitizer.escape(playerEntity.getEntityName()))).queue();
                }
            });

            ServerPlayConnectionEvents.JOIN.register((handler, $2, $3) -> {
                if (DisFabric.config.announcePlayers && !DisFabric.stop) {
                    DisFabric.bridgeChannel.sendMessage(DisFabric.config.texts.joinServer.replace("%playername%", MarkdownSanitizer.escape(handler.player.getEntityName()))).queue();
                }
            });

            ServerPlayConnectionEvents.DISCONNECT.register((handler, $2) -> {
                if (DisFabric.config.announcePlayers && !DisFabric.stop) {
                    DisFabric.bridgeChannel.sendMessage(DisFabric.config.texts.leftServer.replace("%playername%", MarkdownSanitizer.escape(handler.player.getEntityName()))).queue();
                }
            });
        }
    }
}

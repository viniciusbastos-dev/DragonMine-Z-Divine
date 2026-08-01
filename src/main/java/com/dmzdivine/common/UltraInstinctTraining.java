package com.dmzdivine.common;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.DivineConfig;
import com.dmzdivine.common.world.BeerusPlanet;
import com.dmzdivine.common.world.BeerusPlanetData;
import com.dmzdivine.network.DivineNetwork;
import com.dmzdivine.network.S2C.OpenUltraInstinctTrialS2C;
import com.dmzdivine.network.S2C.OpenWhisDialogueS2C;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whis' side of the planet: the Ultra Instinct trial.
 *
 * <p>Ultra Instinct is a stack form skill with two levels (sign, then mastered - see
 * {@code forms/ultrainstinct.json} and {@link DivineFormsInstaller}). Buying it from King Kai with
 * raw TP still works for worlds that already do; this is the other road, and the one that fits the
 * lore: train inside Whis' 30g arena until he judges you ready, then pass his dodging trial.
 *
 * <p>Training time is tracked per player on the planet's own saved data ({@link BeerusPlanetData}),
 * accumulated by {@link com.dmzdivine.server.BeerusPlanetEvents} while the player is inside the
 * arena. The trial itself runs on the client, exactly like the god ritual: the server marks the
 * player pending, and re-checks every prerequisite when the result comes back.
 */
@Mod.EventBusSubscriber(modid = DMZDivine.MODID)
public final class UltraInstinctTraining {

    public static final String UI_SKILL = "ultrainstinct";
    /** sign + mastered. Matches the two entries in ULTRAINSTINCT_TP_COSTS. */
    public static final int MAX_LEVEL = 2;

    private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();

    private UltraInstinctTraining() {
    }

    /** Right-click on Whis: opens his dialogue, with every number resolved here on the server. */
    public static void openDialogue(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            int level = data.getSkills().getSkillLevel(UI_SKILL);
            boolean maxed = level >= MAX_LEVEL;
            int nextLevel = level + 1;

            DivineNetwork.sendTo(player, new OpenWhisDialogueS2C(
                    minutes(trainedTicks(player)),
                    maxed ? 0 : minutes(requiredTicks(nextLevel)),
                    level,
                    maxed ? 0 : tpCost(nextLevel)));
        });
    }

    /** The trial button in that dialogue: either a progress report, or the trial itself. */
    public static void talk(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            int level = data.getSkills().getSkillLevel(UI_SKILL);
            if (level >= MAX_LEVEL) {
                say(player, "message.dmzdivine.whis.mastered", ChatFormatting.AQUA);
                return;
            }

            int nextLevel = level + 1;
            int trainedTicks = trainedTicks(player);
            int requiredTicks = requiredTicks(nextLevel);
            if (trainedTicks < requiredTicks) {
                player.sendSystemMessage(Component.translatable("message.dmzdivine.whis.keep_training",
                                minutes(trainedTicks), minutes(requiredTicks))
                        .withStyle(ChatFormatting.AQUA));
                return;
            }

            int cost = tpCost(nextLevel);
            if (data.getResources().getTrainingPoints() < cost) {
                player.sendSystemMessage(Component.translatable("message.dmzdivine.whis.no_tp", cost)
                        .withStyle(ChatFormatting.RED));
                return;
            }

            say(player, nextLevel == 1 ? "message.dmzdivine.whis.trial_start" : "message.dmzdivine.whis.trial_start_mastered",
                    ChatFormatting.AQUA);
            PENDING.add(player.getUUID());
            DivineNetwork.sendTo(player, new OpenUltraInstinctTrialS2C());
        });
    }

    /** Handles the trial result reported by the client. */
    public static void handleResult(ServerPlayer player, boolean success) {
        if (player == null || !PENDING.remove(player.getUUID())) return;

        if (!success) {
            player.sendSystemMessage(Component.translatable("message.dmzdivine.whis.trial_failed")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            int level = data.getSkills().getSkillLevel(UI_SKILL);
            if (level >= MAX_LEVEL) return;

            int nextLevel = level + 1;
            // Re-check: the client controls the minigame, so the server never trusts that the
            // prerequisites still held while it ran.
            if (trainedTicks(player) < requiredTicks(nextLevel)) return;

            int cost = tpCost(nextLevel);
            if (data.getResources().getTrainingPoints() < cost) {
                player.sendSystemMessage(Component.translatable("message.dmzdivine.whis.no_tp", cost)
                        .withStyle(ChatFormatting.RED));
                return;
            }

            data.getSkills().setSkillLevel(UI_SKILL, nextLevel);
            if (data.getSkills().getSkillLevel(UI_SKILL) < nextLevel) {
                DMZDivine.LOGGER.error("Failed to grant Ultra Instinct level {} to {}: skill stayed at {}. Is "
                                + "forms/ultrainstinct.json installed and wired into skills.json?",
                        nextLevel, player.getGameProfile().getName(), data.getSkills().getSkillLevel(UI_SKILL));
                player.sendSystemMessage(Component.translatable("message.dmzdivine.whis.misconfigured")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            data.getResources().removeTrainingPoints(cost);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);

            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.4F);
            player.sendSystemMessage(Component.translatable(nextLevel == 1
                            ? "message.dmzdivine.whis.trial_won_sign"
                            : "message.dmzdivine.whis.trial_won_mastered")
                    .withStyle(ChatFormatting.AQUA));
            DMZDivine.LOGGER.info("{} passed Whis' Ultra Instinct trial (level {})",
                    player.getGameProfile().getName(), nextLevel);
        });
    }

    /** Ticks of arena training this player has banked, or 0 when the planet was never generated. */
    public static int trainedTicks(ServerPlayer player) {
        ServerLevel planet = player.server.getLevel(BeerusPlanet.DIMENSION);
        if (planet == null) return 0;
        return BeerusPlanetData.get(planet).getArenaTicks(player.getUUID());
    }

    /** Arena training needed for a given Ultra Instinct level - each level asks for that much again. */
    public static int requiredTicks(int level) {
        return DivineConfig.WHIS_TRAINING_MINUTES.get() * 60 * 20 * Math.max(1, level);
    }

    public static int tpCost(int level) {
        return DivineConfig.WHIS_TRIAL_TP.get() * Math.max(1, level);
    }

    public static int minutes(int ticks) {
        return ticks / (60 * 20);
    }

    private static void say(ServerPlayer player, String key, ChatFormatting color) {
        player.sendSystemMessage(Component.translatable(key).withStyle(color));
    }

    /** A player who logs out mid-trial should not stay pending forever. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    /** Only used by the debug command, to show where a player stands. */
    public static int currentLevel(StatsData data) {
        return data.getSkills().getSkillLevel(UI_SKILL);
    }
}

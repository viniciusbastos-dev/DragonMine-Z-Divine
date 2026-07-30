package com.dmzdivine.common;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.DivineConfig;
import com.dmzdivine.network.DivineNetwork;
import com.dmzdivine.network.S2C.OpenGodRitualS2C;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side brain of the Super Saiyan God ritual.
 *
 * Mirrors the base mod's Ultimate ritual flow (Elder Kai): the minigame runs on the
 * client, but the server decides who may start it, tracks who is mid-ritual, and
 * re-validates every prerequisite before granting the skill. The unlock itself is
 * {@code godforms} level 1 - a skill the base mod already understands natively
 * (form JSONs with formType "godforms", divine ki-sense gating, etc.).
 */
@Mod.EventBusSubscriber(modid = DMZDivine.MODID)
public final class GodRitual {

    public static final String GODFORMS_SKILL = "godforms";

    private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();

    private GodRitual() {
    }

    /** Validates prerequisites and, if met, opens the minigame on the player's client. */
    public static void start(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            Component blocker = getBlocker(data);
            if (blocker != null) {
                player.sendSystemMessage(blocker);
                return;
            }
            PENDING.add(player.getUUID());
            DivineNetwork.sendTo(player, new OpenGodRitualS2C());
        });
    }

    /** Handles the minigame result reported by the client. */
    public static void handleResult(ServerPlayer player, boolean success) {
        if (player == null || !PENDING.remove(player.getUUID())) return;

        if (!success) {
            player.sendSystemMessage(Component.translatable("message.dmzdivine.ritual.failed")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (getBlocker(data) != null) return;

            String race = data.getCharacter().getRaceName();

            // Skill.setLevel clamps to maxLevel, and for a race form skill that max only comes from the
            // race's formSkillsCosts prices. Skills.setSkillLevel would create the skill using
            // Skills.calculateMaxLevel, which reads skills.json's generic cost map - and that map has no
            // godforms entry (unlike "ultimate", which is why the base mod's Elder Kai ritual can just
            // call setSkillLevel). Registering the real max first is what keeps level 1 from being
            // silently clamped to 0.
            int maxLevel = godformsMaxLevel(race);
            data.getSkills().registerDefaultSkill(GODFORMS_SKILL, maxLevel);
            data.getSkills().setSkillLevel(GODFORMS_SKILL, 1);
            data.updateTransformationSkillLimits(race);
            data.getResources().removeTrainingPoints(DivineConfig.RITUAL_REQUIRED_TP.get());

            if (data.getSkills().getSkillLevel(GODFORMS_SKILL) < 1) {
                DMZDivine.LOGGER.error("Failed to grant godforms to {}: level stayed 0 (godforms maxLevel={})",
                        player.getGameProfile().getName(), maxLevel);
                player.sendSystemMessage(Component.translatable("message.dmzdivine.ritual.misconfigured")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);

            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 1.2F);
            player.sendSystemMessage(Component.translatable("message.dmzdivine.ritual.success")
                    .withStyle(ChatFormatting.GOLD));
            DMZDivine.LOGGER.info("{} completed the Super Saiyan God ritual", player.getGameProfile().getName());
        });
    }

    /** Returns the reason this player cannot attempt the ritual, or null if allowed. */
    private static Component getBlocker(StatsData data) {
        if (data.getSkills().getSkillLevel(GODFORMS_SKILL) >= 1) {
            return Component.translatable("message.dmzdivine.ritual.already");
        }

        String race = data.getCharacter().getRaceName();
        String raceLower = race != null ? race.toLowerCase(Locale.ROOT) : "";
        boolean raceAllowed = DivineConfig.RITUAL_RACES.get().stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(raceLower));
        if (!raceAllowed) {
            return Component.translatable("message.dmzdivine.ritual.wrong_race");
        }

        // Refuse up front rather than after the whole minigame: without prices for godforms in the
        // race's character.json the skill's max level is 0 and the unlock cannot be stored.
        if (godformsMaxLevel(race) < 1) {
            DMZDivine.LOGGER.error("godforms has no prices in config/dragonminez/races/{}/character.json "
                    + "- the ritual cannot grant the skill. See docs/reference-configs/README.md", raceLower);
            return Component.translatable("message.dmzdivine.ritual.misconfigured");
        }

        String requiredSkill = DivineConfig.RITUAL_REQUIRED_SKILL.get();
        int requiredLevel = DivineConfig.RITUAL_REQUIRED_SKILL_LEVEL.get();
        if (!requiredSkill.isBlank() && data.getSkills().getSkillLevel(requiredSkill) < requiredLevel) {
            return Component.translatable("message.dmzdivine.ritual.form_level", requiredSkill, requiredLevel);
        }

        if (data.getResources().getAlignment() < DivineConfig.RITUAL_MIN_ALIGNMENT.get()) {
            return Component.translatable("message.dmzdivine.ritual.alignment");
        }

        int requiredTp = DivineConfig.RITUAL_REQUIRED_TP.get();
        if (data.getResources().getTrainingPoints() < requiredTp) {
            return Component.translatable("message.dmzdivine.ritual.tp", requiredTp);
        }

        return null;
    }

    /** Max level the godforms skill can reach for this race, i.e. how many TP prices it defines. */
    private static int godformsMaxLevel(String race) {
        RaceCharacterConfig charConfig = ConfigManager.getRaceCharacter(race);
        if (charConfig == null) return 0;
        Integer[] prices = charConfig.getFormSkillTpCosts(GODFORMS_SKILL);
        return prices != null ? prices.length : 0;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }
}

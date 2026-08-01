package com.dmzdivine.common;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.DivineConfig;
import com.dmzdivine.network.DivineNetwork;
import com.dmzdivine.network.S2C.OpenGodRitualS2C;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
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
 * re-validates every prerequisite before granting the unlock. Unlike the addon's earlier
 * approach, the unlock itself is not a separate skill: God/Blue/Blue Evolved live in their own
 * "divineforms" group but share the Saiyan's "superforms" skill/ladder (levels 9-11, right after
 * supersaiyan4's level 8) - a group can share a formType/skill with another group while still
 * rendering as its own sibling branch (see ssgrades.json vs supersaiyan.json in the base mod), so
 * God shows up next to Mastered in the normal Super Saiyan menu instead of the "Extra Forms"
 * radial node - that node is hardcoded to the "godforms" formType in the base mod's MoreFormsNode.
 * Level 9's price is -1 in character.json (see DivineFormsInstaller), which blocks both normal purchase paths in
 * UpdateSkillC2S, leaving the ritual as the only way in - the same trick the base mod uses for
 * its own "ultimate" skill.
 */
@Mod.EventBusSubscriber(modid = DMZDivine.MODID)
public final class GodRitual {

    private static final String SUPERFORMS_SKILL = "superforms";
    private static final String GOD_GROUP = "divineforms";
    private static final String GOD_FORM = "supersaiyangod";

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
            int godLevel = requiredGodLevel(race);

            // Skill.setLevel clamps to the Skill's *current* maxLevel, and that max only comes from
            // character.json's price list length - refreshing it here first is what keeps setting
            // level 9 from silently clamping back down to 8. Doing this after setSkillLevel (like the
            // old godforms flow did) would be too late.
            data.updateTransformationSkillLimits(race);
            data.getSkills().setSkillLevel(SUPERFORMS_SKILL, godLevel);
            data.getResources().removeTrainingPoints(DivineConfig.RITUAL_REQUIRED_TP.get());

            if (data.getSkills().getSkillLevel(SUPERFORMS_SKILL) < godLevel) {
                DMZDivine.LOGGER.error("Failed to grant Super Saiyan God to {}: superforms stayed at {} (needed {})",
                        player.getGameProfile().getName(), data.getSkills().getSkillLevel(SUPERFORMS_SKILL), godLevel);
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
        String race = data.getCharacter().getRaceName();
        int godLevel = requiredGodLevel(race);

        if (godLevel > 0 && data.getSkills().getSkillLevel(SUPERFORMS_SKILL) >= godLevel) {
            return Component.translatable("message.dmzdivine.ritual.already");
        }

        String raceLower = race != null ? race.toLowerCase(Locale.ROOT) : "";
        boolean raceAllowed = DivineConfig.RITUAL_RACES.get().stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(raceLower));
        if (!raceAllowed) {
            return Component.translatable("message.dmzdivine.ritual.wrong_race");
        }

        // Refuse up front rather than after the whole minigame: without an unlockOnSkillLevel on
        // supersaiyangod, or without enough price levels to cover it, the unlock cannot be stored.
        RaceCharacterConfig charConfig = ConfigManager.getRaceCharacter(race);
        Integer[] prices = charConfig != null ? charConfig.getFormSkillTpCosts(SUPERFORMS_SKILL) : null;
        if (godLevel < 1 || prices == null || prices.length < godLevel) {
            DMZDivine.LOGGER.error("supersaiyangod needs superforms level {} but races/{}/character.json only has {} "
                            + "price levels - the ritual cannot grant it. See docs/reference-configs/README.md",
                    godLevel, raceLower, prices != null ? prices.length : 0);
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

    /** The superforms skill level supersaiyangod is configured to unlock at, or 0 if misconfigured. */
    private static int requiredGodLevel(String race) {
        FormConfig config = ConfigManager.getFormGroup(race, GOD_GROUP);
        FormConfig.FormData form = config != null ? config.getForm(GOD_FORM) : null;
        return form != null ? form.getUnlockOnSkillLevel() : 0;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }
}

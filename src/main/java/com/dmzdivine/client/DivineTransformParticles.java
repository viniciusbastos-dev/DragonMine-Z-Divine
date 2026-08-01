package com.dmzdivine.client;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.DivineConfig;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.particles.DivineParticle;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Blue ki gathers around a player charging into a god-type form, then bursts when the form lands
 * and the form's own divine aura takes over.
 *
 * The aura afterwards is not ours - it comes from auraType/auraColor in the form JSON, which
 * DragonMineZ renders natively (it already ships god_aura.png). What JSON cannot express is the
 * charge-up effect, which is why this lives in the addon.
 *
 * Client-side by design, mirroring how the base mod spawns its own aura particles: StatsData is
 * synced to clients, so no packet of ours is needed and remote players get the effect too. Note
 * the base mod's own divine particles are white and gated behind its BetaWhitelist, so these are
 * spawned independently rather than reusing that path.
 */
@Mod.EventBusSubscriber(modid = DMZDivine.MODID, value = Dist.CLIENT)
public final class DivineTransformParticles {

    private static final Set<UUID> chargingDivine = new HashSet<>();

    private DivineTransformParticles() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!DivineConfig.TRANSFORM_PARTICLES.get()) {
            if (!chargingDivine.isEmpty()) chargingDivine.clear();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        Set<UUID> charging = new HashSet<>();
        for (Player player : mc.level.players()) {
            StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
            if (data == null || !data.getStatus().isHasCreatedCharacter()) continue;

            if (isChargingDivineForm(data)) {
                charging.add(player.getUUID());
                spawnGatheringKi(player);
            } else if (chargingDivine.contains(player.getUUID()) && hasDivineFormActive(data)) {
                spawnLandingBurst(player, data);
            }

            if (isUltraInstinctMastered(data)) {
                spawnAmbientMastered(player, data);
            }
        }

        chargingDivine.clear();
        chargingDivine.addAll(charging);
    }

    private static boolean isChargingDivineForm(StatsData data) {
        if (!data.getStatus().isActionCharging()) return false;
        if (data.getStatus().getSelectedAction() != ActionMode.FORM) return false;
        if (TransformationsHelper.getNextAvailableForm(data) == null) return false;
        return isGodGroup(data.getCharacter().getRaceName(), TransformationsHelper.getTransformTargetGroup(data));
    }

    private static boolean hasDivineFormActive(StatsData data) {
        Character character = data.getCharacter();
        return character.hasActiveForm() && isGodGroup(character.getRaceName(), character.getActiveFormGroup());
    }

    /** Matches on formType rather than the group name, so renaming the group in JSON keeps working. */
    private static boolean isGodGroup(String race, String group) {
        if (group == null || group.isEmpty()) return false;
        FormConfig config = ConfigManager.getFormGroup(race, group);
        if (config == null || config.getFormType() == null) return false;
        return config.getFormType().toLowerCase(Locale.ROOT).contains("god");
    }

    private static void spawnGatheringKi(Player player) {
        float[] rgb = ColorUtils.hexToRgb(DivineConfig.TRANSFORM_PARTICLE_COLOR.get());
        if (rgb == null) return;

        RandomSource random = player.getRandom();
        int count = 2 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            double radius = 0.9 + random.nextDouble() * 0.9;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double y = player.getY() + random.nextDouble() * 1.9;

            DivineParticle particle = spawn(player.getX() + offsetX, y, player.getZ() + offsetZ, rgb);
            if (particle == null) continue;
            particle.resize(0.9f);
            // Drift inward and up: the ki is being pulled into the player, not thrown off them.
            particle.setParticleSpeed(-offsetX * 0.06, 0.02 + random.nextDouble() * 0.03, -offsetZ * 0.06);
        }
    }

    /** Ultra Instinct Mastered is a stack form, not a primary one, so it never goes through
     * isChargingDivineForm/hasDivineFormActive above (those only look at the primary form slot) -
     * this ambient trickle runs independently, for as long as the stack form is active, charging
     * or not. Sign is deliberately excluded: it's meant to be nearly invisible. */
    private static boolean isUltraInstinctMastered(StatsData data) {
        Character character = data.getCharacter();
        return character.hasActiveStackForm()
                && "ultrainstinct".equalsIgnoreCase(character.getActiveStackFormGroup())
                && "mastered".equalsIgnoreCase(character.getActiveStackForm());
    }

    private static void spawnAmbientMastered(Player player, StatsData data) {
        FormConfig.FormData formData = data.getCharacter().getActiveStackFormData();
        float[] rgb = formData != null ? formData.getRgbAuraColor() : null;
        if (rgb == null) rgb = ColorUtils.hexToRgb(DivineConfig.TRANSFORM_PARTICLE_COLOR.get());
        if (rgb == null) return;

        RandomSource random = player.getRandom();
        if (random.nextInt(3) != 0) return; // a light, continuous trickle - not a burst

        double radius = 0.3 + random.nextDouble() * 0.5;
        double angle = random.nextDouble() * Math.PI * 2.0;
        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        double y = player.getY() + random.nextDouble() * 1.9;

        DivineParticle particle = spawn(player.getX() + offsetX, y, player.getZ() + offsetZ, rgb);
        if (particle == null) return;
        particle.resize(0.6f);
        // Gentle upward drift off the body, unlike the charge-up's inward pull or the burst's outward throw.
        particle.setParticleSpeed(offsetX * 0.01, 0.03 + random.nextDouble() * 0.02, offsetZ * 0.01);
    }

    private static void spawnLandingBurst(Player player, StatsData data) {
        int count = DivineConfig.TRANSFORM_BURST_PARTICLES.get();
        if (count <= 0) return;

        FormConfig.FormData active = data.getCharacter().getActiveFormData();
        float[] rgb = active != null ? active.getRgbAuraColor() : null;
        if (rgb == null) rgb = ColorUtils.hexToRgb(DivineConfig.TRANSFORM_PARTICLE_COLOR.get());
        if (rgb == null) return;

        RandomSource random = player.getRandom();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = random.nextDouble() * 0.6;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double y = player.getY() + random.nextDouble() * 1.9;

            DivineParticle particle = spawn(player.getX() + offsetX, y, player.getZ() + offsetZ, rgb);
            if (particle == null) continue;
            particle.resize(1.4f);
            double speed = 0.12 + random.nextDouble() * 0.15;
            particle.setParticleSpeed(Math.cos(angle) * speed, 0.05 + random.nextDouble() * 0.1, Math.sin(angle) * speed);
        }
    }

    private static DivineParticle spawn(double x, double y, double z, float[] rgb) {
        Particle particle = Minecraft.getInstance().particleEngine
                .createParticle(MainParticles.DIVINE.get(), x, y, z, rgb[0], rgb[1], rgb[2]);
        return particle instanceof DivineParticle divine ? divine : null;
    }
}

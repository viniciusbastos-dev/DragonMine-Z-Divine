package com.dmzdivine.server;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.DivineConfig;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ultra Instinct's auto-dodge. Hooks LivingAttackEvent - the point in vanilla's damage pipeline
 * that precedes LivingHurtEvent entirely - instead of DMZEvent.DamageModifyEvent, which only
 * zeroes the damage *number*. Canceling that later event still let the base mod's own
 * LivingHurtEvent handler (CombatEvent.onLivingHurt) finish with amount 0, which still applies
 * vanilla's knockback/red-flash/hurt-sound/invulnerability-tick: a "successful" 0-damage hit
 * still looks and feels like a hit. Canceling LivingAttackEvent instead means the attack never
 * happened at all - nothing else downstream runs.
 *
 * Only cancels attacks whose damage source traces back to a living entity - player or mob alike
 * (melee, ki blasts, strikes), never environmental damage (fall/fire/etc, whose DamageSource has
 * no owning entity).
 *
 * Chance is mastery + VIT (weights configurable, meant to sum to 1.0): mastery alone is
 * deliberately capped short of 100%, VIT fills the rest. The per-dodge resource cost never scales
 * down with mastery, so sustaining a high dodge rate still requires real investment in the
 * chosen resource's stat, not just time spent mastering the form.
 */
@Mod.EventBusSubscriber(modid = DMZDivine.MODID)
public final class UltraInstinctEvasion {

    private static final String GROUP = "ultrainstinct";

    private UltraInstinctEvasion() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        StatsProvider.get(StatsCapability.INSTANCE, victim).ifPresent(data -> {
            String activeGroup = data.getCharacter().getActiveStackFormGroup();
            String activeForm = data.getCharacter().getActiveStackForm();
            if (!GROUP.equalsIgnoreCase(activeGroup) || activeForm.isEmpty()) return;

            double mastery = data.getCharacter().getStackFormMasteries().getMastery(GROUP, activeForm);
            double masteryPart = (mastery / 100.0) * DivineConfig.UI_MASTERY_WEIGHT.get();
            double vitPart = Math.min(1.0, data.getStats().getVitality() / (double) DivineConfig.UI_VIT_REFERENCE.get())
                    * DivineConfig.UI_VIT_WEIGHT.get();
            double chance = Math.min(1.0, masteryPart + vitPart);

            boolean useStamina = "stamina".equalsIgnoreCase(DivineConfig.UI_DODGE_RESOURCE.get());
            float cost = DivineConfig.UI_DODGE_COST.get();
            float available = useStamina ? data.getResources().getCurrentStamina() : data.getResources().getCurrentEnergy();

            if (victim.getRandom().nextDouble() >= chance) return;
            if (available < cost) return; // not enough to sustain it - the hit lands normally

            if (useStamina) data.getResources().removeStamina((int) cost);
            else data.getResources().removeEnergy((int) cost);

            event.setCanceled(true);

            int variant = evasionVariant(victim, attacker);
            NetworkHandler.sendToTrackingEntityAndSelf(
                    new TriggerAnimationS2C(victim.getUUID(), TriggerAnimationS2C.AnimationType.EVASION, variant), victim);
            victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    MainSounds.EVASION1.get(), SoundSource.PLAYERS, 1.0F, 1.1F + victim.getRandom().nextFloat() * 0.2F);
        });
    }

    /** 1=front, 2=back, 3=left, 0=right - matches PlayerGeoAnimatableMixin's evasion switch. */
    private static int evasionVariant(ServerPlayer victim, LivingEntity attacker) {
        double dx = attacker.getX() - victim.getX();
        double dz = attacker.getZ() - victim.getZ();
        double yaw = Math.toRadians(victim.getYRot());
        double forwardDot = dx * -Math.sin(yaw) + dz * Math.cos(yaw);
        double rightDot = dx * Math.cos(yaw) + dz * Math.sin(yaw);

        if (Math.abs(forwardDot) >= Math.abs(rightDot)) return forwardDot >= 0 ? 1 : 2;
        return rightDot >= 0 ? 0 : 3;
    }
}

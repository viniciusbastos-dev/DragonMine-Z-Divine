package com.dmzdivine.server;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.DivineConfig;
import com.dmzdivine.common.world.BeerusPlanet;
import com.dmzdivine.network.DivineNetwork;
import com.dmzdivine.network.S2C.MeditationStateS2C;
import com.dragonminez.common.config.TpSource;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Meditation training: sit still inside Whis' arena, in Ultra Instinct, and Training Points come in
 * every second for as long as the meditation screen stays open.
 *
 * <p>The reward is a fraction of what one stat point currently costs the player rather than a flat
 * number - the same choice the base mod makes for its training minigames ({@code TrainingRewardC2S}
 * pays {@code computeTpsPerLevel(currentTpc, ...)}), because a flat rate is enormous at the start of
 * a save and pointless by the end of one. The arena's own multiplier lands on top by itself: every
 * {@code addTrainingPoints} call passes through the base mod's {@code TPGainEvent} handler, which
 * runs the amount through {@code calculateTPGain}, which reads the same {@code getTpHTCMultiplier}
 * our {@link com.dmzdivine.mixin.StatsDataTpMixin} answers inside.
 *
 * <p>Two things bound the AFK farm, both deliberate: it only counts inside the arena, so the player
 * has to have reached Beerus' planet and stay there, and it drains a share of their ki (or stamina)
 * every second, so the pool they built decides how long a session lasts. The drain has to out-pace
 * that resource's passive regeneration or it is not a brake at all - see the config comment.
 *
 * <p>State is deliberately in memory only: a meditation session is not worth persisting across a
 * restart, and everything it grants is already written into the player's stats each second.
 */
@Mod.EventBusSubscriber(modid = DMZDivine.MODID)
public final class MeditationTraining {

    /** The id this training is listed under in the base mod's minigame screen and known-minigame set. */
    public static final String MINIGAME_ID = "meditation";

    private static final String UI_GROUP = "ultrainstinct";
    private static final int INTERVAL = 20;
    /** Squared blocks of drift tolerated before the session counts as "got up and left". */
    private static final double MAX_DRIFT_SQR = 0.25;

    private static final Map<UUID, Session> MEDITATING = new ConcurrentHashMap<>();

    /** Where the player sat down, and what the session has paid out so far. */
    private static final class Session {
        private final Vec3 anchor;
        private int totalTp;
        private int seconds;

        private Session(Vec3 anchor) {
            this.anchor = anchor;
        }
    }

    private MeditationTraining() {
    }

    /**
     * Whis teaching the meditation, from his dialogue. He is the only source: the technique is his,
     * and it stays greyed out in the minigames list until he has handed it over.
     *
     * <p>Recorded as a known minigame on the player's own character data - the same store the base
     * mod's masters write to when they teach theirs ({@code Character.addKnownMinigame}). That is
     * what makes {@code MinigamesScreen} light the entry up on its own, and it rides along with the
     * stats sync instead of needing one of ours.
     *
     * <p>He teaches rather than starting a session, because he stands just outside the arena ring
     * ({@link BeerusPlanet#WHIS_POS}) - close enough to talk to from inside it, but not reliably, and
     * a lesson that only works from one exact tile would be a bad lesson.
     */
    public static void teach(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            boolean firstTime = !data.getCharacter().isMinigameKnown(MINIGAME_ID);
            if (firstTime) {
                data.getCharacter().addKnownMinigame(MINIGAME_ID);
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            }

            player.sendSystemMessage(Component.translatable(firstTime
                            ? "message.dmzdivine.meditation.taught"
                            : "message.dmzdivine.meditation.reminder")
                    .withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.translatable("message.dmzdivine.meditation.how")
                    .withStyle(ChatFormatting.GRAY));
        });
    }

    private static boolean isTaught(StatsData data) {
        return data.getCharacter().isMinigameKnown(MINIGAME_ID);
    }

    /** The player opened the meditation screen: every prerequisite is checked here, not there. */
    public static void requestStart(ServerPlayer player) {
        if (MEDITATING.containsKey(player.getUUID())) return;
        if (!DivineConfig.MEDITATION_ENABLED.get()) return;

        Component blocker = blocker(player);
        if (blocker != null) {
            player.sendSystemMessage(blocker.copy().withStyle(ChatFormatting.GRAY));
            return;
        }
        start(player);
    }

    /** The player closed the screen. Ends the session without any complaint - that is the normal exit. */
    public static void requestStop(ServerPlayer player) {
        stop(player, "message.dmzdivine.meditation.stopped");
    }

    public static boolean isMeditating(ServerPlayer player) {
        return MEDITATING.containsKey(player.getUUID());
    }

    /** Why this player cannot start meditating, or null when they can. */
    private static Component blocker(ServerPlayer player) {
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (data == null || !data.getStatus().isHasCreatedCharacter()) {
            return Component.translatable("message.dmzdivine.meditation.not_taught");
        }
        if (!isTaught(data)) {
            return Component.translatable("message.dmzdivine.meditation.not_taught");
        }
        if (!BeerusPlanet.isInTrainingArena(player)) {
            return Component.translatable("message.dmzdivine.meditation.need_arena");
        }
        if (!isUltraInstinctActive(data)) {
            return Component.translatable("message.dmzdivine.meditation.need_ui");
        }
        return null;
    }

    /** Sign or Mastered - both count, this is meditation, not a display of power. */
    private static boolean isUltraInstinctActive(StatsData data) {
        return data.getCharacter().hasActiveStackForm()
                && UI_GROUP.equalsIgnoreCase(data.getCharacter().getActiveStackFormGroup());
    }

    private static void start(ServerPlayer player) {
        Session session = new Session(player.position());
        MEDITATING.put(player.getUUID(), session);

        NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(),
                TriggerAnimationS2C.AnimationType.KI_ANIMATION, 1, -1, "base.meditation"), player);
        // Opens the screen. The rate is only an estimate until the first payout, but showing 0 for a
        // second reads as "this is not working".
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data ->
                DivineNetwork.sendTo(player, new MeditationStateS2C(true, effectiveTp(data, tpPerSecond(data)), 0, 0)));
    }

    private static void stop(ServerPlayer player, String messageKey) {
        if (MEDITATING.remove(player.getUUID()) == null) return;

        NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(),
                TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), player);
        DivineNetwork.sendTo(player, new MeditationStateS2C(false, 0, 0, 0));
        if (messageKey != null) {
            player.sendSystemMessage(Component.translatable(messageKey).withStyle(ChatFormatting.GRAY));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        Session session = MEDITATING.get(player.getUUID());
        if (session == null) return;

        // Checked every tick, not every second: being moved should break the pose immediately. The
        // open screen already swallows movement keys, so this catches knockback and teleports.
        if (!player.isAlive() || player.position().distanceToSqr(session.anchor) > MAX_DRIFT_SQR) {
            stop(player, "message.dmzdivine.meditation.moved");
            return;
        }

        if (player.tickCount % INTERVAL == 0) {
            award(player, session);
        }
    }

    private static void award(ServerPlayer player, Session session) {
        if (!DivineConfig.MEDITATION_ENABLED.get()) {
            stop(player, null);
            return;
        }
        if (!BeerusPlanet.isInTrainingArena(player)) {
            stop(player, "message.dmzdivine.meditation.left_arena");
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!isUltraInstinctActive(data)) {
                stop(player, "message.dmzdivine.meditation.form_lost");
                return;
            }

            boolean useStamina = "stamina".equalsIgnoreCase(DivineConfig.MEDITATION_RESOURCE.get());
            float cost = (float) ((useStamina ? data.getMaxStamina() : data.getMaxEnergy())
                    * DivineConfig.MEDITATION_RESOURCE_PERCENT.get() / 100.0);
            float available = useStamina
                    ? data.getResources().getCurrentStamina()
                    : data.getResources().getCurrentEnergy();
            if (available < cost) {
                stop(player, "message.dmzdivine.meditation.exhausted");
                return;
            }

            float tp = tpPerSecond(data);
            if (tp <= 0) return;

            if (useStamina) data.getResources().removeStamina(cost);
            else data.getResources().removeEnergy(cost);

            // shareWithParty false: an idle trickle is the player's own, and sharing it would let one
            // meditating member drip TP into a whole party that is doing nothing.
            data.getResources().addTrainingPoints(tp, false);

            session.seconds++;
            int credited = effectiveTp(data, tp);
            session.totalTp += credited;
            DivineNetwork.sendTo(player, new MeditationStateS2C(true, credited, session.totalTp, session.seconds));
        });
    }

    /**
     * A slice of one stat point's current price. {@code getSingleStatCost} returns
     * {@link Integer#MAX_VALUE} when the server has manual TP purchases turned off, which would turn
     * the fraction into nonsense - that case takes the flat fallback instead.
     */
    private static float tpPerSecond(StatsData data) {
        int statCost = data.getSingleStatCost(data.getStats().getTotalStats());
        if (statCost <= 0 || statCost == Integer.MAX_VALUE) {
            return DivineConfig.MEDITATION_FALLBACK_TP.get();
        }
        float tp = (float) (statCost * DivineConfig.MEDITATION_STAT_COST_FRACTION.get());
        return tp > 0 && tp < 1 ? 1 : tp;
    }

    /**
     * What the player actually receives, for the screen to show. {@code addTrainingPoints} fires the
     * base mod's TPGainEvent, whose handler replaces the amount with {@code calculateTPGain(base,
     * STORY)} - the arena multiplier, gravity, weight bells and the rest. Reporting the raw number
     * instead would show a third of what lands.
     */
    private static int effectiveTp(StatsData data, float base) {
        return data.calculateTPGain((int) base, TpSource.STORY);
    }

    /**
     * Being hit breaks the pose - the whole point is that nothing is happening. An attack Ultra
     * Instinct already dodged arrives here cancelled ({@link UltraInstinctEvasion} runs at HIGH
     * priority) and is left alone: the body answered without the mind, which is the meditation
     * working, not breaking.
     */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof ServerPlayer player && MEDITATING.containsKey(player.getUUID())) {
            stop(player, "message.dmzdivine.meditation.interrupted");
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player, null);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MEDITATING.remove(event.getEntity().getUUID());
    }
}

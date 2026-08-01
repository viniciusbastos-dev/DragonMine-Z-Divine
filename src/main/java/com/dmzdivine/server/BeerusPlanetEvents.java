package com.dmzdivine.server;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.DivineConfig;
import com.dmzdivine.common.UltraInstinctTraining;
import com.dmzdivine.common.world.BeerusPlanet;
import com.dmzdivine.common.world.BeerusPlanetBuilder;
import com.dmzdivine.common.world.BeerusPlanetData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Everything that happens to a player because they are on Beerus' planet:
 *
 * <ul>
 *   <li>building the hub/arena the first time anyone arrives, and putting the arrival back on the
 *       landing pad afterwards (the pod drops them at fixed coordinates that are still raw hills
 *       until the build runs);</li>
 *   <li>banking arena training time, which is what Whis' Ultra Instinct trial asks for.</li>
 * </ul>
 *
 * <p>The two things the arena is actually famous for are not here, because both have to answer inside
 * the base mod's own math to show up in its HUD and stat screens: the extra gravity lives in
 * {@link com.dmzdivine.mixin.GravityLogicMixin} and the TP bonus in
 * {@link com.dmzdivine.mixin.StatsDataTpMixin}.
 */
@Mod.EventBusSubscriber(modid = DMZDivine.MODID)
public final class BeerusPlanetEvents {

    /** How often arena time is banked, in ticks. Also the action bar refresh rate. */
    private static final int TRAINING_INTERVAL = 20;

    /** Players currently inside the arena, so entering/leaving can be announced exactly once. */
    private static final Set<UUID> INSIDE_ARENA = new HashSet<>();

    private BeerusPlanetEvents() {
    }

    /**
     * Says out loud whether the planet exists. The dimension comes from this addon's datapack, so a
     * missing or unparseable JSON only shows up as an empty pod destination hours later - this turns
     * that into one line in the server log at boot.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (event.getServer().getLevel(BeerusPlanet.DIMENSION) != null) {
            DMZDivine.LOGGER.info("Beerus' planet is loaded ({})", BeerusPlanet.DIMENSION_ID);
        } else {
            DMZDivine.LOGGER.warn("Beerus' planet ({}) is NOT loaded - the space pod destination will do "
                    + "nothing. Is data/dmzdivine/dimension/beerus_planet.json present and valid?",
                    BeerusPlanet.DIMENSION_ID);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        INSIDE_ARENA.remove(player.getUUID());
        if (!event.getTo().equals(BeerusPlanet.DIMENSION)) return;

        ServerLevel planet = player.server.getLevel(BeerusPlanet.DIMENSION);
        if (planet == null) return;

        if (BeerusPlanetBuilder.buildIfNeeded(planet)) {
            placeOnLandingPad(player);
        }
        player.sendSystemMessage(Component.translatable("message.dmzdivine.beerus.arrival")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!BeerusPlanet.isOnPlanet(player)) return;

        ServerLevel planet = player.serverLevel();
        if (BeerusPlanetBuilder.buildIfNeeded(planet)) {
            placeOnLandingPad(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        INSIDE_ARENA.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        boolean inArena = BeerusPlanet.isInTrainingArena(player) && player.isAlive();
        boolean wasInside = INSIDE_ARENA.contains(player.getUUID());

        if (!inArena) {
            if (wasInside) {
                INSIDE_ARENA.remove(player.getUUID());
                player.sendSystemMessage(Component.translatable("message.dmzdivine.arena.left")
                        .withStyle(ChatFormatting.GRAY));
            }
            return;
        }

        if (!wasInside) {
            INSIDE_ARENA.add(player.getUUID());
            player.sendSystemMessage(Component.translatable("message.dmzdivine.arena.entered",
                            DivineConfig.ARENA_GRAVITY.get().intValue(),
                            String.format("%.1f", DivineConfig.ARENA_TP_MULTIPLIER.get()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        if (player.tickCount % TRAINING_INTERVAL != 0) return;

        BeerusPlanetData data = BeerusPlanetData.get(player.serverLevel());
        data.addArenaTicks(player.getUUID(), TRAINING_INTERVAL);
        showTrainingProgress(player, data);
    }

    private static void showTrainingProgress(ServerPlayer player, BeerusPlanetData data) {
        int trained = data.getArenaTicks(player.getUUID());
        int level = currentUiLevel(player);
        if (level >= UltraInstinctTraining.MAX_LEVEL) {
            player.displayClientMessage(Component.translatable("message.dmzdivine.arena.hud_mastered",
                    UltraInstinctTraining.minutes(trained)), true);
            return;
        }

        int required = UltraInstinctTraining.requiredTicks(level + 1);
        player.displayClientMessage(Component.translatable("message.dmzdivine.arena.hud",
                UltraInstinctTraining.minutes(trained), UltraInstinctTraining.minutes(required)), true);
    }

    private static int currentUiLevel(ServerPlayer player) {
        return com.dragonminez.common.stats.StatsProvider
                .get(com.dragonminez.common.stats.StatsCapability.INSTANCE, player)
                .map(UltraInstinctTraining::currentLevel)
                .orElse(0);
    }

    /** Puts an arriving player (and the pod they rode in on) back on the pad after a fresh build. */
    private static void placeOnLandingPad(ServerPlayer player) {
        double x = BeerusPlanet.LANDING.getX() + 0.5;
        double y = BeerusPlanet.LANDING.getY();
        double z = BeerusPlanet.LANDING.getZ() + 0.5;

        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            vehicle.teleportTo(x, y, z);
        }
        player.teleportTo(x, y, z);
    }
}

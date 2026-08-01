package com.dmzdivine.server;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.common.GodRitual;
import com.dmzdivine.common.UltraInstinctTraining;
import com.dmzdivine.common.world.BeerusPlanet;
import com.dmzdivine.common.world.BeerusPlanetBuilder;
import com.dmzdivine.common.world.BeerusPlanetData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Admin/debug entry points:
 * <pre>
 * /dmzdivine godritual [player]  - attempt the Super Saiyan God ritual (Beerus and Goku also start it)
 * /dmzdivine beerus travel          - (ops) land on the planet's pad without a space pod
 * /dmzdivine beerus training        - how much arena training is banked for Whis' trial
 * /dmzdivine beerus training add &lt;minutes&gt; [player] - (ops) bank arena training without standing in it
 * /dmzdivine beerus respawnmasters  - (ops) clear duplicated Beerus/Whis and place one of each
 * </pre>
 */
@Mod.EventBusSubscriber(modid = DMZDivine.MODID)
public final class DivineCommands {

    private DivineCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dmzdivine")
                .then(Commands.literal("godritual")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            GodRitual.start(player);
                            return 1;
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> {
                                    GodRitual.start(EntityArgument.getPlayer(ctx, "player"));
                                    return 1;
                                })))
                .then(Commands.literal("beerus")
                        .then(Commands.literal("travel")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> travel(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("training")
                                .executes(ctx -> reportTraining(ctx.getSource().getPlayerOrException()))
                                .then(Commands.literal("add")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 100000))
                                                .executes(ctx -> addTraining(ctx.getSource(),
                                                        ctx.getSource().getPlayerOrException(),
                                                        IntegerArgumentType.getInteger(ctx, "minutes")))
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .executes(ctx -> addTraining(ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                IntegerArgumentType.getInteger(ctx, "minutes")))))))
                        .then(Commands.literal("respawnmasters")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> respawnMasters(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("rebuild")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> rebuild(ctx.getSource().getPlayerOrException())))));
    }

    private static int travel(ServerPlayer player) {
        ServerLevel planet = player.server.getLevel(BeerusPlanet.DIMENSION);
        if (planet == null) {
            player.sendSystemMessage(Component.literal(
                    "Beerus' planet is not loaded - is dmzdivine:beerus_planet present in the datapack?"));
            return 0;
        }

        BeerusPlanetBuilder.buildIfNeeded(planet);
        player.teleportTo(planet,
                BeerusPlanet.LANDING.getX() + 0.5,
                BeerusPlanet.LANDING.getY(),
                BeerusPlanet.LANDING.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        return 1;
    }

    /** Re-lays the hub and arena over an existing planet - e.g. after the ground blocks changed. */
    private static int rebuild(ServerPlayer player) {
        ServerLevel planet = player.server.getLevel(BeerusPlanet.DIMENSION);
        if (planet == null) {
            player.sendSystemMessage(Component.literal("Beerus' planet is not loaded."));
            return 0;
        }

        BeerusPlanetBuilder.rebuild(planet);
        player.sendSystemMessage(Component.literal("Rebuilt the hub and Whis' arena."));
        return 1;
    }

    /** Cleans up duplicated Beerus/Whis (worlds visited before the spawn-once fix) and reseats one of each. */
    private static int respawnMasters(ServerPlayer player) {
        ServerLevel planet = player.server.getLevel(BeerusPlanet.DIMENSION);
        if (planet == null) {
            player.sendSystemMessage(Component.literal("Beerus' planet is not loaded."));
            return 0;
        }

        BeerusPlanetBuilder.respawnMasters(planet);
        player.sendSystemMessage(Component.literal("Beerus and Whis were cleared and placed again."));
        return 1;
    }

    private static int reportTraining(ServerPlayer player) {
        ServerLevel planet = player.server.getLevel(BeerusPlanet.DIMENSION);
        int ticks = planet == null ? 0 : BeerusPlanetData.get(planet).getArenaTicks(player.getUUID());
        player.sendSystemMessage(Component.literal("Arena training: " + UltraInstinctTraining.minutes(ticks) + " min"));
        return 1;
    }

    /**
     * Banks arena training straight into {@link BeerusPlanetData}, the same place the arena ticker
     * writes to - so it counts towards Whis' trial and is spent by it exactly like time actually
     * trained. The planet has to exist: that saved data lives in its dimension folder.
     */
    private static int addTraining(CommandSourceStack source, ServerPlayer target, int minutes) {
        ServerLevel planet = target.server.getLevel(BeerusPlanet.DIMENSION);
        if (planet == null) {
            source.sendFailure(Component.literal(
                    "Beerus' planet is not loaded - is dmzdivine:beerus_planet present in the datapack?"));
            return 0;
        }

        BeerusPlanetData data = BeerusPlanetData.get(planet);
        data.addArenaTicks(target.getUUID(), UltraInstinctTraining.ticks(minutes));
        int total = UltraInstinctTraining.minutes(data.getArenaTicks(target.getUUID()));

        source.sendSuccess(() -> Component.literal("Added " + minutes + " min of arena training to "
                + target.getGameProfile().getName() + " (now " + total + " min)"), true);
        return 1;
    }
}

package com.dmzdivine.server;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.common.GodRitual;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Temporary entry point for the god ritual until the custom master NPC exists:
 * /dmzdivine godritual           - attempt the ritual yourself
 * /dmzdivine godritual <player>  - (ops) start the ritual for someone else
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
                                }))));
    }
}

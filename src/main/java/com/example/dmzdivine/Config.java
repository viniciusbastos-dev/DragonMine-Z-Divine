package com.example.dmzdivine;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// Forge config scaffold. Left empty on purpose - add ForgeConfigSpec values here as the addon
// grows (e.g. ritual minigame difficulty, per-form toggles not expressible in DragonMineZ's own JSON).
@Mod.EventBusSubscriber(modid = DMZDivine.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
    }
}

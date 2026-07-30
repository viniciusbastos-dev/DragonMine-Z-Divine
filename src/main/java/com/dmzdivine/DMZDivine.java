package com.dmzdivine;

import com.dmzdivine.common.DivineFormsInstaller;
import com.dmzdivine.network.DivineNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import org.slf4j.Logger;

@Mod(DMZDivine.MODID)
public class DMZDivine {
    public static final String MODID = "dmzdivine";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DMZDivine(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);

        context.registerConfig(ModConfig.Type.COMMON, DivineConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // enqueueWork, not the parallel setup thread: DivineFormsInstaller mutates DragonMineZ's
        // static config maps, and mod setup runs concurrently across mods.
        event.enqueueWork(() -> {
            DivineNetwork.register();
            DivineFormsInstaller.install();
        });
        LOGGER.info("DMZ Divine initialized");
    }
}

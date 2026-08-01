package com.dmzdivine.client;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.common.init.DivineEntities;
import com.dragonminez.client.init.entities.renderer.MasterEntityRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Reuses the base mod's own master renderer - see GodMasterEntity's javadoc for why that's safe. */
@Mod.EventBusSubscriber(modid = DMZDivine.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DivineEntityRenderers {

    private DivineEntityRenderers() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DivineEntities.GOD_MASTER.get(), MasterEntityRenderer::new);
        event.registerEntityRenderer(DivineEntities.BEERUS_MASTER.get(), MasterEntityRenderer::new);
        event.registerEntityRenderer(DivineEntities.WHIS_MASTER.get(), MasterEntityRenderer::new);
    }
}

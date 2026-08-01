package com.dmzdivine.client;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.network.C2S.MeditationActionC2S;
import com.dmzdivine.network.DivineNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * The addon's keybinds. Only one so far: the one that asks to start meditating.
 *
 * <p>The key does nothing locally - it sends {@link MeditationActionC2S} and lets the server decide,
 * which is also what opens the screen. It only ever starts: closing the screen is what stops a
 * session, so the key cannot get out of step with the server. Registration is on the mod bus, the
 * polling on the Forge bus, so the two live in one class behind two subscriber annotations.
 */
public final class DivineKeybinds {

    public static final String CATEGORY = "key.categories.dmzdivine";

    public static final KeyMapping MEDITATE = new KeyMapping(
            "key.dmzdivine.meditate",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY);

    private DivineKeybinds() {
    }

    @Mod.EventBusSubscriber(modid = DMZDivine.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {

        private Registration() {
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(MEDITATE);
        }
    }

    @Mod.EventBusSubscriber(modid = DMZDivine.MODID, value = Dist.CLIENT)
    public static final class Input {

        private Input() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            // Drains the click queue: a press while a screen was open should not fire later.
            while (MEDITATE.consumeClick()) {
                DivineNetwork.sendToServer(new MeditationActionC2S(true));
            }
        }
    }
}

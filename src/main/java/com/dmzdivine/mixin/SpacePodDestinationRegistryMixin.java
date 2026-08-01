package com.dmzdivine.mixin;

import com.dmzdivine.common.world.BeerusPlanet;
import com.dragonminez.common.spacepod.SpacePodDestinationDefinition;
import com.dragonminez.common.spacepod.SpacePodDestinationRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.gson.JsonElement;

import java.util.List;
import java.util.Map;

/**
 * Points the space pod's "beerus" destination at our planet.
 *
 * <p>A datapack file can't do this: the base mod's own {@code spacepod/destinations.json} declares
 * {@code "replace": true} and its resource key sorts after any file we could ship, so it clears
 * whatever we loaded first and re-adds Beerus locked behind {@code "NEVER"} (it was reserved for the
 * DMZ Super addon's dimension). Rewriting the finished list is the one hook that survives that,
 * and it stays correct if the base mod later edits its own entry - we only replace the one id.
 *
 * <p>Runs server-side; clients get the rewritten list through the base mod's existing
 * {@code SyncSpacePodDestinationsS2C} on datapack sync.
 */
@Mixin(value = SpacePodDestinationRegistry.class, remap = false)
public abstract class SpacePodDestinationRegistryMixin {

    @Shadow
    private static List<SpacePodDestinationDefinition> serverDestinations;

    @Inject(method = "apply", at = @At("TAIL"))
    private void dmzdivine$installBeerusPlanet(Map<ResourceLocation, JsonElement> map,
                                               ResourceManager resourceManager,
                                               ProfilerFiller profiler,
                                               CallbackInfo ci) {
        serverDestinations = BeerusPlanet.withBeerusDestination(serverDestinations);
    }
}

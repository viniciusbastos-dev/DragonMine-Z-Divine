package com.dmzdivine.common.world;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.DivineConfig;
import com.dragonminez.common.spacepod.SpacePodDestinationDefinition;
import com.dragonminez.common.spacepod.SpacePodUnlockExpression;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometry and identity of Beerus' planet: the dimension key, the layout the builder carves out,
 * and the space pod destination that replaces the base mod's permanently locked one.
 *
 * <p>The base mod already ships a "beerus" destination in {@code data/dragonminez/spacepod/destinations.json},
 * pointing at {@code dmzsuper:beerus_planet} with {@code unlock_rules: "NEVER"} - a hook left for the
 * DMZ Super addon. We can't out-vote it from a datapack file (that file sets {@code replace: true} and
 * sorts after ours, wiping anything we load first), so {@link com.dmzdivine.mixin.SpacePodDestinationRegistryMixin}
 * rewrites the entry after the base mod's reload listener is done - see {@link #withBeerusDestination}.
 */
public final class BeerusPlanet {

    public static final ResourceLocation DIMENSION_ID =
            new ResourceLocation(DMZDivine.MODID, "beerus_planet");
    public static final ResourceKey<Level> DIMENSION =
            ResourceKey.create(Registries.DIMENSION, DIMENSION_ID);

    /** Destination id in the pod's GUI. Reuses the base mod's id so its entry is replaced, not duplicated. */
    public static final String DESTINATION_ID = "beerus";
    /** Icon 5 of the base mod's space pod sheet is already the Beerus planet icon. */
    private static final int DESTINATION_ICON = 5;
    /** Translation key already present in the base mod's lang files ("Beerus Planet"). */
    private static final String DESTINATION_NAME = "gui.dragonminez.spacepod.beerus";

    // --- Layout. The builder flattens everything below to FLOOR_Y and clears the air above it. ---

    /** Ground level of the whole built area. Terrain noise peaks at y=116, so the pads sit above it. */
    public static final int FLOOR_Y = 118;
    /** Where the pod lands and where players stand when they arrive. */
    public static final BlockPos LANDING = new BlockPos(0, FLOOR_Y + 1, 0);
    /** Flattened hub around the landing pad: safe ground, planet gravity only. */
    public static final int HUB_RADIUS = 30;

    /** Center of Whis' training arena - the high gravity zone. */
    public static final BlockPos ARENA_CENTER = new BlockPos(72, FLOOR_Y, 0);
    /** Radius of the arena floor, and of the gravity/TP zone (they line up on purpose). */
    public static final int ARENA_RADIUS = 22;

    public static final BlockPos BEERUS_POS = new BlockPos(-8, FLOOR_Y + 1, 6);
    public static final BlockPos WHIS_POS = new BlockPos(ARENA_CENTER.getX() - ARENA_RADIUS - 4, FLOOR_Y + 1, 0);

    private BeerusPlanet() {
    }

    public static boolean isOnPlanet(Level level) {
        return level != null && level.dimension().equals(DIMENSION);
    }

    public static boolean isOnPlanet(Entity entity) {
        return entity != null && isOnPlanet(entity.level());
    }

    /**
     * True inside Whis' arena - the cylinder that carries the extra gravity and the training TP bonus.
     * Vertically generous so flying inside the ring still counts, but not infinite: fly high enough and
     * you leave the training field, which is what makes "escape upwards" a real choice.
     */
    public static boolean isInTrainingArena(Entity entity) {
        if (!isOnPlanet(entity)) return false;
        double dx = entity.getX() - (ARENA_CENTER.getX() + 0.5);
        double dz = entity.getZ() - (ARENA_CENTER.getZ() + 0.5);
        if (dx * dx + dz * dz > (double) ARENA_RADIUS * ARENA_RADIUS) return false;
        double y = entity.getY();
        return y >= FLOOR_Y - 4 && y <= FLOOR_Y + 48;
    }

    /**
     * Returns the destination list with our Beerus entry in place of the base mod's locked stub
     * (matched by id), appending it when the base mod ever drops its own entry.
     */
    public static List<SpacePodDestinationDefinition> withBeerusDestination(
            List<SpacePodDestinationDefinition> loaded) {
        SpacePodDestinationDefinition ours = buildDestination();
        List<SpacePodDestinationDefinition> result = new ArrayList<>(loaded != null ? loaded : List.of());

        for (int i = 0; i < result.size(); i++) {
            if (DESTINATION_ID.equals(result.get(i).id())) {
                result.set(i, ours);
                return List.copyOf(result);
            }
        }

        result.add(ours);
        return List.copyOf(result);
    }

    private static SpacePodDestinationDefinition buildDestination() {
        // Fixed coordinates: the landing pad is the one spot guaranteed to be flat and lit, and
        // keeping the player's overworld x/z (the default when no coords are given) would drop them
        // into unbuilt hills.
        return new SpacePodDestinationDefinition(
                DESTINATION_ID,
                DESTINATION_NAME,
                true,
                DIMENSION_ID.toString(),
                DESTINATION_ICON,
                null,
                LANDING.getX() + 0.5,
                (double) LANDING.getY(),
                LANDING.getZ() + 0.5,
                true,
                new SpacePodUnlockExpression.LevelAtLeast(DivineConfig.BEERUS_UNLOCK_LEVEL.get())
        );
    }
}

package com.dmzdivine.common.world;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.common.entity.DivineMasterEntity;
import com.dmzdivine.common.init.DivineBlocks;
import com.dmzdivine.common.init.DivineEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Carves the two built areas of Beerus' planet out of the generated terrain, once per world.
 *
 * <p>The planet's worldgen is plain rolling hills; everything readable - the landing pad, the path,
 * Whis' arena ring - is placed here instead of through structure NBT, because the whole layout has
 * to line up exactly with the coordinates {@link BeerusPlanet} uses for the pod's landing spot and
 * for the high gravity zone. Anything defined twice (a structure file plus a constant) would drift.
 *
 * <p>Runs on first arrival, not at world load: generating and editing ~10 chunks is not something to
 * do on every server start for a dimension nobody has visited.
 */
public final class BeerusPlanetBuilder {

    // Resolved lazily: the builder runs long after registration, but these are static fields on a
    // class the server may touch early, and RegistryObject.get() before registry freeze would throw.
    private static BlockState ground() {
        return DivineBlocks.BEERUS_STONE.get().defaultBlockState();
    }

    private static BlockState topsoil() {
        return DivineBlocks.BEERUS_GRASS_BLOCK.get().defaultBlockState();
    }

    private static final BlockState PAD = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState PAD_RING = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
    private static final BlockState PATH = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState ARENA_FLOOR = Blocks.POLISHED_BASALT.defaultBlockState();
    private static final BlockState ARENA_RIM = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState PILLAR = Blocks.OBSIDIAN.defaultBlockState();
    private static final BlockState LAMP = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** How far down a flattened column keeps filling before giving up on meeting the natural ground. */
    private static final int MAX_FILL_DEPTH = 28;
    /** Headroom cleared above the floor so the plateau isn't buried under the hills it replaces. */
    private static final int CLEAR_HEIGHT = 40;

    private BeerusPlanetBuilder() {
    }

    /** Builds the planet if this world hasn't been built yet. Returns true if it built anything now. */
    public static boolean buildIfNeeded(ServerLevel level) {
        if (!BeerusPlanet.isOnPlanet(level)) return false;

        BeerusPlanetData data = BeerusPlanetData.get(level);
        if (data.isBuilt()) {
            spawnMastersOnce(level, data);
            return false;
        }

        long started = System.currentTimeMillis();
        buildHub(level);
        buildPath(level);
        buildArena(level);
        buildTree(level, new BlockPos(-17, BeerusPlanet.FLOOR_Y + 1, -15));
        spawnMastersOnce(level, data);

        data.setBuilt(true);
        DMZDivine.LOGGER.info("Built Beerus' planet ({} ms)", System.currentTimeMillis() - started);
        return true;
    }

    // --- Areas ---

    private static void buildHub(ServerLevel level) {
        int radius = BeerusPlanet.HUB_RADIUS;
        forEachDisc(0, 0, radius, (x, z, distance) -> {
            flattenColumn(level, x, z, topsoil());

            BlockPos floor = new BlockPos(x, BeerusPlanet.FLOOR_Y, z);
            if (distance <= 5.0) {
                level.setBlock(floor, PAD, Block.UPDATE_CLIENTS);
            } else if (distance <= 6.0) {
                level.setBlock(floor, PAD_RING, Block.UPDATE_CLIENTS);
            }
        });

        // Four lamps around the landing pad, so the pad reads as a pad at night (the sky here is fixed
        // to night, and the planet has no other light source).
        for (int[] offset : new int[][]{{7, 0}, {-7, 0}, {0, 7}, {0, -7}}) {
            BlockPos base = new BlockPos(offset[0], BeerusPlanet.FLOOR_Y + 1, offset[1]);
            level.setBlock(base, PILLAR, Block.UPDATE_CLIENTS);
            level.setBlock(base.above(), PILLAR, Block.UPDATE_CLIENTS);
            level.setBlock(base.above(2), LAMP, Block.UPDATE_CLIENTS);
        }
    }

    /** A walkable strip from the hub to the arena, along +X. */
    private static void buildPath(ServerLevel level) {
        int fromX = BeerusPlanet.HUB_RADIUS - 2;
        int toX = BeerusPlanet.ARENA_CENTER.getX() - BeerusPlanet.ARENA_RADIUS + 1;

        for (int x = fromX; x <= toX; x++) {
            for (int z = -2; z <= 2; z++) {
                flattenColumn(level, x, z, PATH);
            }
        }
    }

    private static void buildArena(ServerLevel level) {
        int centerX = BeerusPlanet.ARENA_CENTER.getX();
        int centerZ = BeerusPlanet.ARENA_CENTER.getZ();
        int radius = BeerusPlanet.ARENA_RADIUS;

        forEachDisc(centerX, centerZ, radius + 2, (x, z, distance) -> {
            BlockState surface = distance > radius ? ARENA_RIM : ARENA_FLOOR;
            flattenColumn(level, x, z, surface);
        });

        // Eight pillars on the rim: the visible edge of the 30g field, so players can see where the
        // heavy gravity starts instead of only feeling it.
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 / 8) * i;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
            for (int y = 1; y <= 6; y++) {
                level.setBlock(new BlockPos(x, BeerusPlanet.FLOOR_Y + y, z), PILLAR, Block.UPDATE_CLIENTS);
            }
            level.setBlock(new BlockPos(x, BeerusPlanet.FLOOR_Y + 7, z), LAMP, Block.UPDATE_CLIENTS);
        }
    }

    /** The planet's landmark tree. Rough on purpose - it reads as a canopy from the ground. */
    private static void buildTree(ServerLevel level, BlockPos base) {
        int trunkHeight = 16;
        for (int y = 0; y < trunkHeight; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlock(base.offset(x, y, z), Blocks.OAK_WOOD.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }

        BlockPos crown = base.above(trunkHeight);
        int leafRadius = 8;
        for (int x = -leafRadius; x <= leafRadius; x++) {
            for (int y = -4; y <= leafRadius - 2; y++) {
                for (int z = -leafRadius; z <= leafRadius; z++) {
                    double squash = y < 0 ? 1.6 : 1.0;
                    double distance = Math.sqrt(x * x + (y * squash) * (y * squash) + z * z);
                    if (distance > leafRadius) continue;
                    BlockPos pos = crown.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) continue;
                    level.setBlock(pos, Blocks.OAK_LEAVES.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    // --- Masters ---

    /**
     * Places Beerus and Whis exactly once per world. Scanning for them instead would be wrong: a
     * player arriving teleports before their chunks finish loading, the search comes back empty, and
     * every visit stacks another copy of both NPCs on top of the old ones.
     */
    private static void spawnMastersOnce(ServerLevel level, BeerusPlanetData data) {
        if (data.areMastersSpawned()) return;
        spawnMasters(level);
        data.setMastersSpawned(true);
    }

    /**
     * Lays the hub and arena down again over whatever is there now. For worlds built before a change
     * to the layout or to the planet's blocks - regenerating the dimension would be the alternative,
     * and that costs the players everything else they left here.
     */
    public static void rebuild(ServerLevel level) {
        long started = System.currentTimeMillis();
        buildHub(level);
        buildPath(level);
        buildArena(level);
        buildTree(level, new BlockPos(-17, BeerusPlanet.FLOOR_Y + 1, -15));
        BeerusPlanetData.get(level).setBuilt(true);
        DMZDivine.LOGGER.info("Rebuilt Beerus' planet ({} ms)", System.currentTimeMillis() - started);
    }

    /** Removes every master this addon placed here and puts a single fresh pair back. */
    public static void respawnMasters(ServerLevel level) {
        for (EntityType<? extends DivineMasterEntity> type : List.of(
                DivineEntities.BEERUS_MASTER.get(), DivineEntities.WHIS_MASTER.get())) {
            for (DivineMasterEntity existing : level.getEntities(type,
                    new AABB(BeerusPlanet.LANDING).inflate(512.0), entity -> true)) {
                existing.discard();
            }
        }
        spawnMasters(level);
        BeerusPlanetData.get(level).setMastersSpawned(true);
    }

    private static void spawnMasters(ServerLevel level) {
        spawnMaster(level, DivineEntities.BEERUS_MASTER.get(), BeerusPlanet.BEERUS_POS, -90.0f);
        spawnMaster(level, DivineEntities.WHIS_MASTER.get(), BeerusPlanet.WHIS_POS, 90.0f);
    }

    private static void spawnMaster(ServerLevel level,
                                    EntityType<? extends DivineMasterEntity> type,
                                    BlockPos pos,
                                    float yRot) {
        DivineMasterEntity master = type.create(level, null, null, pos, MobSpawnType.COMMAND, false, false);
        if (master == null) {
            DMZDivine.LOGGER.error("Could not create master entity {} on Beerus' planet", type.getDescriptionId());
            return;
        }
        master.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yRot, 0.0f);
        level.addFreshEntity(master);
    }

    // --- Helpers ---

    /**
     * Clears the air column above the floor and fills downwards until it meets solid ground, so a
     * flattened area sits on the terrain as a plateau instead of floating over it.
     */
    private static void flattenColumn(ServerLevel level, int x, int z, BlockState surface) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = BeerusPlanet.FLOOR_Y + 1; y <= BeerusPlanet.FLOOR_Y + CLEAR_HEIGHT; y++) {
            cursor.set(x, y, z);
            if (!level.getBlockState(cursor).isAir()) {
                level.setBlock(cursor, AIR, Block.UPDATE_CLIENTS);
            }
        }

        level.setBlock(new BlockPos(x, BeerusPlanet.FLOOR_Y, z), surface, Block.UPDATE_CLIENTS);

        for (int depth = 1; depth <= MAX_FILL_DEPTH; depth++) {
            cursor.set(x, BeerusPlanet.FLOOR_Y - depth, z);
            if (!level.getBlockState(cursor).isAir()) break;
            level.setBlock(cursor, ground(), Block.UPDATE_CLIENTS);
        }
    }

    private static void forEachDisc(int centerX, int centerZ, int radius, DiscConsumer consumer) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) continue;
                consumer.accept(centerX + dx, centerZ + dz, distance);
            }
        }
    }

    @FunctionalInterface
    private interface DiscConsumer {
        void accept(int x, int z, double distance);
    }
}

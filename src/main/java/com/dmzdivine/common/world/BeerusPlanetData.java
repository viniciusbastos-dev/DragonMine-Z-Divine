package com.dmzdivine.common.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-world state of Beerus' planet, saved inside the planet's own dimension folder: whether the
 * hub/arena have already been carved out, and how long each player has trained inside Whis' arena.
 *
 * <p>The training time is what gates Whis' Ultra Instinct trial - it is deliberately stored on the
 * world and not on the player's DragonMineZ stats, so it survives nothing but this world (a fresh
 * world means training the god of destruction's planet again from scratch).
 */
public class BeerusPlanetData extends SavedData {

    private static final String FILE_ID = "dmzdivine_beerus";
    private static final String KEY_BUILT = "Built";
    private static final String KEY_MASTERS = "MastersSpawned";
    private static final String KEY_TRAINING = "ArenaTraining";
    private static final String KEY_UUID = "Player";
    private static final String KEY_TICKS = "Ticks";

    private boolean built;
    private boolean mastersSpawned;
    private final Map<UUID, Integer> arenaTicks = new HashMap<>();

    public static BeerusPlanetData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BeerusPlanetData::load, BeerusPlanetData::new, FILE_ID);
    }

    public BeerusPlanetData() {
    }

    public static BeerusPlanetData load(CompoundTag tag) {
        BeerusPlanetData data = new BeerusPlanetData();
        data.built = tag.getBoolean(KEY_BUILT);
        data.mastersSpawned = tag.getBoolean(KEY_MASTERS);

        ListTag list = tag.getList(KEY_TRAINING, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID(KEY_UUID)) continue;
            data.arenaTicks.put(entry.getUUID(KEY_UUID), entry.getInt(KEY_TICKS));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean(KEY_BUILT, built);
        tag.putBoolean(KEY_MASTERS, mastersSpawned);

        ListTag list = new ListTag();
        arenaTicks.forEach((id, ticks) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(KEY_UUID, id);
            entry.putInt(KEY_TICKS, ticks);
            list.add(entry);
        });
        tag.put(KEY_TRAINING, list);
        return tag;
    }

    public boolean isBuilt() {
        return built;
    }

    public void setBuilt(boolean built) {
        this.built = built;
        setDirty();
    }

    /**
     * Whether Beerus and Whis have been placed. Kept as a flag instead of scanning for them on
     * arrival: the chunks they stand in are usually still loading when a player lands, so an entity
     * search comes back empty and spawns a second copy of each - which is exactly what happened.
     */
    public boolean areMastersSpawned() {
        return mastersSpawned;
    }

    public void setMastersSpawned(boolean spawned) {
        this.mastersSpawned = spawned;
        setDirty();
    }

    public int getArenaTicks(UUID player) {
        return arenaTicks.getOrDefault(player, 0);
    }

    public void addArenaTicks(UUID player, int ticks) {
        if (ticks <= 0) return;
        // Saturating: the command can hand out arbitrary amounts, and a wrapped total would read as
        // "no training at all".
        arenaTicks.merge(player, ticks, (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b));
        setDirty();
    }

    /**
     * Takes banked training back out, which is what a passed trial costs: each Ultra Instinct level
     * is paid for with its own stretch of arena time instead of a running total.
     */
    public void spendArenaTicks(UUID player, int ticks) {
        if (ticks <= 0) return;
        int left = Math.max(0, getArenaTicks(player) - ticks);
        arenaTicks.put(player, left);
        setDirty();
    }
}

package com.dmzdivine.common.init;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.common.entity.BeerusMasterEntity;
import com.dmzdivine.common.entity.GodMasterEntity;
import com.dmzdivine.common.entity.WhisMasterEntity;
import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The addon's teaching NPCs. Each is registered under the entity path the base mod's
 * {@code MasterGlobalModel} resolves assets from (see GodMasterEntity's javadoc for why that works):
 *
 * <ul>
 *   <li>{@code master_goku} - reuses the base mod's shipped Goku model/texture/animation.</li>
 *   <li>{@code master_beerus} / {@code master_whis} - the base mod registers entity types under
 *       these names but ships no assets for them (they fall back to the robot model), so this addon
 *       supplies {@code assets/dragonminez/{geo,textures}/entity/master/master_{beerus,whis}.*}.
 *       The animation still falls back to Goku's idle, which the shared bone names make work.</li>
 * </ul>
 */
public final class DivineEntities {

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DMZDivine.MODID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DMZDivine.MODID);

    public static final RegistryObject<EntityType<GodMasterEntity>> GOD_MASTER = ENTITY_TYPES.register("master_goku",
            () -> EntityType.Builder.of(GodMasterEntity::new, MobCategory.CREATURE)
                    .sized(0.8f, 2.0f)
                    .build(DMZDivine.MODID + ":master_goku"));

    public static final RegistryObject<EntityType<BeerusMasterEntity>> BEERUS_MASTER = ENTITY_TYPES.register("master_beerus",
            () -> EntityType.Builder.of(BeerusMasterEntity::new, MobCategory.CREATURE)
                    .sized(0.8f, 2.0f)
                    .build(DMZDivine.MODID + ":master_beerus"));

    public static final RegistryObject<EntityType<WhisMasterEntity>> WHIS_MASTER = ENTITY_TYPES.register("master_whis",
            () -> EntityType.Builder.of(WhisMasterEntity::new, MobCategory.CREATURE)
                    .sized(0.8f, 2.3f)
                    .build(DMZDivine.MODID + ":master_whis"));

    /** Colors mirror Goku's gi: orange body, blue undershirt. */
    public static final RegistryObject<Item> GOD_MASTER_SPAWN_EGG = ITEMS.register("master_goku_spawn_egg",
            () -> new ForgeSpawnEggItem(GOD_MASTER, 0xFF7A1A, 0x1E4FD1, new Item.Properties()));

    /** Beerus: purple hide, gold sash. */
    public static final RegistryObject<Item> BEERUS_MASTER_SPAWN_EGG = ITEMS.register("master_beerus_spawn_egg",
            () -> new ForgeSpawnEggItem(BEERUS_MASTER, 0x6B4FA0, 0xE0B84C, new Item.Properties()));

    /** Whis: pale blue skin, white hair. */
    public static final RegistryObject<Item> WHIS_MASTER_SPAWN_EGG = ITEMS.register("master_whis_spawn_egg",
            () -> new ForgeSpawnEggItem(WHIS_MASTER, 0x7FB6D6, 0xF2F2F2, new Item.Properties()));

    private DivineEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(DivineEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GOD_MASTER.get(), MastersEntity.createAttributes().build());
        event.put(BEERUS_MASTER.get(), MastersEntity.createAttributes().build());
        event.put(WHIS_MASTER.get(), MastersEntity.createAttributes().build());
    }
}

package com.dmzdivine.common.init;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.common.entity.GodMasterEntity;
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
 * The god ritual's master NPC. Registered under the entity path "master_goku" so it renders with
 * the base mod's real Goku assets - see GodMasterEntity's javadoc for why that works.
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

    /** Colors mirror Goku's gi: orange body, blue undershirt. */
    public static final RegistryObject<Item> GOD_MASTER_SPAWN_EGG = ITEMS.register("master_goku_spawn_egg",
            () -> new ForgeSpawnEggItem(GOD_MASTER, 0xFF7A1A, 0x1E4FD1, new Item.Properties()));

    private DivineEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(DivineEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GOD_MASTER.get(), MastersEntity.createAttributes().build());
    }
}

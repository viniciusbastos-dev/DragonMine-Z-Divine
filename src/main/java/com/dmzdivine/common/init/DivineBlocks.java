package com.dmzdivine.common.init;

import com.dmzdivine.DMZDivine;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The ground of Beerus' planet.
 *
 * <p>Its own blocks rather than vanilla grass/dirt/stone so the planet can look like somewhere else:
 * the surface rule in {@code worldgen/noise_settings/beerus_planet.json} places these, and
 * {@code BeerusPlanetBuilder} uses the same ones when it flattens the hub and the arena.
 *
 * <p>The shipped textures are flat placeholders - they exist so the blocks render and so there is a
 * file at the right path to overwrite. Replace the PNGs under
 * {@code assets/dmzdivine/textures/block/} and nothing else needs to change.
 */
@Mod.EventBusSubscriber(modid = DMZDivine.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DivineBlocks {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DMZDivine.MODID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DMZDivine.MODID);

    public static final RegistryObject<Block> BEERUS_GRASS_BLOCK = register("beerus_grass_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)));
    public static final RegistryObject<Block> BEERUS_DIRT = register("beerus_dirt",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT)));
    public static final RegistryObject<Block> BEERUS_STONE = register("beerus_stone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)));

    private DivineBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    private static RegistryObject<Block> register(String name, java.util.function.Supplier<Block> block) {
        RegistryObject<Block> registered = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.NATURAL_BLOCKS) return;
        event.accept(BEERUS_GRASS_BLOCK.get());
        event.accept(BEERUS_DIRT.get());
        event.accept(BEERUS_STONE.get());
    }
}

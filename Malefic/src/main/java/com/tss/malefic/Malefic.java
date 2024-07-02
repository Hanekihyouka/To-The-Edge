package com.tss.malefic;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tss.malefic.content.mobeffects.Sticky;
import com.tss.malefic.content.mobeffects.SwiftFuse;
import com.tss.malefic.content.mobeffects.Webbed;
import com.tss.malefic.content.mobs.spawn.EmptyPredicate;
import com.tss.malefic.content.mobs.spawn.HostileSpawnPredicate;
import com.tss.malefic.content.mobs.spawn.SlimeSpawnPredicate;
import com.tss.malefic.handler.EventHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Malefic.MODID)
public class Malefic
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "malefic";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<MobEffect> MOBEFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    //public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    /*public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =  DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MODID);

    public static final RegistryObject<Codec<EntitySpawnBiomeModifier>> ENTITY_SPAWN_BIOME_MODIFIER_CODEC = BIOME_MODIFIER_SERIALIZERS.register("entity_spawn_modifier", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    // declare fields
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(EntitySpawnBiomeModifier::biomes),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(EntitySpawnBiomeModifier::feature)
                    // declare constructor
            ).apply(builder,EntitySpawnBiomeModifier::new)));*/
    public static final RegistryObject<MobEffect> MOBEFFECT_STICKY = MOBEFFECTS.register("sticky", () -> new Sticky());
    public static final RegistryObject<MobEffect> MOBEFFECT_WEBBED = MOBEFFECTS.register("webbed", () -> new Webbed());
    //public static final RegistryObject<MobEffect> MOBEFFECT_SWIFTFUSE = MOBEFFECTS.register("swiftfuse", () -> new SwiftFuse());
    public Malefic()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventHandler forgeEventHandler = new EventHandler();
        MinecraftForge.EVENT_BUS.register(forgeEventHandler);
        //modEventBus.register(forgeEventHandler);



        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        MOBEFFECTS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        //CREATIVE_MODE_TABS.register(modEventBus);
        //BIOME_MODIFIER_SERIALIZERS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        //modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        /*if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));*/

        //LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        //Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("Malefic loaded, Good luck.");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            //LOGGER.info("HELLO FROM CLIENT SETUP");
            //LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    // replace spawnrule
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class spawnPlacementRegister{
        @SubscribeEvent
        public static void spawnPlacementRegisterEvent(SpawnPlacementRegisterEvent e){
            e.register(EntityType.ZOMBIE, new HostileSpawnPredicate(), SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(EntityType.ZOMBIE_VILLAGER, new HostileSpawnPredicate(), SpawnPlacementRegisterEvent.Operation.REPLACE);

            e.register(EntityType.SPIDER, new HostileSpawnPredicate(),SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(EntityType.SKELETON, new HostileSpawnPredicate(),SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(EntityType.CREEPER, new HostileSpawnPredicate(),SpawnPlacementRegisterEvent.Operation.REPLACE);

            e.register(EntityType.DROWNED, new HostileSpawnPredicate(),SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(EntityType.STRAY, new HostileSpawnPredicate(),SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(EntityType.HUSK, new HostileSpawnPredicate(),SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(EntityType.WITHER_SKELETON, new HostileSpawnPredicate(), SpawnPlacementRegisterEvent.Operation.REPLACE);

            e.register(EntityType.RAVAGER, new HostileSpawnPredicate(), SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(EntityType.ILLUSIONER, new HostileSpawnPredicate(), SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(EntityType.VINDICATOR, new HostileSpawnPredicate(), SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(EntityType.VEX, new HostileSpawnPredicate(), SpawnPlacementRegisterEvent.Operation.REPLACE);

            e.register(EntityType.GUARDIAN, new HostileSpawnPredicate(), SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(EntityType.PHANTOM, new HostileSpawnPredicate(), SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(EntityType.BLAZE, new HostileSpawnPredicate(),SpawnPlacementRegisterEvent.Operation.REPLACE);

            e.register(EntityType.SLIME, new SlimeSpawnPredicate(),SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }
}

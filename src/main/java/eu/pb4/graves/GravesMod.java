package eu.pb4.graves;

import eu.pb4.graves.registry.*;
import eu.pb4.graves.compat.GomlCompat;
import eu.pb4.graves.compat.InventorioCompat;
import eu.pb4.graves.compat.TrinketsCompat;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.other.Commands;
import eu.pb4.graves.other.VanillaInventoryMask;
import eu.pb4.polymer.api.block.PolymerBlockUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class GravesMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Universal Graves");
    public static ModContainer CONTAINER = FabricLoader.getInstance().getModContainer("universal-graves").get();
    public static String VERSION = CONTAINER.getMetadata().getVersion().getFriendlyString();

    public static final List<Runnable> DO_ON_NEXT_TICK = new ArrayList<>();

    @Override
    public void onInitialize() {
        this.crabboardDetection();
        FabricLoader loader = FabricLoader.getInstance();
        GenericModInfo.build(CONTAINER);

        Registry.register(Registry.ITEM, new Identifier("universal_graves", "grave_compass"), GraveCompassItem.INSTANCE);
        Registry.register(Registry.ITEM, new Identifier("universal_graves", "visual_grave"), VisualGraveBlockItem.INSTANCE);
        Registry.register(Registry.ITEM, new Identifier("universal_graves", "icon"), IconItem.INSTANCE);
        Registry.register(Registry.BLOCK, new Identifier("universal_graves", "grave"), GraveBlock.INSTANCE);
        Registry.register(Registry.BLOCK, new Identifier("universal_graves", "visual_grave"), VisualGraveBlock.INSTANCE);
        GraveBlockEntity.BLOCK_ENTITY_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, "universal_graves:grave", FabricBlockEntityTypeBuilder.create(GraveBlockEntity::new, GraveBlock.INSTANCE).build(null));
        VisualGraveBlockEntity.BLOCK_ENTITY_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, "universal_graves:visual_grave", FabricBlockEntityTypeBuilder.create(VisualGraveBlockEntity::new, VisualGraveBlock.INSTANCE).build(null));
        Commands.register();
        PolymerBlockUtils.registerBlockEntity(GraveBlockEntity.BLOCK_ENTITY_TYPE);
        PolymerBlockUtils.registerBlockEntity(VisualGraveBlockEntity.BLOCK_ENTITY_TYPE);

        GraveNetworking.initialize();
        new GraveGameRules();

        GravesApi.registerInventoryMask(new Identifier("vanilla"), VanillaInventoryMask.INSTANCE);

        if (loader.isModLoaded("goml")) {
            GomlCompat.register();
        }
        if (loader.isModLoaded("inventorio")) {
            InventorioCompat.register();
        }
        if (loader.isModLoaded("trinkets")) {
            TrinketsCompat.register();
        }

        ServerLifecycleEvents.SERVER_STARTING.register((server) -> ConfigManager.loadConfig());
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.crabboardDetection();
        });

        ServerWorldEvents.LOAD.register(((server, world) -> {
            if (world == server.getOverworld()) {
                GraveManager.INSTANCE = (GraveManager) world.getPersistentStateManager()
                        .getOrCreate((nbt) -> GraveManager.fromNbt(nbt, server), GraveManager::new, "universal-graves");
            }
        }));


        ServerTickEvents.END_SERVER_TICK.register(s -> {
            GraveManager.INSTANCE.tick(s);

            var copied = new ArrayList<>(DO_ON_NEXT_TICK);
            DO_ON_NEXT_TICK.clear();
            for (var c : copied) {
                c.run();
            }
        });
    }

    private void crabboardDetection() {
        if (FabricLoader.getInstance().isModLoaded("cardboard")) {
            LOGGER.error("");
            LOGGER.error("Cardboard detected! This mod doesn't work with it!");
            LOGGER.error("You won't get any support as long as it's present!");
            LOGGER.error("");
            LOGGER.error("Read more: https://gist.github.com/Patbox/e44844294c358b614d347d369b0fc3bf");
            LOGGER.error("");
        }
    }
}

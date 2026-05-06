package org.orsa.octorace;

import com.mojang.logging.LogUtils;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.orsa.octorace.block.JumpPadBlock;
import org.orsa.octorace.command.OctoraceCommand;
import org.orsa.octorace.config.RaceConfig;
import org.orsa.octorace.event.PlayerMovementListener;
import org.orsa.octorace.game.RaceManager;
import org.slf4j.Logger;
import net.minecraft.resources.Identifier;

public class Octorace implements ModInitializer {
    public static final String MOD_ID = "octorace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceKey<Block> JUMP_PAD_BLOCK_KEY = ResourceKey.create(
            BuiltInRegistries.BLOCK.key(),
            Identifier.fromNamespaceAndPath(MOD_ID, "jump_pad")
    );

    public static final Block JUMP_PAD_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            JUMP_PAD_BLOCK_KEY,
            new JumpPadBlock(BlockBehaviour.Properties.of()
                    .setId(JUMP_PAD_BLOCK_KEY)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
            )
    );

    public static final ResourceKey<Item> JUMP_PAD_ITEM_KEY = ResourceKey.create(
            BuiltInRegistries.ITEM.key(),
            Identifier.fromNamespaceAndPath(MOD_ID, "jump_pad")
    );

    public static final Item JUMP_PAD_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            JUMP_PAD_ITEM_KEY,
            new PolymerBlockItem(
                    JUMP_PAD_BLOCK,
                    new Item.Properties().setId(JUMP_PAD_ITEM_KEY).useBlockDescriptionPrefix()
            )
    );

    public static RaceManager RACE_MANAGER;

    @Override
    public void onInitialize() {
        LOGGER.info("Octorace initializing...");

        AutoConfig.register(RaceConfig.class, GsonConfigSerializer::new);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            RaceConfig config = AutoConfig.getConfigHolder(RaceConfig.class).getConfig();
            RACE_MANAGER = new RaceManager(server, config);
            LOGGER.info("Octorace loaded {} checkpoints from config.", config.getCheckpointCount());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (RACE_MANAGER != null) {
                RACE_MANAGER.getConfig().save();
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (RACE_MANAGER != null) {
                RACE_MANAGER.tick();
            }
        });

        PlayerMovementListener.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            OctoraceCommand.register(dispatcher);
        });

        LOGGER.info("Octorace initialized.");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

package org.orsa.octorace;

import com.mojang.logging.LogUtils;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.orsa.octorace.block.JumpPadBlock;
import org.orsa.octorace.block.SpeedPadBlock;
import org.orsa.octorace.command.OctoraceCommand;
import org.orsa.octorace.config.RaceConfig;
import org.orsa.octorace.event.PlayerMovementListener;
import org.orsa.octorace.game.RaceManager;
import org.slf4j.Logger;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;

public class Octorace implements ModInitializer {
    public static final String MOD_ID = "octorace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static RaceManager RACE_MANAGER;

    public static MinecraftServer SERVER;

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.addModAssets(MOD_ID);
        PolymerResourcePackUtils.markAsRequired();

        JumpPadBlock.register();
        SpeedPadBlock.register();
        AutoConfig.register(RaceConfig.class, GsonConfigSerializer::new);

        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);

        PlayerMovementListener.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            OctoraceCommand.register(dispatcher);
        });

        LOGGER.info("Octorace initialized.");
    }

    private void onServerStarting(MinecraftServer server) {
        SERVER = server;

        RaceConfig config = AutoConfig.getConfigHolder(RaceConfig.class).getConfig();
        RACE_MANAGER = new RaceManager(server, config);
        LOGGER.info("Octorace loaded {} checkpoints from config.", config.getCheckpointCount());
    }

    private void onServerStopping(MinecraftServer server) {
        if (RACE_MANAGER != null) {
            RACE_MANAGER.getConfig().save();
        }
    }

    private void onEndServerTick(MinecraftServer server) {
        if (RACE_MANAGER != null) {
            RACE_MANAGER.tick();
        }

        JumpPadBlock.endOfTick();
        SpeedPadBlock.endOfTick();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

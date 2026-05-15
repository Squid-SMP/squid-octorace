package org.orsa.octorace;

import com.mojang.logging.LogUtils;
import de.maxhenkel.admiral.MinecraftAdmiral;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.orsa.octorace.command.CustomArgumentTypes;
import org.orsa.octorace.block.BoostPadBlock;
import org.orsa.octorace.block.JumpPadBlock;
import org.orsa.octorace.block.SpeedPadBlock;
import org.orsa.octorace.command.octorace.*;
import org.orsa.octorace.item.OctoraceTrident;
import org.orsa.octorace.item.UnmoveableItem;
import org.orsa.octorace.item.WandItem;
import org.orsa.octorace.config.RaceConfig;
import org.orsa.octorace.event.PlayerEventListener;
import org.orsa.octorace.game.PartyManager;
import org.orsa.octorace.game.RaceManager;
import org.slf4j.Logger;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;

import java.util.List;

public class Octorace implements ModInitializer {
    public static final String MOD_ID = "octorace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String PLAYER_PERM = "octorace.player";
    public static final String ADMIN_PERM = "octorace.admin";

    public static RaceManager RACE_MANAGER;
    public static PartyManager PARTY_MANAGER;

    public static MinecraftServer SERVER;

    public static WandItem WAND_ITEM;

    public static JumpPadBlock JUMP_PAD_BLOCK;
    public static BoostPadBlock BOOST_PAD_BLOCK;
    public static SpeedPadBlock SPEED_PAD_BLOCK;

    public static List<UnmoveableItem> unmoveableItems;

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.addModAssets(MOD_ID);
        PolymerResourcePackUtils.markAsRequired();

        WAND_ITEM = WandItem.register();

        JUMP_PAD_BLOCK = JumpPadBlock.register();
        BOOST_PAD_BLOCK = BoostPadBlock.register();
        SPEED_PAD_BLOCK = SpeedPadBlock.register();

        AutoConfig.register(RaceConfig.class, GsonConfigSerializer::new);

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);

        PlayerEventListener.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            MinecraftAdmiral.builder(dispatcher, registryAccess)
                    .addArgumentTypes(CustomArgumentTypes::register)
                    .addCommandClasses(StartCommand.class)
                    .addCommandClasses(PartyCommand.class)
                    .addCommandClasses(WandCommand.class)
                    .addCommandClasses(CheckpointCommand.class)
                    .addCommandClasses(SetStartCommand.class)
                    .addCommandClasses(SetLobbyCommand.class)
                    .build();
        });

        LOGGER.info("Octorace initialized.");
    }

    private void onServerStarted(MinecraftServer server) {
        SERVER = server;

        RaceConfig config = AutoConfig.getConfigHolder(RaceConfig.class).getConfig();
        RACE_MANAGER = new RaceManager(config);
        PARTY_MANAGER = new PartyManager();
        LOGGER.info("Octorace loaded {} checkpoints from config.", config.getCheckpointCount());

        OctoraceTrident.init();

        unmoveableItems = List.of(OctoraceTrident.unmoveable);
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

        JUMP_PAD_BLOCK.tick();
        BOOST_PAD_BLOCK.tick();
        SPEED_PAD_BLOCK.tick();

        OctoraceTrident.tick();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void playSoundFor(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        var level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.MASTER, volume, pitch);
    }

}

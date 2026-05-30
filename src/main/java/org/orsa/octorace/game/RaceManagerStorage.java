package org.orsa.octorace.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.orsa.octorace.Octorace.SERVER;

public class RaceManagerStorage extends SavedData {
    public List<TimeTrialsData> timeTrialsEntries;
    public List<UUID> playersInRace;

    public RaceManagerStorage() {
        timeTrialsEntries = new ArrayList<>();
        playersInRace = new ArrayList<>();
    }

    private static final Codec<TimeTrialsData> TIMETRIALS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("uuid").forGetter(entry -> entry.uuid),
        Codec.DOUBLE.fieldOf("timeMs").forGetter(entry -> entry.timeMs),
        Codec.STRING.fieldOf("displayName").forGetter(entry -> entry.displayName)
    ).apply(instance, TimeTrialsData::new));

    private static final Codec<List<TimeTrialsData>> ENTRIES_LIST_CODEC = TIMETRIALS_CODEC.listOf();
    private static final Codec<List<UUID>> PLAYERS_LIST_CODEC = UUIDUtil.CODEC.listOf();

    private static final Codec<RaceManagerStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ENTRIES_LIST_CODEC.optionalFieldOf("timeTrialsEntries", List.of()).forGetter(s -> s.timeTrialsEntries),
        PLAYERS_LIST_CODEC.optionalFieldOf("playersInRace", List.of()).forGetter(s -> s.playersInRace)
    ).apply(instance, RaceManagerStorage::fromCodec));

    private static RaceManagerStorage fromCodec(List<TimeTrialsData> entries, List<UUID> players) {
        var storage = new RaceManagerStorage();
        storage.timeTrialsEntries = new ArrayList<>(entries);
        storage.playersInRace = new ArrayList<>(players);
        return storage;
    }

    public static final SavedDataType<RaceManagerStorage> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("octorace", "octorace_storage"),
        RaceManagerStorage::new,
        CODEC,
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    public static RaceManagerStorage get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void addTimeTrialsEntry(RaceParticipant participant, double time, String displayName) {
        var alreadyHas = false;
        var uuid = participant.uuid;
        var player = participant.player;

        boolean isWorldRecord = false;
        var fastestTime = timeTrialsEntries.stream().mapToDouble(entry -> entry.timeMs).min();
        if (fastestTime.isPresent()) {
            isWorldRecord = time < fastestTime.getAsDouble();
        }

        for (var entry : timeTrialsEntries) {
            if (entry.uuid.equals(uuid)) {
                alreadyHas = true;

                if (time < entry.timeMs) {
                    entry.timeMs = time;

                    if (!isWorldRecord) {
                        var message = Component.literal("New personal best!").withStyle(ChatFormatting.GREEN);
                        player.sendSystemMessage(message);
                    }
                }

                break;
            }
        }

        if (!alreadyHas) {
            var data = new TimeTrialsData(uuid, time, displayName);
            timeTrialsEntries.add(data);
        }

        if (isWorldRecord) {
            var message = Component.literal(displayName).withStyle(ChatFormatting.BOLD);
            var beaten = Component.literal(" has beaten the Octorace world record with a time of ").withStyle(ChatFormatting.GREEN);
            var timee = Component.literal(String.format("%.2fs", time / 1000.0)).withStyle(ChatFormatting.GOLD);
            var exclamation = Component.literal("!").withStyle(ChatFormatting.GREEN);
            message.append(beaten).append(timee).append(exclamation);

            for (var serverPlayer : SERVER.getPlayerList().getPlayers()) {
                serverPlayer.sendSystemMessage(message);
            }
        }

        setDirty();
    }

    private void checkForWorldRecord(RaceParticipant participant, double time, String displayName) {
        var fastestEntry = timeTrialsEntries.stream().min(Comparator.comparingDouble(entry -> entry.timeMs));
    }
}
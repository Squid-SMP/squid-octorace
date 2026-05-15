package org.orsa.octorace.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        "octorace_storage",
        RaceManagerStorage::new,
        CODEC,
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    public static RaceManagerStorage get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void addTimeTrialsEntry(UUID uuid, double time, String displayName) {
        var alreadyHas = false;

        for (var entry : timeTrialsEntries) {
            if (entry.uuid.equals(uuid)) {
                alreadyHas = true;

                if (entry.timeMs < time) {
                    entry.timeMs = time;
                }

                break;
            }
        }

        if (!alreadyHas) {
            var data = new TimeTrialsData(uuid, time, displayName);
            timeTrialsEntries.add(data);
        }

        setDirty();
    }
}
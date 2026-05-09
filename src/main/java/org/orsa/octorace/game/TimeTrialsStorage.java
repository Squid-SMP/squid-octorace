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

public class TimeTrialsStorage extends SavedData {
    public List<TimeTrialsData> entries;

    public TimeTrialsStorage() {
        entries = new ArrayList<>();
    }

    private TimeTrialsStorage(List<TimeTrialsData> entries) {
        this.entries = new ArrayList<>(entries);
    }

    private static final Codec<TimeTrialsData> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("uuid").forGetter(entry -> entry.uuid),
        Codec.DOUBLE.fieldOf("timeMs").forGetter(entry -> entry.timeMs),
        Codec.STRING.fieldOf("displayName").forGetter(entry -> entry.displayName)
    ).apply(instance, TimeTrialsData::new));

    private static final Codec<TimeTrialsStorage> CODEC = ENTRY_CODEC.listOf()
            .xmap(TimeTrialsStorage::new, storage -> storage.entries);

    public static final SavedDataType<TimeTrialsStorage> TYPE = new SavedDataType<>(
        "time_trials",
        TimeTrialsStorage::new,
        CODEC,
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    public static TimeTrialsStorage get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void addEntry(UUID uuid, double time, String displayName) {
        var alreadyHas = false;

        for (var entry : entries) {
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
            entries.add(data);
        }

        setDirty();
    }
}
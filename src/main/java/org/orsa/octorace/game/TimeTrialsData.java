package org.orsa.octorace.game;

import java.util.UUID;

public class TimeTrialsData {
    public UUID uuid;
    public double timeMs;
    public String displayName;

    public TimeTrialsData(UUID uuid, double timeMs, String displayName) {
        this.uuid = uuid;
        this.timeMs = timeMs;
        this.displayName = displayName;
    }
}

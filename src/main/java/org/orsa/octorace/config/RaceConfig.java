package org.orsa.octorace.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@Config(name = "octorace")
public class RaceConfig implements ConfigData {

	private String dimensionId = null;
	private SerializableVec3 startPosition = null;
	private float startYaw = 0f;

	private List<Checkpoint> checkpoints = new ArrayList<>();

	public String getDimensionId() {
		return dimensionId;
	}

	public void setDimensionId(String dimensionId) {
		this.dimensionId = dimensionId;
	}

	public Identifier getDimensionIdentifier() {
		return dimensionId == null ? null : Identifier.parse(dimensionId);
	}

	public Vec3 getStartPosition() {
		return startPosition == null ? null : new Vec3(startPosition.x, startPosition.y, startPosition.z);
	}

	public void setStartPosition(Vec3 pos, float yaw) {
		this.startPosition = new SerializableVec3(pos.x, pos.y, pos.z);
		this.startYaw = yaw;
	}

	public float getStartYaw() { return startYaw; }

	public List<Checkpoint> getCheckpoints() {
		return checkpoints;
	}

	public int getCheckpointCount() {
		return checkpoints.size();
	}

	public void addCheckpoint(Checkpoint cp) {
		checkpoints.add(cp);
		save();
	}

	public boolean removeCheckpoint(int index) {
		if (index < 0 || index >= checkpoints.size()) {
			return false;
		}

		checkpoints.remove(index);
		save();

		return true;
	}

	public void clearCheckpoints() {
		checkpoints.clear();
		save();
	}

	public boolean isReady() {
		return startPosition != null && !checkpoints.isEmpty() && dimensionId != null;
	}

	public void save() {
		AutoConfig.getConfigHolder(RaceConfig.class).save();
	}

	public static class SerializableVec3 {
		public double x, y, z;

		public SerializableVec3(double x, double y, double z) {
			this.x = x; this.y = y; this.z = z;
		}
	}
}

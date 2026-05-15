package org.orsa.octorace.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Config(name = "octorace")
public class RaceConfig implements ConfigData {

	private String dimensionId = null;
	private SerializableVec3 startPosition = null;
	private float startYaw = 0f;

	private SerializableVec3 lobbyPosition = null;
	private float lobbyYaw = 0f;

	public List<Checkpoint> checkpoints = new ArrayList<>();

	public String getDimensionId() {
		return dimensionId;
	}

	public void setDimensionId(String dimensionId) {
		this.dimensionId = dimensionId;
	}

	public Identifier getDimensionIdentifier() {
		return dimensionId == null ? null : Identifier.parse(dimensionId);
	}

	public ResourceKey<Level> getDimensionKey() {
		return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, getDimensionIdentifier());
	}

	public void setStartPosition(Vec3 pos, float yaw) {
		this.startPosition = new SerializableVec3(pos.x, pos.y, pos.z);
		this.startYaw = yaw;
	}

	public Vec3 getStartPosition() {
		return startPosition == null ? null : new Vec3(startPosition.x, startPosition.y, startPosition.z);
	}

	public float getStartYaw() { return startYaw; }

	public void setLobbyPosition(Vec3 pos, float yaw) {
		this.lobbyPosition = new SerializableVec3(pos.x, pos.y, pos.z);
		this.lobbyYaw = yaw;
	}

	public Vec3 getLobbyPosition() {
		return lobbyPosition == null ? null : new Vec3(lobbyPosition.x, lobbyPosition.y, lobbyPosition.z);
	}

	public float getLobbyYaw() { return lobbyYaw; }

	public int getCheckpointCount() {
		return checkpoints.size();
	}

	public Optional<Checkpoint> getCheckpoint(int id) {
		if (id < 0 || id >= checkpoints.size()) {
			return Optional.empty();
		}
		return Optional.of(checkpoints.get(id));
	}

	public Checkpoint addCheckpoint(BlockPos pos1, BlockPos pos2, String name) {
		double minX = Math.min(pos1.getX(), pos2.getX());
		double minY = Math.min(pos1.getY(), pos2.getY());
		double minZ = Math.min(pos1.getZ(), pos2.getZ());
		double maxX = Math.max(pos1.getX(), pos2.getX()) + 1.0;
		double maxY = Math.max(pos1.getY(), pos2.getY()) + 1.0;
		double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1.0;

		Checkpoint checkpoint = new Checkpoint(minX, minY, minZ, maxX, maxY, maxZ, name);
		checkpoints.add(checkpoint);

		save();

		return checkpoint;
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

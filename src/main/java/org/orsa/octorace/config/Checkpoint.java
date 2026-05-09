package org.orsa.octorace.config;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Checkpoint {
	public double minX, minY, minZ;
	public double maxX, maxY, maxZ;

	public String name;
	public String dimensionId;

	public Checkpoint(double minX, double minY, double minZ,
	                  double maxX, double maxY, double maxZ,
	                  String name, String dimensionId) {
		this.minX = Math.min(minX, maxX);
		this.minY = Math.min(minY, maxY);
		this.minZ = Math.min(minZ, maxZ);
		this.maxX = Math.max(minX, maxX);
		this.maxY = Math.max(minY, maxY);
		this.maxZ = Math.max(minZ, maxZ);
		this.name = name;
		this.dimensionId = dimensionId;
	}

	public AABB toBox() {
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public Vec3 center() {
		return new Vec3((minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0);
	}

	public String describe() {
		return String.format("%s (%.1f,%.1f,%.1f → %.1f,%.1f,%.1f)",
				name == null ? "checkpoint" : name,
				minX, minY, minZ, maxX, maxY, maxZ);
	}

	public Boolean playerIntersects(ServerPlayer player) {
		var checkpointBox = toBox();
		var playerBox = player.getBoundingBox();
		return checkpointBox.intersects(playerBox);
	}
}

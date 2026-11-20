package com.verrysilly.deathcoords.client;

import com.verrysilly.deathcoords.DeathCoordinatesLogger;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DeathBeacons {
	private static final String FILE_NAME = "death-beacons.log";

	private static final double CLICK_HALF_WIDTH = 0.4;
	private static final double MAX_CLICK_DISTANCE = 128.0;

	private static final List<DeathRecord> ACTIVE = new ArrayList<>();
	private static boolean attackWasDown;

	private DeathBeacons() {
	}

	public static void load() {
		ACTIVE.clear();
		Path file = file();
		if (!Files.exists(file)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(file)) {
				DeathRecord.fromLogLine(line).ifPresent(ACTIVE::add);
			}
		} catch (IOException e) {
			DeathCoordinatesLogger.LOGGER.warn("Could not read {}", FILE_NAME, e);
		}
	}

	private static void save() {
		try {
			List<String> lines = new ArrayList<>();
			for (DeathRecord record : ACTIVE) {
				lines.add(record.toLogLine());
			}
			Files.write(file(), lines);
		} catch (IOException e) {
			DeathCoordinatesLogger.LOGGER.warn("Could not write {}", FILE_NAME, e);
		}
	}

	private static Path file() {
		return FabricLoader.getInstance().getGameDir().resolve(FILE_NAME);
	}

	public static void add(MinecraftClient client, DeathRecord death) {
		ACTIVE.add(new DeathRecord(death.time(), worldKey(client), death.dimension(), death.x(), death.y(), death.z()));
		save();
	}

	static List<DeathRecord> inCurrentWorld(MinecraftClient client) {
		if (ACTIVE.isEmpty() || client.world == null) {
			return List.of();
		}
		String key = worldKey(client);
		String dimension = client.world.getRegistryKey().getValue().toString();
		List<DeathRecord> result = new ArrayList<>();
		for (DeathRecord record : ACTIVE) {
			if (record.where().equals(key) && record.dimension().equals(dimension)) {
				result.add(record);
			}
		}
		return result;
	}

	public static int clearCurrentWorld(MinecraftClient client) {
		String key = worldKey(client);
		int before = ACTIVE.size();
		ACTIVE.removeIf(record -> record.where().equals(key));
		save();
		return before - ACTIVE.size();
	}

	static int beamBottom(ClientWorld world, DeathRecord beacon) {
		return Math.max(beacon.y(), world.getBottomY());
	}

	static int beamTop(ClientWorld world) {
		return world.getBottomY() + world.getHeight();
	}

	private static String worldKey(MinecraftClient client) {
		ServerInfo server = client.getCurrentServerEntry();
		if (server != null) {
			return server.address;
		}
		if (client.getServer() != null) {
			String name = client.getServer().getSaveProperties().getLevelName();
			return "singleplayer:" + name.replace('|', '/');
		}
		return "unknown";
	}

	public static void onTick(MinecraftClient client) {
		KeyBinding attack = client.options.attackKey;
		boolean down = attack.isPressed();
		boolean freshPress = down && !attackWasDown;
		attackWasDown = down;

		ClientPlayerEntity player = client.player;
		if (!freshPress || player == null || client.world == null || client.currentScreen != null) {
			return;
		}

		DeathRecord hit = beaconUnderCrosshair(client, player);
		if (hit == null) {
			return;
		}

		ACTIVE.remove(hit);
		save();

		while (attack.wasPressed()) {
			
		}
		attack.setPressed(false);

		player.sendMessage(Text.literal("[Death] Beacon disabled").formatted(Formatting.GRAY), true);
	}

	private static DeathRecord beaconUnderCrosshair(MinecraftClient client, ClientPlayerEntity player) {
		ClientWorld world = client.world;
		Vec3d origin = player.getEyePos();
		Vec3d direction = player.getRotationVec(1.0F);

		DeathRecord best = null;
		double bestDistance = MAX_CLICK_DISTANCE;
		for (DeathRecord beacon : inCurrentWorld(client)) {
			double cx = beacon.x() + 0.5;
			double cz = beacon.z() + 0.5;
			Box box = new Box(
				cx - CLICK_HALF_WIDTH, beamBottom(world, beacon), cz - CLICK_HALF_WIDTH,
				cx + CLICK_HALF_WIDTH, beamTop(world), cz + CLICK_HALF_WIDTH
			);
			double distance = rayHitDistance(origin, direction, box);
			if (distance >= 0 && distance < bestDistance) {
				bestDistance = distance;
				best = beacon;
			}
		}
		if (best == null) {
			return null;
		}

		Vec3d hitPoint = origin.add(direction.multiply(bestDistance));
		HitResult blocked = world.raycast(new RaycastContext(
			origin, hitPoint, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player
		));
		return blocked.getType() == HitResult.Type.MISS ? best : null;
	}

	private static double rayHitDistance(Vec3d origin, Vec3d dir, Box box) {
		double near = Double.NEGATIVE_INFINITY;
		double far = Double.POSITIVE_INFINITY;

		double[] o = {origin.x, origin.y, origin.z};
		double[] d = {dir.x, dir.y, dir.z};
		double[] min = {box.minX, box.minY, box.minZ};
		double[] max = {box.maxX, box.maxY, box.maxZ};

		for (int i = 0; i < 3; i++) {
			if (Math.abs(d[i]) < 1.0E-9) {
				if (o[i] < min[i] || o[i] > max[i]) {
					return -1;
				}
				continue;
			}
			double t1 = (min[i] - o[i]) / d[i];
			double t2 = (max[i] - o[i]) / d[i];
			near = Math.max(near, Math.min(t1, t2));
			far = Math.min(far, Math.max(t1, t2));
			if (near > far) {
				return -1;
			}
		}
		return near >= 0 ? near : -1;
	}
}

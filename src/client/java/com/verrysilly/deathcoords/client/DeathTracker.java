package com.verrysilly.deathcoords.client;

import com.verrysilly.deathcoords.DeathCoordinatesLogger;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

public final class DeathTracker {
	private static final String LOG_FILE_NAME = "death-coordinates.log";

	private static WeakReference<ClientPlayerEntity> lastLoggedPlayer = new WeakReference<>(null);
	private static DeathRecord lastDeath;

	private DeathTracker() {
	}

	public static void onTick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) {
			return;
		}

		if (player.isDead() && lastLoggedPlayer.get() != player) {
			lastLoggedPlayer = new WeakReference<>(player);
			recordDeath(client, player);
		}
	}

	private static void recordDeath(MinecraftClient client, ClientPlayerEntity player) {
		BlockPos pos = player.getBlockPos();
		String dimension = client.world.getRegistryKey().getValue().toString();

		ServerInfo server = client.getCurrentServerEntry();
		String where = server != null ? server.address : "singleplayer";

		DeathRecord record = new DeathRecord(LocalDateTime.now(), where, dimension, pos.getX(), pos.getY(), pos.getZ());
		lastDeath = record;
		DeathBeacons.add(client, record);

		player.sendMessage(format(record, false), false);
		appendToLog(record);
	}

	public static Text lastDeathText() {
		DeathRecord record = lastDeath != null ? lastDeath : readLastDeathFromLog();
		if (record == null) {
			return Text.literal("[Death] No death recorded yet.").formatted(Formatting.GRAY);
		}
		return format(record, true);
	}

	private static Text format(DeathRecord record, boolean withTime) {
		MutableText text = Text.literal("[Death] ").formatted(Formatting.RED);
		if (withTime) {
			text.append(Text.literal(record.timeLabel() + "  ").formatted(Formatting.GRAY));
		}
		text.append(Text.literal(String.format("X: %d  Y: %d  Z: %d", record.x(), record.y(), record.z())).formatted(Formatting.GOLD));
		text.append(Text.literal("  (" + record.dimensionLabel() + ")").formatted(Formatting.AQUA));

		String hint = record.conversionHint();
		if (!hint.isEmpty()) {
			text.append(Text.literal("  " + hint).formatted(Formatting.GRAY));
		}
		return text;
	}

	private static Path logFile() {
		return FabricLoader.getInstance().getGameDir().resolve(LOG_FILE_NAME);
	}

	private static void appendToLog(DeathRecord record) {
		try {
			Files.writeString(
				logFile(),
				record.toLogLine() + System.lineSeparator(),
				StandardOpenOption.CREATE,
				StandardOpenOption.APPEND
			);
		} catch (IOException e) {
			DeathCoordinatesLogger.LOGGER.warn("Could not write to {}", LOG_FILE_NAME, e);
		}
	}

	private static DeathRecord readLastDeathFromLog() {
		Path file = logFile();
		if (!Files.exists(file)) {
			return null;
		}
		try {
			List<String> lines = Files.readAllLines(file);
			for (int i = lines.size() - 1; i >= 0; i--) {
				var parsed = DeathRecord.fromLogLine(lines.get(i));
				if (parsed.isPresent()) {
					return parsed.get();
				}
			}
		} catch (IOException e) {
			DeathCoordinatesLogger.LOGGER.warn("Could not read {}", LOG_FILE_NAME, e);
		}
		return null;
	}
}

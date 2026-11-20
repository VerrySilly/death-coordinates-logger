package com.verrysilly.deathcoords.client;

import com.verrysilly.deathcoords.DeathCoordinatesLogger;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

public final class DeathTracker {
	private static final String LOG_FILE_NAME = "death-coordinates.log";

	private static WeakReference<LocalPlayer> lastLoggedPlayer = new WeakReference<>(null);
	private static DeathRecord lastDeath;

	private DeathTracker() {
	}

	public static void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return;
		}

		if (player.isDeadOrDying() && lastLoggedPlayer.get() != player) {
			lastLoggedPlayer = new WeakReference<>(player);
			recordDeath(client, player);
		}
	}

	private static void recordDeath(Minecraft client, LocalPlayer player) {
		BlockPos pos = player.blockPosition();
		String dimension = client.level.dimension().identifier().toString();

		ServerData server = client.getCurrentServer();
		String where = server != null ? server.ip : "singleplayer";

		DeathRecord record = new DeathRecord(LocalDateTime.now(), where, dimension, pos.getX(), pos.getY(), pos.getZ());
		lastDeath = record;
		DeathBeacons.add(client, record);

		player.sendSystemMessage(format(record, false));
		appendToLog(record);
	}

	public static Component lastDeathText() {
		DeathRecord record = lastDeath != null ? lastDeath : readLastDeathFromLog();
		if (record == null) {
			return Component.literal("[Death] No death recorded yet.").withStyle(ChatFormatting.GRAY);
		}
		return format(record, true);
	}

	private static Component format(DeathRecord record, boolean withTime) {
		MutableComponent text = Component.literal("[Death] ").withStyle(ChatFormatting.RED);
		if (withTime) {
			text.append(Component.literal(record.timeLabel() + "  ").withStyle(ChatFormatting.GRAY));
		}
		text.append(Component.literal(String.format("X: %d  Y: %d  Z: %d", record.x(), record.y(), record.z())).withStyle(ChatFormatting.GOLD));
		text.append(Component.literal("  (" + record.dimensionLabel() + ")").withStyle(ChatFormatting.AQUA));

		String hint = record.conversionHint();
		if (!hint.isEmpty()) {
			text.append(Component.literal("  " + hint).withStyle(ChatFormatting.GRAY));
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

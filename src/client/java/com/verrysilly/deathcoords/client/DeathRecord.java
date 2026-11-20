package com.verrysilly.deathcoords.client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public record DeathRecord(LocalDateTime time, String where, String dimension, int x, int y, int z) {
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String SEPARATOR = " | ";

	public String toLogLine() {
		return String.join(SEPARATOR, FORMAT.format(time), where, dimension, x + "," + y + "," + z);
	}

	public static Optional<DeathRecord> fromLogLine(String line) {
		try {
			String[] parts = line.split(" \\| ");
			if (parts.length != 4) {
				return Optional.empty();
			}
			String[] coords = parts[3].trim().split(",");
			if (coords.length != 3) {
				return Optional.empty();
			}
			return Optional.of(new DeathRecord(
				LocalDateTime.parse(parts[0].trim(), FORMAT),
				parts[1].trim(),
				parts[2].trim(),
				Integer.parseInt(coords[0].trim()),
				Integer.parseInt(coords[1].trim()),
				Integer.parseInt(coords[2].trim())
			));
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	public String timeLabel() {
		return FORMAT.format(time);
	}

	public String dimensionLabel() {
		return switch (dimension) {
			case "minecraft:overworld" -> "Overworld";
			case "minecraft:the_nether" -> "Nether";
			case "minecraft:the_end" -> "The End";
			default -> dimension;
		};
	}

	public String conversionHint() {
		return switch (dimension) {
			case "minecraft:overworld" -> "Nether: X " + Math.floorDiv(x, 8) + ", Z " + Math.floorDiv(z, 8);
			case "minecraft:the_nether" -> "Overworld: X " + (x * 8) + ", Z " + (z * 8);
			default -> "";
		};
	}
}

package com.verrysilly.deathcoords.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DeathCoordinatesLoggerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DeathBeacons.load();
		DeathBeaconRenderer.register();

		ClientTickEvents.START_CLIENT_TICK.register(DeathBeacons::onTick);
		ClientTickEvents.END_CLIENT_TICK.register(DeathTracker::onTick);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("lastdeath").executes(context -> {
				context.getSource().sendFeedback(DeathTracker.lastDeathText());
				return 1;
			}));

			dispatcher.register(ClientCommandManager.literal("clearbeacons").executes(context -> {
				int removed = DeathBeacons.clearCurrentWorld(MinecraftClient.getInstance());
				context.getSource().sendFeedback(
					Text.literal("[Death] Removed " + removed + " beacon(s)").formatted(Formatting.GRAY)
				);
				return 1;
			}));
		});
	}
}

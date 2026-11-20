package com.verrysilly.deathcoords.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class DeathCoordinatesLoggerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DeathBeacons.load();
		DeathBeaconRenderer.register();

		ClientTickEvents.START_CLIENT_TICK.register(DeathBeacons::onTick);
		ClientTickEvents.END_CLIENT_TICK.register(DeathTracker::onTick);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommands.literal("lastdeath").executes(context -> {
				context.getSource().sendFeedback(DeathTracker.lastDeathText());
				return 1;
			}));

			dispatcher.register(ClientCommands.literal("clearbeacons").executes(context -> {
				int removed = DeathBeacons.clearCurrentWorld(Minecraft.getInstance());
				context.getSource().sendFeedback(
					Component.literal("[Death] Removed " + removed + " beacon(s)").withStyle(ChatFormatting.GRAY)
				);
				return 1;
			}));
		});
	}
}

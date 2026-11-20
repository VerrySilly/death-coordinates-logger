package com.verrysilly.deathcoords.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class DeathBeaconRenderer {
	private static final int RED = 0xFFFF0000;
	private static final Identifier BEAM_TEXTURE = Identifier.ofVanilla("textures/entity/beacon_beam.png");

	private DeathBeaconRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_ENTITIES.register(DeathBeaconRenderer::render);
	}

	private static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = client.world;
		if (world == null) {
			return;
		}

		List<DeathRecord> beacons = DeathBeacons.inCurrentWorld(client);
		if (beacons.isEmpty()) {
			return;
		}

		MatrixStack matrices = context.matrices();
		OrderedRenderCommandQueue queue = context.commandQueue();
		Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
		float tickProgress = client.getRenderTickCounter().getTickProgress(false);
		long time = world.getTime();

		for (DeathRecord beacon : beacons) {
			int bottom = DeathBeacons.beamBottom(world, beacon);
			int height = DeathBeacons.beamTop(world) - bottom;

			matrices.push();
			matrices.translate(beacon.x() - camera.x, bottom - camera.y, beacon.z() - camera.z);
			BeaconBlockEntityRenderer.renderBeam(matrices, queue, BEAM_TEXTURE, tickProgress, (float) time, 0, height, RED, 0.2F, 0.25F);
			matrices.pop();
		}
	}
}
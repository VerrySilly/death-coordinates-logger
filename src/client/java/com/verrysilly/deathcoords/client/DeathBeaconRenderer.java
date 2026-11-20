package com.verrysilly.deathcoords.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class DeathBeaconRenderer {
	private static final int RED = 0xFFFF0000;
	private static final Identifier BEAM_TEXTURE = BeaconRenderer.BEAM_LOCATION;

	private DeathBeaconRenderer() {
	}

	public static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(DeathBeaconRenderer::render);
	}

	private static void render(LevelRenderContext context) {
		Minecraft client = Minecraft.getInstance();
		ClientLevel world = client.level;
		if (world == null) {
			return;
		}

		List<DeathRecord> beacons = DeathBeacons.inCurrentWorld(client);
		if (beacons.isEmpty()) {
			return;
		}

		PoseStack matrices = context.poseStack();
		SubmitNodeCollector queue = context.submitNodeCollector();
		Vec3 camera = context.levelState().cameraRenderState.pos;
		float tickProgress = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		long time = world.getGameTime();

		for (DeathRecord beacon : beacons) {
			int bottom = DeathBeacons.beamBottom(world, beacon);
			int height = DeathBeacons.beamTop(world) - bottom;

			matrices.pushPose();
			matrices.translate(beacon.x() - camera.x, bottom - camera.y, beacon.z() - camera.z);
			BeaconRenderer.submitBeaconBeam(matrices, queue, BEAM_TEXTURE, tickProgress, (float) time, 0, height, RED, 0.2F, 0.25F);
			matrices.popPose();
		}
	}
}
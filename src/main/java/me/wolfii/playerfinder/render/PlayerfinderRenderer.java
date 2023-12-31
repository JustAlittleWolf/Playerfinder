package me.wolfii.playerfinder.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.wolfii.playerfinder.Config;
import me.wolfii.playerfinder.PlayerFinder;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

public class PlayerfinderRenderer {

    /*
     * Since I wasn't able to reverse engineer how to draw lines on top of the world
     * a portion of this code comes from https://github.com/AdvancedXRay/XRay-Fabric
     */
    public static void render(WorldRenderContext context) {
        if (PlayerFinder.rendermode == Rendermode.NONE) return;

        ArrayList<PlayerEntity> playersToRender = EntityHelper.getPlayerEntitiesToHighlight();
        if (playersToRender.isEmpty()) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float tickDelta = context.tickDelta();
        Camera camera = context.camera();
        Vec3d cameraPos = camera.getPos();
        for (PlayerEntity playerEntity : playersToRender) {
            switch (PlayerFinder.rendermode) {
                case BOTH:
                    drawPlayerHitbox(playerEntity, tickDelta, buffer);
                    drawPlayerTracer(playerEntity, camera, tickDelta, buffer);
                    break;
                case TRACERS:
                    drawPlayerTracer(playerEntity, camera, tickDelta, buffer);
                    break;
                case HITBOXES:
                    drawPlayerHitbox(playerEntity, tickDelta, buffer);
                    break;
            }
        }

        VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);

        vertexBuffer.bind();
        vertexBuffer.upload(buffer.end());
        VertexBuffer.unbind();

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        MatrixStack poseStack = RenderSystem.getModelViewStack();
        poseStack.push();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        context.projectionMatrix().lookAt(cameraPos.toVector3f(), cameraPos.toVector3f().add(camera.getHorizontalPlane()), camera.getVerticalPlane());
        vertexBuffer.bind();
        vertexBuffer.draw(poseStack.peek().getPositionMatrix(), new Matrix4f(context.projectionMatrix()), RenderSystem.getShader());
        VertexBuffer.unbind();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);

        poseStack.pop();
        RenderSystem.applyModelViewMatrix();
    }

    private static void drawPlayerHitbox(PlayerEntity playerEntity, float tickDelta, BufferBuilder buffer) {
        Box box = EntityHelper.getOffsetBoundingBox(playerEntity, tickDelta);

        PlayerfinderRenderer.drawBox(buffer, box, 1.0f, 1.0f, 1.0f, 1.0f);
        if (Config.renderEyeHeight) drawEyeHeight(buffer, box, playerEntity);
        if (Config.renderFacing) drawFacing(buffer, box, playerEntity, tickDelta);
    }

    private static void drawEyeHeight(BufferBuilder buffer, Box box, PlayerEntity playerEntity) {
        PlayerfinderRenderer.drawBox(buffer, box.minX, playerEntity.getStandingEyeHeight() - 0.01f + box.minY, box.minZ, box.maxX, playerEntity.getStandingEyeHeight() + 0.01f + box.minY, box.maxZ, 1.0f, 0.0f, 0.0f, 1.0f);
    }

    private static void drawFacing(BufferBuilder buffer, Box box, PlayerEntity playerEntity, float tickDelta) {
        Vec3d vec3d = playerEntity.getRotationVec(tickDelta);
        double entityCenterX = box.minX + (box.maxX - box.minX) / 2.0d;
        double entityCenterZ = box.minZ + (box.maxZ - box.minZ) / 2.0d;
        buffer.vertex(entityCenterX, playerEntity.getStandingEyeHeight() + box.minY, entityCenterZ).color(0, 0, 255, 255).normal((float) vec3d.x, (float) vec3d.y, (float) vec3d.z).next();
        buffer.vertex(vec3d.x * 2.0 + entityCenterX, playerEntity.getStandingEyeHeight() + vec3d.y * 2.0 + box.minY, vec3d.z * 2.0 + entityCenterZ).color(0, 0, 255, 255).normal((float) vec3d.x, (float) vec3d.y, (float) vec3d.z).next();
    }

    private static void drawPlayerTracer(PlayerEntity playerEntity, Camera camera, float tickDelta, BufferBuilder buffer) {
        Box box = EntityHelper.getOffsetBoundingBox(playerEntity, tickDelta);
        double entityCenterX = box.minX + (box.maxX - box.minX) / 2.0d;
        double entityCenterY = box.minY + (box.maxY - box.minY) / 2.0d;
        double entityCenterZ = box.minZ + (box.maxZ - box.minZ) / 2.0d;
        Vec3d cameraPos = new Vec3d(camera.getPos().x, camera.getPos().y, camera.getPos().z);
        Vector3f horizontalPlane = camera.getHorizontalPlane();
        if (horizontalPlane.x == 0) horizontalPlane.x = 0.0000001f;
        if (horizontalPlane.y == 0) horizontalPlane.y = 0.0000001f;
        if (horizontalPlane.z == 0) horizontalPlane.z = 0.0000001f;
        double scaleFactor = Math.min((Math.abs(1f / camera.getHorizontalPlane().x) + Math.abs(1f / camera.getHorizontalPlane().y) + Math.abs(1f / camera.getHorizontalPlane().z)) / 3f, 100f);
        cameraPos = cameraPos.add(camera.getHorizontalPlane().x * scaleFactor, camera.getHorizontalPlane().y * scaleFactor, camera.getHorizontalPlane().z * scaleFactor);
        buffer.vertex(entityCenterX, entityCenterY, entityCenterZ).color(255, 255, 255, 255).next();
        buffer.vertex(cameraPos.x, cameraPos.y, cameraPos.z).color(255, 255, 255, 255).next();
    }

    private static void drawBox(VertexConsumer vertexConsumer, Box box, float red, float green, float blue, float alpha) {
        PlayerfinderRenderer.drawBox(vertexConsumer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha, red, green, blue);
    }

    private static void drawBox(VertexConsumer vertexConsumer, double x1, double y1, double z1, double x2, double y2, double z2, float red, float green, float blue, float alpha) {
        PlayerfinderRenderer.drawBox(vertexConsumer, x1, y1, z1, x2, y2, z2, red, green, blue, alpha, red, green, blue);
    }

    private static void drawBox(VertexConsumer vertexConsumer, double x1, double y1, double z1, double x2, double y2, double z2, float red, float green, float blue, float alpha, float xAxisRed, float yAxisGreen, float zAxisBlue) {
        vertexConsumer.vertex(x1, y1, z1).color(red, yAxisGreen, zAxisBlue, alpha).normal(1.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(x2, y1, z1).color(red, yAxisGreen, zAxisBlue, alpha).normal(1.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(x1, y1, z1).color(xAxisRed, green, zAxisBlue, alpha).normal(0.0f, 1.0f, 0.0f).next();
        vertexConsumer.vertex(x1, y2, z1).color(xAxisRed, green, zAxisBlue, alpha).normal(0.0f, 1.0f, 0.0f).next();
        vertexConsumer.vertex(x1, y1, z1).color(xAxisRed, yAxisGreen, blue, alpha).normal(0.0f, 0.0f, 1.0f).next();
        vertexConsumer.vertex(x1, y1, z2).color(xAxisRed, yAxisGreen, blue, alpha).normal(0.0f, 0.0f, 1.0f).next();
        vertexConsumer.vertex(x2, y1, z1).color(red, green, blue, alpha).normal(0.0f, 1.0f, 0.0f).next();
        vertexConsumer.vertex(x2, y2, z1).color(red, green, blue, alpha).normal(0.0f, 1.0f, 0.0f).next();
        vertexConsumer.vertex(x2, y2, z1).color(red, green, blue, alpha).normal(-1.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(x1, y2, z1).color(red, green, blue, alpha).normal(-1.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(x1, y2, z1).color(red, green, blue, alpha).normal(0.0f, 0.0f, 1.0f).next();
        vertexConsumer.vertex(x1, y2, z2).color(red, green, blue, alpha).normal(0.0f, 0.0f, 1.0f).next();
        vertexConsumer.vertex(x1, y2, z2).color(red, green, blue, alpha).normal(0.0f, -1.0f, 0.0f).next();
        vertexConsumer.vertex(x1, y1, z2).color(red, green, blue, alpha).normal(0.0f, -1.0f, 0.0f).next();
        vertexConsumer.vertex(x1, y1, z2).color(red, green, blue, alpha).normal(1.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(x2, y1, z2).color(red, green, blue, alpha).normal(1.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(x2, y1, z2).color(red, green, blue, alpha).normal(0.0f, 0.0f, -1.0f).next();
        vertexConsumer.vertex(x2, y1, z1).color(red, green, blue, alpha).normal(0.0f, 0.0f, -1.0f).next();
        vertexConsumer.vertex(x1, y2, z2).color(red, green, blue, alpha).normal(1.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(x2, y2, z2).color(red, green, blue, alpha).normal(1.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(x2, y1, z2).color(red, green, blue, alpha).normal(0.0f, 1.0f, 0.0f).next();
        vertexConsumer.vertex(x2, y2, z2).color(red, green, blue, alpha).normal(0.0f, 1.0f, 0.0f).next();
        vertexConsumer.vertex(x2, y2, z1).color(red, green, blue, alpha).normal(0.0f, 0.0f, 1.0f).next();
        vertexConsumer.vertex(x2, y2, z2).color(red, green, blue, alpha).normal(0.0f, 0.0f, 1.0f).next();
    }
}

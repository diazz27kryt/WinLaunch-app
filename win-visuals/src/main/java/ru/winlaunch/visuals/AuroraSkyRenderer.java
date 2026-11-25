package ru.winlaunch.visuals;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.joml.Matrix4f;

import java.util.Random;

/** Lightweight procedural night sky: dark dome, stars, nebula haze and animated aurora ribbons. */
public final class AuroraSkyRenderer {
    private static final int LONGITUDES = 48;
    private static final int LATITUDES = 18;
    private static final float RADIUS = 96.0f;
    private static MappableRingBuffer vertexBuffer;

    private AuroraSkyRenderer() {}

    public static void render(Matrix4f viewMatrix, float tickDelta) {
        VisualsConfig cfg = VisualsConfig.get();
        float intensity = Math.max(0.1f, Math.min(2.0f, cfg.skyIntensity));
        drawDome(viewMatrix, intensity);
        if (cfg.skyNebula) drawNebula(viewMatrix, intensity);
        if (cfg.skyFog) drawHorizonFog(viewMatrix, intensity);
        if (cfg.skyStars) drawStars(viewMatrix, intensity);
        if (cfg.auroraSky) drawAurora(viewMatrix, tickDelta, intensity);

    }

    private static void drawDome(Matrix4f matrix, float intensity) {
        BufferBuilder b = begin();
        for (int lat = 0; lat < LATITUDES; lat++) {
            float p0 = (float) Math.PI * lat / LATITUDES;
            float p1 = (float) Math.PI * (lat + 1) / LATITUDES;
            for (int lon = 0; lon < LONGITUDES; lon++) {
                float t0 = (float) (Math.PI * 2.0 * lon / LONGITUDES);
                float t1 = (float) (Math.PI * 2.0 * (lon + 1) / LONGITUDES);
                vertex(b, matrix, p0, t0, shade(p0, intensity));
                vertex(b, matrix, p1, t0, shade(p1, intensity));
                vertex(b, matrix, p1, t1, shade(p1, intensity));
                vertex(b, matrix, p0, t1, shade(p0, intensity));
            }
        }
        finish(b);
    }

    private static int shade(float polar, float intensity) {
        float horizon = (float) Math.pow(Math.abs(Math.cos(polar)), 1.7);
        int r = (int) ((5 + 7 * horizon) * intensity);
        int g = (int) ((7 + 5 * horizon) * intensity);
        int bl = (int) ((19 + 22 * horizon) * intensity);
        return argb(255, clamp(r), clamp(g), clamp(bl));
    }

    private static void drawNebula(Matrix4f matrix, float intensity) {
        BufferBuilder b = begin();
        for (int band = 0; band < 3; band++) {
            float center = 0.72f + band * 0.24f;
            for (int i = 0; i < 48; i++) {
                float a0 = (float) (Math.PI * 2.0 * i / 48.0);
                float a1 = (float) (Math.PI * 2.0 * (i + 1) / 48.0);
                float wave0 = center + 0.08f * (float) Math.sin(a0 * 3 + band);
                float wave1 = center + 0.08f * (float) Math.sin(a1 * 3 + band);
                int c0 = band == 1 ? argb(22, 90, 65, 160) : argb(18, 45, 90, 160);
                int c1 = scaleAlpha(c0, intensity);
                vertex(b, matrix, wave0 - 0.12f, a0, c1);
                vertex(b, matrix, wave0 + 0.12f, a0, c1);
                vertex(b, matrix, wave1 + 0.12f, a1, c1);
                vertex(b, matrix, wave1 - 0.12f, a1, c1);
            }
        }
        finish(b);
    }


    private static void drawHorizonFog(Matrix4f matrix, float intensity) {
        BufferBuilder b = begin();
        for (int i = 0; i < 64; i++) {
            float a0 = (float) (Math.PI * 2.0 * i / 64.0);
            float a1 = (float) (Math.PI * 2.0 * (i + 1) / 64.0);
            float wave0 = 1.43f + 0.025f * (float) Math.sin(a0 * 4.0);
            float wave1 = 1.43f + 0.025f * (float) Math.sin(a1 * 4.0);
            int color = scaleAlpha(argb(30, 55, 65, 120), intensity);
            vertex(b, matrix, wave0 - 0.09f, a0, scaleAlpha(color, 0.25f));
            vertex(b, matrix, wave0 + 0.09f, a0, color);
            vertex(b, matrix, wave1 + 0.09f, a1, color);
            vertex(b, matrix, wave1 - 0.09f, a1, scaleAlpha(color, 0.25f));
        }
        finish(b);
    }

    private static void drawStars(Matrix4f matrix, float intensity) {
        BufferBuilder b = begin();
        Random random = new Random(84121L);
        for (int i = 0; i < 210; i++) {
            float polar = 0.12f + random.nextFloat() * 1.43f;
            float angle = random.nextFloat() * (float) (Math.PI * 2.0);
            float size = 0.0018f + random.nextFloat() * 0.0028f;
            float alpha = (0.32f + random.nextFloat() * 0.55f) * Math.min(1f, intensity);
            int color = argb((int) (alpha * 255), 190 + random.nextInt(66), 205 + random.nextInt(50), 225 + random.nextInt(31));
            quadOnSphere(b, matrix, polar, angle, size, color);
        }
        finish(b);
    }

    private static void drawAurora(Matrix4f matrix, float tickDelta, float intensity) {
        BufferBuilder b = begin();
        float time = (System.currentTimeMillis() % 120000L) / 120000.0f * (float) (Math.PI * 2.0);
        for (int ribbon = 0; ribbon < 4; ribbon++) {
            float base = 0.52f + ribbon * 0.105f;
            for (int i = 0; i < 64; i++) {
                float a0 = (float) (Math.PI * 2.0 * i / 64.0);
                float a1 = (float) (Math.PI * 2.0 * (i + 1) / 64.0);
                float wave0 = (float) Math.sin(a0 * (2.0 + ribbon * 0.25) + time + ribbon) * 0.09f;
                float wave1 = (float) Math.sin(a1 * (2.0 + ribbon * 0.25) + time + ribbon) * 0.09f;
                float p00 = base + wave0;
                float p01 = p00 + 0.075f + 0.025f * (float) Math.sin(time * 1.4f + a0 * 4);
                float p10 = base + wave1;
                float p11 = p10 + 0.075f + 0.025f * (float) Math.sin(time * 1.4f + a1 * 4);
                int color = ribbon % 2 == 0 ? argb((int)(62 * intensity), 35, 240, 190) : argb((int)(48 * intensity), 100, 105, 255);
                color = scaleAlpha(color, intensity);
                vertex(b, matrix, p00, a0, color);
                vertex(b, matrix, p01, a0, scaleAlpha(color, 0.25f));
                vertex(b, matrix, p11, a1, scaleAlpha(color, 0.25f));
                vertex(b, matrix, p10, a1, color);
            }
        }
        finish(b);
    }

    private static void quadOnSphere(BufferBuilder b, Matrix4f matrix, float polar, float angle, float size, int color) {
        vertex(b, matrix, polar - size, angle - size, color);
        vertex(b, matrix, polar + size, angle - size, color);
        vertex(b, matrix, polar + size, angle + size, color);
        vertex(b, matrix, polar - size, angle + size, color);
    }

    private static void vertex(BufferBuilder b, Matrix4f matrix, float polar, float angle, int color) {
        float sin = (float) Math.sin(polar);
        float x = RADIUS * sin * (float) Math.cos(angle);
        float y = RADIUS * (float) Math.cos(polar);
        float z = RADIUS * sin * (float) Math.sin(angle);
        b.addVertex(matrix, x, y, z).setColor(color);
    }

    private static BufferBuilder begin() {
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    private static void finish(BufferBuilder buffer) {
        MeshData mesh = buffer.buildOrThrow();
        MeshData.DrawState drawState = mesh.drawState();
        VertexFormat format = drawState.format();
        int bytes = drawState.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < bytes) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = new MappableRingBuffer(
                () -> "Win Visuals aurora vertices",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                Math.max(bytes, 4096)
            );
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mapped = encoder.mapBuffer(
                vertexBuffer.currentBuffer().slice(0, mesh.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(mesh.vertexBuffer(), mapped.data());
        }

        GpuBuffer indices = format.uploadImmediateIndexBuffer(mesh.indexBuffer());
        Vector4f colorModulator = new Vector4f(1f, 1f, 1f, 1f);
        Vector3f modelOffset = new Vector3f();
        Matrix4f textureMatrix = new Matrix4f();
        var transforms = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrix(), colorModulator, modelOffset, textureMatrix
        );

        Minecraft client = Minecraft.getInstance();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Win Visuals aurora",
                client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(RenderPipelines.DEBUG_QUADS);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, vertexBuffer.currentBuffer());
            pass.setIndexBuffer(indices, drawState.indexType());
            pass.drawIndexed(0, 0, drawState.indexCount(), 1);
        }
        mesh.close();
        vertexBuffer.rotate();
    }

    private static int scaleAlpha(int color, float scale) {
        int alpha = (color >>> 24) & 255;
        return (Math.min(255, Math.max(0, (int) (alpha * scale))) << 24) | (color & 0x00FFFFFF);
    }

    private static int argb(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}

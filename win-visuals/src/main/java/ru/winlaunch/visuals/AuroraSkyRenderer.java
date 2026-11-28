package ru.winlaunch.visuals;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Win Visuals night sky. A single sphere mesh is drawn with the {@code winvisuals:core/sky}
 * shader, which procedurally renders the dark gradient, nebulae, stars and the animated aurora.
 * Nothing is rebuilt per frame: the animation is driven by a time value passed to the shader.
 */
public final class AuroraSkyRenderer {
    private static final int LONGITUDES = 40;
    private static final int LATITUDES = 20;
    private static final float RADIUS = 100.0f;
    private static final long START_NANOS = System.nanoTime();

    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("winvisuals", "pipeline/sky"))
        .withVertexShader(Identifier.fromNamespaceAndPath("winvisuals", "core/sky"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("winvisuals", "core/sky"))
        .withoutBlend()
        .withDepthWrite(false)
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
        .build();

    // Reused every frame so the sky does not create garbage 60+ times a second.
    private static final Vector4f PARAMS = new Vector4f();
    private static final Vector3f EXTRA = new Vector3f();
    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final Matrix4f TEXTURE = new Matrix4f();

    private static GpuBuffer domeBuffer;
    private static int domeIndexCount;

    private AuroraSkyRenderer() {}

    /** Draws the sky. {@code pose} is the sky pose stack matrix, {@code starAngle} the vanilla star rotation. */
    public static void render(Matrix4f pose, float starAngle) {
        VisualsConfig cfg = VisualsConfig.get();
        ensureDome();

        float intensity = Math.max(0.1f, Math.min(2.0f, cfg.skyIntensity));
        float time = (System.nanoTime() - START_NANOS) / 1.0e9f;
        // The shader cost is almost all in the nebula and aurora, so the governor drops those first.
        boolean aurora = cfg.auroraSky && PerfGovernor.auroraAllowed();
        boolean nebula = cfg.skyNebula && PerfGovernor.nebulaAllowed();
        PARAMS.set(time, aurora ? intensity : 0f, nebula ? intensity : 0f, cfg.skyStars ? 1f : 0f);
        EXTRA.set(cfg.skyFog ? 1f : 0f, starAngle, 1f);

        MODEL_VIEW.set(RenderSystem.getModelViewMatrix()).mul(pose);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
            .writeTransform(MODEL_VIEW, PARAMS, EXTRA, TEXTURE.identity());

        Minecraft client = Minecraft.getInstance();
        GpuTextureView color = client.getMainRenderTarget().getColorTextureView();
        GpuTextureView depth = client.getMainRenderTarget().getDepthTextureView();
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuffer = indices.getBuffer(domeIndexCount);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Win Visuals sky", color, OptionalInt.empty(), depth, OptionalDouble.empty())) {
            pass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, domeBuffer);
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.drawIndexed(0, 0, domeIndexCount, 1);
        }
    }

    private static void ensureDome() {
        if (domeBuffer != null && !domeBuffer.isClosed()) return;
        int vertexSize = DefaultVertexFormat.POSITION.getVertexSize();
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(LONGITUDES * LATITUDES * 4 * vertexSize)) {
            BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            for (int lat = 0; lat < LATITUDES; lat++) {
                float p0 = (float) Math.PI * lat / LATITUDES;
                float p1 = (float) Math.PI * (lat + 1) / LATITUDES;
                for (int lon = 0; lon < LONGITUDES; lon++) {
                    float t0 = (float) (Math.PI * 2.0 * lon / LONGITUDES);
                    float t1 = (float) (Math.PI * 2.0 * (lon + 1) / LONGITUDES);
                    vertex(b, p0, t0);
                    vertex(b, p1, t0);
                    vertex(b, p1, t1);
                    vertex(b, p0, t1);
                }
            }
            try (MeshData mesh = b.buildOrThrow()) {
                domeIndexCount = mesh.drawState().indexCount();
                domeBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "Win Visuals sky dome", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, mesh.vertexBuffer());
            }
        }
    }

    private static void vertex(BufferBuilder b, float polar, float angle) {
        float sin = (float) Math.sin(polar);
        b.addVertex(RADIUS * sin * (float) Math.cos(angle), RADIUS * (float) Math.cos(polar),
            RADIUS * sin * (float) Math.sin(angle));
    }
}

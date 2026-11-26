#!/usr/bin/env bash
S=$1
show() { f=$(find "$S" -path "*$1" | head -n 1); echo "=============== $1 lines $2-$3"; sed -n "$2,$3p" "$f"; }
show net/minecraft/client/renderer/RenderPipelines.java 40 130
show net/minecraft/client/renderer/RenderPipelines.java 585 640
show net/minecraft/client/renderer/RenderPipelines.java 640 700
f=$(find "$S" -path "*com/mojang/blaze3d/pipeline/RenderPipeline.java" | head -1); echo "=============== RenderPipeline.java"; grep -nE 'public .*\(|class |interface ' "$f" | head -90
f=$(find "$S" -path "*com/mojang/blaze3d/pipeline/BlendFunction.java" | head -1); echo "=============== BlendFunction"; grep -nE 'public static final' "$f"
f=$(find "$S" -path "*com/mojang/blaze3d/platform/DepthTestFunction.java" | head -1); echo "=============== DepthTestFunction"; cat "$f" | head -30
f=$(find "$S" -path "*com/mojang/blaze3d/platform/BlendFactor.java" | head -1); echo "=============== BlendFactor"; grep -nE '^\s+[A-Z_]+[,;(]' "$f" | head -20
f=$(find "$S" -path "*com/mojang/blaze3d/systems/GpuDevice.java" | head -1); echo "=============== GpuDevice"; grep -nE 'createBuffer|createRenderPipeline|precompile' "$f"
f=$(find "$S" -path "*com/mojang/blaze3d/buffers/GpuBuffer.java" | head -1); echo "=============== GpuBuffer"; grep -nE 'USAGE|public ' "$f" | head -30
f=$(find "$S" -path "*com/mojang/blaze3d/vertex/MeshData.java" | head -1); echo "=============== MeshData"; grep -nE 'public ' "$f" | head -30
f=$(find "$S" -path "*net/minecraft/client/renderer/state/SkyRenderState.java" | head -1); echo "=============== SkyRenderState"; cat "$f" | sed -n 1,60p
show net/minecraft/client/renderer/LevelRenderer.java 1195 1230

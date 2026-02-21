package com.windwoif.balance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector3f;

public class CubeRenderer {

    /**
     * Renders a single axis-aligned face (quad) defined by its two axes ranges and the fixed axis value.
     * The face is subdivided into 1x1 unit cells to achieve proper texture tiling.
     *
     * @param poseStack    the pose stack (should be positioned at the cube center)
     * @param consumer     the vertex consumer
     * @param sprite       the texture sprite to use
     * @param normal       the normal vector of the face (assumed to be ±1 on one axis, 0 on others)
     * @param fixedAxis    the fixed axis ('x', 'y', or 'z') and its coordinate
     * @param uAxis        the axis used for U texture coordinate ('x' or 'z')
     * @param vAxis        the axis used for V texture coordinate ('x', 'y', or 'z')
     * @param uMin         minimum bound of the face along U axis (local coordinates)
     * @param uMax         maximum bound of the face along U axis
     * @param vMin         minimum bound of the face along V axis
     * @param vMax         maximum bound of the face along V axis
     * @param color        ARGB color
     * @param light        packed light value (block + sky)
     */
    public static void renderFace(PoseStack poseStack, VertexConsumer consumer,
                                  TextureAtlasSprite sprite,
                                  Vector3f normal,
                                  char fixedAxis, float fixedValue,
                                  char uAxis, char vAxis,
                                  float uMin, float uMax, float vMin, float vMax,
                                  int color, int light) {
        var matrix = poseStack.last().pose();

        int uStartCell = (int) Math.floor(uMin);
        int uEndCell = (int) Math.ceil(uMax);
        int vStartCell = (int) Math.floor(vMin);
        int vEndCell = (int) Math.ceil(vMax);

        for (int iu = uStartCell; iu < uEndCell; iu++) {
            for (int iv = vStartCell; iv < vEndCell; iv++) {
                float cellUMin = iu;
                float cellUMax = iu + 1;
                float cellVMin = iv;
                float cellVMax = iv + 1;

                float drawUMin = Math.max(cellUMin, uMin);
                float drawUMax = Math.min(cellUMax, uMax);
                float drawVMin = Math.max(cellVMin, vMin);
                float drawVMax = Math.min(cellVMax, vMax);

                if (drawUMax <= drawUMin || drawVMax <= drawVMin) continue;

                float texUMin = (drawUMin - cellUMin) / 1f;
                float texUMax = (drawUMax - cellUMin) / 1f;
                float texVMin = (drawVMin - cellVMin) / 1f;
                float texVMax = (drawVMax - cellVMin) / 1f;

                float u1 = sprite.getU(texUMin * 16);
                float u2 = sprite.getU(texUMax * 16);
                float v1 = sprite.getV(texVMin * 16);
                float v2 = sprite.getV(texVMax * 16);

                float[] pos = new float[3];
                for (int corner = 0; corner < 4; corner++) {
                    float u = (corner == 0 || corner == 3) ? drawUMin : drawUMax;
                    float v = (corner == 0 || corner == 1) ? drawVMin : drawVMax;

                    if (uAxis == 'x') pos[0] = u; else if (uAxis == 'y') pos[1] = u; else if (uAxis == 'z') pos[2] = u;
                    if (vAxis == 'x') pos[0] = v; else if (vAxis == 'y') pos[1] = v; else if (vAxis == 'z') pos[2] = v;

                    if (fixedAxis == 'x') pos[0] = fixedValue;
                    else if (fixedAxis == 'y') pos[1] = fixedValue;
                    else if (fixedAxis == 'z') pos[2] = fixedValue;

                    float texU = (corner == 0 || corner == 3) ? u1 : u2;
                    float texV = (corner == 0 || corner == 1) ? v1 : v2;

                    consumer.vertex(matrix, pos[0], pos[1], pos[2])
                            .color(color)
                            .uv(texU, texV)
                            .uv2(light)
                            .normal(normal.x(), normal.y(), normal.z())
                            .endVertex();
                }
            }
        }
    }

    public static void renderLiquidCube(PoseStack poseStack, VertexConsumer consumer,
                                        TextureAtlasSprite sprite,
                                        float minX, float minY, float minZ,
                                        float maxX, float maxY, float maxZ,
                                        int color, int light) {
        float skew = 1/16384f;

        renderFace(poseStack, consumer, sprite, new Vector3f(0, -1, 0), 'y', minY - skew, 'x', 'z', minX, maxX, minZ, maxZ, color, light);
        renderFace(poseStack, consumer, sprite, new Vector3f(0, 1, 0), 'y', maxY + skew, 'x', 'z', minX, maxX, minZ, maxZ, color, light);

        renderFace(poseStack, consumer, sprite, new Vector3f(0, 0, -1), 'z', minZ - skew, 'x', 'y', minX, maxX, minY, maxY, color, light);
        renderFace(poseStack, consumer, sprite, new Vector3f(0, 0, 1), 'z', maxZ + skew, 'x', 'y', minX, maxX, minY, maxY, color, light);

        renderFace(poseStack, consumer, sprite, new Vector3f(-1, 0, 0), 'x', minX - skew, 'z', 'y', minZ, maxZ, minY, maxY, color, light);
        renderFace(poseStack, consumer, sprite, new Vector3f(1, 0, 0), 'x', maxX + skew, 'z', 'y', minZ, maxZ, minY, maxY, color, light);
    }
}
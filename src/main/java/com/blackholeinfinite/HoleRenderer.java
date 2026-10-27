package com.blackholeinfinite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;

/**
 * Renderizacao independente da logica: o raio desenhado e limitado pela distancia de renderizacao
 * (so afeta o visual; a massa/alcance logicos continuam crescendo).
 */
public class HoleRenderer {
    private final Sphere sphere = new Sphere();

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || ClientProxy.holes.isEmpty()) return;
        Entity v = mc.getRenderViewEntity();
        if (v == null) return;
        float pt = e.partialTicks;
        double vx = v.lastTickPosX + (v.posX - v.lastTickPosX) * pt;
        double vy = v.lastTickPosY + (v.posY - v.lastTickPosY) * pt;
        double vz = v.lastTickPosZ + (v.posZ - v.lastTickPosZ) * pt;
        int dim = mc.theWorld.provider.getDimensionId();
        double cap = Math.max(64.0, mc.gameSettings.renderDistanceChunks * 16.0) * 0.9;
        float time = (float) (mc.theWorld.getTotalWorldTime() % 1000000L) + pt;
        boolean fog = GL11.glIsEnabled(GL11.GL_FOG);

        for (ClientProxy.ClientHole h : ClientProxy.holes.values()) {
            if (h.dim != dim) continue;
            double rx = h.x - vx, ry = h.y - vy, rz = h.z - vz;
            double dist = Math.sqrt(rx * rx + ry * ry + rz * rz);
            double rr = Math.min(h.visual, cap * 0.5);
            if (dist - rr * 4 > cap * 1.3) continue;
            if (Double.isNaN(rr) || rr <= 0) continue;
            draw(rx, ry, rz, (float) rr, time);
        }
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GL11.glLineWidth(1f);
        if (fog) GlStateManager.enableFog();
    }

    private void draw(double x, double y, double z, float r, float time) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableFog();

        // nucleo escuro (esfera 3D)
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.color(0f, 0f, 0f, 1f);
        sphere.setDrawStyle(GLU.GLU_FILL);
        sphere.setNormals(GLU.GLU_NONE);
        sphere.draw(r, 48, 24);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.depthMask(false);

        // disco de acrecao roxo (girando)
        GlStateManager.pushMatrix();
        GlStateManager.rotate(18f, 1f, 0f, 1f);
        GlStateManager.rotate(time * 3f, 0f, 1f, 0f);
        for (int k = 0; k < 3; k++) {
            float in = r * (1.15f + 0.4f * k), out = in + r * (0.55f - 0.1f * k);
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i <= 64; i++) {
                double a = i * (Math.PI * 2 / 64);
                float c = (float) Math.cos(a), s = (float) Math.sin(a);
                float br = 0.6f + 0.4f * (float) Math.sin(a * 3 + time * 0.15f + k);
                GL11.glColor4f(0.75f * br, 0.2f * br, 1f, 0.85f);
                GL11.glVertex3f(in * c, 0, in * s);
                GL11.glColor4f(0.4f * br, 0.05f, 0.8f, 0f);
                GL11.glVertex3f(out * c, 0, out * s);
            }
            GL11.glEnd();
        }
        GlStateManager.popMatrix();

        // aneis gravitacionais convergindo para o centro
        GL11.glLineWidth(2f);
        for (int k = 0; k < 4; k++) {
            float ph = ((time * 0.012f) + k / 4f) % 1f;
            float rad = r * (2f + (1f - ph) * 14f);
            float alpha = ph * 0.55f;
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glColor4f(0.7f, 0.25f, 1f, alpha);
            for (int i = 0; i < 64; i++) {
                double a = i * (Math.PI * 2 / 64);
                GL11.glVertex3f(rad * (float) Math.cos(a), 0, rad * (float) Math.sin(a));
            }
            GL11.glEnd();
        }
        GlStateManager.popMatrix();
    }
}

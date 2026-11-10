package com.neant.client;

import com.neant.Neant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Neant.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            VfxManager.tick();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || VfxManager.ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float partial = event.getPartialTick();
        VfxDraw draw = new VfxDraw(mc.renderBuffers().bufferSource(), event.getPoseStack().last().pose(),
                event.getCamera());
        for (Fx fx : VfxManager.ACTIVE) {
            fx.render(draw, fx.age + partial, partial);
        }
        draw.flush();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (VfxManager.ACTIVE.isEmpty()) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        float partial = (float) event.getPartialTick();
        float shake = 0.0F;
        for (Fx fx : VfxManager.ACTIVE) {
            shake += fx.shake(cam, fx.age + partial);
        }
        if (shake <= 0.001F) {
            return;
        }
        shake = Math.min(shake, 1.5F);
        Minecraft mc = Minecraft.getInstance();
        float time = (mc.player != null ? mc.player.tickCount : 0) + partial;
        event.setPitch(event.getPitch() + (float) (Math.sin(time * 1.9D) + 0.5D * Math.sin(time * 3.7D)) * shake * 1.6F);
        event.setYaw(event.getYaw() + (float) (Math.cos(time * 2.1D) + 0.5D * Math.sin(time * 4.3D)) * shake * 1.6F);
        event.setRoll(event.getRoll() + (float) Math.sin(time * 2.7D) * shake * 1.2F);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (VfxManager.ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        float partial = event.getPartialTick();

        float best = 0.0F;
        int bestColor = 0xFFFFFF;
        for (Fx fx : VfxManager.ACTIVE) {
            float t = fx.age + partial;
            float f = fx.flash(cam, t);
            if (f > best) {
                best = f;
                bestColor = fx.flashColor();
            }
            fx.overlay(g, w, h, t, cam);
        }
        if (best > 0.01F) {
            int alpha = (int) (Math.min(best, 0.92F) * 255.0F);
            g.fill(0, 0, w, h, (alpha << 24) | (bestColor & 0xFFFFFF));
        }
    }

    private ClientEvents() {}
}

package com.neant.client;

import com.neant.Neant;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Types de rendu des effets : lumiere additive (VFX) et fumee sombre (trou noir). */
public final class VfxRenderTypes extends RenderType {

    private static final ResourceLocation GLOW = tex("glow");
    private static final ResourceLocation LINE = tex("line");
    private static final ResourceLocation DISC = tex("disc");
    private static final ResourceLocation STAR = tex("star");

    private static final RenderStateShard.ShaderStateShard SHADER =
            new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);

    /** Fusion additive : la lumiere s'ajoute a ce qui est derriere (comme un vrai halo). */
    private static final RenderStateShard.TransparencyStateShard ADD = new RenderStateShard.TransparencyStateShard(
            "neant_additive",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    private static final RenderStateShard.TransparencyStateShard ALPHA = new RenderStateShard.TransparencyStateShard(
            "neant_alpha",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    public static final RenderType GLOW_ADD = make("glow_add", GLOW, ADD, true);
    public static final RenderType LINE_ADD = make("line_add", LINE, ADD, true);
    public static final RenderType STAR_ADD = make("star_add", STAR, ADD, true);
    public static final RenderType DISC_DARK = make("disc_dark", DISC, ALPHA, true);
    public static final RenderType GLOW_DARK = make("glow_dark", GLOW, ALPHA, true);

    /** Les memes, sans test de profondeur : utilises pour les halos tres proches de la camera. */
    public static final RenderType GLOW_ADD_NODEPTH = make("glow_add_nodepth", GLOW, ADD, false);

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(Neant.MODID, "textures/vfx/" + name + ".png");
    }

    private static RenderType make(String name, ResourceLocation texture,
                                   RenderStateShard.TransparencyStateShard transparency, boolean depthTest) {
        CompositeState state = CompositeState.builder()
                .setShaderState(SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, true, false))
                .setTransparencyState(transparency)
                .setCullState(NO_CULL)
                .setDepthTestState(depthTest ? LEQUAL_DEPTH_TEST : NO_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create(Neant.MODID + "_" + name, DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, 262144, false, true, state);
    }

    // Constructeur requis pour acceder aux champs proteges de RenderType ; jamais instancie.
    private VfxRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                           boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }
}

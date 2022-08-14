package cofh.thermal.innovation.client.renderer;

import cofh.core.entity.Knife;
import cofh.core.util.helpers.RenderHelper;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.innovation.entity.GrappleHook;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

public class GrappleHookRenderer extends EntityRenderer<GrappleHook> {

    protected static final ItemRenderer itemRenderer = RenderHelper.renderItem();

    public GrappleHookRenderer(EntityRendererProvider.Context ctx) {

        super(ctx);
    }

    @Override
    public void render(GrappleHook hook, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource buffer, int packedLight) {

        stack.pushPose();
        stack.mulPose(Vector3f.YP.rotationDegrees(MathHelper.interpolate(partialTicks, hook.yRotO, hook.yRot) - 90.0F));
        stack.mulPose(Vector3f.ZP.rotationDegrees(MathHelper.interpolate(partialTicks, hook.xRotO, hook.xRot)));
        stack.scale(1.25F, 1.25F, 1.25F);
        itemRenderer.renderStatic(hook.renderItem(), TransformType.GROUND, packedLight, OverlayTexture.NO_OVERLAY, stack, buffer, 0);
        stack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(GrappleHook p_114482_) {

        return InventoryMenu.BLOCK_ATLAS;
    }

}

package cofh.thermal.innovation.client.renderer;

import cofh.thermal.innovation.common.item.RFGrappleItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class RFGrappleBEWLR extends BlockEntityWithoutLevelRenderer {

    public static final RFGrappleBEWLR INSTANCE = new RFGrappleBEWLR(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());

    public RFGrappleBEWLR(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {

        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transform, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int overlayCoord) {

        if (stack.getItem() instanceof RFGrappleItem item) {
            item.render(stack, transform, poseStack, buffer, packedLight, overlayCoord);
        } else {
            super.renderByItem(stack, transform, poseStack, buffer, packedLight, overlayCoord);
        }
    }
    
}

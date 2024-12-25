package cofh.thermal.innovation.client.event;

import cofh.thermal.innovation.client.renderer.RFGrappleBEWLR;
import cofh.thermal.innovation.common.item.RFGrappleItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Queue;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_INNOVATION;

@Mod.EventBusSubscriber (value = Dist.CLIENT, modid = ID_THERMAL_INNOVATION)
public class TInoClientEvents {

    private TInoClientEvents() {

    }

    @SubscribeEvent
    public static void registerReloadListeners(final RegisterClientReloadListenersEvent event) {

        event.registerReloadListener(RFGrappleBEWLR.INSTANCE);
    }

    @SubscribeEvent
    public static void renderGrappleHooks(RenderLevelStageEvent event) {

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            PoseStack stack = event.getPoseStack();
            stack.pushPose();
            Vec3 pos = event.getCamera().getPosition();
            stack.translate(-pos.x, -pos.y, -pos.z);
            RFGrappleItem.renderHooks(stack, event.getLevelRenderer().renderBuffers.bufferSource(), event.getPartialTick());
            stack.popPose();
        };
    }

}

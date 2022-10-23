package cofh.thermal.innovation.event;

import cofh.thermal.innovation.client.model.FluidReservoirItemModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_INNOVATION;
import cofh.thermal.innovation.client.renderer.GrappleHookRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static cofh.thermal.innovation.init.TInoEntities.*;

@Mod.EventBusSubscriber (value = Dist.CLIENT, modid = ID_THERMAL_INNOVATION, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TInoClientSetupEvents {

    private TInoClientSetupEvents() {

    }

    @SubscribeEvent
    public static void registerModels(final RegisterGeometryLoaders event) {

        event.register("reservoir", new FluidReservoirItemModel.Loader());
    }

    @SubscribeEvent
    public void entityRendererSetup(final EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(GRAPPLE_HOOK.get(), GrappleHookRenderer::new);
    }

}

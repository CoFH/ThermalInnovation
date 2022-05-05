package cofh.thermal.innovation.event;

import cofh.thermal.innovation.client.model.FluidReservoirItemModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static cofh.lib.util.constants.Constants.ID_THERMAL;
import static cofh.lib.util.constants.Constants.ID_THERMAL_INNOVATION;

@Mod.EventBusSubscriber (value = Dist.CLIENT, modid = ID_THERMAL_INNOVATION, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TInoClientSetupEvents {

    private TInoClientSetupEvents() {

    }

    @SubscribeEvent
    public static void registerModels(final ModelRegistryEvent event) {

        ModelLoaderRegistry.registerLoader(new ResourceLocation(ID_THERMAL, "reservoir"), new FluidReservoirItemModel.Loader());
    }

}

package cofh.thermal.innovation.event;

import cofh.thermal.innovation.client.model.FluidReservoirItemModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_INNOVATION;

@Mod.EventBusSubscriber (value = Dist.CLIENT, modid = ID_THERMAL_INNOVATION, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TInoClientSetupEvents {

    private TInoClientSetupEvents() {

    }

    @SubscribeEvent
    public static void registerModels(final RegisterGeometryLoaders event) {

        event.register("reservoir", new FluidReservoirItemModel.Loader());
    }

}

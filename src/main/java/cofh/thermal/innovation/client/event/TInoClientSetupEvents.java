package cofh.thermal.innovation.client.event;

import cofh.thermal.innovation.client.model.FluidReservoirItemModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders;

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

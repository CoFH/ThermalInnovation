package cofh.thermal.innovation;

import cofh.thermal.innovation.config.TInoToolConfig;
import cofh.thermal.innovation.init.TInoBlocks;
import cofh.thermal.innovation.init.TInoItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static cofh.lib.util.constants.Constants.ID_THERMAL_INNOVATION;
import static cofh.thermal.core.ThermalCore.CONFIG_MANAGER;
import static cofh.thermal.lib.common.ThermalFlags.*;
import static cofh.thermal.lib.common.ThermalIDs.ID_CHARGE_BENCH;
import static cofh.thermal.lib.common.ThermalIDs.ID_DEVICE_POTION_DIFFUSER;

@Mod (ID_THERMAL_INNOVATION)
public class ThermalInnovation {

    public ThermalInnovation() {

        setFeatureFlags();

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        CONFIG_MANAGER.register(modEventBus)
                .addServerConfig(new TInoToolConfig());

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        TInoBlocks.register();
        TInoItems.register();
    }

    private void setFeatureFlags() {

        setFlag(FLAG_DIVING_ARMOR, true);
        setFlag(FLAG_HAZMAT_ARMOR, true);

        setFlag(FLAG_POTION_AUGMENTS, true);

        setFlag(FLAG_TOOL_COMPONENTS, true);

        setFlag(FLAG_BASIC_EXPLOSIVES, true);
        setFlag(FLAG_ELEMENTAL_EXPLOSIVES, true);

        setFlag(ID_DEVICE_POTION_DIFFUSER, true);
        setFlag(ID_CHARGE_BENCH, true);
    }

    // region INITIALIZATION
    private void commonSetup(final FMLCommonSetupEvent event) {

        event.enqueueWork(TInoItems::setup);
    }

    private void clientSetup(final FMLClientSetupEvent event) {

    }
    // endregion
}

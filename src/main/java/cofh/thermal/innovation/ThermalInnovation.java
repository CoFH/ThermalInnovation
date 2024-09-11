package cofh.thermal.innovation;

//import cofh.thermal.innovation.client.renderer.GrappleHookRenderer;

import cofh.lib.common.energy.EnergyContainerItemWrapper;
import cofh.lib.common.fluid.FluidContainerItemWrapper;
import cofh.thermal.innovation.common.config.TInoToolConfig;
import cofh.thermal.innovation.common.item.*;
import cofh.thermal.innovation.init.registries.TInoBlocks;
import cofh.thermal.innovation.init.registries.TInoEntities;
import cofh.thermal.innovation.init.registries.TInoItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import static cofh.lib.util.FlagManager.setFlag;
import static cofh.lib.util.constants.ModIds.ID_THERMAL_INNOVATION;
import static cofh.thermal.core.ThermalCore.CONFIG_MANAGER;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.lib.util.ThermalFlags.*;
import static cofh.thermal.lib.util.ThermalIDs.ID_CHARGE_BENCH;
import static cofh.thermal.lib.util.ThermalIDs.ID_DEVICE_POTION_DIFFUSER;

@Mod (ID_THERMAL_INNOVATION)
public class ThermalInnovation {

    public ThermalInnovation(ModContainer modContainer, IEventBus modEventBus) {

        setFeatureFlags();

        CONFIG_MANAGER.register(modEventBus)
                .addServerConfig(new TInoToolConfig());

        modEventBus.addListener(this::entityRendererSetup);
        modEventBus.addListener(this::capabilitySetup);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        TInoBlocks.register();
        TInoItems.register();
        TInoEntities.register();
    }

    private void setFeatureFlags() {

        setFlag(FLAG_DIVING_ARMOR, true);
        setFlag(FLAG_HAZMAT_ARMOR, true);

        setFlag(FLAG_AREA_AUGMENTS, true);
        setFlag(FLAG_POTION_AUGMENTS, true);

        setFlag(FLAG_TOOL_COMPONENTS, true);

        setFlag(FLAG_BASIC_EXPLOSIVES, true);
        setFlag(FLAG_ELEMENTAL_EXPLOSIVES, true);

        setFlag(ID_DEVICE_POTION_DIFFUSER, true);
        setFlag(ID_CHARGE_BENCH, true);
    }

    // region INITIALIZATION
    private void entityRendererSetup(final EntityRenderersEvent.RegisterRenderers event) {

        //event.registerEntityRenderer(GRAPPLE_HOOK.get(), GrappleHookRenderer::new);
    }

    private void capabilitySetup(RegisterCapabilitiesEvent event) {

        ITEMS.getRegistryObjects().values().forEach((holder) -> {

            if (holder.value() instanceof RFDrillItem item) {
                item.registerCapabilities(event);
            }
            if (holder.value() instanceof RFSawItem item) {
                item.registerCapabilities(event);
            }
            if (holder.value() instanceof RFCapacitorItem item) {
                event.registerItem(Capabilities.EnergyStorage.ITEM, (itemStack, context) -> new EnergyContainerItemWrapper(itemStack, item), holder.value());
            }
            if (holder.value() instanceof RFMagnetItem item) {
                event.registerItem(Capabilities.EnergyStorage.ITEM, (itemStack, context) -> new EnergyContainerItemWrapper(itemStack, item), holder.value());
            }
            if (holder.value() instanceof FluidReservoirItem item) {
                event.registerItem(Capabilities.FluidHandler.ITEM, (itemStack, context) -> new FluidContainerItemWrapper(itemStack, item), holder.value());
            }
            if (holder.value() instanceof PotionInfuserItem item) {
                event.registerItem(Capabilities.FluidHandler.ITEM, (itemStack, context) -> new FluidContainerItemWrapper(itemStack, item), holder.value());
            }
            if (holder.value() instanceof PotionQuiverItem item) {
                item.registerCapabilities(event);
            }
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

        event.enqueueWork(TInoItems::setup);
    }

    private void clientSetup(final FMLClientSetupEvent event) {

    }
    // endregion
}

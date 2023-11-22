package cofh.thermal.innovation.common.config;

import cofh.core.common.config.IBaseConfig;
import cofh.core.common.item.EnergyContainerItem;
import cofh.core.common.item.FluidContainerItem;
import cofh.thermal.innovation.common.item.RFMagnetItem;
import cofh.thermal.lib.common.item.EnergyContainerItemAugmentable;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.function.Supplier;

import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.innovation.init.registries.TInoIDs.*;

public class TInoToolConfig implements IBaseConfig {

    @Override
    public void apply(ForgeConfigSpec.Builder builder) {

        builder.push("Tools");

        builder.push("Drill");

        rfDrillMaxEnergy = builder
                .comment("This sets the maximum base RF capacity for the Fluxbore.")
                .defineInRange("Base Capacity", 50000, 1000, 10000000);

        rfDrillTransfer = builder
                .comment("This sets the base RF/t transfer for the Fluxbore.")
                .defineInRange("Base Transfer", 1000, 1, 10000000);

        rfDrillEnergyPerUse = builder
                .comment("This sets the energy required to break a single block.")
                .defineInRange("Energy Per Block", 200, 1, 10000);

        builder.pop();

        builder.push("Saw");

        rfSawMaxEnergy = builder
                .comment("This sets the maximum base RF capacity for the Fluxsaw.")
                .defineInRange("Base Capacity", 50000, 1000, 10000000);

        rfSawTransfer = builder
                .comment("This sets the base RF/t transfer for the Fluxsaw.")
                .defineInRange("Base Transfer", 1000, 1, 10000000);

        rfSawEnergyPerUse = builder
                .comment("This sets the energy required to break a single block.")
                .defineInRange("Energy Per Block", 200, 1, 10000);

        builder.pop();

        builder.push("Capacitor");

        rfCapacitorMaxEnergy = builder
                .comment("This sets the maximum base RF capacity for the Flux Capacitor.")
                .defineInRange("Base Capacity", 500000, 1000, 10000000);

        rfCapacitorTransfer = builder
                .comment("This sets the base RF/t transfer for the Flux Capacitor.")
                .defineInRange("Base Transfer", 1000, 1, 10000000);

        builder.pop();

        builder.push("Magnet");

        rfMagnetMaxEnergy = builder
                .comment("This sets the maximum base RF capacity for the FluxoMagnet.")
                .defineInRange("Base Capacity", 50000, 1000, 10000000);

        rfMagnetTransfer = builder
                .comment("This sets the base RF/t transfer for the FluxoMagnet.")
                .defineInRange("Base Transfer", 1000, 1, 10000000);

        rfMagnetEnergyPerItem = builder
                .comment("This sets the energy used per item picked up.")
                .defineInRange("Energy Per Item", 25, 1, 1000);

        rfMagnetEnergyPerUse = builder
                .comment("This sets the energy required to use (right click) the FluxoMagnet.")
                .defineInRange("Energy Per Use", 200, 1, 10000);

        rfMagnetObeyPickupDelay = builder
                .comment("If TRUE, the FluxoMagnet will obey Item Pickup Delay.")
                .define("Obey Item Pickup Delay", true);

        builder.pop();

        builder.push("Reservoir");

        fluidReservoirCapacity = builder
                .comment("This sets the maximum base fluid capacity for the Reservoir.")
                .defineInRange("Base Capacity", 20000, 1000, 10000000);

        builder.pop();

        builder.push("Potion Infuser");

        potionInfuserCapacity = builder
                .comment("This sets the maximum base fluid capacity for the Potion Infuser.")
                .defineInRange("Base Capacity", 4000, 1000, 10000000);

        builder.pop();

        builder.push("Potion Quiver");

        potionQuiverCapacity = builder
                .comment("This sets the maximum base fluid capacity for the Alchemical Quiver.")
                .defineInRange("Base Capacity", 4000, 1000, 10000000);

        builder.pop();

        builder.pop();
    }

    @Override
    public void refresh() {

        setEnergyParams(ID_FLUX_DRILL, rfDrillMaxEnergy.get(), rfDrillTransfer.get());
        setEnergyParams(ID_FLUX_SAW, rfSawMaxEnergy.get(), rfSawTransfer.get());
        setEnergyParams(ID_FLUX_CAPACITOR, rfCapacitorMaxEnergy.get(), rfCapacitorTransfer.get());
        setEnergyParams(ID_FLUX_MAGNET, rfMagnetMaxEnergy.get(), rfMagnetTransfer.get());

        setFluidCapacity(ID_FLUID_RESERVOIR, fluidReservoirCapacity.get());
        setFluidCapacity(ID_POTION_INFUSER, potionInfuserCapacity.get());
        setFluidCapacity(ID_POTION_QUIVER, potionQuiverCapacity.get());

        setEnergyPerUse(ID_FLUX_DRILL, rfDrillEnergyPerUse.get());
        setEnergyPerUse(ID_FLUX_SAW, rfSawEnergyPerUse.get());

        setMagnetParameters(ID_FLUX_MAGNET, rfMagnetObeyPickupDelay.get(), rfMagnetEnergyPerItem.get(), rfMagnetEnergyPerUse.get());
    }

    private void setEnergyParams(String id, int energy, int transfer) {

        Item item = ITEMS.get(id);
        if (item instanceof EnergyContainerItem ec) {
            ec.setMaxEnergy(energy);
            ec.setMaxTransfer(transfer);
        }
    }

    private void setFluidCapacity(String id, int capacity) {

        Item item = ITEMS.get(id);
        if (item instanceof FluidContainerItem fc) {
            fc.setFluidCapacity(capacity);
        }
    }

    private void setEnergyPerUse(String id, int energyPerUse) {

        Item item = ITEMS.get(id);
        if (item instanceof EnergyContainerItemAugmentable ec) {
            ec.setEnergyPerUse(energyPerUse);
        }
    }

    private void setMagnetParameters(String id, boolean obeyPickupDelay, int energyPerItem, int energyPerUse) {

        Item item = ITEMS.get(id);
        if (item instanceof RFMagnetItem mag) {
            mag.setObeyItemPickupDelay(obeyPickupDelay);
            mag.setEnergyPerItem(energyPerItem);
            mag.setEnergyPerUse(energyPerUse);
        }
    }

    // region CONFIG VARIABLES
    private Supplier<Integer> rfDrillMaxEnergy;
    private Supplier<Integer> rfDrillTransfer;
    private Supplier<Integer> rfDrillEnergyPerUse;

    private Supplier<Integer> rfSawMaxEnergy;
    private Supplier<Integer> rfSawTransfer;
    private Supplier<Integer> rfSawEnergyPerUse;

    private Supplier<Integer> rfCapacitorMaxEnergy;
    private Supplier<Integer> rfCapacitorTransfer;

    private Supplier<Integer> rfMagnetMaxEnergy;
    private Supplier<Integer> rfMagnetTransfer;
    private Supplier<Integer> rfMagnetEnergyPerItem;
    private Supplier<Integer> rfMagnetEnergyPerUse;
    private Supplier<Boolean> rfMagnetObeyPickupDelay;

    private Supplier<Integer> fluidReservoirCapacity;
    private Supplier<Integer> potionInfuserCapacity;
    private Supplier<Integer> potionQuiverCapacity;
    // endregion
}

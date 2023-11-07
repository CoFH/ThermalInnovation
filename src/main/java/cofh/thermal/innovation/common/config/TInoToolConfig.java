package cofh.thermal.innovation.common.config;

import cofh.core.common.config.IBaseConfig;
import cofh.core.common.item.EnergyContainerItem;
import cofh.core.common.item.FluidContainerItem;
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

        builder.pop();

        builder.push("Saw");

        rfSawMaxEnergy = builder
                .comment("This sets the maximum base RF capacity for the Fluxsaw.")
                .defineInRange("Base Capacity", 50000, 1000, 10000000);

        builder.pop();

        builder.push("Capacitor");

        rfCapacitorMaxEnergy = builder
                .comment("This sets the maximum base RF capacity for the Flux Capacitor.")
                .defineInRange("Base Capacity", 500000, 1000, 10000000);

        builder.pop();

        builder.push("Magnet");

        rfMagnetMaxEnergy = builder
                .comment("This sets the maximum base RF capacity for the Flux Magnet.")
                .defineInRange("Base Capacity", 50000, 1000, 10000000);

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

        setMaxEnergy(ID_FLUX_DRILL, rfDrillMaxEnergy.get());
        setMaxEnergy(ID_FLUX_SAW, rfSawMaxEnergy.get());
        setMaxEnergy(ID_FLUX_CAPACITOR, rfCapacitorMaxEnergy.get());
        setMaxEnergy(ID_FLUX_MAGNET, rfMagnetMaxEnergy.get());
        setFluidCapacity(ID_FLUID_RESERVOIR, fluidReservoirCapacity.get());
        setFluidCapacity(ID_POTION_INFUSER, potionInfuserCapacity.get());
        setFluidCapacity(ID_POTION_QUIVER, potionQuiverCapacity.get());
    }

    private void setMaxEnergy(String id, int energy) {

        Item item = ITEMS.get(id);
        if (item instanceof EnergyContainerItem ec) {
            ec.setMaxEnergy(energy);
        }
    }

    private void setFluidCapacity(String id, int capacity) {

        Item item = ITEMS.get(id);
        if (item instanceof FluidContainerItem fc) {
            fc.setFluidCapacity(capacity);
        }
    }

    // region CONFIG VARIABLES
    private Supplier<Integer> rfDrillMaxEnergy;
    private Supplier<Integer> rfSawMaxEnergy;
    private Supplier<Integer> rfCapacitorMaxEnergy;
    private Supplier<Integer> rfMagnetMaxEnergy;
    private Supplier<Integer> fluidReservoirCapacity;
    private Supplier<Integer> potionInfuserCapacity;
    private Supplier<Integer> potionQuiverCapacity;
    // endregion
}

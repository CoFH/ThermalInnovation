package cofh.thermal.innovation.config;

import cofh.lib.config.IBaseConfig;
import net.minecraftforge.common.ForgeConfigSpec;

import static cofh.thermal.innovation.init.TInoReferences.*;

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

        if (FLUX_DRILL_ITEM == null) {
            return;
        }
        FLUX_DRILL_ITEM.setMaxEnergy(rfDrillMaxEnergy.get());
        FLUX_SAW_ITEM.setMaxEnergy(rfSawMaxEnergy.get());
        FLUX_CAPACITOR_ITEM.setMaxEnergy(rfCapacitorMaxEnergy.get());
        FLUX_MAGNET_ITEM.setMaxEnergy(rfMagnetMaxEnergy.get());
        FLUID_RESERVOIR_ITEM.setFluidCapacity(fluidReservoirCapacity.get());
        POTION_INFUSER_ITEM.setFluidCapacity(potionInfuserCapacity.get());
        POTION_QUIVER_ITEM.setFluidCapacity(potionQuiverCapacity.get());
    }

    // region CONFIG VARIABLES
    private ForgeConfigSpec.IntValue rfDrillMaxEnergy;
    private ForgeConfigSpec.IntValue rfSawMaxEnergy;
    private ForgeConfigSpec.IntValue rfCapacitorMaxEnergy;
    private ForgeConfigSpec.IntValue rfMagnetMaxEnergy;
    private ForgeConfigSpec.IntValue fluidReservoirCapacity;
    private ForgeConfigSpec.IntValue potionInfuserCapacity;
    private ForgeConfigSpec.IntValue potionQuiverCapacity;
    // endregion
}

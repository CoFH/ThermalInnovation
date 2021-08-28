package cofh.thermal.innovation.init;

import cofh.thermal.innovation.item.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

import static cofh.lib.util.constants.Constants.ID_THERMAL_INNOVATION;
import static cofh.thermal.core.util.RegistrationHelper.registerItem;
import static cofh.thermal.lib.common.ThermalItemGroups.THERMAL_TOOLS;

public class TInoItems {

    private TInoItems() {

    }

    public static void register() {

        registerTools();
        registerArmor();
    }

    // region HELPERS
    private static void registerTools() {

        ItemGroup group = THERMAL_TOOLS;

        int energy = 50000;
        int xfer = 1000;
        int fluid = 4000;
        int arrows = 80;

        registerItem("flux_drill", () -> new RFDrillItem(new Item.Properties().stacksTo(1).tab(group), energy, xfer).setModId(ID_THERMAL_INNOVATION));
        registerItem("flux_saw", () -> new RFSawItem(new Item.Properties().stacksTo(1).tab(group), energy, xfer).setModId(ID_THERMAL_INNOVATION));
        // registerItem("flux_pump", () -> new RFPumpItem(new Item.Properties().maxStackSize(1).group(group), energy, xfer).setModId(ID_THERMAL_INNOVATION));
        registerItem("flux_capacitor", () -> new RFCapacitorItem(new Item.Properties().stacksTo(1).tab(group), energy * 10, xfer).setModId(ID_THERMAL_INNOVATION));
        registerItem("flux_magnet", () -> new RFMagnetItem(new Item.Properties().stacksTo(1).tab(group), energy, xfer).setModId(ID_THERMAL_INNOVATION));

        registerItem("fluid_reservoir", () -> new FluidReservoirItem(new Item.Properties().stacksTo(1).tab(group), fluid * 4).setModId(ID_THERMAL_INNOVATION));

        registerItem("potion_infuser", () -> new PotionInfuserItem(new Item.Properties().stacksTo(1).tab(group), fluid).setModId(ID_THERMAL_INNOVATION));
        registerItem("potion_quiver", () -> new PotionQuiverItem(new Item.Properties().stacksTo(1).tab(group), fluid, arrows).setModId(ID_THERMAL_INNOVATION));
    }

    private static void registerArmor() {

    }
    // endregion
}

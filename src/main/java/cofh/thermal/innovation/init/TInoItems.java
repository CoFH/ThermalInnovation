package cofh.thermal.innovation.init;

import cofh.thermal.innovation.item.*;
import net.minecraft.world.item.Item;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_INNOVATION;
import static cofh.thermal.core.util.RegistrationHelper.registerItem;
import static cofh.thermal.innovation.init.TInoIDs.*;
import static cofh.thermal.lib.common.ThermalCreativeTabs.toolsTab;

public class TInoItems {

    private TInoItems() {

    }

    public static void register() {

        registerTools();
        registerArmor();
    }

    public static void setup() {

        RFDrillItem.setupEnchants();
        RFSawItem.setupEnchants();
    }

    // region HELPERS
    private static void registerTools() {

        int energy = 50000;
        int xfer = 1000;
        int fluid = 4000;
        int arrows = 80;

        toolsTab(registerItem(ID_FLUX_DRILL, () -> new RFDrillItem(new Item.Properties().stacksTo(1), energy, xfer).setModId(ID_THERMAL_INNOVATION)));
        toolsTab(registerItem(ID_FLUX_SAW, () -> new RFSawItem(new Item.Properties().stacksTo(1), energy, xfer).setModId(ID_THERMAL_INNOVATION)));
        // registerItem(ID_FLUX_PUMP, () -> new RFPumpItem(new Item.Properties().maxStackSize(1).group(group), energy, xfer).setModId(ID_THERMAL_INNOVATION));
        toolsTab(registerItem(ID_FLUX_CAPACITOR, () -> new RFCapacitorItem(new Item.Properties().stacksTo(1), energy * 10, xfer).setModId(ID_THERMAL_INNOVATION)));
        toolsTab(registerItem(ID_FLUX_MAGNET, () -> new RFMagnetItem(new Item.Properties().stacksTo(1), energy, xfer).setModId(ID_THERMAL_INNOVATION)));
        //registerItem(ID_FLUX_GRAPPLE, () -> new RFGrappleItem(new Item.Properties().stacksTo(1), energy, xfer).setModId(ID_THERMAL_INNOVATION));

        toolsTab(registerItem(ID_FLUID_RESERVOIR, () -> new FluidReservoirItem(new Item.Properties().stacksTo(1), fluid * 4).setModId(ID_THERMAL_INNOVATION)));

        toolsTab(registerItem(ID_POTION_INFUSER, () -> new PotionInfuserItem(new Item.Properties().stacksTo(1), fluid).setModId(ID_THERMAL_INNOVATION)));
        toolsTab(registerItem(ID_POTION_QUIVER, () -> new PotionQuiverItem(new Item.Properties().stacksTo(1), fluid, arrows).setModId(ID_THERMAL_INNOVATION)));
    }

    private static void registerArmor() {

    }
    // endregion
}

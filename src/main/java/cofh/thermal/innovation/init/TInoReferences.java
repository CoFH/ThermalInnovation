package cofh.thermal.innovation.init;

import cofh.thermal.innovation.item.*;
import net.minecraftforge.registries.ObjectHolder;

import static cofh.lib.util.constants.Constants.ID_THERMAL;
import static cofh.thermal.innovation.init.TInoIDs.*;

@ObjectHolder (ID_THERMAL)
public class TInoReferences {

    private TInoReferences() {

    }

    @ObjectHolder (ID_FLUX_DRILL)
    public static final RFDrillItem FLUX_DRILL_ITEM = null;

    @ObjectHolder (ID_FLUX_SAW)
    public static final RFSawItem FLUX_SAW_ITEM = null;

    @ObjectHolder (ID_FLUX_CAPACITOR)
    public static final RFCapacitorItem FLUX_CAPACITOR_ITEM = null;

    @ObjectHolder (ID_FLUX_MAGNET)
    public static final RFMagnetItem FLUX_MAGNET_ITEM = null;

    @ObjectHolder (ID_FLUID_RESERVOIR)
    public static final FluidReservoirItem FLUID_RESERVOIR_ITEM = null;

    @ObjectHolder (ID_POTION_INFUSER)
    public static final PotionInfuserItem POTION_INFUSER_ITEM = null;

    @ObjectHolder (ID_POTION_QUIVER)
    public static final PotionQuiverItem POTION_QUIVER_ITEM = null;

}

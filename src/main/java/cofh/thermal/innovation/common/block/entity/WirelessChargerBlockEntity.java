package cofh.thermal.innovation.common.block.entity;

import cofh.core.util.helpers.AugmentDataHelper;
import cofh.lib.api.block.entity.ITickableTile;
import cofh.lib.common.energy.EnergyStorageCoFH;
import cofh.thermal.lib.common.block.entity.AugmentableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.core.common.config.ThermalCoreConfig.storageAugments;
import static cofh.thermal.lib.util.ThermalAugmentRules.createAllowValidator;

public class WirelessChargerBlockEntity extends AugmentableBlockEntity implements ITickableTile.IServerTickable {

    public static final BiPredicate<ItemStack, List<ItemStack>> AUG_VALIDATOR = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_RF, TAG_AUGMENT_TYPE_AREA_EFFECT);

    public static final int BASE_CAPACITY = 500000;
    public static final int BASE_XFER = 4000;

    public WirelessChargerBlockEntity(BlockPos pos, BlockState state) {

        super(null, pos, state);

        energyStorage = new EnergyStorageCoFH(BASE_CAPACITY, BASE_XFER);

        addAugmentSlots(storageAugments);
        initHandlers();
    }

    @Override
    public void tickServer() {

    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return null;
    }

    // region AUGMENTS
    @Override
    protected Predicate<ItemStack> augValidator() {

        return item -> AugmentDataHelper.hasAugmentData(item) && AUG_VALIDATOR.test(item, getAugmentsAsList());
    }
    // endregion
}

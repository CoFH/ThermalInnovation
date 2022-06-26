package cofh.thermal.innovation.item;

import cofh.core.capability.CapabilityAreaEffect;
import cofh.core.item.IMultiModeItem;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.AreaEffectHelper;
import cofh.core.util.helpers.ChatHelper;
import cofh.lib.api.capability.IAreaEffectItem;
import cofh.lib.api.item.IEnergyContainerItem;
import cofh.lib.energy.EnergyContainerItemWrapper;
import cofh.thermal.core.config.ThermalCoreConfig;
import cofh.thermal.lib.item.EnergyContainerItemAugmentable;
import cofh.thermal.lib.item.IFlexibleEnergyContainerItem;
import com.google.common.collect.ImmutableList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static cofh.core.util.helpers.AugmentableHelper.getPropertyWithDefault;
import static cofh.core.util.helpers.AugmentableHelper.setAttributeFromAugmentAdd;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.lib.common.ThermalAugmentRules.createAllowValidator;

public class RFPumpItem extends EnergyContainerItemAugmentable implements IMultiModeItem, IFlexibleEnergyContainerItem {

    public static final int ENERGY_PER_USE = 200;

    public RFPumpItem(Properties builder, int maxEnergy, int maxTransfer) {

        super(builder, maxEnergy, maxTransfer);

        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("charged"), (stack, world, entity, seed) -> getEnergyStored(stack) > 0 ? 1F : 0F);
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("active"), (stack, world, entity, seed) -> getEnergyStored(stack) > 0 && hasActiveTag(stack) ? 1F : 0F);

        numSlots = () -> ThermalCoreConfig.toolAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_RF, TAG_AUGMENT_TYPE_AREA_EFFECT, TAG_AUGMENT_TYPE_FILTER);
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        int radius = getMode(stack) * 2 + 1;
        if (radius <= 1) {
            tooltip.add(Component.translatable("info.cofh.single_block").withStyle(ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.translatable("info.cofh.area").append(": " + radius + "x" + radius).withStyle(ChatFormatting.ITALIC));
        }
        if (getNumModes(stack) > 1) {
            addModeChangeTooltip(this, stack, worldIn, tooltip, flagIn);
        }
        super.tooltipDelegate(stack, worldIn, tooltip, flagIn);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {

        // TODO: CHANGE
        return super.use(worldIn, playerIn, handIn);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {

        if (!hasActiveTag(stack)) {
            return;
        }
        long activeTime = stack.getOrCreateTag().getLong(TAG_ACTIVE);

        if (entityIn.level.getGameTime() > activeTime) {
            stack.getOrCreateTag().remove(TAG_ACTIVE);
        }
    }

    // region HELPERS
    @Override
    protected void setAttributesFromAugment(ItemStack container, CompoundTag augmentData) {

        CompoundTag subTag = container.getTagElement(TAG_PROPERTIES);
        if (subTag == null) {
            return;
        }
        setAttributeFromAugmentAdd(subTag, augmentData, TAG_AUGMENT_RADIUS);

        super.setAttributesFromAugment(container, augmentData);
    }

    protected int getEnergyPerUse(ItemStack stack) {

        return ENERGY_PER_USE;
    }

    protected int getRadius(ItemStack stack) {

        return (int) getPropertyWithDefault(stack, TAG_AUGMENT_RADIUS, 0.0F);
    }
    // endregion

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {

        return new RFPumpItemWrapper(stack, this);
    }

    // region IAugmentableItem
    @Override
    public void updateAugmentState(ItemStack container, List<ItemStack> augments) {

        super.updateAugmentState(container, augments);

        if (getMode(container) >= getNumModes(container)) {
            setMode(container, getNumModes(container) - 1);
        }
    }
    // endregion

    // region IMultiModeItem
    @Override
    public int getNumModes(ItemStack stack) {

        return 1 + getRadius(stack);
    }

    @Override
    public void onModeChange(Player player, ItemStack stack) {

        player.level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.4F, 1.0F - 0.1F * getMode(stack));
        int radius = getMode(stack) * 2 + 1;
        if (radius <= 1) {
            ChatHelper.sendIndexedChatMessageToPlayer(player, Component.translatable("info.cofh.single_block"));
        } else {
            ChatHelper.sendIndexedChatMessageToPlayer(player, Component.translatable("info.cofh.area").append(": " + radius + "x" + radius));
        }
    }
    // endregion

    // region CAPABILITY WRAPPER
    protected class RFPumpItemWrapper extends EnergyContainerItemWrapper implements IAreaEffectItem {

        private final LazyOptional<IAreaEffectItem> holder = LazyOptional.of(() -> this);

        RFPumpItemWrapper(ItemStack containerIn, IEnergyContainerItem itemIn) {

            super(containerIn, itemIn, itemIn.getEnergyCapability());
        }

        @Override
        public ImmutableList<BlockPos> getAreaEffectBlocks(BlockPos pos, Player player) {

            return AreaEffectHelper.getBucketableBlocksRadius(container, pos, player, getMode(container));
        }

        // region ICapabilityProvider
        @Override
        @Nonnull
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

            if (cap == CapabilityAreaEffect.AREA_EFFECT_ITEM_CAPABILITY) {
                return CapabilityAreaEffect.AREA_EFFECT_ITEM_CAPABILITY.orEmpty(cap, holder);
            }
            return super.getCapability(cap, side);
        }
        // endregion
    }
    // endregion
}

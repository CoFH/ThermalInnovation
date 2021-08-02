package cofh.thermal.innovation.item;

import cofh.core.item.FluidContainerItemAugmentable;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.ChatHelper;
import cofh.lib.item.IMultiModeItem;
import cofh.thermal.lib.common.ThermalConfig;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.thermal.lib.common.ThermalAugmentRules.FLUID_VALIDATOR;

public class FluidReservoirItem extends FluidContainerItemAugmentable implements IMultiModeItem {

    protected static final int FILL = 0;
    protected static final int EMPTY = 1;

    public FluidReservoirItem(Properties builder, int fluidCapacity) {

        super(builder, fluidCapacity);

        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("filled"), (stack, world, entity) -> getFluidAmount(stack) > 0 ? 1F : 0F);
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("active"), (stack, world, entity) -> getFluidAmount(stack) > 0 && getMode(stack) > 0 ? 1F : 0F);

        numSlots = () -> ThermalConfig.toolAugments;
        augValidator = FLUID_VALIDATOR;
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {

        tooltip.add(isActive(stack)
                ? new TranslationTextComponent("info.cofh_use_sneak_deactivate").mergeStyle(TextFormatting.DARK_GRAY)
                : new TranslationTextComponent("info.cofh.use_sneak_activate").mergeStyle(TextFormatting.DARK_GRAY));

        tooltip.add(getTextComponent("info.thermal.reservoir.mode." + getMode(stack)).mergeStyle(TextFormatting.ITALIC));
        addIncrementModeChangeTooltip(stack, worldIn, tooltip, flagIn);

        super.tooltipDelegate(stack, worldIn, tooltip, flagIn);
    }

    // region IFluidContainerItem
    // TODO: Determine if there should be limitations.
    //    @Override
    //    public int fill(ItemStack container, FluidStack resource, FluidAction action) {
    //
    //        if (getMode(container) != FILL) {
    //            return 0;
    //        }
    //        return super.fill(container, resource, action);
    //    }
    //
    //    @Override
    //    public FluidStack drain(ItemStack container, int maxDrain, FluidAction action) {
    //
    //        if (getMode(container) != EMPTY) {
    //            return FluidStack.EMPTY;
    //        }
    //        return super.drain(container, maxDrain, action);
    //    }
    // endregion

    // region IMultiModeItem
    @Override
    public void onModeChange(PlayerEntity player, ItemStack stack) {

        player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.4F, 0.6F + 0.2F * getMode(stack));
        ChatHelper.sendIndexedChatMessageToPlayer(player, new TranslationTextComponent("info.thermal.reservoir.mode." + getMode(stack)));
    }
    // endregion
}

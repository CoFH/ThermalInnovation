package cofh.thermal.innovation.item;

import cofh.core.item.InventoryContainerItemAugmentable;
import cofh.core.util.ProxyUtils;
import cofh.core.util.filter.EmptyFilter;
import cofh.core.util.filter.FilterRegistry;
import cofh.core.util.helpers.ChatHelper;
import cofh.lib.inventory.SimpleItemInv;
import cofh.lib.item.IMultiModeItem;
import cofh.lib.util.filter.IFilter;
import cofh.lib.util.filter.IFilterableItem;
import cofh.lib.util.helpers.FilterHelper;
import cofh.thermal.lib.common.ThermalConfig;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import static cofh.lib.util.constants.NBTTags.*;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.thermal.core.init.TCoreSounds.SOUND_MAGNET;
import static cofh.thermal.lib.common.ThermalAugmentRules.createAllowValidator;

public class SatchelItem extends InventoryContainerItemAugmentable implements IFilterableItem, IMultiModeItem {

    protected static final int MAP_CAPACITY = 128;
    protected static final WeakHashMap<ItemStack, IFilter> FILTERS = new WeakHashMap<>(MAP_CAPACITY);

    public SatchelItem(Properties builder, int slots) {

        super(builder, slots);

        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("active"), (stack, world, entity) -> getMode(stack) > 0 ? 1F : 0F);

        numSlots = () -> ThermalConfig.storageAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_RF, TAG_AUGMENT_TYPE_AREA_EFFECT, TAG_AUGMENT_TYPE_FILTER);
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {

        tooltip.add(getTextComponent("info.thermal.magnet.use").mergeStyle(TextFormatting.GRAY));
        if (FilterHelper.hasFilter(stack)) {
            tooltip.add(getTextComponent("info.thermal.magnet.use.sneak").mergeStyle(TextFormatting.DARK_GRAY));
        }
        tooltip.add(getTextComponent("info.thermal.magnet.mode." + getMode(stack)).mergeStyle(TextFormatting.ITALIC));
        addIncrementModeChangeTooltip(stack, worldIn, tooltip, flagIn);

        super.tooltipDelegate(stack, worldIn, tooltip, flagIn);
    }

    //    @Override
    //    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
    //
    //        ItemStack stack = playerIn.getHeldItem(handIn);
    //        return useDelegate(stack, playerIn, handIn) ? ActionResult.resultSuccess(stack) : ActionResult.resultPass(stack);
    //    }
    //
    //    @Override
    //    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
    //
    //        return useDelegate(stack, context.getPlayer(), context.getHand()) ? ActionResultType.SUCCESS : ActionResultType.PASS;
    //    }

    @Override

    // TODO - Filter Logic here
    protected IItemHandler readInventoryFromNBT(ItemStack container) {

        CompoundNBT containerTag = getOrCreateInvTag(container);
        SimpleItemInv inventory = new SimpleItemInv(new ArrayList<>(getSlots(container)));
        inventory.read(containerTag);

        return inventory;
    }

    // region IFilterableItem
    @Override
    public IFilter getFilter(ItemStack stack) {

        String filterType = FilterHelper.getFilterType(stack);
        if (filterType.isEmpty()) {
            return EmptyFilter.INSTANCE;
        }
        IFilter ret = FILTERS.get(stack);
        if (ret != null) {
            return ret;
        }
        if (FILTERS.size() > MAP_CAPACITY) {
            FILTERS.clear();
        }
        FILTERS.put(stack, FilterRegistry.getHeldFilter(filterType, stack.getTag()));
        return FILTERS.get(stack);
    }

    @Override
    public void onFilterChanged(ItemStack stack) {

        FILTERS.remove(stack);
    }
    // endregion

    // region IMultiModeItem
    @Override
    public void onModeChange(PlayerEntity player, ItemStack stack) {

        player.world.playSound(null, player.getPosition(), SOUND_MAGNET, SoundCategory.PLAYERS, 0.4F, 0.8F + 0.4F * getMode(stack));
        ChatHelper.sendIndexedChatMessageToPlayer(player, new TranslationTextComponent("info.thermal.satchel.mode." + getMode(stack)));
    }
    // endregion
}

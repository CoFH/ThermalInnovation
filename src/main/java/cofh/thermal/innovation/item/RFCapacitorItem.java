package cofh.thermal.innovation.item;

import cofh.core.compat.curios.CuriosProxy;
import cofh.core.item.EnergyContainerItemAugmentable;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.ChatHelper;
import cofh.core.util.helpers.EnergyHelper;
import cofh.lib.item.IColorableItem;
import cofh.lib.item.IMultiModeItem;
import cofh.lib.util.Utils;
import cofh.thermal.lib.common.ThermalConfig;
import com.google.common.collect.Iterables;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IDyeableArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.thermal.lib.common.ThermalAugmentRules.ENERGY_STORAGE_VALIDATOR;

public class RFCapacitorItem extends EnergyContainerItemAugmentable implements IColorableItem, IDyeableArmorItem, IMultiModeItem {

    protected static final int EQUIPMENT = 0;
    protected static final int INVENTORY = 1;

    public RFCapacitorItem(Properties builder, int maxEnergy, int maxTransfer) {

        super(builder, maxEnergy, maxTransfer);

        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("color"), (stack, world, entity) -> (hasCustomColor(stack) ? 1.0F : 0));
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("state"), (stack, world, entity) -> (isActive(stack) ? 0.5F : 0) + (getMode(stack) / 8.0F));
        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalConfig.storageAugments;
        augValidator = ENERGY_STORAGE_VALIDATOR;
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {

        tooltip.add(isActive(stack)
                ? new TranslationTextComponent("info.cofh_use_sneak_deactivate").withStyle(TextFormatting.DARK_GRAY)
                : new TranslationTextComponent("info.cofh.use_sneak_activate").withStyle(TextFormatting.DARK_GRAY));

        tooltip.add(getTextComponent("info.thermal.capacitor.mode." + getMode(stack)).withStyle(TextFormatting.ITALIC));
        addIncrementModeChangeTooltip(stack, worldIn, tooltip, flagIn);

        super.tooltipDelegate(stack, worldIn, tooltip, flagIn);
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {

        ItemStack stack = playerIn.getItemInHand(handIn);
        return useDelegate(stack, playerIn) ? ActionResult.success(stack) : ActionResult.pass(stack);
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {

        return useDelegate(stack, context.getPlayer()) ? ActionResultType.SUCCESS : ActionResultType.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {

        if (Utils.isClientWorld(worldIn) || Utils.isFakePlayer(entityIn) || !isActive(stack)) {
            return;
        }
        Iterable<ItemStack> equipment;
        PlayerEntity player = (PlayerEntity) entityIn;

        switch (getMode(stack)) {
            case EQUIPMENT:
                equipment = player.getAllSlots();
                break;
            case INVENTORY:
                equipment = player.inventory.items;
                break;
            default:
                equipment = Iterables.concat(Arrays.asList(player.inventory.items, player.inventory.armor, player.inventory.offhand));
        }
        int extract = this.getExtract(stack);
        for (ItemStack equip : equipment) {
            if (stack.isEmpty() || equip.equals(stack)) {
                continue;
            }
            equip.getCapability(EnergyHelper.getEnergySystem(), null)
                    .ifPresent(e -> this.extractEnergy(stack, e.receiveEnergy(Math.min(extract, this.getEnergyStored(stack)), false), player.abilities.instabuild));
        }
        if (getMode(stack) != INVENTORY) {
            CuriosProxy.getAllWorn(player).ifPresent(c -> {
                for (int i = 0; i < c.getSlots(); ++i) {
                    ItemStack equip = c.getStackInSlot(i);
                    if (stack.isEmpty() || equip.equals(stack)) {
                        continue;
                    }
                    equip.getCapability(EnergyHelper.getEnergySystem(), null)
                            .ifPresent(e -> this.extractEnergy(stack, e.receiveEnergy(Math.min(extract, this.getEnergyStored(stack)), false), player.abilities.instabuild));
                }
            });
        }
    }

    // region HELPERS
    protected boolean useDelegate(ItemStack stack, PlayerEntity player) {

        if (Utils.isFakePlayer(player)) {
            return false;
        }
        if (player.isSecondaryUseActive()) {
            setActive(stack, !isActive(stack));
            player.level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.2F, isActive(stack) ? 0.8F : 0.5F);
            return true;
        }
        return false;
    }
    // endregion

    // region IMultiModeItem
    @Override
    public int getNumModes(ItemStack stack) {

        return 3;
    }

    @Override
    public void onModeChange(PlayerEntity player, ItemStack stack) {

        player.level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundCategory.PLAYERS, 0.4F, (isActive(stack) ? 0.7F : 0.5F) + 0.1F * getMode(stack));
        ChatHelper.sendIndexedChatMessageToPlayer(player, new TranslationTextComponent("info.thermal.capacitor.mode." + getMode(stack)));
    }
    // endregion
}

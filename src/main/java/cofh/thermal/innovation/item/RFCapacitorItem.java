package cofh.thermal.innovation.item;

import cofh.core.compat.curios.CuriosProxy;
import cofh.core.item.IMultiModeItem;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.ChatHelper;
import cofh.lib.api.item.IColorableItem;
import cofh.lib.util.Utils;
import cofh.thermal.core.config.ThermalCoreConfig;
import cofh.thermal.lib.item.EnergyContainerItemAugmentable;
import cofh.thermal.lib.item.IFlexibleEnergyContainerItem;
import cofh.thermal.lib.util.ThermalEnergyHelper;
import com.google.common.collect.Iterables;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.thermal.lib.common.ThermalAugmentRules.ENERGY_STORAGE_VALIDATOR;

public class RFCapacitorItem extends EnergyContainerItemAugmentable implements IColorableItem, DyeableLeatherItem, IMultiModeItem, IFlexibleEnergyContainerItem {

    protected static final int EQUIPMENT = 0;
    protected static final int INVENTORY = 1;

    public RFCapacitorItem(Properties builder, int maxEnergy, int maxTransfer) {

        super(builder, maxEnergy, maxTransfer);

        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("color"), (stack, world, entity, seed) -> (hasCustomColor(stack) ? 1.0F : 0));
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("state"), (stack, world, entity, seed) -> (isActive(stack) ? 0.5F : 0) + (getMode(stack) / 8.0F));
        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalCoreConfig.storageAugments;
        augValidator = ENERGY_STORAGE_VALIDATOR;
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        tooltip.add(isActive(stack)
                ? new TranslatableComponent("info.cofh_use_sneak_deactivate").withStyle(ChatFormatting.DARK_GRAY)
                : new TranslatableComponent("info.cofh.use_sneak_activate").withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(getTextComponent("info.thermal.capacitor.mode." + getMode(stack)).withStyle(ChatFormatting.ITALIC));
        addModeChangeTooltip(this, stack, worldIn, tooltip, flagIn);

        super.tooltipDelegate(stack, worldIn, tooltip, flagIn);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {

        ItemStack stack = playerIn.getItemInHand(handIn);
        return useDelegate(stack, playerIn) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {

        return useDelegate(stack, context.getPlayer()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {

        if (Utils.isClientWorld(worldIn) || Utils.isFakePlayer(entityIn) || !isActive(stack)) {
            return;
        }
        Iterable<ItemStack> equipment;
        Player player = (Player) entityIn;

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
            if (equip.isEmpty() || equip.equals(stack)) {
                continue;
            }
            equip.getCapability(ThermalEnergyHelper.getBaseEnergySystem(), null)
                    .ifPresent(e -> this.extractEnergy(stack, e.receiveEnergy(Math.min(extract, this.getEnergyStored(stack)), false), player.abilities.instabuild));
        }
        if (getMode(stack) != INVENTORY) {
            CuriosProxy.getAllWorn(player).ifPresent(c -> {
                for (int i = 0; i < c.getSlots(); ++i) {
                    ItemStack equip = c.getStackInSlot(i);
                    if (equip.isEmpty() || equip.equals(stack)) {
                        continue;
                    }
                    equip.getCapability(ThermalEnergyHelper.getBaseEnergySystem(), null)
                            .ifPresent(e -> this.extractEnergy(stack, e.receiveEnergy(Math.min(extract, this.getEnergyStored(stack)), false), player.abilities.instabuild));
                }
            });
        }
    }

    // region HELPERS
    protected boolean useDelegate(ItemStack stack, Player player) {

        if (Utils.isFakePlayer(player)) {
            return false;
        }
        if (player.isSecondaryUseActive()) {
            setActive(stack, !isActive(stack));
            player.level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2F, isActive(stack) ? 0.8F : 0.5F);
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
    public void onModeChange(Player player, ItemStack stack) {

        player.level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.4F, (isActive(stack) ? 0.7F : 0.5F) + 0.1F * getMode(stack));
        ChatHelper.sendIndexedChatMessageToPlayer(player, new TranslatableComponent("info.thermal.capacitor.mode." + getMode(stack)));
    }
    // endregion
}

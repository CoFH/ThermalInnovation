package cofh.thermal.innovation.item;

import cofh.core.item.IMultiModeItem;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.ChatHelper;
import cofh.lib.api.item.IColorableItem;
import cofh.lib.util.Utils;
import cofh.thermal.core.config.ThermalCoreConfig;
import cofh.thermal.innovation.entity.GrappleHook;
import cofh.thermal.lib.item.EnergyContainerItemAugmentable;
import cofh.thermal.lib.item.IFlexibleEnergyContainerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

import static cofh.core.util.helpers.AugmentableHelper.*;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.thermal.lib.common.ThermalAugmentRules.createAllowValidator;

public class RFGrappleItem extends EnergyContainerItemAugmentable implements IColorableItem, DyeableLeatherItem, IMultiModeItem, IFlexibleEnergyContainerItem {

    protected static final int ENERGY_PER_USE = 200;

    public RFGrappleItem(Properties builder, int maxEnergy, int maxTransfer) {

        super(builder, maxEnergy, maxTransfer);

        //ProxyUtils.registerItemModelProperty(this, new ResourceLocation("color"), (stack, world, entity, seed) -> (hasCustomColor(stack) ? 1.0F : 0));
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("state"), (stack, world, entity, seed) -> (getEnergyStored(stack) > 0 ? 0.5F : 0) + (stack.getOrCreateTag().contains(TAG_HOOK) ? 0.25F : 0));
        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalCoreConfig.toolAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_RF, TAG_AUGMENT_TYPE_AREA_EFFECT, TAG_AUGMENT_TYPE_FILTER);
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        tooltip.add(getTextComponent("info.thermal.grapple.mode." + getMode(stack)).withStyle(ChatFormatting.ITALIC));
        addModeChangeTooltip(this, stack, worldIn, tooltip, flagIn);

        super.tooltipDelegate(stack, worldIn, tooltip, flagIn);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        return useDelegate(stack, player, hand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {

        //return useDelegate(stack, context.getPlayer(), context.getHand()).getResult();
        return super.onItemUseFirst(stack, context);
    }

    @Override
    public void onUsingTick(ItemStack stack, LivingEntity living, int count) {

        Level level = living.level;
        if (level.isClientSide || !(living instanceof Player) || Utils.isFakePlayer(living) || getMode(stack) > 0) {
            living.releaseUsingItem();
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int itemSlot, boolean isSelected) {

        //if (level.isClientSide || !(entity instanceof Player) || Utils.isFakePlayer(entity)) {
        //    return;
        //}
        //CompoundTag tag = stack.getOrCreateTag();
        //if (tag.contains(TAG_HOOK)) {
        //    Entity hook = level.getEntity(tag.getInt(TAG_HOOK));
        //    System.out.println(tag + " " + hook);
        //    float range = getReach(stack);
        //    float r2 = range * range * 1.1F;
        //    if (!(hook instanceof GrappleHook && hook.isAlive() && hook.distanceToSqr(entity) < r2)) {
        //        if (hook != null) {
        //            hook.discard();
        //        }
        //        tag.remove(TAG_HOOK);
        //    } else if (Utils.timeCheckQuarter() && !Utils.isCreativePlayer(entity)) {
        //        extractEnergy(stack, ENERGY_PER_USE, false);
        //    }
        //}
    }

    // region HELPERS
    protected InteractionResultHolder<ItemStack> useDelegate(ItemStack stack, Player player, InteractionHand hand) {

        //if (Utils.isFakePlayer(player)) {
        //    return InteractionResultHolder.pass(stack);
        //}
        //Level level = player.level;
        //CompoundTag tag = stack.getOrCreateTag();
        //if (tag.contains(TAG_HOOK)) {
        //    Entity entity = level.getEntity(tag.getInt(TAG_HOOK));
        //    if (entity instanceof GrappleHook) {
        //        if (getMode(stack) <= 0) {
        //            player.startUsingItem(hand);
        //        } else {
        //            entity.discard();
        //        }
        //    } else {
        //        tag.remove(TAG_HOOK);
        //    }
        //} else if (Utils.isCreativePlayer(player) || extractEnergy(stack, ENERGY_PER_USE, false) == ENERGY_PER_USE) {
        //    if (!level.isClientSide) {
        //        GrappleHook hook = new GrappleHook(level, player);
        //        hook.shoot(player.getLookAngle().scale(getShootSpeed(stack)));
        //        level.addFreshEntity(hook);
        //        tag.putInt(TAG_HOOK, hook.getId());
        //    }
        //} else {
        //    return InteractionResultHolder.pass(stack);
        //}

        Level level = player.level;
        if (!level.isClientSide) {
            level.addFreshEntity(new GrappleHook(level, player, player.getLookAngle().scale(getShootSpeed(stack))));
            //tag.putInt(TAG_HOOK, hook.getId());
        }
        return InteractionResultHolder.success(stack);
    }

     public float getReach(ItemStack stack) {

        return 24.0F * getPropertyWithDefault(stack, TAG_AUGMENT_REACH, 1.0F);
    }

    public float getShootSpeed(ItemStack stack) {

        return getBaseMod(stack); //TODO tweak
    }

    public float getPullSpeed(ItemStack stack) {

        return getBaseMod(stack); //TODO tweak
    }

    @Override
    protected void setAttributesFromAugment(ItemStack container, CompoundTag augmentData) {

        CompoundTag subTag = container.getTagElement(TAG_PROPERTIES);
        if (subTag == null) {
            return;
        }
        setAttributeFromAugmentAdd(subTag, augmentData, TAG_AUGMENT_REACH);
        //TODO reach

        super.setAttributesFromAugment(container, augmentData);
    }
    // endregion

    // region IMultiModeItem
    @Override
    public void onModeChange(Player player, ItemStack stack) {

        player.level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.4F, 0.8F + 0.4F * getMode(stack));
        ChatHelper.sendIndexedChatMessageToPlayer(player, Component.translatable("info.thermal.grapple.mode." + getMode(stack)));
    }
    // endregion
}

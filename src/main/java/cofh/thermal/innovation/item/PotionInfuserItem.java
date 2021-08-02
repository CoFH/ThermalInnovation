package cofh.thermal.innovation.item;

import cofh.core.item.FluidContainerItemAugmentable;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.ChatHelper;
import cofh.core.util.helpers.FluidHelper;
import cofh.lib.item.IMultiModeItem;
import cofh.lib.util.Utils;
import cofh.thermal.lib.common.ThermalConfig;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

import static cofh.lib.util.constants.NBTTags.*;
import static cofh.lib.util.helpers.AugmentableHelper.setAttributeFromAugmentAdd;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.thermal.lib.common.ThermalAugmentRules.createAllowValidator;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

public class PotionInfuserItem extends FluidContainerItemAugmentable implements IMultiModeItem {

    protected static final int TIME_CONSTANT = 32;

    protected static final int MB_PER_CYCLE = 50;
    protected static final int MB_PER_USE = 250;

    public PotionInfuserItem(Properties builder, int fluidCapacity) {

        this(builder, fluidCapacity, FluidHelper::hasPotionTag);

        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("filled"), (stack, world, entity) -> getFluidAmount(stack) > 0 ? 1F : 0F);
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("active"), (stack, world, entity) -> getFluidAmount(stack) > 0 && getMode(stack) > 0 ? 1F : 0F);

        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalConfig.toolAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_RF, TAG_AUGMENT_TYPE_POTION, TAG_AUGMENT_TYPE_FILTER);
    }

    public PotionInfuserItem(Properties builder, int fluidCapacity, Predicate<FluidStack> validator) {

        super(builder, fluidCapacity, validator);
    }

    public PotionInfuserItem setNumSlots(IntSupplier numSlots) {

        this.numSlots = numSlots;
        return this;
    }

    public PotionInfuserItem setAugValidator(BiPredicate<ItemStack, List<ItemStack>> augValidator) {

        this.augValidator = augValidator;
        return this;
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {

        tooltip.add(getTextComponent("info.thermal.infuser.use").mergeStyle(TextFormatting.GRAY));
        tooltip.add(getTextComponent("info.thermal.infuser.use.sneak").mergeStyle(TextFormatting.DARK_GRAY));

        tooltip.add(getTextComponent("info.thermal.infuser.mode." + getMode(stack)).mergeStyle(TextFormatting.ITALIC));
        addIncrementModeChangeTooltip(stack, worldIn, tooltip, flagIn);

        FluidStack fluid = getFluid(stack);
        List<EffectInstance> effects = new ArrayList<>();
        for (EffectInstance effect : PotionUtils.getEffectsFromTag(fluid.getTag())) {
            effects.add(new EffectInstance(effect.getPotion(), getEffectDuration(effect, stack), getEffectAmplifier(effect, stack), effect.isAmbient(), effect.doesShowParticles()));
        }
        potionTooltip(stack, worldIn, tooltip, flagIn, effects);
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {

        if (getFluidAmount(stack) <= 0) {
            return super.getRGBDurabilityForDisplay(stack);
        }
        return getFluid(stack).getFluid().getAttributes().getColor(getFluid(stack));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {

        ItemStack stack = playerIn.getHeldItem(handIn);
        return useDelegate(stack, playerIn, handIn) ? ActionResult.resultSuccess(stack) : ActionResult.resultPass(stack);
    }

    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity player, LivingEntity entity, Hand hand) {

        FluidStack fluid = getFluid(stack);
        if (fluid != null && fluid.getAmount() >= MB_PER_USE) {
            if (Utils.isServerWorld(entity.world)) {
                for (EffectInstance effect : PotionUtils.getEffectsFromTag(fluid.getTag())) {
                    if (effect.getPotion().isInstant()) {
                        effect.getPotion().affectEntity(player, player, entity, effect.getAmplifier(), 0.5D);
                    } else {
                        EffectInstance potion = new EffectInstance(effect.getPotion(), getEffectDuration(effect, stack) / 2, getEffectAmplifier(effect, stack), effect.isAmbient(), effect.doesShowParticles());
                        entity.addPotionEffect(potion);
                    }
                }
                if (!player.abilities.isCreativeMode) {
                    drain(stack, MB_PER_USE, EXECUTE);
                }
            }
            player.swingArm(hand);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {

        return useDelegate(stack, context.getPlayer(), context.getHand()) ? ActionResultType.SUCCESS : ActionResultType.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {

        if (worldIn.getGameTime() % TIME_CONSTANT != 0) {
            return;
        }
        if (Utils.isClientWorld(worldIn)) {
            return;
        }
        if (!(entityIn instanceof LivingEntity) || Utils.isFakePlayer(entityIn) || getMode(stack) <= 0) {
            return;
        }
        LivingEntity living = (LivingEntity) entityIn;
        FluidStack fluid = getFluid(stack);
        if (fluid != null && fluid.getAmount() >= MB_PER_CYCLE) {
            boolean used = false;
            for (EffectInstance effect : PotionUtils.getEffectsFromTag(fluid.getTag())) {
                EffectInstance active = living.getActivePotionMap().get(effect.getPotion());

                if (active != null && active.getDuration() >= 40) {
                    continue;
                }
                if (effect.getPotion().isInstant()) {
                    effect.getPotion().affectEntity(null, null, (LivingEntity) entityIn, effect.getAmplifier(), 0.5D);
                } else {
                    EffectInstance potion = new EffectInstance(effect.getPotion(), getEffectDuration(effect, stack) / 4, getEffectAmplifier(effect, stack), effect.isAmbient(), false);
                    living.addPotionEffect(potion);
                }
                used = true;
            }
            if (entityIn instanceof PlayerEntity && ((PlayerEntity) entityIn).abilities.isCreativeMode) {
                return;
            }
            if (used) {
                drain(stack, MB_PER_CYCLE, EXECUTE);
            }
        }
    }

    // region HELPERS
    @Override
    protected void setAttributesFromAugment(ItemStack container, CompoundNBT augmentData) {

        CompoundNBT subTag = container.getChildTag(TAG_PROPERTIES);
        if (subTag == null) {
            return;
        }
        setAttributeFromAugmentAdd(subTag, augmentData, TAG_AUGMENT_POTION_AMPLIFIER);
        setAttributeFromAugmentAdd(subTag, augmentData, TAG_AUGMENT_POTION_DURATION);

        super.setAttributesFromAugment(container, augmentData);
    }

    protected boolean useDelegate(ItemStack stack, PlayerEntity player, Hand hand) {

        if (Utils.isFakePlayer(player) || !player.isSecondaryUseActive()) {
            return false;
        }
        if (Utils.isServerWorld(player.world)) {
            FluidStack fluid = getFluid(stack);
            if (fluid != null && (fluid.getAmount() >= MB_PER_USE || player.abilities.isCreativeMode)) {
                for (EffectInstance effect : PotionUtils.getEffectsFromTag(fluid.getTag())) {
                    if (effect.getPotion().isInstant()) {
                        effect.getPotion().affectEntity(null, null, player, getEffectAmplifier(effect, stack), 1.0D);
                    } else {
                        EffectInstance potion = new EffectInstance(effect.getPotion(), getEffectDuration(effect, stack), getEffectAmplifier(effect, stack), effect.isAmbient(), false);
                        player.addPotionEffect(potion);
                    }
                }
                if (!player.abilities.isCreativeMode) {
                    drain(stack, MB_PER_USE, EXECUTE);
                }
            }
        }
        player.swingArm(hand);
        stack.setAnimationsToGo(5);
        return true;
    }
    // endregion

    // region IMultiModeItem
    @Override
    public void onModeChange(PlayerEntity player, ItemStack stack) {

        player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.4F, 0.6F + 0.2F * getMode(stack));
        ChatHelper.sendIndexedChatMessageToPlayer(player, new TranslationTextComponent("info.thermal.infuser.mode." + getMode(stack)));
    }
    // endregion
}

package cofh.thermal.innovation.item;

import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.ChatHelper;
import cofh.core.util.helpers.FluidHelper;
import cofh.lib.item.IColorableItem;
import cofh.lib.item.IMultiModeItem;
import cofh.lib.util.Utils;
import cofh.thermal.core.config.ThermalCoreConfig;
import cofh.thermal.lib.item.FluidContainerItemAugmentable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
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

public class PotionInfuserItem extends FluidContainerItemAugmentable implements IColorableItem, DyeableLeatherItem, IMultiModeItem {

    protected static final int TIME_CONSTANT = 32;

    protected static final int MB_PER_CYCLE = 50;
    protected static final int MB_PER_USE = 250;

    public PotionInfuserItem(Properties builder, int fluidCapacity) {

        this(builder, fluidCapacity, FluidHelper::hasPotionTag);

        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("color"), (stack, world, entity, seed) -> (hasCustomColor(stack) ? 1.0F : 0));
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("state"), (stack, world, entity, seed) -> (getFluidAmount(stack) > 0 ? 0.5F : 0) + (getMode(stack) > 0 ? 0.25F : 0));
        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalCoreConfig.toolAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_FLUID, TAG_AUGMENT_TYPE_POTION, TAG_AUGMENT_TYPE_FILTER);
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
    protected void tooltipDelegate(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        tooltip.add(getTextComponent("info.thermal.infuser.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(getTextComponent("info.thermal.infuser.use.sneak").withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(getTextComponent("info.thermal.infuser.mode." + getMode(stack)).withStyle(ChatFormatting.ITALIC));
        addModeChangeTooltip(this, stack, worldIn, tooltip, flagIn);

        FluidStack fluid = getFluid(stack);
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance effect : PotionUtils.getAllEffects(fluid.getTag())) {
            effects.add(new MobEffectInstance(effect.getEffect(), getEffectDuration(effect, stack), getEffectAmplifier(effect, stack), effect.isAmbient(), effect.isVisible()));
        }
        potionTooltip(stack, worldIn, tooltip, flagIn, effects);
    }

    @Override
    public int getBarColor(ItemStack stack) {

        if (getFluidAmount(stack) <= 0) {
            return super.getBarColor(stack);
        }
        return getFluid(stack).getFluid().getAttributes().getColor(getFluid(stack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {

        ItemStack stack = playerIn.getItemInHand(handIn);
        return useDelegate(stack, playerIn, handIn) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {

        FluidStack fluid = getFluid(stack);
        if (fluid != null && fluid.getAmount() >= MB_PER_USE) {
            if (Utils.isServerWorld(entity.level)) {
                for (MobEffectInstance effect : PotionUtils.getAllEffects(fluid.getTag())) {
                    if (effect.getEffect().isInstantenous()) {
                        effect.getEffect().applyInstantenousEffect(player, player, entity, effect.getAmplifier(), 0.5D);
                    } else {
                        MobEffectInstance potion = new MobEffectInstance(effect.getEffect(), getEffectDuration(effect, stack) / 2, getEffectAmplifier(effect, stack), effect.isAmbient(), effect.isVisible());
                        entity.addEffect(potion);
                    }
                }
                if (!player.abilities.instabuild) {
                    drain(stack, MB_PER_USE, EXECUTE);
                }
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    //        @Override
    //        public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
    //
    //            return useDelegate(stack, context.getPlayer(), context.getHand()) ? ActionResultType.SUCCESS : ActionResultType.PASS;
    //        }

    @Override
    public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {

        if (!Utils.timeCheck()) {
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
            for (MobEffectInstance effect : PotionUtils.getAllEffects(fluid.getTag())) {
                MobEffectInstance active = living.getActiveEffectsMap().get(effect.getEffect());

                if (active != null && active.getDuration() >= 40) {
                    continue;
                }
                if (effect.getEffect().isInstantenous()) {
                    effect.getEffect().applyInstantenousEffect(null, null, (LivingEntity) entityIn, effect.getAmplifier(), 0.5D);
                } else {
                    MobEffectInstance potion = new MobEffectInstance(effect.getEffect(), getEffectDuration(effect, stack) / 4, getEffectAmplifier(effect, stack), effect.isAmbient(), false);
                    living.addEffect(potion);
                }
                used = true;
            }
            if (entityIn instanceof Player && ((Player) entityIn).abilities.instabuild) {
                return;
            }
            if (used) {
                drain(stack, MB_PER_CYCLE, EXECUTE);
            }
        }
    }

    // region HELPERS
    @Override
    protected void setAttributesFromAugment(ItemStack container, CompoundTag augmentData) {

        CompoundTag subTag = container.getTagElement(TAG_PROPERTIES);
        if (subTag == null) {
            return;
        }
        setAttributeFromAugmentAdd(subTag, augmentData, TAG_AUGMENT_POTION_AMPLIFIER);
        setAttributeFromAugmentAdd(subTag, augmentData, TAG_AUGMENT_POTION_DURATION);

        super.setAttributesFromAugment(container, augmentData);
    }

    protected boolean useDelegate(ItemStack stack, Player player, InteractionHand hand) {

        if (Utils.isFakePlayer(player) || !player.isSecondaryUseActive()) {
            return false;
        }
        if (Utils.isServerWorld(player.level)) {
            FluidStack fluid = getFluid(stack);
            if (fluid != null && (fluid.getAmount() >= MB_PER_USE || player.abilities.instabuild)) {
                for (MobEffectInstance effect : PotionUtils.getAllEffects(fluid.getTag())) {
                    if (effect.getEffect().isInstantenous()) {
                        effect.getEffect().applyInstantenousEffect(null, null, player, getEffectAmplifier(effect, stack), 1.0D);
                    } else {
                        MobEffectInstance potion = new MobEffectInstance(effect.getEffect(), getEffectDuration(effect, stack), getEffectAmplifier(effect, stack), effect.isAmbient(), false);
                        player.addEffect(potion);
                    }
                }
                if (!player.abilities.instabuild) {
                    drain(stack, MB_PER_USE, EXECUTE);
                }
            }
        }
        player.swing(hand);
        stack.setPopTime(5);
        return true;
    }
    // endregion

    // region IColorableItem
    @Override
    public int getColor(ItemStack item, int colorIndex) {

        if (colorIndex == 0) {
            CompoundTag nbt = item.getTagElement("display");
            return nbt != null && nbt.contains("color", 99) ? nbt.getInt("color") : 0xFFFFFF;
        } else if (colorIndex == 2) {
            return getFluidAmount(item) > 0 ? getFluid(item).getFluid().getAttributes().getColor(getFluid(item)) : 0xFFFFFF;
        }
        return 0xFFFFFF;
    }
    // endregion

    // region IMultiModeItem
    @Override
    public void onModeChange(Player player, ItemStack stack) {

        player.level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, 0.6F + 0.2F * getMode(stack));
        ChatHelper.sendIndexedChatMessageToPlayer(player, new TranslatableComponent("info.thermal.infuser.mode." + getMode(stack)));
    }
    // endregion
}

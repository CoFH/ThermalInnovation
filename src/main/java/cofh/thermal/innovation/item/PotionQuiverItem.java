package cofh.thermal.innovation.item;

import cofh.core.item.FluidContainerItemAugmentable;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.ChatHelper;
import cofh.core.util.helpers.FluidHelper;
import cofh.lib.capability.CapabilityArchery;
import cofh.lib.capability.IArcheryAmmoItem;
import cofh.lib.fluid.FluidContainerItemWrapper;
import cofh.lib.fluid.IFluidContainerItem;
import cofh.lib.item.IColorableItem;
import cofh.lib.item.IMultiModeItem;
import cofh.lib.util.Utils;
import cofh.thermal.lib.common.ThermalConfig;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static cofh.lib.item.ContainerType.ITEM;
import static cofh.lib.util.Utils.getItemEnchantmentLevel;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.lib.util.helpers.ArcheryHelper.findArrows;
import static cofh.lib.util.helpers.AugmentableHelper.getPropertyWithDefault;
import static cofh.lib.util.helpers.AugmentableHelper.setAttributeFromAugmentAdd;
import static cofh.lib.util.helpers.ItemHelper.areItemStacksEqualIgnoreTags;
import static cofh.lib.util.helpers.StringHelper.*;
import static cofh.thermal.lib.common.ThermalAugmentRules.createAllowValidator;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

public class PotionQuiverItem extends FluidContainerItemAugmentable implements IColorableItem, IDyeableArmorItem, IMultiModeItem {

    protected static final int MB_PER_USE = 50;

    protected int arrowCapacity;

    public PotionQuiverItem(Properties builder, int fluidCapacity, int arrowCapacity) {

        this(builder, fluidCapacity, arrowCapacity, FluidHelper::hasPotionTag);

        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("color"), (stack, world, entity) -> (hasCustomColor(stack) ? 1.0F : 0));
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("arrows"), (stack, world, entity) -> getStoredArrows(stack) / (float) getMaxArrows(stack));
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("state"), (stack, world, entity) -> (getFluidAmount(stack) > 0 ? 0.5F : 0) + (getMode(stack) > 0 ? 0.25F : 0));
        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalConfig.toolAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_FLUID, TAG_AUGMENT_TYPE_POTION, TAG_AUGMENT_TYPE_FILTER);
    }

    public PotionQuiverItem(Properties builder, int fluidCapacity, int arrowCapacity, Predicate<FluidStack> validator) {

        super(builder, fluidCapacity, validator);
        this.arrowCapacity = arrowCapacity;
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {

        tooltip.add(getTextComponent("info.thermal.quiver.use").withStyle(TextFormatting.GRAY));
        tooltip.add(getTextComponent("info.thermal.quiver.use.sneak").withStyle(TextFormatting.DARK_GRAY));

        tooltip.add(getTextComponent("info.thermal.quiver.mode." + getMode(stack)).withStyle(TextFormatting.ITALIC));
        addIncrementModeChangeTooltip(stack, worldIn, tooltip, flagIn);

        tooltip.add(getTextComponent(localize("info.cofh.arrows") + ": " + (isCreative(stack, ITEM)
                ? localize("info.cofh.infinite")
                : getStoredArrows(stack) + " / " + format(getMaxArrows(stack)))));

        FluidStack fluid = getFluid(stack);
        List<EffectInstance> effects = new ArrayList<>();
        for (EffectInstance effect : PotionUtils.getAllEffects(fluid.getTag())) {
            effects.add(new EffectInstance(effect.getEffect(), getEffectDuration(effect, stack), getEffectAmplifier(effect, stack), effect.isAmbient(), effect.isVisible()));
        }
        potionTooltip(stack, worldIn, tooltip, flagIn, effects, 0.125F);
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {

        if (getFluidAmount(stack) <= 0) {
            return super.getRGBDurabilityForDisplay(stack);
        }
        return getFluid(stack).getFluid().getAttributes().getColor(getFluid(stack));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {

        return !oldStack.equals(newStack) && (slotChanged || !areItemStacksEqualIgnoreTags(oldStack, newStack, TAG_ARROWS, TAG_FLUID));
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {

        ItemStack stack = playerIn.getItemInHand(handIn);
        return useDelegate(stack, playerIn, handIn) ? ActionResult.success(stack) : ActionResult.pass(stack);
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {

        return useDelegate(stack, context.getPlayer(), context.getHand()) ? ActionResultType.SUCCESS : ActionResultType.PASS;
    }

    // region HELPERS
    protected void setAttributesFromAugment(ItemStack container, CompoundNBT augmentData) {

        CompoundNBT subTag = container.getTagElement(TAG_PROPERTIES);
        if (subTag == null) {
            return;
        }
        setAttributeFromAugmentAdd(subTag, augmentData, TAG_AUGMENT_POTION_AMPLIFIER);
        setAttributeFromAugmentAdd(subTag, augmentData, TAG_AUGMENT_POTION_DURATION);

        super.setAttributesFromAugment(container, augmentData);
    }

    protected boolean useDelegate(ItemStack stack, PlayerEntity player, Hand hand) {

        if (Utils.isFakePlayer(player)) {
            return false;
        }
        if (player.isSecondaryUseActive()) {
            ItemStack arrows = findArrows(player);
            if (!arrows.isEmpty() && arrows.getCount() < arrows.getMaxStackSize()) {
                arrows.grow(removeArrows(stack, arrows.getMaxStackSize() - arrows.getCount(), false));
            } else {
                arrows = new ItemStack(Items.ARROW, Math.min(getStoredArrows(stack), 64));
                if (Utils.addToPlayerInventory(player, arrows)) {
                    removeArrows(stack, arrows.getCount(), false);
                }
            }
        } else {
            if (player.abilities.instabuild) {
                putArrows(stack, getMaxArrows(stack), false);
            } else {
                ItemStack arrows = findArrows(player);
                arrows.shrink(putArrows(stack, arrows.getCount(), false));
            }
        }
        stack.setPopTime(5);
        return true;
    }

    protected int getStoredArrows(ItemStack stack) {

        return isCreative(stack, ITEM) ? getMaxArrows(stack) : stack.getOrCreateTag().getInt(TAG_ARROWS);
    }

    protected int getMaxArrows(ItemStack stack) {

        float base = getPropertyWithDefault(stack, TAG_AUGMENT_BASE_MOD, 1.0F);
        return getMaxStored(stack, Math.round(arrowCapacity * base));
    }

    protected int putArrows(ItemStack stack, int maxArrows, boolean simulate) {

        int stored = getStoredArrows(stack);
        int toAdd = Math.min(maxArrows, getMaxArrows(stack) - stored);

        if (!simulate && !isCreative(stack, ITEM)) {
            stored += toAdd;
            stack.getOrCreateTag().putInt(TAG_ARROWS, stored);
        }
        return toAdd;
    }

    protected int removeArrows(ItemStack stack, int maxArrows, boolean simulate) {

        if (isCreative(stack, ITEM)) {
            return maxArrows;
        }
        int stored = Math.min(stack.getOrCreateTag().getInt(TAG_ARROWS), getMaxArrows(stack));
        int toRemove = Math.min(maxArrows, stored);
        if (!simulate) {
            stored -= toRemove;
            stack.getOrCreateTag().putInt(TAG_ARROWS, stored);
        }
        return toRemove;
    }
    // endregion

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {

        return new PotionQuiverItemWrapper(stack, this);
    }

    // region IAugmentableItem
    @Override
    public void updateAugmentState(ItemStack container, List<ItemStack> augments) {

        super.updateAugmentState(container, augments);

        int arrowExcess = getStoredArrows(container) - getMaxArrows(container);
        if (arrowExcess > 0) {
            removeArrows(container, arrowExcess, false);
        }
    }
    // endregion

    // region IColorableItem
    @Override
    public int getColor(ItemStack item, int colorIndex) {

        if (colorIndex == 0) {
            CompoundNBT nbt = item.getTagElement("display");
            return nbt != null && nbt.contains("color", 99) ? nbt.getInt("color") : 0xFFFFFF;
        } else if (colorIndex == 2) {
            return getFluidAmount(item) > 0 ? getFluid(item).getFluid().getAttributes().getColor(getFluid(item)) : 0xFFFFFF;
        }
        return 0xFFFFFF;
    }
    // endregion

    // region IMultiModeItem
    @Override
    public void onModeChange(PlayerEntity player, ItemStack stack) {

        player.level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.4F, 0.6F + 0.2F * getMode(stack));
        ChatHelper.sendIndexedChatMessageToPlayer(player, new TranslationTextComponent("info.thermal.quiver.mode." + getMode(stack)));
    }
    // endregion

    // region CAPABILITY WRAPPER
    protected class PotionQuiverItemWrapper extends FluidContainerItemWrapper implements IArcheryAmmoItem {

        private final LazyOptional<IArcheryAmmoItem> holder = LazyOptional.of(() -> this);

        PotionQuiverItemWrapper(ItemStack containerIn, IFluidContainerItem itemIn) {

            super(containerIn, itemIn);
        }

        @Override
        public void onArrowLoosed(PlayerEntity shooter) {

            if (shooter != null) {
                if (!shooter.abilities.instabuild) {
                    removeArrows(container, 1, false);
                    drain(MB_PER_USE, getMode(container) == 1 ? EXECUTE : SIMULATE);
                }
            }
        }

        @Override
        public AbstractArrowEntity createArrowEntity(World world, PlayerEntity shooter) {

            FluidStack fluid = getFluid(container);
            ItemStack arrowStack;

            if (getMode(container) == 1 && fluid != null && fluid.getAmount() >= MB_PER_USE) {
                if (fluid.getTag().contains("CustomPotionEffects")) {
                    List<EffectInstance> effects = new ArrayList<>();
                    for (EffectInstance effect : PotionUtils.getAllEffects(fluid.getTag())) {
                        effects.add(new EffectInstance(effect.getEffect(), getEffectDuration(effect, container), getEffectAmplifier(effect, container), effect.isAmbient(), effect.isVisible()));
                    }
                    arrowStack = PotionUtils.setCustomEffects(new ItemStack(Items.TIPPED_ARROW), effects);
                } else {
                    arrowStack = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), PotionUtils.getPotion(fluid.getTag()));
                }
                return ((TippedArrowItem) arrowStack.getItem()).createArrow(world, arrowStack, shooter);
            }
            arrowStack = new ItemStack(Items.ARROW);
            return ((ArrowItem) arrowStack.getItem()).createArrow(world, arrowStack, shooter);
        }

        @Override
        public boolean isEmpty(PlayerEntity shooter) {

            if (isCreative(container, ITEM) || (shooter != null && shooter.abilities.instabuild)) {
                return false;
            }
            return getStoredArrows(container) <= 0;
        }

        @Override
        public boolean isInfinite(ItemStack bow, PlayerEntity shooter) {

            return shooter != null && shooter.abilities.instabuild || getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow) > 0;
        }

        // region ICapabilityProvider
        @Override
        @Nonnull
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

            if (cap == CapabilityArchery.AMMO_ITEM_CAPABILITY) {
                return CapabilityArchery.AMMO_ITEM_CAPABILITY.orEmpty(cap, holder);
            }
            return super.getCapability(cap, side);
        }
        // endregion
    }
    // endregion
}

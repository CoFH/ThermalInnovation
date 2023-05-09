package cofh.thermal.innovation.item;

import cofh.core.capability.CapabilityArchery;
import cofh.core.item.IMultiModeItem;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.ChatHelper;
import cofh.core.util.helpers.FluidHelper;
import cofh.lib.api.capability.IArcheryAmmoItem;
import cofh.lib.api.item.IColorableItem;
import cofh.lib.api.item.IFluidContainerItem;
import cofh.lib.fluid.FluidContainerItemWrapper;
import cofh.lib.util.Utils;
import cofh.thermal.core.config.ThermalCoreConfig;
import cofh.thermal.lib.item.FluidContainerItemAugmentable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static cofh.core.util.helpers.ArcheryHelper.findArrows;
import static cofh.core.util.helpers.AugmentableHelper.getPropertyWithDefault;
import static cofh.core.util.helpers.AugmentableHelper.setAttributeFromAugmentAdd;
import static cofh.core.util.helpers.ItemHelper.areItemStacksEqualIgnoreTags;
import static cofh.lib.api.ContainerType.ITEM;
import static cofh.lib.util.Utils.getItemEnchantmentLevel;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.lib.util.helpers.StringHelper.*;
import static cofh.thermal.lib.common.ThermalAugmentRules.createAllowValidator;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

public class PotionQuiverItem extends FluidContainerItemAugmentable implements IColorableItem, DyeableLeatherItem, IMultiModeItem {

    protected static final int MB_PER_USE = 50;

    protected int arrowCapacity;

    public PotionQuiverItem(Properties builder, int fluidCapacity, int arrowCapacity) {

        this(builder, fluidCapacity, arrowCapacity, FluidHelper::hasPotionTag);

        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("color"), (stack, world, entity, seed) -> (hasCustomColor(stack) ? 1.0F : 0));
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("arrows"), (stack, world, entity, seed) -> getStoredArrows(stack) / (float) getMaxArrows(stack));
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("state"), (stack, world, entity, seed) -> (getFluidAmount(stack) > 0 ? 0.5F : 0) + (getMode(stack) > 0 ? 0.25F : 0));
        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalCoreConfig.toolAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_FLUID, TAG_AUGMENT_TYPE_POTION, TAG_AUGMENT_TYPE_FILTER);
    }

    public PotionQuiverItem(Properties builder, int fluidCapacity, int arrowCapacity, Predicate<FluidStack> validator) {

        super(builder, fluidCapacity, validator);
        this.arrowCapacity = arrowCapacity;
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        tooltip.add(getTextComponent("info.thermal.quiver.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(getTextComponent("info.thermal.quiver.use.sneak").withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(getTextComponent("info.thermal.quiver.mode." + getMode(stack)).withStyle(ChatFormatting.ITALIC));
        addModeChangeTooltip(this, stack, worldIn, tooltip, flagIn);

        tooltip.add(getTextComponent(localize("info.cofh.arrows") + ": " + (isCreative(stack, ITEM)
                ? localize("info.cofh.infinite")
                : getStoredArrows(stack) + " / " + format(getMaxArrows(stack)))));

        FluidStack fluid = getFluid(stack);
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance effect : PotionUtils.getAllEffects(fluid.getTag())) {
            effects.add(new MobEffectInstance(effect.getEffect(), getEffectDuration(effect, stack), getEffectAmplifier(effect, stack), effect.isAmbient(), effect.isVisible()));
        }
        potionTooltip(stack, worldIn, tooltip, flagIn, effects, 0.125F);
    }

    @Override
    public int getBarColor(ItemStack stack) {

        if (getFluidAmount(stack) <= 0) {
            return super.getBarColor(stack);
        }
        return getFluid(stack).getFluid().getAttributes().getColor(getFluid(stack));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {

        return !oldStack.equals(newStack) && (slotChanged || !areItemStacksEqualIgnoreTags(oldStack, newStack, TAG_ARROWS, TAG_FLUID));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {

        ItemStack stack = playerIn.getItemInHand(handIn);
        return useDelegate(stack, playerIn, handIn) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
    }

    //    @Override
    //    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
    //
    //        return useDelegate(stack, context.getPlayer(), context.getHand()) ? ActionResultType.SUCCESS : ActionResultType.PASS;
    //    }

    // region HELPERS
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
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {

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
        ChatHelper.sendIndexedChatMessageToPlayer(player, new TranslatableComponent("info.thermal.quiver.mode." + getMode(stack)));
    }
    // endregion

    // region CAPABILITY WRAPPER
    protected class PotionQuiverItemWrapper extends FluidContainerItemWrapper implements IArcheryAmmoItem {

        private final LazyOptional<IArcheryAmmoItem> holder = LazyOptional.of(() -> this);

        PotionQuiverItemWrapper(ItemStack containerIn, IFluidContainerItem itemIn) {

            super(containerIn, itemIn);
        }

        @Override
        public void onArrowLoosed(Player shooter) {

            if (shooter != null) {
                if (!shooter.abilities.instabuild) {
                    removeArrows(container, 1, false);
                    drain(MB_PER_USE, getMode(container) == 1 ? EXECUTE : SIMULATE);
                }
            }
        }

        @Override
        public AbstractArrow createArrowEntity(Level world, Player shooter) {

            FluidStack fluid = getFluid(container);
            ItemStack arrowStack;

            if (getMode(container) == 1 && fluid != null && fluid.getAmount() >= MB_PER_USE) {
                List<MobEffectInstance> effects = new ArrayList<>();
                for (MobEffectInstance effect : PotionUtils.getAllEffects(fluid.getTag())) {
                    effects.add(new MobEffectInstance(effect.getEffect(), getEffectDuration(effect, container), getEffectAmplifier(effect, container), effect.isAmbient(), effect.isVisible()));
                }
                arrowStack = PotionUtils.setCustomEffects(new ItemStack(Items.TIPPED_ARROW), effects);
                return ((TippedArrowItem) arrowStack.getItem()).createArrow(world, arrowStack, shooter);
            }
            arrowStack = new ItemStack(Items.ARROW);
            return ((ArrowItem) arrowStack.getItem()).createArrow(world, arrowStack, shooter);
        }

        @Override
        public boolean isEmpty(Player shooter) {

            if (isCreative(container, ITEM) || (shooter != null && shooter.abilities.instabuild)) {
                return false;
            }
            return getStoredArrows(container) <= 0;
        }

        @Override
        public boolean isInfinite(ItemStack bow, Player shooter) {

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

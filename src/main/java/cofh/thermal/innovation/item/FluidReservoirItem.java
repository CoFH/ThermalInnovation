package cofh.thermal.innovation.item;

import cofh.core.compat.curios.CuriosProxy;
import cofh.core.item.IMultiModeItem;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.FluidHelper;
import cofh.lib.api.item.IColorableItem;
import cofh.lib.util.Utils;
import cofh.thermal.core.config.ThermalCoreConfig;
import cofh.thermal.lib.item.FluidContainerItemAugmentable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static cofh.lib.util.Constants.BUCKET_VOLUME;
import static cofh.lib.util.constants.NBTTags.TAG_AUGMENT_TYPE_FLUID;
import static cofh.lib.util.constants.NBTTags.TAG_AUGMENT_TYPE_UPGRADE;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.thermal.lib.common.ThermalAugmentRules.createAllowValidator;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

public class FluidReservoirItem extends FluidContainerItemAugmentable implements IColorableItem, DyeableLeatherItem, IMultiModeItem {

    protected static final int FILL = 0;
    protected static final int EMPTY = 1;

    public FluidReservoirItem(Properties builder, int fluidCapacity) {

        super(builder, fluidCapacity);

        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalCoreConfig.storageAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_FLUID);
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        tooltip.add(isActive(stack)
                ? Component.translatable("info.cofh_use_sneak_deactivate").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("info.cofh.use_sneak_activate").withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(getTextComponent("info.thermal.reservoir.mode." + getMode(stack)).withStyle(ChatFormatting.ITALIC));
        addModeChangeTooltip(this, stack, worldIn, tooltip, flagIn);

        super.tooltipDelegate(stack, worldIn, tooltip, flagIn);
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

    @Override
    public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {

        if (Utils.isClientWorld(worldIn) || Utils.isFakePlayer(entityIn) || !isActive(stack)) {
            return;
        }
        Player player = (Player) entityIn;
        for (ItemStack equip : player.getAllSlots()) {
            if (stack.isEmpty() || equip.equals(stack)) {
                continue;
            }
            equip.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)
                    .ifPresent(c -> this.drainInternal(stack, c.fill(new FluidStack(getFluid(stack), Math.min(getFluidAmount(stack), BUCKET_VOLUME)), EXECUTE), player.abilities.instabuild ? SIMULATE : EXECUTE));
        }
        CuriosProxy.getAllWorn(player).ifPresent(c -> {
            for (int i = 0; i < c.getSlots(); ++i) {
                ItemStack equip = c.getStackInSlot(i);
                if (stack.isEmpty() || equip.equals(stack)) {
                    continue;
                }
                equip.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)
                        .ifPresent(f -> this.drainInternal(stack, f.fill(new FluidStack(getFluid(stack), Math.min(getFluidAmount(stack), BUCKET_VOLUME)), EXECUTE), player.abilities.instabuild ? SIMULATE : EXECUTE));
            }
        });
    }

    // region HELPERS
    protected boolean useDelegate(ItemStack stack, Player player, InteractionHand hand) {

        if (Utils.isFakePlayer(player)) {
            return false;
        }
        if (player.isSecondaryUseActive()) {
            setActive(stack, !isActive(stack));
            player.level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2F, isActive(stack) ? 0.8F : 0.5F);
            return true;
        }
        if (getMode(stack) == FILL) {
            return doBucketFill(stack, player, hand);
        }
        if (getMode(stack) == EMPTY) {
            return doBucketEmpty(stack, player, hand);
        }
        return false;
    }

    protected boolean doBucketFill(ItemStack stack, @Nonnull Player player, InteractionHand hand) {

        if (getSpace(stack) < BUCKET_VOLUME) {
            return false;
        }
        Level world = player.getCommandSenderWorld();
        BlockHitResult traceResult = getPlayerPOVHitResult(world, player, ClipContext.Fluid.SOURCE_ONLY);

        if (traceResult.getType() == HitResult.Type.MISS) {
            return false;
        }
        if (traceResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = traceResult.getBlockPos();
        Direction sideHit = traceResult.getDirection();
        if (world.mayInteract(player, pos)) {
            if (player.mayUseItemAt(pos, sideHit, stack)) {
                FluidActionResult result = FluidUtil.tryPickUpFluid(stack, player, world, pos, sideHit);
                if (result.isSuccess()) {
                    if (!player.abilities.instabuild) {
                        player.setItemInHand(hand, result.getResult());
                        player.awardStat(Stats.ITEM_USED.get(this));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean doBucketEmpty(ItemStack stack, @Nonnull Player player, InteractionHand hand) {

        if (getFluidAmount(stack) < BUCKET_VOLUME) {
            return false;
        }
        Level world = player.getCommandSenderWorld();
        BlockHitResult traceResult = getPlayerPOVHitResult(world, player, ClipContext.Fluid.NONE);

        if (traceResult.getType() == HitResult.Type.MISS) {
            return false;
        }
        if (traceResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = traceResult.getBlockPos();
        Direction sideHit = traceResult.getDirection();
        if (world.mayInteract(player, pos)) {
            BlockPos targetPos = pos.relative(sideHit);
            if (player.mayUseItemAt(targetPos, sideHit.getOpposite(), stack)) {
                FluidActionResult result = FluidUtil.tryPlaceFluid(player, world, hand, targetPos, stack, new FluidStack(getFluid(stack), BUCKET_VOLUME));
                if (result.isSuccess()) {
                    if (!player.abilities.instabuild) {
                        player.setItemInHand(hand, result.getResult());
                        player.awardStat(Stats.ITEM_USED.get(this));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    protected FluidStack drainInternal(ItemStack container, int maxDrain, FluidAction action) {

        return super.drain(container, maxDrain, action);
    }
    // endregion

    // region IFluidContainerItem
    // TODO: Determine if there should be limitations.
    @Override
    public int fill(ItemStack container, FluidStack resource, FluidAction action) {

        if (getMode(container) != FILL) {
            return 0;
        }
        return super.fill(container, resource, action);
    }

    @Override
    public FluidStack drain(ItemStack container, int maxDrain, FluidAction action) {

        if (getMode(container) != EMPTY) {
            return FluidStack.EMPTY;
        }
        return super.drain(container, maxDrain, action);
    }
    // endregion

    // region IMultiModeItem
    @Override
    public void onModeChange(Player player, ItemStack stack) {

        player.level.playSound(null, player.blockPosition(), getMode(stack) == FILL ? SoundEvents.BOTTLE_FILL : SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.6F, 1.0F);
        ProxyUtils.setOverlayMessage(player, Component.translatable("info.thermal.reservoir.mode." + getMode(stack)));
    }
    // endregion

    // region IColorableItem
    @Override
    public int getColor(ItemStack item, int colorIndex) {

        if (colorIndex == 1) {
            CompoundTag nbt = item.getTagElement("display");
            return nbt != null && nbt.contains("color", 99) ? nbt.getInt("color") : 0xFFFFFF;
        } else if (colorIndex == 2) {
            return getFluidAmount(item) > 0 ? FluidHelper.color(getFluid(item)) : 0xFFFFFF;
        }
        return 0xFFFFFF;
    }
    // endregion
}

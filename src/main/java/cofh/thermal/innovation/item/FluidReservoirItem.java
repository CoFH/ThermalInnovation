package cofh.thermal.innovation.item;

import cofh.core.compat.curios.CuriosProxy;
import cofh.core.item.FluidContainerItemAugmentable;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.ChatHelper;
import cofh.lib.item.IMultiModeItem;
import cofh.lib.util.Utils;
import cofh.thermal.lib.common.ThermalConfig;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static cofh.lib.util.constants.Constants.BUCKET_VOLUME;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.thermal.lib.common.ThermalAugmentRules.FLUID_STORAGE_VALIDATOR;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

public class FluidReservoirItem extends FluidContainerItemAugmentable implements IMultiModeItem {

    protected static final int FILL = 0;
    protected static final int EMPTY = 1;

    public FluidReservoirItem(Properties builder, int fluidCapacity) {

        super(builder, fluidCapacity);

        numSlots = () -> ThermalConfig.storageAugments;
        augValidator = FLUID_STORAGE_VALIDATOR;
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {

        tooltip.add(isActive(stack)
                ? new TranslationTextComponent("info.cofh_use_sneak_deactivate").withStyle(TextFormatting.DARK_GRAY)
                : new TranslationTextComponent("info.cofh.use_sneak_activate").withStyle(TextFormatting.DARK_GRAY));

        tooltip.add(getTextComponent("info.thermal.reservoir.mode." + getMode(stack)).withStyle(TextFormatting.ITALIC));
        addIncrementModeChangeTooltip(stack, worldIn, tooltip, flagIn);

        super.tooltipDelegate(stack, worldIn, tooltip, flagIn);
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

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {

        if (Utils.isClientWorld(worldIn) || Utils.isFakePlayer(entityIn) || !isActive(stack)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entityIn;
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
    protected boolean useDelegate(ItemStack stack, PlayerEntity player, Hand hand) {

        if (Utils.isFakePlayer(player)) {
            return false;
        }
        if (player.isSecondaryUseActive()) {
            setActive(stack, !isActive(stack));
            player.level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.2F, isActive(stack) ? 0.8F : 0.5F);
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

    protected boolean doBucketFill(ItemStack stack, @Nonnull PlayerEntity player, Hand hand) {

        if (getSpace(stack) < BUCKET_VOLUME) {
            return false;
        }
        World world = player.getCommandSenderWorld();
        BlockRayTraceResult traceResult = getPlayerPOVHitResult(world, player, RayTraceContext.FluidMode.SOURCE_ONLY);

        if (traceResult.getType() == RayTraceResult.Type.MISS) {
            return false;
        }
        if (traceResult.getType() != RayTraceResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = traceResult.getBlockPos();
        Direction sideHit = traceResult.getDirection();
        if (world.mayInteract(player, pos)) {
            if (player.mayUseItemAt(pos, sideHit, stack)) {
                FluidActionResult result = FluidUtil.tryPickUpFluid(stack, player, world, pos, sideHit);
                if (result.isSuccess() && !player.abilities.instabuild) {
                    player.setItemInHand(hand, result.getResult());
                    player.awardStat(Stats.ITEM_USED.get(this));
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean doBucketEmpty(ItemStack stack, @Nonnull PlayerEntity player, Hand hand) {

        if (getFluidAmount(stack) < BUCKET_VOLUME) {
            return false;
        }
        World world = player.getCommandSenderWorld();
        BlockRayTraceResult traceResult = getPlayerPOVHitResult(world, player, RayTraceContext.FluidMode.SOURCE_ONLY);

        if (traceResult.getType() == RayTraceResult.Type.MISS) {
            return false;
        }
        if (traceResult.getType() != RayTraceResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = traceResult.getBlockPos();
        Direction sideHit = traceResult.getDirection();
        if (world.mayInteract(player, pos)) {
            BlockPos targetPos = pos.relative(sideHit);
            if (player.mayUseItemAt(targetPos, sideHit.getOpposite(), stack)) {
                FluidActionResult result = FluidUtil.tryPlaceFluid(player, world, hand, targetPos, stack, new FluidStack(getFluid(stack), BUCKET_VOLUME));
                if (result.isSuccess() && !player.abilities.instabuild) {
                    player.setItemInHand(hand, result.getResult());
                    player.awardStat(Stats.ITEM_USED.get(this));
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
    public void onModeChange(PlayerEntity player, ItemStack stack) {

        player.level.playSound(null, player.blockPosition(), getMode(stack) == FILL ? SoundEvents.BOTTLE_FILL : SoundEvents.BOTTLE_EMPTY, SoundCategory.PLAYERS, 0.6F, 1.0F);
        ChatHelper.sendIndexedChatMessageToPlayer(player, new TranslationTextComponent("info.thermal.reservoir.mode." + getMode(stack)));
    }
    // endregion
}

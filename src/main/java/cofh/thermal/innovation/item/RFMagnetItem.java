package cofh.thermal.innovation.item;

import cofh.core.util.ProxyUtils;
import cofh.core.util.filter.EmptyFilter;
import cofh.core.util.filter.FilterRegistry;
import cofh.core.util.helpers.ChatHelper;
import cofh.lib.item.IColorableItem;
import cofh.lib.item.IMultiModeItem;
import cofh.lib.util.RayTracer;
import cofh.lib.util.Utils;
import cofh.lib.util.filter.IFilter;
import cofh.lib.util.filter.IFilterableItem;
import cofh.lib.util.helpers.FilterHelper;
import cofh.thermal.lib.common.ThermalConfig;
import cofh.thermal.lib.item.EnergyContainerItemAugmentable;
import cofh.thermal.lib.item.IFlexibleEnergyContainerItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.IDyeableArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import static cofh.lib.util.constants.NBTTags.*;
import static cofh.lib.util.helpers.AugmentableHelper.*;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.thermal.core.init.TCoreSounds.SOUND_MAGNET;
import static cofh.thermal.lib.common.ThermalAugmentRules.createAllowValidator;

public class RFMagnetItem extends EnergyContainerItemAugmentable implements IColorableItem, IDyeableArmorItem, IFilterableItem, IMultiModeItem, IFlexibleEnergyContainerItem {

    protected static final int MAP_CAPACITY = 128;
    protected static final WeakHashMap<ItemStack, IFilter> FILTERS = new WeakHashMap<>(MAP_CAPACITY);

    protected static final int RADIUS = 4;
    protected static final int REACH = 64;

    protected static final int PICKUP_DELAY = 32;

    protected static final int ENERGY_PER_ITEM = 25;
    protected static final int ENERGY_PER_USE = 200;

    public RFMagnetItem(Properties builder, int maxEnergy, int maxTransfer) {

        super(builder, maxEnergy, maxTransfer);

        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("color"), (stack, world, entity) -> (hasCustomColor(stack) ? 1.0F : 0));
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("state"), (stack, world, entity) -> (getEnergyStored(stack) > 0 ? 0.5F : 0) + (getMode(stack) > 0 ? 0.25F : 0));
        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalConfig.toolAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_RF, TAG_AUGMENT_TYPE_AREA_EFFECT, TAG_AUGMENT_TYPE_FILTER);
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {

        tooltip.add(getTextComponent("info.thermal.magnet.use").withStyle(TextFormatting.GRAY));
        if (FilterHelper.hasFilter(stack)) {
            tooltip.add(getTextComponent("info.thermal.magnet.use.sneak").withStyle(TextFormatting.DARK_GRAY));
        }
        tooltip.add(getTextComponent("info.thermal.magnet.mode." + getMode(stack)).withStyle(TextFormatting.ITALIC));
        addModeChangeTooltip(this, stack, worldIn, tooltip, flagIn);

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

        if (!Utils.timeCheckQuarter(worldIn)) {
            return;
        }
        if (Utils.isClientWorld(worldIn) || Utils.isFakePlayer(entityIn) || getMode(stack) <= 0) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entityIn;

        if (getEnergyStored(stack) < ENERGY_PER_ITEM && !player.abilities.instabuild) {
            return;
        }
        int radius = getRadius(stack);
        int radSq = radius * radius;

        AxisAlignedBB area = new AxisAlignedBB(player.blockPosition().offset(-radius, -radius, -radius), player.blockPosition().offset(1 + radius, 1 + radius, 1 + radius));
        List<ItemEntity> items = worldIn.getEntitiesOfClass(ItemEntity.class, area, EntityPredicates.ENTITY_STILL_ALIVE);

        if (Utils.isClientWorld(worldIn)) {
            for (ItemEntity item : items) {
                if (item.hasPickUpDelay() || item.getPersistentData().getBoolean(TAG_CONVEYOR_COMPAT)) {
                    continue;
                }
                if (item.position().distanceToSqr(player.position()) <= radSq) {
                    worldIn.addParticle(RedstoneParticleData.REDSTONE, item.getX(), item.getY(), item.getZ(), 0, 0, 0);
                }
            }
        } else {
            Predicate<ItemStack> filterRules = getFilter(stack).getItemRules();
            int itemCount = 0;
            for (ItemEntity item : items) {
                if (item.hasPickUpDelay() || item.getPersistentData().getBoolean(TAG_CONVEYOR_COMPAT) || !filterRules.test(item.getItem())) {
                    continue;
                }
                if (item.getThrower() == null || !item.getThrower().equals(player.getUUID()) || item.age >= PICKUP_DELAY) {
                    if (item.position().distanceToSqr(player.position()) <= radSq) {
                        item.setPos(player.getX(), player.getY(), player.getZ());
                        item.setPickUpDelay(0);
                        ++itemCount;
                    }
                }
            }
            if (!player.abilities.instabuild) {
                extractEnergy(stack, ENERGY_PER_ITEM * itemCount, false);
            }
        }
    }

    // region HELPERS
    protected boolean useDelegate(ItemStack stack, PlayerEntity player, Hand hand) {

        if (Utils.isFakePlayer(player)) {
            return false;
        }
        if (player.isSecondaryUseActive() && hand == Hand.MAIN_HAND) {
            if (player instanceof ServerPlayerEntity && FilterHelper.hasFilter(stack)) {
                NetworkHooks.openGui((ServerPlayerEntity) player, getFilter(stack));
                return true;
            }
            return false;
        } else if (getEnergyStored(stack) >= ENERGY_PER_USE || player.abilities.instabuild) {
            BlockRayTraceResult traceResult = RayTracer.retrace(player, REACH);
            if (traceResult.getType() != RayTraceResult.Type.BLOCK) {
                return false;
            }
            int radius = getRadius(stack);
            int radSq = radius * radius;

            World world = player.getCommandSenderWorld();
            BlockPos pos = traceResult.getBlockPos();

            AxisAlignedBB area = new AxisAlignedBB(pos.offset(-radius, -radius, -radius), pos.offset(1 + radius, 1 + radius, 1 + radius));
            List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, area, EntityPredicates.ENTITY_STILL_ALIVE);

            if (Utils.isClientWorld(world)) {
                for (ItemEntity item : items) {
                    if (item.position().distanceToSqr(traceResult.getLocation()) <= radSq) {
                        world.addParticle(RedstoneParticleData.REDSTONE, item.getX(), item.getY(), item.getZ(), 0, 0, 0);
                    }
                }
            } else {
                Predicate<ItemStack> filterRules = getFilter(stack).getItemRules();
                int itemCount = 0;
                for (ItemEntity item : items) {
                    if (item.hasPickUpDelay() || item.getPersistentData().getBoolean(TAG_CONVEYOR_COMPAT) || !filterRules.test(item.getItem())) {
                        continue;
                    }
                    if (item.position().distanceToSqr(traceResult.getLocation()) <= radSq) {
                        item.setPos(player.getX(), player.getY(), player.getZ());
                        item.setPickUpDelay(0);
                        ++itemCount;
                    }
                }
                if (!player.abilities.instabuild && itemCount > 0) {
                    extractEnergy(stack, ENERGY_PER_USE + ENERGY_PER_ITEM * itemCount, false);
                }
            }
            player.swing(hand);
            stack.setPopTime(5);
            player.level.playSound(null, player.blockPosition(), SOUND_MAGNET, SoundCategory.PLAYERS, 0.4F, 1.0F);
        }
        return true;
    }

    protected int getRadius(ItemStack stack) {

        float base = getPropertyWithDefault(stack, TAG_AUGMENT_BASE_MOD, 1.0F);
        float mod = getPropertyWithDefault(stack, TAG_AUGMENT_RADIUS, 1.0F);
        return Math.round(RADIUS + mod * base);
    }

    @Override
    protected void setAttributesFromAugment(ItemStack container, CompoundNBT augmentData) {

        CompoundNBT subTag = container.getTagElement(TAG_PROPERTIES);
        if (subTag == null) {
            return;
        }
        setAttributeFromAugmentAdd(subTag, augmentData, TAG_AUGMENT_RADIUS);

        setAttributeFromAugmentString(subTag, augmentData, TAG_FILTER_TYPE);

        super.setAttributesFromAugment(container, augmentData);
    }
    // endregion

    // region IAugmentableItem
    @Override
    public void updateAugmentState(ItemStack container, List<ItemStack> augments) {

        super.updateAugmentState(container, augments);

        if (!FilterHelper.hasFilter(container)) {
            container.getOrCreateTag().remove(TAG_FILTER);
        }
        FILTERS.remove(container);
    }
    // endregion

    // region IFilterable
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

        player.level.playSound(null, player.blockPosition(), SOUND_MAGNET, SoundCategory.PLAYERS, 0.4F, 0.8F + 0.4F * getMode(stack));
        ChatHelper.sendIndexedChatMessageToPlayer(player, new TranslationTextComponent("info.thermal.magnet.mode." + getMode(stack)));
    }
    // endregion
}

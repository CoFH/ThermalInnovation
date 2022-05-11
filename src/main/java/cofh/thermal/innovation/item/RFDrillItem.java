package cofh.thermal.innovation.item;

import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.ChatHelper;
import cofh.lib.capability.CapabilityAreaEffect;
import cofh.lib.capability.IAreaEffect;
import cofh.lib.energy.EnergyContainerItemWrapper;
import cofh.lib.energy.IEnergyContainerItem;
import cofh.lib.item.IColorableItem;
import cofh.lib.item.IMultiModeItem;
import cofh.lib.util.Utils;
import cofh.lib.util.helpers.AreaEffectHelper;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.core.config.ThermalCoreConfig;
import cofh.thermal.lib.item.EnergyContainerItemAugmentable;
import cofh.thermal.lib.item.IFlexibleEnergyContainerItem;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static cofh.lib.util.constants.NBTTags.*;
import static cofh.lib.util.helpers.AugmentableHelper.getPropertyWithDefault;
import static cofh.lib.util.helpers.AugmentableHelper.setAttributeFromAugmentAdd;
import static cofh.thermal.lib.common.ThermalAugmentRules.createAllowValidator;
import static net.minecraftforge.common.ToolActions.PICKAXE_DIG;
import static net.minecraftforge.common.ToolActions.SHOVEL_DIG;

public class RFDrillItem extends EnergyContainerItemAugmentable implements IColorableItem, DyeableLeatherItem, IMultiModeItem, IFlexibleEnergyContainerItem {

    protected static final Set<ToolAction> DEFAULT_DRILL_ACTIONS = toolActions(PICKAXE_DIG, SHOVEL_DIG);
    protected static final Set<Material> MATERIALS = new ObjectOpenHashSet<>();
    protected static final Set<Enchantment> VALID_ENCHANTS = new ObjectOpenHashSet<>();

    public static final int ENERGY_PER_USE = 200;

    static {
        MATERIALS.add(Material.HEAVY_METAL);
        MATERIALS.add(Material.METAL);
        MATERIALS.add(Material.STONE);
    }

    public static void setupEnchants() {

        VALID_ENCHANTS.add(Enchantments.BLOCK_EFFICIENCY);
        VALID_ENCHANTS.add(Enchantments.SILK_TOUCH);
        VALID_ENCHANTS.add(Enchantments.BLOCK_FORTUNE);
    }

    public RFDrillItem(Properties builder, int maxEnergy, int maxTransfer) {

        super(builder, maxEnergy, maxTransfer);

        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("color"), (stack, world, entity, seed) -> (hasCustomColor(stack) ? 1.0F : 0));
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("state"), (stack, world, entity, seed) -> (getEnergyStored(stack) > 0 ? 0.5F : 0) + (isActive(stack) ? 0.25F : 0));
        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalCoreConfig.toolAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_RF, TAG_AUGMENT_TYPE_AREA_EFFECT);
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        int radius = getMode(stack) * 2 + 1;
        if (radius <= 1) {
            tooltip.add(new TranslatableComponent("info.cofh.single_block").withStyle(ChatFormatting.ITALIC));
        } else {
            tooltip.add(new TranslatableComponent("info.cofh.area").append(": " + radius + "x" + radius).withStyle(ChatFormatting.ITALIC));
        }
        if (getNumModes(stack) > 1) {
            addModeChangeTooltip(this, stack, worldIn, tooltip, flagIn);
        }
        super.tooltipDelegate(stack, worldIn, tooltip, flagIn);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {

        return super.canApplyAtEnchantingTable(stack, enchantment) || VALID_ENCHANTS.contains(enchantment);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {

        return MATERIALS.contains(state.getMaterial()) || state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL) ? getEfficiency(stack) : super.getDestroySpeed(stack, state);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {

        Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();
        if (slot == EquipmentSlot.MAINHAND) {
            float damage = getAttackDamage(stack);
            float speed = getAttackSpeed(stack);
            if (damage != 0.0F) {
                multimap.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", damage, AttributeModifier.Operation.ADDITION));
            }
            if (speed != 0.0F) {
                multimap.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", speed, AttributeModifier.Operation.ADDITION));
            }
        }
        return multimap;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {

        return DEFAULT_DRILL_ACTIONS.contains(toolAction);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {

        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return TierSortingRegistry.isCorrectTierForDrops(Tiers.values()[MathHelper.clamp(getHarvestLevel(stack), 0, 4)], state);
        }
        return false;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (attacker instanceof Player && !((Player) attacker).abilities.instabuild) {
            extractEnergy(stack, getEnergyPerUse(stack) * 2, false);
        }
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving) {

        if (Utils.isServerWorld(worldIn) && state.getDestroySpeed(worldIn, pos) != 0.0F) {
            if (entityLiving instanceof Player && !((Player) entityLiving).abilities.instabuild) {
                extractEnergy(stack, getEnergyPerUse(stack), false);
            }
        }
        return true;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {

        setActive(stack, entity);
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {

        if (!hasActiveTag(stack)) {
            return;
        }
        long activeTime = stack.getOrCreateTag().getLong(TAG_ACTIVE);

        if (entityIn.level.getGameTime() > activeTime) {
            stack.getOrCreateTag().remove(TAG_ACTIVE);
        }
    }

    // region HELPERS
    @Override
    protected void setAttributesFromAugment(ItemStack container, CompoundTag augmentData) {

        CompoundTag subTag = container.getTagElement(TAG_PROPERTIES);
        if (subTag == null) {
            return;
        }
        setAttributeFromAugmentAdd(subTag, augmentData, TAG_AUGMENT_RADIUS);

        super.setAttributesFromAugment(container, augmentData);
    }

    protected boolean hasEnergy(ItemStack stack) {

        return getEnergyStored(stack) >= getEnergyPerUse(stack);
    }

    protected float getAttackDamage(ItemStack stack) {

        return hasEnergy(stack) ? 2.0F + getBaseMod(stack) : 0.0F;
    }

    protected float getAttackSpeed(ItemStack stack) {

        return hasEnergy(stack) ? -2.4F + getBaseMod(stack) / 10 : -3.0F;
    }

    protected float getEfficiency(ItemStack stack) {

        return hasEnergy(stack) ? 5.0F + getBaseMod(stack) : 1.0F;
    }

    protected int getEnergyPerUse(ItemStack stack) {

        return ENERGY_PER_USE;
    }

    protected int getHarvestLevel(ItemStack stack) {

        return hasEnergy(stack) ? Math.max(2, (int) getBaseMod(stack)) : -1;
    }

    protected int getRadius(ItemStack stack) {

        return (int) getPropertyWithDefault(stack, TAG_AUGMENT_RADIUS, 0.0F);
    }
    // endregion

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {

        return new RFDrillItemWrapper(stack, this);
    }

    // region IAugmentableItem
    @Override
    public void updateAugmentState(ItemStack container, List<ItemStack> augments) {

        super.updateAugmentState(container, augments);

        if (getMode(container) >= getNumModes(container)) {
            setMode(container, getNumModes(container) - 1);
        }
    }
    // endregion

    // region IMultiModeItem
    @Override
    public int getNumModes(ItemStack stack) {

        return 1 + getRadius(stack);
    }

    @Override
    public void onModeChange(Player player, ItemStack stack) {

        if (getNumModes(stack) <= 1) {
            return;
        }
        player.level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.4F, 1.0F - 0.1F * getMode(stack));
        int radius = getMode(stack) * 2 + 1;
        if (radius <= 1) {
            ChatHelper.sendIndexedChatMessageToPlayer(player, new TranslatableComponent("info.cofh.single_block"));
        } else {
            ChatHelper.sendIndexedChatMessageToPlayer(player, new TranslatableComponent("info.cofh.area").append(": " + radius + "x" + radius));
        }
    }
    // endregion

    // region CAPABILITY WRAPPER
    protected class RFDrillItemWrapper extends EnergyContainerItemWrapper implements IAreaEffect {

        private final LazyOptional<IAreaEffect> holder = LazyOptional.of(() -> this);

        RFDrillItemWrapper(ItemStack containerIn, IEnergyContainerItem itemIn) {

            super(containerIn, itemIn, itemIn.getEnergyCapability());
        }

        @Override
        public ImmutableList<BlockPos> getAreaEffectBlocks(BlockPos pos, Player player) {

            return AreaEffectHelper.getBreakableBlocksRadius(container, pos, player, getMode(container));
        }

        // region ICapabilityProvider
        @Override
        @Nonnull
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

            if (cap == CapabilityAreaEffect.AREA_EFFECT_ITEM_CAPABILITY) {
                return CapabilityAreaEffect.AREA_EFFECT_ITEM_CAPABILITY.orEmpty(cap, holder);
            }
            return super.getCapability(cap, side);
        }
        // endregion
    }
    // endregion
}

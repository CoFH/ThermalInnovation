package cofh.thermal.innovation.common.item;

import cofh.core.client.event.CoreClientEvents;
import cofh.core.common.item.IBlockRayTraceItem;
import cofh.core.common.item.ITrackedItem;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.RenderHelper;
import cofh.core.util.helpers.vfx.Color;
import cofh.core.util.helpers.vfx.VFXHelper;
import cofh.lib.util.Sided;
import cofh.lib.util.Utils;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.core.ThermalCore;
import cofh.thermal.core.common.config.ThermalCoreConfig;
import cofh.thermal.innovation.client.renderer.RFGrappleBEWLR;
import cofh.thermal.lib.common.item.EnergyContainerItemAugmentable;
import cofh.thermal.lib.common.item.IFlexibleEnergyContainerItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.Lazy;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static cofh.core.util.helpers.AugmentableHelper.getPropertyWithDefault;
import static cofh.core.util.helpers.AugmentableHelper.setAttributeFromAugmentAdd;
import static cofh.lib.util.Constants.RGB_DURABILITY_FLUX;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.lib.util.ThermalAugmentRules.createAllowValidator;

public class RFGrappleItem extends EnergyContainerItemAugmentable implements IFlexibleEnergyContainerItem, ITrackedItem, IBlockRayTraceItem {

    protected static final Sided<Map<Player, Hook>> HOOKS = new Sided<>(WeakHashMap::new);
    protected static final Queue<RenderInfo> RENDER_INFO = new ArrayDeque<>();
    protected static final Color OUTER = Color.fromRGB(RGB_DURABILITY_FLUX);
    protected static final Color INNER = Color.WHITE.scaleAlpha(0.6F);
    protected static final Supplier<ItemStack> HOOK = Lazy.of(() -> ThermalCore.ITEMS.get("grapple_hook").getDefaultInstance());

    protected int energyPerUse = 200;
    protected int energyPerTick = 50;

    public RFGrappleItem(Properties builder, int maxEnergy, int maxTransfer) {

        super(builder, maxEnergy, maxTransfer);

        //ProxyUtils.registerItemModelProperty(this, new ResourceLocation("color"), (stack, world, entity, seed) -> (hasCustomColor(stack) ? 1.0F : 0));
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("state"), this::getModelState);
        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalCoreConfig.toolAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_RF, TAG_AUGMENT_REACH);
    }

    @Override
    public void setEnergyPerUse(int energyPerUse) {

        this.energyPerUse = energyPerUse;
    }

    public void setEnergyPerTick(int energyPerTick) {

        this.energyPerTick = energyPerTick;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        return new InteractionResultHolder<>(useDelegate(level, player, stack, hand), stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {

        return useDelegate(context.getLevel(), context.getPlayer(), stack, context.getHand());
    }

    private InteractionResult useDelegate(Level level, Player player, ItemStack stack, InteractionHand hand) {

        if (player != null && !Utils.isFakePlayer(player)) {
            Hook existing = HOOKS.get(level).get(player);
            if (existing != null) {
                HOOKS.get(level).put(player, existing.retract(player, stack));
            } else if (extractEnergy(stack, getEnergyPerUse(stack), level.isClientSide || player.isCreative()) > 0) {
                HOOKS.get(level).put(player, new ShotHook(stack, player, hand));
            }
        }
        return InteractionResult.CONSUME;
    }

    public static void tickPlayerHook(Player player) {

        Level level = player.level;
        Hook hook = HOOKS.get(level).remove(player);
        if (hook != null) {
            ItemStack stack = player.getItemInHand(hook.hand);
            if (stack.getItem() instanceof RFGrappleItem) {
                hook = hook.tick(level, player, stack);
                if (hook != null) {
                    HOOKS.get(level).put(player, hook);
                }
            }
        }
    }

    // region CLIENT
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {

        consumer.accept(new IClientItemExtensions() {

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {

                return RFGrappleBEWLR.INSTANCE;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {

                return HOOKS.client().containsKey(entity) ? HumanoidModel.ArmPose.BOW_AND_ARROW : IClientItemExtensions.super.getArmPose(entity, hand, stack);
            }

        });
    }

    public void render(ItemStack stack, ItemDisplayContext transform, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int overlayCoord) {

        LivingEntity holder = CoreClientEvents.itemHolder;
        if (holder != null && RenderHelper.isHandTransform(transform)) {
            Hook hook = HOOKS.client().get(holder);
            Vector4f start;
            if (transform.firstPerson()) {
                boolean sprint = holder.isSprinting();
                if (RenderHelper.isRightArm(holder, hook.hand)) {
                    // right arm
                    start = new Vector4f(0.5F, 0.0F, sprint ? -0.25F : -0.4F, 1);
                } else {
                    // left arm
                    start = new Vector4f(0.5F, sprint ? 0.2F : 0.35F, sprint ? 0.4F : 0.6F, 1);
                }
            } else {
                start = new Vector4f(0, 0.02F, -0.35F, 1);
            }
            poseStack.last().pose().transform(start);
            RENDER_INFO.add(new RenderInfo(hook, holder, start));
        }
        RenderHelper.renderItem().renderStatic(stack, transform, packedLight, overlayCoord, poseStack, buffer, null, -1);
    }

    public static void renderHooks(PoseStack stack, MultiBufferSource buffer, float partialTick) {

        Level level = ProxyUtils.getClientWorld();
        while (!RENDER_INFO.isEmpty()) {
            RenderInfo info = RENDER_INFO.poll();
            LivingEntity entity = info.entity;
            Hook hook = info.hook;
            if (entity.level.equals(level)) {
                Vec3 pos = hook.pos(entity, partialTick);
                Vector4f end = new Vector4f(0.2F, -0.05F, 0, 1);
                stack.pushPose();
                stack.translate(pos.x, pos.y, pos.z);
                stack.scale(1.25F, 1.25F, 1.25F);
                float toR = (float) MathHelper.TO_RAD;
                float facing = RenderHelper.isRightArm(entity, hook.hand) ? 25 : -25;
                float xRot = hook.xRot(entity, partialTick);
                float cos = MathHelper.cos(xRot * toR);
                stack.mulPose(new Quaternionf().rotationYXZ(
                        toR * (90 - hook.yRot(entity, partialTick) + cos * facing),
                        -MathHelper.sin(xRot * toR) * facing * toR,
                        toR * (45 + xRot - 15 * cos)));
                end.mul(stack.last().pose());
                RenderHelper.renderItem().renderStatic(HOOK.get(), ItemDisplayContext.GROUND, hook.light(level, partialTick, pos), OverlayTexture.NO_OVERLAY, stack, buffer, null, 0);
                stack.popPose();
                VFXHelper.renderBeam(info.start, end, VFXHelper.normal(stack), buffer, RenderHelper.FULL_BRIGHT, 0.2F, OUTER, INNER);
            }
        }
    }

    protected float getModelState(ItemStack stack, @Nullable Level level, @Nullable LivingEntity entity, int seed) {

        if (seed == -1) {
            return 0.5F;
        }
        if (entity != null) {
            Hook hook = HOOKS.client().get(entity);
            if (hook != null && entity.getItemInHand(hook.hand) == stack) {
                return 0.75F;
            }
        }
        return 0F;
    }
    // endregion

    // region HELPERS
    protected float getReach(ItemStack stack) {

        return 20.0F * (1.0F + getPropertyWithDefault(stack, TAG_AUGMENT_REACH, 1.0F));
    }

    protected float getShootSpeed(ItemStack stack) {

        return getBaseMod(stack) * 0.75F + 1.25F;
    }

    protected float getPullSpeed(ItemStack stack) {

        return getBaseMod(stack) * 0.025F + 0.025F;
    }

    protected int getCooldown(ItemStack stack) {

        return MathHelper.ceil(80 / getBaseMod(stack));
    }

    protected int getEnergyPerUse(ItemStack stack) {

        return energyPerUse;
    }

    protected int getEnergyPerTick(ItemStack stack) {

        return energyPerTick;
    }

    @Override
    protected void setAttributesFromAugment(ItemStack container, CompoundTag augmentData) {

        CompoundTag subTag = container.getTagElement(TAG_PROPERTIES);
        if (subTag == null) {
            return;
        }
        setAttributeFromAugmentAdd(subTag, augmentData, TAG_AUGMENT_REACH);
        super.setAttributesFromAugment(container, augmentData);
    }
    // endregion

    // region ITrackedItem
    @Override
    public void onSwapFrom(Player player, InteractionHand hand, ItemStack from, ItemStack to, int duration) {

        Hook hook = HOOKS.get(player.level).remove(player);
        if (hook != null) {
            ReturnHook ret = hook.retract(player, from);
            player.getCooldowns().addCooldown(this, getCooldown(from) + ret.life - ret.age);
        }
    }

    @Override
    public boolean matches(ItemStack from, ItemStack to) {

        return Utils.matchesExcluding(from, to, TAG_ENERGY);
    }
    // endregion

    // region IBlockRayTraceItem
    @Override
    public void handleBlockRayTrace(ServerLevel level, ServerPlayer player, InteractionHand hand, ItemStack stack, Vec3 origin, BlockHitResult result) {

        Hook hook = HOOKS.server().get(player);
        if (hook != null) {
            HOOKS.server().put(player, hook.retract(player, stack));
        }
    }
    // endregion

    protected abstract class Hook {

        protected Vec3 pos;
        protected final InteractionHand hand;

        public Hook(Vec3 pos, InteractionHand hand) {

            this.pos = pos;
            this.hand = hand;
        }

        protected abstract Vec3 pos(LivingEntity entity, float partialTick);

        protected abstract float yRot(LivingEntity entity, float partialTick);

        protected abstract float xRot(LivingEntity entity, float partialTick);

        protected int light(Level level, float partialTick, Vec3 pos) {

            return LevelRenderer.getLightColor(level, BlockPos.containing(pos));
        }

        abstract Hook tick(Level level, Player player, ItemStack stack);

        public ReturnHook retract(LivingEntity entity, ItemStack stack) {

            return new ReturnHook(stack, pos, hand, entity);
        }

    }

    protected class ShotHook extends Hook {

        protected Vec3 velocity;
        protected final float xRot;
        protected final float yRot;

        public ShotHook(ItemStack stack, Player player, InteractionHand hand) {

            super(player.getEyePosition(), hand);
            float speed = getShootSpeed(stack);
            this.velocity = player.getLookAngle().scale(speed);
            this.xRot = player.getXRot();
            this.yRot = player.getYRot();
        }

        @Override
        protected Vec3 pos(LivingEntity entity, float partialTick) {

            return pos.add(velocity.scale(partialTick));
        }

        @Override
        protected float xRot(LivingEntity entity, float partialTick) {

            return xRot;
        }

        @Override
        protected float yRot(LivingEntity entity, float partialTick) {

            return yRot;
        }

        @Override
        public Hook tick(Level level, Player player, ItemStack stack) {

            float r = getReach(stack);
            if (player.getEyePosition().distanceToSqr(pos) > r * r) {
                return retract(player, stack);
            }
            Vec3 next = pos.add(velocity);
            //TODO block backlist
            BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
            if (hit.getType() == HitResult.Type.MISS) {
                pos = next;
                return this;
            }
            if (level.isClientSide) {
                BlockPos pos = hit.getBlockPos();
                BlockState state = level.getBlockState(pos);
                BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(pos);
                Vec3 loc = hit.getLocation();
                for (int i = 0; i < 6; ++i) {
                    level.addParticle(option, loc.x, loc.y, loc.z, 0, 0.1, 0);
                }
                SoundType sound = state.getSoundType(level, pos, player);
                level.playSound(player, player, sound.getBreakSound(), player.getSoundSource(), sound.getVolume() * 0.75F, sound.getPitch());
            }
            return new PullHook(hit, hand, xRot, yRot);
            //return ArcheryHelper.findHitEntities(level, entity, pos, hit.getLocation(), 0.1, e -> e.isAlive() && e.isPickable() && !e.isSpectator() && !(e instanceof HangingEntity))
            //        .min(ArcheryHelper.compareHitDistance(pos))
            //        .map(result -> (Hook) new EntityHook(result))
            //        .orElseGet(() -> {
            //            if (hit.getType() == HitResult.Type.MISS) {
            //                pos = next;
            //                return this;
            //            }
            //            return new BlockHook(hit);
            //        });
        }

    }

    protected class ReturnHook extends Hook {

        protected int age = 0;
        protected final int life;

        public ReturnHook(Vec3 pos, InteractionHand hand, int life) {

            super(pos, hand);
            this.life = life;
        }

        public ReturnHook(ItemStack stack, Vec3 pos, InteractionHand hand, LivingEntity entity) {

            this(pos, hand, (int) (Math.min(entity.getEyePosition().distanceTo(pos), getReach(stack)) / getShootSpeed(stack)));
        }

        @Override
        protected Vec3 pos(LivingEntity entity, float partialTick) {

            return entity.getEyePosition(partialTick).subtract(pos).scale(1 - MathHelper.cos(MathHelper.F_HALF_PI * (age + partialTick) / life)).add(pos);
        }

        @Override
        protected float xRot(LivingEntity entity, float partialTick) {

            Vec3 disp = pos.subtract(entity.getEyePosition(partialTick));
            return (float) (-Mth.atan2(disp.y, disp.horizontalDistance()) * MathHelper.TO_DEG);
        }

        @Override
        protected float yRot(LivingEntity entity, float partialTick) {

            Vec3 disp = pos.subtract(entity.getEyePosition(partialTick));
            return (float) (Mth.atan2(disp.z, disp.x) * MathHelper.TO_DEG) - 90F;
        }

        @Override
        public Hook tick(Level level, Player player, ItemStack stack) {

            if (++age > life) {
                player.getCooldowns().addCooldown(RFGrappleItem.this, getCooldown(stack));
                return null;
            }
            return this;
        }

        @Override
        public ReturnHook retract(LivingEntity entity, ItemStack stack) {

            return this;
        }

    }

    protected class PullHook extends Hook {

        protected boolean overcome = false;
        protected int life = 40;
        protected Vec3 prev;
        protected final float xRot;
        protected final float yRot;
        protected final BlockPos block;
        protected final Direction face;
        protected final BlockPos adj;

        public PullHook(BlockHitResult result, InteractionHand hand, float xRot, float yRot) {

            super(result.getLocation(), hand);
            this.xRot = xRot;
            this.yRot = yRot;
            block = result.getBlockPos();
            face = result.getDirection();
            adj = block.relative(face);
        }

        @Override
        protected Vec3 pos(LivingEntity entity, float partialTick) {

            return pos;
        }

        @Override
        protected float xRot(LivingEntity entity, float partialTick) {

            return xRot;
        }

        @Override
        protected float yRot(LivingEntity entity, float partialTick) {

            return yRot;
        }

        @Override
        protected int light(Level level, float partialTick, Vec3 pos) {

            return LevelRenderer.getLightColor(level, adj);
        }

        @Override
        public Hook tick(Level level, Player player, ItemStack stack) {

            if (tickDelegate(level, player, stack)) {
                return this;
            }
            sendBlockRayTrace(player, hand, pos, new BlockHitResult(pos, face, block, false));
            return retract(player, stack);
        }

        protected boolean tickDelegate(Level level, LivingEntity entity, ItemStack stack) {

            if (!level.isEmptyBlock(block) && --life >= 0) {
                Vec3 disp = pos.subtract(entity.getEyePosition());
                float reach = getReach(stack) * 1.25F;
                if (disp.lengthSqr() < reach * reach && valid(level, entity, disp) &&
                        extractEnergy(stack, getEnergyPerTick(stack), level.isClientSide || Utils.isCreativePlayer(entity)) > 0) {
                    return execute(level, entity, stack, disp);
                }
            }
            return false;
        }

        protected boolean execute(Level level, LivingEntity entity, ItemStack stack, Vec3 displacement) {

            entity.resetFallDistance();
            if (level.isClientSide) {
                Vec3 push = displacement.normalize().scale(getPullSpeed(stack));
                double y = 0;
                if (!(entity.isFallFlying() || entity instanceof Player player && player.abilities.flying)) {
                    y += entity.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get());
                }
                Vec3 velocity = entity.getDeltaMovement();
                double max = push.y * 7;
                if (max < 0 && velocity.y < max) {
                    y += max - velocity.y;
                }
                entity.addDeltaMovement(push.add(0, y, 0));
                entity.hurtMarked = true;
            }
            return true;
        }

        protected boolean valid(Level level, LivingEntity entity, Vec3 disp) {

            if (level.isClientSide) {
                Vec3 center = new Vec3(entity.getX(), entity.getY(0.5), entity.getZ());
                if (prev == null) {
                    prev = center;
                    return true;
                }
                boolean towards = center.subtract(prev).dot(disp) > 0;
                prev = center;
                if (overcome) {
                    return towards;
                }
                overcome = towards;
            }
            return true;
        }

    }

    //protected class EntityHook extends PullHook {
    //
    //    protected final Entity target;
    //
    //    public EntityHook(EntityHitResult result) {
    //
    //        super(result.getLocation().subtract(result.getEntity().position()));
    //        target = result.getEntity();
    //    }
    //
    //    @Override
    //    protected boolean tickDelegate(Level level, LivingEntity entity, ItemStack stack) {
    //
    //        return target.isAlive() && target.level.equals(level) && super.tickDelegate(level, entity, stack);
    //    }
    //
    //    @Override
    //    protected Vec3 pos() {
    //
    //        return target.position().add(pos);
    //    }
    //
    //    @Override
    //    protected boolean execute(LivingEntity entity, ItemStack stack, Vec3 displacement) {
    //
    //        float force = getPullSpeed(stack);
    //        double me = 1.0 - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
    //        double mt = multiplier(target);
    //        if (me <= 0 && mt <= 0) {
    //            return false;
    //        }
    //        double ve = me * volume(entity);
    //        double pe = ve / (ve + mt * volume(target));
    //        Vec3 disp = displacement.normalize();
    //        entity.addDeltaMovement(disp.scale(force * pe)
    //                .add(0, entity.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get()), 0));
    //        disp = disp.scale(force * (pe - 1.0));
    //        if (target instanceof LivingEntity living) {
    //            disp = disp.add(0, living.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get()), 0);
    //        }
    //        target.addDeltaMovement(disp);
    //        entity.hurtMarked = true;
    //        return true;
    //    }
    //
    //    protected double multiplier(Entity entity) {
    //
    //        if (entity instanceof LivingEntity living) {
    //            return 1.0 - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
    //        } else if (entity instanceof HangingEntity) {
    //            return 0.0;
    //        }
    //        return 1.0;
    //    }
    //
    //    protected double volume(Entity entity) {
    //
    //        float width = entity.getBbWidth();
    //        return entity.getBbHeight() * width * width;
    //    }
    //
    //}

    protected record RenderInfo(Hook hook, LivingEntity entity, Vector4f start) {

    }

}

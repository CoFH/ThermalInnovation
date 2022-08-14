package cofh.thermal.innovation.entity;

import cofh.core.util.helpers.ArcheryHelper;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.innovation.init.TInoReferences;
import cofh.thermal.innovation.item.RFGrappleItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Comparator;

import static cofh.core.CoFHCore.ITEMS;
import static cofh.lib.util.constants.NBTTags.TAG_HOOK;
import static cofh.thermal.innovation.entity.GrappleHook.HookState.*;
import static cofh.thermal.innovation.init.TInoEntities.GRAPPLE_HOOK;

public class GrappleHook extends Projectile {

    protected static final int AUTO_MAX_DURATION = 80;
    protected double length = 1.0F;
    protected HookState state = HookState.SHOOT;
    protected int stateAge = 0;
    @Nullable
    protected Player player;
    @Nullable
    protected LivingEntity hookedIn;
    protected ItemStack render = new ItemStack(ITEMS.get("grapple_hook"));

    public GrappleHook(EntityType<? extends Projectile> type, Level level) {

        super(type, level);
    }

    public GrappleHook(Level level, Player owner) {

        super(GRAPPLE_HOOK.get(), level);
        setOwner(owner);
        this.player = owner;
        this.length = this.position().distanceTo(getMidPos(owner));
    }

    public void shoot(Vec3 velocity) {

        this.setDeltaMovement(velocity);
        this.setYRot((float) (Mth.atan2(velocity.x, velocity.z) * MathHelper.TO_DEG));
        this.setXRot((float) (Mth.atan2(velocity.y, velocity.horizontalDistance()) * MathHelper.TO_DEG));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public ItemStack renderItem() {

        return render;
    }

    @Override
    public Packet<?> getAddEntityPacket() {

        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isAttackable() {

        return false;
    }

    @Override
    protected void defineSynchedData() {

    }

    public ItemStack getStack(Player owner) {

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = owner.getItemInHand(hand);
            if (stack.getItem() instanceof RFGrappleItem && stack.getOrCreateTag().getInt(TAG_HOOK) == this.getId()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void tick() {

        Player owner = player;
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        ItemStack stack = getStack(owner);
        if (stack.isEmpty()) {
            discard();
            return;
        }
        RFGrappleItem grapple = (RFGrappleItem) stack.getItem();
        float range = grapple.getReach(stack);
        if (range * range * 1.1F < displacement(owner).lengthSqr()) {
            discard();
            return;
        }
        super.tick();
        ++stateAge;

        switch (state) {
            case HOOKED_BLOCK:
                if (!blockHookTick(owner, stack, grapple)) {
                    setState(RETRACT);
                }
                return;
            case HOOKED_ENTITY:
                if (!(canHitEntity(hookedIn) && entityHookTick(owner, hookedIn, stack, grapple))) {
                    setState(RETRACT);
                }
                return;
            case SHOOT:
                double speed = grapple.getShootSpeed(stack);
                if (stateAge > range / speed) {
                    setState(RETRACT);
                }
                calculateCollision(this.level);
                checkInsideBlocks();
                break;
            case RETRACT:
                speed = grapple.getShootSpeed(stack);
                Vec3 disp = getMidPos(owner).subtract(position());
                double dist2 = disp.lengthSqr();
                if (dist2 < 2.25) {
                    discard();
                    return;
                }
                if (dist2 > speed * speed) {
                    disp = disp.normalize().scale(speed);
                }
                setDeltaMovement(disp);
                noPhysics = true;
                break;
        }

        if (!this.isAlive()) {
            return;
        }
        Vec3 velocity = getDeltaMovement();
        if (this.isInWater()) {
            for (int i = 0; i < 4; ++i) {
                level.addParticle(ParticleTypes.BUBBLE, getX() + velocity.x * 0.75D, getY() + velocity.y * 0.75D, getZ() + velocity.z * 0.75D, velocity.x, velocity.y, velocity.z);
            }
        }
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
    }

    public void calculateCollision(Level world) {

        Vec3 start = this.position();
        Vec3 end = start.add(this.getDeltaMovement());
        BlockHitResult blockResult = this.getBlockHitResult(world, start, end);
        if (blockResult.getType() != HitResult.Type.MISS) {
            end = blockResult.getLocation();
        }
        ArcheryHelper.findHitEntities(world, this, start, end, this::canHitEntity)
                .min(Comparator.comparingDouble(r -> r.distanceTo(this)))
                .ifPresentOrElse(result -> {
                    if (ForgeEventFactory.onProjectileImpact(this, result)) {
                        this.onHitEntity(result);
                    }
                }, () -> {
                    if (blockResult.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, blockResult)) {
                        this.onHitBlock(blockResult);
                    }
                });
    }

    protected boolean blockHookTick(Player owner, ItemStack stack, RFGrappleItem grapple) {

        if (level.isEmptyBlock(blockPosition())) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }
        Vec3 ownerVel = owner.getDeltaMovement();
        Vec3 dir = displacement(owner);
        double dist = dir.length();
        dir = dir.scale(1.0F / dist);
        double dot = dir.dot(ownerVel);
        if (grapple.getMode(stack) <= 0) {
            double speed = grapple.getPullSpeed(stack);
            if (owner.isUsingItem() && owner.getUseItem().equals(stack)) {
                this.length = this.length + (owner.isSecondaryUseActive() ? speed : -speed);
            }
            double diff = dist - this.length - dot;
            if (diff > 0) {
                accelerate(owner, dir.scale(diff));
            }
        } else if (dot < 0 || stateAge > AUTO_MAX_DURATION || movement(owner) < 0.03F) { //TODO adjust threshold
            return false;
        } else {
            this.length = dist;
            accelerate(owner, dir.scale(grapple.getPullSpeed(stack)));
        }
        return true;
    }

    protected boolean entityHookTick(Player owner, LivingEntity target, ItemStack stack, RFGrappleItem grapple) {

        setPos(getMidPos(target));
        Vec3 ownerVel = owner.getDeltaMovement();
        Vec3 dir = displacement(owner);
        double dist = dir.length();
        dir = dir.scale(1.0F / dist);
        double dot = dir.dot(ownerVel);
        if (grapple.getMode(stack) <= 0) {
            //TODO
            //double speed = grapple.getPullSpeed(stack);
            //if (owner.isUsingItem() && owner.getUseItem().equals(stack)) {
            //    this.length = this.length + (owner.isSecondaryUseActive() ? speed : -speed);
            //}
            //double diff = dist - this.length - dot;
            //if (diff > 0) {
            //    accelerate(owner, dir.scale(diff));
            //}
        } else if (dot < 0 || stateAge > AUTO_MAX_DURATION || (movement(owner) < 0.03F && movement(target) < 0.03F)) { //TODO adjust threshold
            return false;
        } else {
            this.length = dist;
            float speed = grapple.getPullSpeed(stack);
            double proportion = proportion(owner, target);
            accelerate(owner, dir.scale(speed * proportion));
            accelerate(target, dir.scale(speed * (1.0 - proportion)));
        }
        return true;
    }

    public BlockHitResult getBlockHitResult(Level world, Vec3 startPos, Vec3 endPos) {

        return world.clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    }

    @Override
    protected void onHit(HitResult result) {

        if (level.isClientSide) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.COPPER_FALL, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {

        onHit(result);
        setState(HOOKED_BLOCK);

        //TODO
        super.onHitBlock(result);
        Vec3 disp = result.getLocation().subtract(this.getX(), this.getY(), this.getZ());
        this.setDeltaMovement(disp);
        Vec3 vec31 = disp.normalize().scale(0.05F);
        this.setPosRaw(this.getX() - vec31.x, this.getY() - vec31.y, this.getZ() - vec31.z);


        ItemStack stack = getStack(player);
        RFGrappleItem grapple = (RFGrappleItem) stack.getItem();
        if (grapple.getMode(stack) > 0) {
            float speed = grapple.getPullSpeed(stack);
            Vec3 acc = this.position().subtract(player.position()).normalize().scale(speed);
            accelerate(player, acc);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {

        onHit(result);
        Entity target = result.getEntity();
        if (!canHitEntity(target)) {
            setState(RETRACT);
            return;
        }
        setState(HOOKED_ENTITY);
        hookedIn = (LivingEntity) target;
        Vec3 pos = getMidPos(target);
        ItemStack stack = getStack(player);
        RFGrappleItem grapple = (RFGrappleItem) stack.getItem();
        if (grapple.getMode(stack) > 0) {
            float speed = grapple.getPullSpeed(stack);
            setPos(pos);
            Vec3 dir = pos.subtract(player.position()).normalize();
            double proportion = proportion(player, hookedIn);
            accelerate(player, dir.scale(speed * proportion));
            accelerate(target, dir.scale(speed * (1.0 - proportion)));
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {

        return entity instanceof LivingEntity && !entity.isSpectator() && entity.isAlive() &&
                entity.isPickable() && (player == null || !player.isPassengerOfSameVehicle(entity));
    }

    public void setState(HookState state) {

        this.state = state;
        stateAge = 0;
    }

    public Vec3 displacement(Entity entity) {

        return position().subtract(getMidPos(entity));
    }

    public static Vec3 getMidPos(Entity entity) {

        return entity.position().add(0, entity.getBbHeight() * 0.5F, 0);
    }

    public static double movement(Entity entity) {

        return MathHelper.distSqr(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
    }

    public static void accelerate(Entity entity, Vec3 a) {

        entity.setDeltaMovement(entity.getDeltaMovement().add(a));
        entity.hasImpulse = true;
        if (a.y() > 0) {
            entity.fallDistance = 0;
        }
    }

    public static double proportion(LivingEntity e1, LivingEntity e2) {

        double v1 = volume(e1);
        double v2 = volume(e2);
        double m1 = v1 / (1.0 - e1.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        double m2 = v2 / (1.0 - e2.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        if (Double.isInfinite(m1)) {
            if (Double.isInfinite(m2)) {
                return v2 / (v1 + v2);
            }
            return 0.0;
        } else if (Double.isInfinite(m2)) {
            return 1.0;
        }
        return m2 / (m1 + m2);
    }

    public static double volume(Entity entity) {

        float width = entity.getBbWidth();
        return entity.getBbHeight() * width * width;
    }

    protected enum HookState {
        SHOOT,
        HOOKED_ENTITY,
        HOOKED_BLOCK,
        RETRACT
    }

}

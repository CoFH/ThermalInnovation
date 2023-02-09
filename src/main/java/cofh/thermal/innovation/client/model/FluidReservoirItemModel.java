package cofh.thermal.innovation.client.model;

import cofh.core.item.IMultiModeItem;
import cofh.core.util.helpers.FluidHelper;
import cofh.lib.api.item.ICoFHItem;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.CompositeModel;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.geometry.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static cofh.lib.util.Constants.BUCKET_VOLUME;
import static cofh.lib.util.constants.ModIds.ID_THERMAL_INNOVATION;

public final class FluidReservoirItemModel implements IUnbakedGeometry<FluidReservoirItemModel> {

    // Depth offsets to prevent Z-fighting
    private static final Transformation FLUID_TRANSFORM = new Transformation(Vector3f.ZERO, Quaternion.ONE, new Vector3f(1, 1, 1.002f), Quaternion.ONE);
    private static final Transformation COVER_TRANSFORM = new Transformation(Vector3f.ZERO, Quaternion.ONE, new Vector3f(1, 1, 1.004f), Quaternion.ONE);
    // Transformer to set quads to max brightness
    private static final IQuadTransformer MAX_LIGHTMAP_TRANSFORMER = QuadTransformers.applyingLightmap(0x00F000F0);

    @Nonnull
    private final FluidStack fluidStack;

    private final int mode;
    private final boolean active;
    private final boolean color;

    public FluidReservoirItemModel(FluidStack fluidStack, int mode, boolean active, boolean color) {

        this.fluidStack = fluidStack;
        this.mode = mode;
        this.active = active;
        this.color = color;
    }

    public FluidReservoirItemModel withProperties(FluidStack newFluid, int mode, boolean active, boolean color) {

        return new FluidReservoirItemModel(newFluid, mode, active, color);
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {

        Material particleLocation = context.hasMaterial("particle") ? context.getMaterial("particle") : null;
        Material fluidMaskLocation = context.hasMaterial("fluid_mask") ? context.getMaterial("fluid_mask") : null;

        Material[] inactiveLocations = new Material[2];
        Material[] activeLocations = new Material[2];
        Material[] baseLocations = new Material[2];
        Material[] colorLocations = new Material[2];

        inactiveLocations[0] = context.hasMaterial("mode_0") ? context.getMaterial("mode_0") : null;
        inactiveLocations[1] = context.hasMaterial("mode_1") ? context.getMaterial("mode_1") : null;
        activeLocations[0] = context.hasMaterial("active_0") ? context.getMaterial("active_0") : null;
        activeLocations[1] = context.hasMaterial("active_1") ? context.getMaterial("active_1") : null;

        baseLocations[0] = context.hasMaterial("base_0") ? context.getMaterial("base_0") : null;
        baseLocations[1] = context.hasMaterial("base_1") ? context.getMaterial("base_1") : null;
        colorLocations[0] = context.hasMaterial("color_0") ? context.getMaterial("color_0") : null;
        colorLocations[1] = context.hasMaterial("color_1") ? context.getMaterial("color_1") : null;

        Fluid fluid = fluidStack.getFluid();

        TextureAtlasSprite fluidSprite = fluid != Fluids.EMPTY ? spriteGetter.apply(ForgeHooksClient.getBlockMaterial(IClientFluidTypeExtensions.of(fluid).getStillTexture(fluidStack))) : null;
        TextureAtlasSprite particleSprite = particleLocation != null ? spriteGetter.apply(particleLocation) : null;
        if (particleSprite == null) {
            particleSprite = fluidSprite != null ? fluidSprite : spriteGetter.apply(baseLocations[0]);
        }
        var itemContext = StandaloneGeometryBakingContext.builder(context).withGui3d(false).withUseBlockLight(false).build(modelLocation);
        var modelBuilder = CompositeModel.Baked.builder(itemContext, particleSprite, new ContainedFluidOverrideHandler(bakery, itemContext, this), context.getTransforms());
        var normalRenderTypes = getLayerRenderTypes();

        Material modeLayer = active ? activeLocations[mode % 2] : inactiveLocations[mode % 2];
        if (modeLayer != null) {
            var modeSprite = spriteGetter.apply(modeLayer);
            var unbaked = UnbakedGeometryHelper.createUnbakedItemElements(0, modeSprite);
            var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> modeSprite, modelState, modelLocation);
            modelBuilder.addQuads(normalRenderTypes, quads);
        }
        Material frameLayer = color ? colorLocations[mode % 2] : baseLocations[mode % 2];
        if (frameLayer != null) {
            var frameSprite = spriteGetter.apply(frameLayer);
            var unbaked = UnbakedGeometryHelper.createUnbakedItemElements(1, frameSprite);
            var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> frameSprite, modelState, modelLocation);
            modelBuilder.addQuads(normalRenderTypes, quads);
        }

        if (fluidMaskLocation != null && fluidSprite != null) {
            TextureAtlasSprite templateSprite = spriteGetter.apply(fluidMaskLocation);
            if (templateSprite != null) {
                // Fluid layer
                var transformedState = new SimpleModelState(modelState.getRotation().compose(FLUID_TRANSFORM), modelState.isUvLocked());
                var unbaked = UnbakedGeometryHelper.createUnbakedItemMaskElements(2, templateSprite); // Use template as mask
                var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> fluidSprite, transformedState, modelLocation); // Bake with fluid texture

                var unlit = fluid.getFluidType().getLightLevel() > 0;
                var renderTypes = getLayerRenderTypes();
                if (unlit) {
                    MAX_LIGHTMAP_TRANSFORMER.processInPlace(quads);
                }
                modelBuilder.addQuads(renderTypes, quads);
            }
        }
        modelBuilder.setParticle(particleSprite);
        return modelBuilder.build();
    }

    @Override
    public Collection<Material> getMaterials(IGeometryBakingContext owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {

        Set<Material> texs = Sets.newHashSet();

        if (owner.hasMaterial("particle")) {
            texs.add(owner.getMaterial("particle"));
        }
        if (owner.hasMaterial("fluid_mask")) {
            texs.add(owner.getMaterial("fluid_mask"));
        }
        if (owner.hasMaterial("base_0")) {
            texs.add(owner.getMaterial("base_0"));
        }
        if (owner.hasMaterial("base_1")) {
            texs.add(owner.getMaterial("base_1"));
        }
        if (owner.hasMaterial("color_0")) {
            texs.add(owner.getMaterial("color_0"));
        }
        if (owner.hasMaterial("color_1")) {
            texs.add(owner.getMaterial("color_1"));
        }
        if (owner.hasMaterial("mode_0")) {
            texs.add(owner.getMaterial("mode_0"));
        }
        if (owner.hasMaterial("mode_1")) {
            texs.add(owner.getMaterial("mode_1"));
        }
        if (owner.hasMaterial("active_0")) {
            texs.add(owner.getMaterial("active_0"));
        }
        if (owner.hasMaterial("active_1")) {
            texs.add(owner.getMaterial("active_1"));
        }
        return texs;
    }

    public static RenderTypeGroup getLayerRenderTypes() {

        return new RenderTypeGroup(RenderType.cutout(), ForgeRenderTypes.ITEM_LAYERED_CUTOUT.get());
    }

    public static class Loader implements IGeometryLoader<FluidReservoirItemModel> {

        @Override
        public FluidReservoirItemModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {

            FluidStack stack = FluidStack.EMPTY;
            if (jsonObject.has("fluid")) {
                ResourceLocation fluidName = new ResourceLocation(jsonObject.get("fluid").getAsString());
                Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidName);
                if (fluid != null) {
                    stack = new FluidStack(fluid, BUCKET_VOLUME);
                }
            }
            // create new model with correct liquid
            return new FluidReservoirItemModel(stack, 0, false, false);
        }

    }

    private static final class ContainedFluidOverrideHandler extends ItemOverrides {

        private final Map<List<Integer>, BakedModel> cache = new Object2ObjectOpenHashMap<>(); // contains all the baked models since they'll never change
        private final ModelBakery bakery;
        private final IGeometryBakingContext owner;
        private final FluidReservoirItemModel parent;

        private ContainedFluidOverrideHandler(ModelBakery bakery, IGeometryBakingContext owner, FluidReservoirItemModel parent) {

            this.bakery = bakery;
            this.owner = owner;
            this.parent = parent;
        }

        @Override
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed) {

            int mode = ((IMultiModeItem) stack.getItem()).getMode(stack);
            boolean active = ((ICoFHItem) stack.getItem()).isActive(stack);

            CompoundTag nbt = stack.getTagElement("display");
            boolean color = nbt != null && nbt.contains("color", 99);

            FluidStack fluidStack = FluidHelper.getFluidContainedInItem(stack).orElse(FluidStack.EMPTY);
            List<Integer> fluidHash = Arrays.asList(mode + (active ? 2 : 0) + (color ? 4 : 0), FluidHelper.fluidHashcode(fluidStack));
            if (!cache.containsKey(fluidHash)) {
                FluidReservoirItemModel unbaked = this.parent.withProperties(fluidStack, mode, active, color);
                BakedModel bakedModel = unbaked.bake(owner, bakery, Material::sprite, BlockModelRotation.X0_Y0, this, new ResourceLocation(ID_THERMAL_INNOVATION, "reservoir_override"));
                cache.put(fluidHash, bakedModel);
                return bakedModel;
            }
            return cache.get(fluidHash);
        }

    }

}

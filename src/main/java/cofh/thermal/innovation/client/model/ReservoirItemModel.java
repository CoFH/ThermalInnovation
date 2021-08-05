package cofh.thermal.innovation.client.model;

import cofh.core.util.helpers.FluidHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.*;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.VanillaResourceType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static cofh.lib.util.constants.Constants.BUCKET_VOLUME;
import static cofh.lib.util.constants.Constants.ID_COFH_CORE;

public final class ReservoirItemModel implements IModelGeometry<ReservoirItemModel> {

    // minimal Z offset to prevent depth-fighting
    private static final float NORTH_Z_FLUID = 7.498f / 16f;
    private static final float SOUTH_Z_FLUID = 8.502f / 16f;

    @Nonnull
    private final FluidStack fluidStack;

    public ReservoirItemModel(FluidStack fluidStack) {

        this.fluidStack = fluidStack;
    }

    /**
     * Returns a new ModelDynBucket representing the given fluid, but with the same
     * other properties (flipGas, tint, coverIsMask).
     */
    public ReservoirItemModel withFluid(FluidStack newFluid) {

        return new ReservoirItemModel(newFluid);
    }

    @Override
    public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ItemOverrideList overrides, ResourceLocation modelLocation) {

        RenderMaterial particleLocation = owner.isTexturePresent("particle") ? owner.resolveTexture("particle") : null;
        RenderMaterial baseLocation = owner.isTexturePresent("base") ? owner.resolveTexture("base") : null;
        RenderMaterial fluidMaskLocation = owner.isTexturePresent("fluid") ? owner.resolveTexture("fluid") : null;

        IModelTransform transformsFromModel = owner.getCombinedTransform();
        Fluid fluid = fluidStack.getFluid();

        TextureAtlasSprite fluidSprite = fluid != Fluids.EMPTY ? spriteGetter.apply(ForgeHooksClient.getBlockMaterial(fluid.getAttributes().getStillTexture())) : null;
        ImmutableMap<TransformType, TransformationMatrix> transformMap = PerspectiveMapWrapper.getTransforms(new ModelTransformComposition(transformsFromModel, modelTransform));

        TextureAtlasSprite particleSprite = particleLocation != null ? spriteGetter.apply(particleLocation) : null;
        if (particleSprite == null) particleSprite = fluidSprite;

        TransformationMatrix transform = modelTransform.getRotation();
        ItemMultiLayerBakedModel.Builder builder = ItemMultiLayerBakedModel.builder(owner, particleSprite, new ContainedFluidOverrideHandler(overrides, bakery, owner, this), transformMap);

        if (baseLocation != null) {
            // build base (insidest)
            builder.addQuads(ItemLayerModel.getLayerRenderType(false), ItemLayerModel.getQuadsForSprites(ImmutableList.of(baseLocation), transform, spriteGetter));
        }
        if (fluidMaskLocation != null && fluidSprite != null) {
            TextureAtlasSprite templateSprite = spriteGetter.apply(fluidMaskLocation);
            if (templateSprite != null) {
                // build liquid layer (inside)
                int luminosity = fluid.getAttributes().getLuminosity(fluidStack);
                int color = fluid.getAttributes().getColor(fluidStack);
                builder.addQuads(ItemLayerModel.getLayerRenderType(luminosity > 0), ItemTextureQuadConverter.convertTexture(transform, templateSprite, fluidSprite, NORTH_Z_FLUID, Direction.NORTH, color, 1, luminosity));
                builder.addQuads(ItemLayerModel.getLayerRenderType(luminosity > 0), ItemTextureQuadConverter.convertTexture(transform, templateSprite, fluidSprite, SOUTH_Z_FLUID, Direction.SOUTH, color, 1, luminosity));
            }
        }
        builder.setParticle(particleSprite);
        return builder.build();
    }

    @Override
    public Collection<RenderMaterial> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {

        Set<RenderMaterial> texs = Sets.newHashSet();

        if (owner.isTexturePresent("particle")) {
            texs.add(owner.resolveTexture("particle"));
        }
        if (owner.isTexturePresent("base")) {
            texs.add(owner.resolveTexture("base"));
        }
        if (owner.isTexturePresent("fluid")) {
            texs.add(owner.resolveTexture("fluid"));
        }
        if (owner.isTexturePresent("active")) {
            texs.add(owner.resolveTexture("active"));
        }
        return texs;
    }

    public static class Loader implements IModelLoader<ReservoirItemModel> {

        @Override
        public IResourceType getResourceType() {

            return VanillaResourceType.MODELS;
        }

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager) {
            // no need to clear cache since we create a new model instance
        }

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
            // no need to clear cache since we create a new model instance
        }

        @Override
        public ReservoirItemModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {

            FluidStack stack = FluidStack.EMPTY;
            if (modelContents.has("fluid")) {
                ResourceLocation fluidName = new ResourceLocation(modelContents.get("fluid").getAsString());
                Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidName);
                if (fluid != null) {
                    stack = new FluidStack(fluid, BUCKET_VOLUME);
                }
            }
            // create new model with correct liquid
            return new ReservoirItemModel(stack);
        }

    }

    private static final class ContainedFluidOverrideHandler extends ItemOverrideList {

        private final Map<List<Integer>, IBakedModel> cache = Maps.newHashMap(); // contains all the baked models since they'll never change
        private final ItemOverrideList nested;
        private final ModelBakery bakery;
        private final IModelConfiguration owner;
        private final ReservoirItemModel parent;

        private ContainedFluidOverrideHandler(ItemOverrideList nested, ModelBakery bakery, IModelConfiguration owner, ReservoirItemModel parent) {

            this.nested = nested;
            this.bakery = bakery;
            this.owner = owner;
            this.parent = parent;
        }

        @Override
        public IBakedModel getOverrideModel(IBakedModel originalModel, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity) {

            IBakedModel overriden = nested.getOverrideModel(originalModel, stack, world, entity);

            if (overriden != originalModel) {
                return overriden;
            }
            return FluidHelper.getFluidContainedInItem(stack)
                    .map(fluidStack -> {
                        List<Integer> fluidHash = Arrays.asList(overriden.hashCode(), FluidHelper.fluidHashcode(fluidStack));
                        if (!cache.containsKey(fluidHash)) {
                            ReservoirItemModel unbaked = this.parent.withFluid(fluidStack);
                            IBakedModel bakedModel = unbaked.bake(owner, bakery, ModelLoader.defaultTextureGetter(), ModelRotation.X0_Y0, this, new ResourceLocation(ID_COFH_CORE, "fluid_container_override"));
                            cache.put(fluidHash, bakedModel);
                            return bakedModel;
                        }
                        return cache.get(fluidHash);
                    })
                    .orElse(originalModel); // empty
        }

    }

}

package cofh.thermal.innovation.client.model;

import cofh.core.util.helpers.FluidHelper;
import cofh.lib.item.ICoFHItem;
import cofh.lib.item.IMultiModeItem;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
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

public final class FluidReservoirItemModel implements IModelGeometry<FluidReservoirItemModel> {

    // minimal Z offset to prevent depth-fighting
    private static final float NORTH_Z_FLUID = 7.498F / 16F;
    private static final float SOUTH_Z_FLUID = 8.502F / 16F;

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
    public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ItemOverrideList overrides, ResourceLocation modelLocation) {

        RenderMaterial particleLocation = owner.isTexturePresent("particle") ? owner.resolveTexture("particle") : null;
        RenderMaterial fluidMaskLocation = owner.isTexturePresent("fluid_mask") ? owner.resolveTexture("fluid_mask") : null;

        RenderMaterial[] inactiveLocations = new RenderMaterial[2];
        RenderMaterial[] activeLocations = new RenderMaterial[2];
        RenderMaterial[] baseLocations = new RenderMaterial[2];
        RenderMaterial[] colorLocations = new RenderMaterial[2];

        inactiveLocations[0] = owner.isTexturePresent("mode_0") ? owner.resolveTexture("mode_0") : null;
        inactiveLocations[1] = owner.isTexturePresent("mode_1") ? owner.resolveTexture("mode_1") : null;
        activeLocations[0] = owner.isTexturePresent("active_0") ? owner.resolveTexture("active_0") : null;
        activeLocations[1] = owner.isTexturePresent("active_1") ? owner.resolveTexture("active_1") : null;

        baseLocations[0] = owner.isTexturePresent("base_0") ? owner.resolveTexture("base_0") : null;
        baseLocations[1] = owner.isTexturePresent("base_1") ? owner.resolveTexture("base_1") : null;
        colorLocations[0] = owner.isTexturePresent("color_0") ? owner.resolveTexture("color_0") : null;
        colorLocations[1] = owner.isTexturePresent("color_1") ? owner.resolveTexture("color_1") : null;

        IModelTransform transformsFromModel = owner.getCombinedTransform();
        Fluid fluid = fluidStack.getFluid();

        TextureAtlasSprite fluidSprite = fluid != Fluids.EMPTY ? spriteGetter.apply(ForgeHooksClient.getBlockMaterial(fluid.getAttributes().getStillTexture())) : null;
        ImmutableMap<TransformType, TransformationMatrix> transformMap = PerspectiveMapWrapper.getTransforms(new ModelTransformComposition(transformsFromModel, modelTransform));

        TextureAtlasSprite particleSprite = particleLocation != null ? spriteGetter.apply(particleLocation) : null;
        if (particleSprite == null) particleSprite = fluidSprite;

        TransformationMatrix transform = modelTransform.getRotation();
        ItemMultiLayerBakedModel.Builder builder = ItemMultiLayerBakedModel.builder(owner, particleSprite, new ContainedFluidOverrideHandler(bakery, owner, this), transformMap);

        RenderMaterial modeLayer = active ? activeLocations[mode % 2] : inactiveLocations[mode % 2];
        if (modeLayer != null) {
            builder.addQuads(ItemLayerModel.getLayerRenderType(false), ItemLayerModel.getQuadsForSprite(0, spriteGetter.apply(modeLayer), transform));
        }
        RenderMaterial frameLayer = color ? colorLocations[mode % 2] : baseLocations[mode % 2];
        if (frameLayer != null) {
            builder.addQuads(ItemLayerModel.getLayerRenderType(false), ItemLayerModel.getQuadsForSprite(1, spriteGetter.apply(frameLayer), transform));
        }

        if (fluidMaskLocation != null && fluidSprite != null) {
            TextureAtlasSprite templateSprite = spriteGetter.apply(fluidMaskLocation);
            if (templateSprite != null) {
                // build liquid layer (inside)
                int luminosity = fluid.getAttributes().getLuminosity(fluidStack);
                int color = fluid.getAttributes().getColor(fluidStack);
                builder.addQuads(ItemLayerModel.getLayerRenderType(luminosity > 0), ItemTextureQuadConverter.convertTexture(transform, templateSprite, fluidSprite, NORTH_Z_FLUID, Direction.NORTH, color, 2, luminosity));
                builder.addQuads(ItemLayerModel.getLayerRenderType(luminosity > 0), ItemTextureQuadConverter.convertTexture(transform, templateSprite, fluidSprite, SOUTH_Z_FLUID, Direction.SOUTH, color, 2, luminosity));
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
        if (owner.isTexturePresent("fluid_mask")) {
            texs.add(owner.resolveTexture("fluid_mask"));
        }
        if (owner.isTexturePresent("base_0")) {
            texs.add(owner.resolveTexture("base_0"));
        }
        if (owner.isTexturePresent("base_1")) {
            texs.add(owner.resolveTexture("base_1"));
        }
        if (owner.isTexturePresent("color_0")) {
            texs.add(owner.resolveTexture("color_0"));
        }
        if (owner.isTexturePresent("color_1")) {
            texs.add(owner.resolveTexture("color_1"));
        }
        if (owner.isTexturePresent("mode_0")) {
            texs.add(owner.resolveTexture("mode_0"));
        }
        if (owner.isTexturePresent("mode_1")) {
            texs.add(owner.resolveTexture("mode_1"));
        }
        if (owner.isTexturePresent("active_0")) {
            texs.add(owner.resolveTexture("active_0"));
        }
        if (owner.isTexturePresent("active_1")) {
            texs.add(owner.resolveTexture("active_1"));
        }
        return texs;
    }

    public static class Loader implements IModelLoader<FluidReservoirItemModel> {

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
        public FluidReservoirItemModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {

            FluidStack stack = FluidStack.EMPTY;
            if (modelContents.has("fluid")) {
                ResourceLocation fluidName = new ResourceLocation(modelContents.get("fluid").getAsString());
                Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidName);
                if (fluid != null) {
                    stack = new FluidStack(fluid, BUCKET_VOLUME);
                }
            }
            // create new model with correct liquid
            return new FluidReservoirItemModel(stack, 0, false, false);
        }

    }

    private static final class ContainedFluidOverrideHandler extends ItemOverrideList {

        private final Map<List<Integer>, IBakedModel> cache = new Object2ObjectOpenHashMap<>(); // contains all the baked models since they'll never change
        private final ModelBakery bakery;
        private final IModelConfiguration owner;
        private final FluidReservoirItemModel parent;

        private ContainedFluidOverrideHandler(ModelBakery bakery, IModelConfiguration owner, FluidReservoirItemModel parent) {

            this.bakery = bakery;
            this.owner = owner;
            this.parent = parent;
        }

        @Override
        public IBakedModel resolve(IBakedModel originalModel, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity) {

            int mode = ((IMultiModeItem) stack.getItem()).getMode(stack);
            boolean active = ((ICoFHItem) stack.getItem()).isActive(stack);

            CompoundNBT nbt = stack.getTagElement("display");
            boolean color = nbt != null && nbt.contains("color", 99);

            FluidStack fluidStack = FluidHelper.getFluidContainedInItem(stack).orElse(FluidStack.EMPTY);
            List<Integer> fluidHash = Arrays.asList(mode + (active ? 2 : 0) + (color ? 4 : 0), FluidHelper.fluidHashcode(fluidStack));
            if (!cache.containsKey(fluidHash)) {
                FluidReservoirItemModel unbaked = this.parent.withProperties(fluidStack, mode, active, color);
                IBakedModel bakedModel = unbaked.bake(owner, bakery, ModelLoader.defaultTextureGetter(), ModelRotation.X0_Y0, this, new ResourceLocation(ID_COFH_CORE, "reservoir_override"));
                cache.put(fluidHash, bakedModel);
                return bakedModel;
            }
            return cache.get(fluidHash);
        }

    }

}

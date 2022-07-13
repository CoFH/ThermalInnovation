package cofh.thermal.innovation.client.model;

import cofh.core.item.IMultiModeItem;
import cofh.core.util.helpers.FluidHelper;
import cofh.lib.api.item.ICoFHItem;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.RenderProperties;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static cofh.lib.util.Constants.BUCKET_VOLUME;
import static cofh.lib.util.constants.ModIds.ID_THERMAL_INNOVATION;

public final class FluidReservoirItemModel implements IUnbakedGeometry<FluidReservoirItemModel> {

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
    public BakedModel bake(IGeometryBakingContext owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {

        Material particleLocation = owner.hasMaterial("particle") ? owner.getMaterial("particle") : null;
        Material fluidMaskLocation = owner.hasMaterial("fluid_mask") ? owner.getMaterial("fluid_mask") : null;

        Material[] inactiveLocations = new Material[2];
        Material[] activeLocations = new Material[2];
        Material[] baseLocations = new Material[2];
        Material[] colorLocations = new Material[2];

        inactiveLocations[0] = owner.hasMaterial("mode_0") ? owner.getMaterial("mode_0") : null;
        inactiveLocations[1] = owner.hasMaterial("mode_1") ? owner.getMaterial("mode_1") : null;
        activeLocations[0] = owner.hasMaterial("active_0") ? owner.getMaterial("active_0") : null;
        activeLocations[1] = owner.hasMaterial("active_1") ? owner.getMaterial("active_1") : null;

        baseLocations[0] = owner.hasMaterial("base_0") ? owner.getMaterial("base_0") : null;
        baseLocations[1] = owner.hasMaterial("base_1") ? owner.getMaterial("base_1") : null;
        colorLocations[0] = owner.hasMaterial("color_0") ? owner.getMaterial("color_0") : null;
        colorLocations[1] = owner.hasMaterial("color_1") ? owner.getMaterial("color_1") : null;

        ModelState transformsFromModel = owner.getCombinedTransform();
        Fluid fluid = fluidStack.getFluid();

        TextureAtlasSprite fluidSprite = fluid != Fluids.EMPTY ? spriteGetter.apply(ForgeHooksClient.getBlockMaterial(RenderProperties.get(fluid).getStillTexture(fluidStack))) : null;
        ImmutableMap<ItemTransforms.TransformType, Transformation> transformMap =
                PerspectiveMapWrapper.getTransforms(new CompositeModelState(transformsFromModel, modelTransform));

        TextureAtlasSprite particleSprite = particleLocation != null ? spriteGetter.apply(particleLocation) : null;
        if (particleSprite == null) particleSprite = fluidSprite;

        Transformation transform = modelTransform.getRotation();
        ItemMultiLayerBakedModel.Builder builder = ItemMultiLayerBakedModel.builder(owner, particleSprite, new ContainedFluidOverrideHandler(bakery, owner, this), transformMap);

        Material modeLayer = active ? activeLocations[mode % 2] : inactiveLocations[mode % 2];
        if (modeLayer != null) {
            builder.addQuads(ItemLayerModel.getLayerRenderType(false), ItemLayerModel.getQuadsForSprite(0, spriteGetter.apply(modeLayer), transform));
        }
        Material frameLayer = color ? colorLocations[mode % 2] : baseLocations[mode % 2];
        if (frameLayer != null) {
            builder.addQuads(ItemLayerModel.getLayerRenderType(false), ItemLayerModel.getQuadsForSprite(1, spriteGetter.apply(frameLayer), transform));
        }

        if (fluidMaskLocation != null && fluidSprite != null) {
            TextureAtlasSprite templateSprite = spriteGetter.apply(fluidMaskLocation);
            if (templateSprite != null) {
                // build liquid layer (inside)
                int luminosity = FluidHelper.luminosity(fluidStack);
                int color = FluidHelper.color(fluidStack);
                builder.addQuads(ItemLayerModel.getLayerRenderType(luminosity > 0), ItemTextureQuadConverter.convertTexture(transform, templateSprite, fluidSprite, NORTH_Z_FLUID, Direction.NORTH, color, 2, luminosity));
                builder.addQuads(ItemLayerModel.getLayerRenderType(luminosity > 0), ItemTextureQuadConverter.convertTexture(transform, templateSprite, fluidSprite, SOUTH_Z_FLUID, Direction.SOUTH, color, 2, luminosity));
            }
        }
        builder.setParticle(particleSprite);
        return builder.build();
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

    public static class Loader implements IGeometryLoader<FluidReservoirItemModel> {

        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
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
                BakedModel bakedModel = unbaked.bake(owner, bakery, ForgeModelBakery.defaultTextureGetter(), BlockModelRotation.X0_Y0, this, new ResourceLocation(ID_THERMAL_INNOVATION, "reservoir_override"));
                cache.put(fluidHash, bakedModel);
                return bakedModel;
            }
            return cache.get(fluidHash);
        }

    }

}

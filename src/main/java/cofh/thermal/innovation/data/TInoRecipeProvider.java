package cofh.thermal.innovation.data;

import cofh.lib.data.RecipeProviderCoFH;
import cofh.lib.tags.ItemTagsCoFH;
import cofh.thermal.lib.common.ThermalFlags;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.innovation.init.TInoIDs.*;

public class TInoRecipeProvider extends RecipeProviderCoFH {

    public TInoRecipeProvider(DataGenerator generatorIn) {

        super(generatorIn, ID_THERMAL);
        manager = ThermalFlags.manager();
    }

    @Override
    public String getName() {

        return "Thermal Innovation: Recipes";
    }

    @Override
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer) {

        var reg = ITEMS;

        Item redstoneServo = reg.get("redstone_servo");
        Item rfCoil = reg.get("rf_coil");

        ShapedRecipeBuilder.shaped(reg.get(ID_FLUX_DRILL))
                .define('C', ItemTagsCoFH.GEARS_GOLD)
                .define('G', ItemTagsCoFH.GEARS_TIN)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('P', rfCoil)
                .define('X', reg.get("drill_head"))
                .pattern(" X ")
                .pattern("ICI")
                .pattern("GPG")
                .unlockedBy("has_rf_coil", has(rfCoil))
                .save(consumer);

        ShapedRecipeBuilder.shaped(reg.get(ID_FLUX_SAW))
                .define('C', ItemTagsCoFH.GEARS_GOLD)
                .define('G', ItemTagsCoFH.GEARS_TIN)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('P', rfCoil)
                .define('X', reg.get("saw_blade"))
                .pattern(" X ")
                .pattern("ICI")
                .pattern("GPG")
                .unlockedBy("has_rf_coil", has(rfCoil))
                .save(consumer);

        //        ShapedRecipeBuilder.shaped(reg.get(ID_FLUX_GRAPPLE))
        //                .define('C', ItemTagsCoFH.GEARS_GOLD)
        //                .define('G', ItemTagsCoFH.GEARS_TIN)
        //                .define('I', Tags.Items.INGOTS_IRON)
        //                .define('P', rfCoil)
        //                .define('X', reg.get("grapple_hook"))
        //                .pattern(" X ")
        //                .pattern("ICI")
        //                .pattern("GPG")
        //                .unlockedBy("has_rf_coil", has(rfCoil))
        //                .save(consumer);

        ShapedRecipeBuilder.shaped(reg.get(ID_FLUX_CAPACITOR))
                .define('L', ItemTagsCoFH.INGOTS_LEAD)
                .define('P', rfCoil)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .pattern("RLR")
                .pattern("LPL")
                .pattern(" R ")
                .unlockedBy("has_rf_coil", has(rfCoil))
                .save(consumer);

        ShapedRecipeBuilder.shaped(reg.get(ID_FLUX_MAGNET))
                .define('L', ItemTagsCoFH.INGOTS_LEAD)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('P', rfCoil)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .pattern("IRI")
                .pattern("LIL")
                .pattern(" P ")
                .unlockedBy("has_rf_coil", has(rfCoil))
                .save(consumer);

        ShapedRecipeBuilder.shaped(reg.get(ID_FLUID_RESERVOIR))
                .define('B', Items.BUCKET)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('G', Tags.Items.GLASS)
                .define('P', redstoneServo)
                .define('R', reg.get("cured_rubber"))
                .pattern("CRC")
                .pattern("GBG")
                .pattern(" P ")
                .unlockedBy("has_redstone_servo", has(redstoneServo))
                .save(consumer);

        ShapedRecipeBuilder.shaped(reg.get(ID_POTION_INFUSER))
                .define('B', Items.GLASS_BOTTLE)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('G', ItemTagsCoFH.GEARS_SILVER)
                .define('R', reg.get("cured_rubber"))
                .pattern("RBR")
                .pattern("CGC")
                .pattern(" C ")
                .unlockedBy("has_glass_bottle", has(Items.GLASS_BOTTLE))
                .save(consumer);

        ShapedRecipeBuilder.shaped(reg.get(ID_POTION_QUIVER))
                .define('B', Items.GLASS_BOTTLE)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('G', ItemTagsCoFH.GEARS_SILVER)
                .define('S', Tags.Items.STRING)
                .define('R', reg.get("cured_rubber"))
                .pattern("C C")
                .pattern("BGS")
                .pattern("RCR")
                .unlockedBy("has_glass_bottle", has(Items.GLASS_BOTTLE))
                .save(consumer);

    }

}

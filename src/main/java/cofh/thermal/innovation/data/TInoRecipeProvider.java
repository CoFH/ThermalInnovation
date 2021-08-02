package cofh.thermal.innovation.data;

import cofh.lib.data.RecipeProviderCoFH;
import cofh.lib.util.DeferredRegisterCoFH;
import cofh.lib.util.references.ItemTagsCoFH;
import cofh.thermal.lib.common.ThermalFlags;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

import static cofh.lib.util.constants.Constants.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.ITEMS;

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
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {

        DeferredRegisterCoFH<Item> reg = ITEMS;

        Item rfCoil = reg.get("rf_coil");

        ShapedRecipeBuilder.shapedRecipe(reg.get("flux_drill"))
                .key('C', ItemTagsCoFH.GEARS_GOLD)
                .key('G', ItemTagsCoFH.GEARS_TIN)
                .key('I', Tags.Items.INGOTS_IRON)
                .key('P', rfCoil)
                .key('X', reg.get("drill_head"))
                .patternLine(" X ")
                .patternLine("ICI")
                .patternLine("GPG")
                .addCriterion("has_rf_coil", hasItem(rfCoil))
                .build(consumer);

        ShapedRecipeBuilder.shapedRecipe(reg.get("flux_saw"))
                .key('C', ItemTagsCoFH.GEARS_GOLD)
                .key('G', ItemTagsCoFH.GEARS_TIN)
                .key('I', Tags.Items.INGOTS_IRON)
                .key('P', rfCoil)
                .key('X', reg.get("saw_blade"))
                .patternLine(" X ")
                .patternLine("ICI")
                .patternLine("GPG")
                .addCriterion("has_rf_coil", hasItem(rfCoil))
                .build(consumer);

        ShapedRecipeBuilder.shapedRecipe(reg.get("flux_capacitor"))
                .key('L', ItemTagsCoFH.INGOTS_LEAD)
                .key('P', rfCoil)
                .key('R', Tags.Items.DUSTS_REDSTONE)
                .patternLine("RLR")
                .patternLine("LPL")
                .patternLine(" R ")
                .addCriterion("has_rf_coil", hasItem(rfCoil))
                .build(consumer);

        ShapedRecipeBuilder.shapedRecipe(reg.get("flux_magnet"))
                .key('L', ItemTagsCoFH.INGOTS_LEAD)
                .key('I', Tags.Items.INGOTS_IRON)
                .key('P', rfCoil)
                .key('R', Tags.Items.DUSTS_REDSTONE)
                .patternLine("IRI")
                .patternLine("LIL")
                .patternLine(" P ")
                .addCriterion("has_rf_coil", hasItem(rfCoil))
                .build(consumer);

        ShapedRecipeBuilder.shapedRecipe(reg.get("potion_infuser"))
                .key('B', Items.GLASS_BOTTLE)
                .key('C', ItemTagsCoFH.INGOTS_COPPER)
                .key('G', ItemTagsCoFH.GEARS_SILVER)
                .key('R', reg.get("cured_rubber"))
                .patternLine("RBR")
                .patternLine("CGC")
                .patternLine(" C ")
                .addCriterion("has_glass_bottle", hasItem(Items.GLASS_BOTTLE))
                .build(consumer);

        ShapedRecipeBuilder.shapedRecipe(reg.get("potion_quiver"))
                .key('B', Items.GLASS_BOTTLE)
                .key('C', ItemTagsCoFH.INGOTS_COPPER)
                .key('G', ItemTagsCoFH.GEARS_SILVER)
                .key('S', Tags.Items.STRING)
                .key('R', reg.get("cured_rubber"))
                .patternLine("C C")
                .patternLine("BGS")
                .patternLine("RCR")
                .addCriterion("has_glass_bottle", hasItem(Items.GLASS_BOTTLE))
                .build(consumer);
    }

}

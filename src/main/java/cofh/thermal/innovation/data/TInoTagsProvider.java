package cofh.thermal.innovation.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.innovation.init.TInoIDs.ID_FLUX_DRILL;

public class TInoTagsProvider {

    public static class Block extends BlockTagsProvider {

        public Block(DataGenerator gen, ExistingFileHelper existingFileHelper) {

            super(gen, ID_THERMAL, existingFileHelper);
        }

        @Override
        public String getName() {

            return "Thermal Innovation: Block Tags";
        }

        @Override
        protected void addTags() {

        }

    }

    public static class Item extends ItemTagsProvider {

        public Item(DataGenerator gen, BlockTagsProvider blockTagProvider, ExistingFileHelper existingFileHelper) {

            super(gen, blockTagProvider, ID_THERMAL, existingFileHelper);
        }

        @Override
        public String getName() {

            return "Thermal Innovation: Item Tags";
        }

        @Override
        protected void addTags() {

            tag(ItemTags.CLUSTER_MAX_HARVESTABLES).add(ITEMS.get(ID_FLUX_DRILL));
        }

    }

}

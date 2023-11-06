package cofh.thermal.innovation.data.providers;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.innovation.init.TInoIDs.ID_FLUX_DRILL;

public class TInoTagsProvider {

    public static class Block extends BlockTagsProvider {

        public Block(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {

            super(output, lookupProvider, ID_THERMAL, existingFileHelper);
        }

        @SuppressWarnings ("unchecked")
        @Override
        protected void addTags(HolderLookup.Provider pProvider) {

        }

    }

    public static class Item extends ItemTagsProvider {

        public Item(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider, CompletableFuture<TagsProvider.TagLookup<net.minecraft.world.level.block.Block>> pBlockTags, ExistingFileHelper existingFileHelper) {

            super(pOutput, pLookupProvider, pBlockTags, ID_THERMAL, existingFileHelper);
        }

        @SuppressWarnings ("unchecked")
        @Override
        protected void addTags(HolderLookup.Provider pProvider) {

            tag(ItemTags.CLUSTER_MAX_HARVESTABLES).add(ITEMS.get(ID_FLUX_DRILL));
        }

    }

}

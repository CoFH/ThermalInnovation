//package cofh.thermal.innovation.data;
//
//import net.minecraft.data.DataGenerator;
//import net.minecraft.data.PackOutput;
//import net.minecraftforge.common.data.ExistingFileHelper;
//import net.minecraftforge.data.event.GatherDataEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//
//import static cofh.lib.util.constants.ModIds.ID_THERMAL_INNOVATION;
//
//@Mod.EventBusSubscriber (bus = Mod.EventBusSubscriber.Bus.MOD, modid = ID_THERMAL_INNOVATION)
//public class TInoDataGen {
//
//    @SubscribeEvent
//    public static void gatherData(final GatherDataEvent event) {
//
//        DataGenerator gen = event.getGenerator();
//        PackOutput pOutput = gen.getPackOutput();
//        ExistingFileHelper exFileHelper = event.getExistingFileHelper();
//
//        TInoTagsProvider.Block blockTags = new TInoTagsProvider.Block(pOutput, event.getLookupProvider(), exFileHelper);
//
//        gen.addProvider(event.includeServer(), blockTags);
//        gen.addProvider(event.includeServer(), new TInoTagsProvider.Item(pOutput, event.getLookupProvider(), blockTags.contentsGetter(), exFileHelper));
//
//        gen.addProvider(event.includeServer(), new TInoRecipeProvider(gen));
//    }
//
//}

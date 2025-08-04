package cofh.thermal.innovation.common.event;

import cofh.lib.util.constants.ModIds;
import cofh.thermal.innovation.common.item.RFGrappleItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber (modid = ModIds.ID_THERMAL_INNOVATION)
public class TInoCommonEvents {

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        RFGrappleItem.tickPlayerHook(event.player);
    }

}

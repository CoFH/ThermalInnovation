package cofh.thermal.innovation.event;

import cofh.core.inventory.container.HeldInventoryContainer;
import cofh.lib.util.filter.IFilterOptions;
import cofh.thermal.innovation.item.SatchelItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static cofh.lib.util.constants.Constants.ID_THERMAL_INNOVATION;

@Mod.EventBusSubscriber(modid = ID_THERMAL_INNOVATION)
public class TInoCommonEvents {

    private TInoCommonEvents() {

    }

    @SubscribeEvent
    public static void handleEntityItemPickup(final EntityItemPickupEvent event) {

        if (event.isCanceled()) {
            return;
        }
        PlayerEntity player = event.getPlayer();
        if (player.openContainer instanceof HeldInventoryContainer || player.openContainer instanceof IFilterOptions) {
            return;
        }
        PlayerInventory inventory = player.inventory;
        boolean cancel = false;
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof SatchelItem) {
                cancel |= SatchelItem.onItemPickup(event, stack);
            }
        }
        event.setCanceled(cancel);
    }

}

package dev.visualutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/**
 * Zapis i przywracanie ukladu paska (hotbar). Zapisujesz uklad klawiszem, a po bałaganie w ekwipunku
 * drugi klawisz uklada przedmioty na swoje miejsca - jeden ruch co 2 ticki, jak zwykle klikniecia.
 */
public final class Layout {

    private static final String[] SAVED = new String[9];
    private static boolean hasSaved = false;
    private static boolean restoring = false;
    private static int cooldown = 0;
    private static int steps = 0;

    private Layout() {}

    private static String key(ItemStack stack) {
        if (stack.isEmpty()) return "";
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static void save(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        for (int i = 0; i < 9; i++) {
            SAVED[i] = key(p.getInventory().getItem(i));
        }
        hasSaved = true;
        p.displayClientMessage(Component.literal("Układ paska zapisany"), true);
    }

    public static void restore(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (!hasSaved) {
            p.displayClientMessage(Component.literal("Najpierw zapisz układ paska (klawisz w Sterowaniu)"), true);
            return;
        }
        restoring = true;
        steps = 0;
        cooldown = 0;
        p.displayClientMessage(Component.literal("Układam pasek..."), true);
    }

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (!restoring || p == null || mc.gameMode == null) {
            restoring = false;
            return;
        }
        if (cooldown-- > 0) return;
        cooldown = 1;

        var menu = p.inventoryMenu;
        for (int h = 0; h < 9; h++) {
            String want = SAVED[h];
            if (want == null || want.isEmpty()) continue;
            if (key(menu.getSlot(36 + h).getItem()).equals(want)) continue;

            int from = -1;
            // najpierw szukamy w glownym ekwipunku, potem na pozostalych miejscach paska
            for (int s = 9; s <= 35 && from < 0; s++) {
                if (key(menu.getSlot(s).getItem()).equals(want)) from = s;
            }
            for (int other = 0; other < 9 && from < 0; other++) {
                if (other == h) continue;
                String have = key(menu.getSlot(36 + other).getItem());
                if (have.equals(want) && !want.equals(SAVED[other])) from = 36 + other;
            }
            if (from < 0) continue;

            mc.gameMode.handleInventoryMouseClick(menu.containerId, from, h, ClickType.SWAP, p);
            if (++steps > 24) {
                restoring = false;
                p.displayClientMessage(Component.literal("Pasek ułożony (część przedmiotów nie znaleziona)"), true);
            }
            return;
        }
        restoring = false;
        p.displayClientMessage(Component.literal("Pasek ułożony"), true);
    }
}

package org.redcastlemedia.multitallented.civs.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.redcastlemedia.multitallented.civs.LocaleManager;
import org.redcastlemedia.multitallented.civs.civilians.Civilian;
import org.redcastlemedia.multitallented.civs.civilians.CivilianManager;
import org.redcastlemedia.multitallented.civs.util.CVItem;

public class MainMenu extends Menu {
    private static final String MENU_NAME = "Civs Menu";
    public MainMenu() {
        super(MENU_NAME);
    }

    @Override
    void handleInteract(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack clickedStack = event.getCursor();
        if (clickedStack == null) {
            return;
        }
        if (clickedStack.getItemMeta() == null) {
            return;
        }
        ItemMeta im = clickedStack.getItemMeta();
        String itemName = im.getDisplayName();
        LocaleManager localeManager = LocaleManager.getInstance();
        Civilian civilian = CivilianManager.getInstance().getCivilian(event.getWhoClicked().getUniqueId());
        String locale = civilian.getLocale();
        if (itemName.equals(localeManager.getTranslation(locale, "language-menu"))) {
            event.getWhoClicked().closeInventory();
            event.getWhoClicked().openInventory(LanguageMenu.createMenu(locale));
            return;
        }
        //TODO finish this stub
    }

    public static Inventory createMenu(String locale) {
        Inventory inventory = Bukkit.createInventory(null, 18, MENU_NAME);
        //TODO add items to the inventory

        LocaleManager localeManager = LocaleManager.getInstance();
        CVItem cvItem = new CVItem(Material.GRASS, 1, -1, 100, localeManager.getTranslation(locale, "language-menu"));
        inventory.setItem(17, cvItem.createItemStack());

//        inventory.setItem(0, new ItemStack(Material.MAP));
        return inventory;
    }

}

package org.redcastlemedia.multitallented.civs.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.redcastlemedia.multitallented.civs.LocaleManager;
import org.redcastlemedia.multitallented.civs.civilians.Civilian;
import org.redcastlemedia.multitallented.civs.civilians.CivilianManager;
import org.redcastlemedia.multitallented.civs.items.ItemManager;
import org.redcastlemedia.multitallented.civs.towns.Town;
import org.redcastlemedia.multitallented.civs.towns.TownManager;
import org.redcastlemedia.multitallented.civs.towns.TownType;
import org.redcastlemedia.multitallented.civs.util.CVItem;

import java.util.*;

public class TownListMenu extends Menu {
    public static String MENU_NAME = "CivsTownList";

    public TownListMenu() {
        super(MENU_NAME);
    }

    @Override
    void handleInteract(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.STONE) {
            return;
        }
        ItemStack itemStack = event.getInventory().getItem(2);
        String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
        Civilian civilian = CivilianManager.getInstance().getCivilian(UUID.fromString(itemStack.getItemMeta().getLore().get(0)));
        int page = Integer.parseInt(itemStack.getItemMeta().getDisplayName());
        UUID uuid = null;
        if (itemStack.getItemMeta().getLore().size() > 1) {
            uuid = UUID.fromString(itemStack.getItemMeta().getLore().get(1));
        }

        if (isBackButton(itemStack, civilian.getLocale())) {
            clickBackButton(event.getWhoClicked());
            return;
        }

        LocaleManager localeManager = LocaleManager.getInstance();
        if (event.getCurrentItem().getType() == Material.EMERALD &&
                itemName.equals(localeManager.getTranslation(civilian.getLocale(), "next-button"))) {
            event.getWhoClicked().closeInventory();
            event.getWhoClicked().openInventory(TownListMenu.createMenu(civilian, page + 1, uuid));
            return;
        }
        if (event.getCurrentItem().getType() == Material.REDSTONE &&
                itemName.equals(localeManager.getTranslation(civilian.getLocale(), "prev-button"))) {
            event.getWhoClicked().closeInventory();
            event.getWhoClicked().openInventory(TownListMenu.createMenu(civilian, page - 1, uuid));
            return;
        }
        //TODO open menu for specific town
    }

    public static Inventory createMenu(Civilian civilian, int page, UUID uuid) {
        List<Town> towns = TownManager.getInstance().getTowns();
        if (uuid != null) {
            List<Town> newTownList = new ArrayList<>();
            for (Town town : towns) {
                if (town.getPeople().containsKey(uuid)) {
                    newTownList.add(town);
                }
            }
            towns = newTownList;
        }
        Inventory inventory = Bukkit.createInventory(null, 45, MENU_NAME);

        LocaleManager localeManager = LocaleManager.getInstance();

        //0 Prev button
        if (page > 0) {
            CVItem cvItem = CVItem.createCVItemFromString("REDSTONE");
            cvItem.setDisplayName(localeManager.getTranslation(civilian.getLocale(),
                    "prev-button"));
            inventory.setItem(0, cvItem.createItemStack());
        }

        //2 Icon
        CVItem cvItem = CVItem.createCVItemFromString("STONE");
        cvItem.setDisplayName(page + "");
        List<String> lore = new ArrayList<>();
        lore.add(civilian.getUuid().toString());
        if (uuid != null) {
            lore.add(uuid.toString());
        }
        cvItem.setLore(lore);
        inventory.setItem(2, cvItem.createItemStack());

        //6 Back button
        inventory.setItem(6, getBackButton(civilian));

        int startIndex = page * 36;
        //8 Next button
        if (startIndex + 36 < towns.size()) {
            CVItem cvItem1 = CVItem.createCVItemFromString("EMERALD");
            cvItem1.setDisplayName(localeManager.getTranslation(civilian.getLocale(),
                    "next-button"));
            inventory.setItem(8, cvItem1.createItemStack());
        }

        int i=9;
        for (int k=startIndex; k<towns.size() && k<startIndex+36; k++) {
            Town town = towns.get(k);
            TownType townType = (TownType) ItemManager.getInstance().getItemType(town.getType());
            CVItem cvItem1 = townType.clone();
            cvItem1.setDisplayName(town.getName());
            //TODO add lore
            inventory.setItem(i, cvItem1.createItemStack());
            i++;
        }

        return inventory;
    }
}
package org.redcastlemedia.multitallented.civs.menus;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.redcastlemedia.multitallented.civs.Civs;
import org.redcastlemedia.multitallented.civs.localization.LocaleManager;
import org.redcastlemedia.multitallented.civs.alliances.Alliance;
import org.redcastlemedia.multitallented.civs.civilians.Civilian;
import org.redcastlemedia.multitallented.civs.civilians.CivilianManager;
import org.redcastlemedia.multitallented.civs.regions.Region;
import org.redcastlemedia.multitallented.civs.regions.RegionType;
import org.redcastlemedia.multitallented.civs.towns.Town;
import org.redcastlemedia.multitallented.civs.towns.TownType;
import org.redcastlemedia.multitallented.civs.tutorials.TutorialManager;
import org.redcastlemedia.multitallented.civs.util.CommandUtil;
import org.redcastlemedia.multitallented.civs.util.PermissionUtil;

public abstract class CustomMenu {
    protected HashSet<MenuIcon> itemIndexes;
    protected HashMap<String, Integer> itemsPerPage = new HashMap<>();
    protected HashMap<UUID, HashMap<ItemStack, List<String>>> actions = new HashMap<>();
    protected HashMap<UUID, CycleGUI> cycleItems = new HashMap<>();
    protected int size;
    private String name;

    public abstract Map<String, Object> createData(Civilian civilian, Map<String, String> params);

    public String beforeOpenMenu(Civilian civilian) {
        // optional override
        return null;
    }

    public Inventory createMenu(Civilian civilian, Map<String, String> params) {
        Map<String, Object> newData;
        if (!params.containsKey("preserveData") || !"true".equals(params.get("preserveData"))) {
            MenuManager.clearData(civilian.getUuid());
            newData = new HashMap<>();
        } else {
            newData = MenuManager.getAllData(civilian.getUuid());
        }
        newData.putAll(createData(civilian, params));
        MenuManager.setNewData(civilian.getUuid(), newData);
        MenuManager.putData(civilian.getUuid(), "menuName", name);
        return createMenu(civilian);
    }
    public Inventory createMenuFromHistory(Civilian civilian, Map<String, Object> data) {
        MenuManager.setNewData(civilian.getUuid(), data);
        MenuManager.putData(civilian.getUuid(), "menuName", name);
        return createMenu(civilian);
    }
    public Inventory createMenu(Civilian civilian) {
        actions.put(civilian.getUuid(), new HashMap<>());
        Inventory inventory = Bukkit.createInventory(null, this.size, Civs.NAME + getName());
        HashMap<String, Integer> duplicateCount = new HashMap<>();
        for (MenuIcon menuIcon : itemIndexes) {
            for (Integer i : menuIcon.getIndex()) {
                if (duplicateCount.containsKey(menuIcon.getKey())) {
                    duplicateCount.put(menuIcon.getKey(), duplicateCount.get(menuIcon.getKey()) + 1);
                } else {
                    duplicateCount.put(menuIcon.getKey(), 0);
                }
                ItemStack itemStack = createItemStack(civilian, menuIcon, duplicateCount.get(menuIcon.getKey()));
                if (itemStack.getType() != Material.AIR) {
                    inventory.setItem(i, itemStack);
                }
            }
        }
        return inventory;
    }
    protected ItemStack createItemStack(Civilian civilian, MenuIcon menuIcon, int count) {
        if (!menuIcon.getPerm().isEmpty()) {
            Player player = Bukkit.getPlayer(civilian.getUuid());
            if (!player.isOp() && (Civs.perm == null || !Civs.perm.has(player, menuIcon.getPerm()))) {
                return new ItemStack(Material.AIR);
            }
        }
        if (menuIcon.getKey().equals("prev")) {
            int page = (int) MenuManager.getData(civilian.getUuid(), "page");

            if (page < 1) {
                return new ItemStack(Material.AIR);
            }
        }
        if (menuIcon.getKey().equals("next")) {
            int page = (int) MenuManager.getData(civilian.getUuid(), "page");
            int maxPage = (int) MenuManager.getData(civilian.getUuid(), "maxPage");

            if (page + 1 > maxPage) {
                return new ItemStack(Material.AIR);
            }
        }

        ItemStack itemStack = menuIcon.createCVItem(civilian.getLocale(), count).createItemStack();
        putActions(civilian, menuIcon, itemStack, count);
        return itemStack;
    }
    protected void putActions(Civilian civilian, MenuIcon menuIcon, ItemStack itemStack, int count) {
        List<String> currentActions = new ArrayList<>();
        if (menuIcon.getActions().isEmpty()) {
            currentActions.add(menuIcon.getKey());
        } else {
            for (String action : menuIcon.getActions()) {
                String newAction = action.replace("$count$", "" + count);
                newAction = newAction.replace("$itemName$",
                        ChatColor.stripColor(itemStack.getItemMeta().getDisplayName()));
                currentActions.add(newAction);
            }
        }
        actions.get(civilian.getUuid()).put(itemStack, currentActions);
    }

    public void addCycleItem(UUID uuid, int index, ItemStack is) {
        if (cycleItems.containsKey(uuid)) {
            cycleItems.get(uuid).addCycleItem(index, is);
        } else {
            CycleGUI currentGUI = new CycleGUI(uuid);
            currentGUI.addCycleItem(index, is);
            cycleItems.put(uuid, currentGUI);
        }
    }

    public void loadConfig(HashSet<MenuIcon> itemIndexes,
                    int size, String name) {
        this.itemIndexes = itemIndexes;
        this.size = size;
        this.name = name;
        for (MenuIcon menuIcon : itemIndexes) {
            if (menuIcon.getIndex().size() > 1) {
                itemsPerPage.put(menuIcon.getKey(), menuIcon.getIndex().size());
            }
        }
    }
    public String getName() {
        return this.name;
    }

    public void onInventoryDrag(InventoryDragEvent event) {
        // optional override
    }

    public void onInventoryClick(InventoryClickEvent event) {
        Civilian civilian = CivilianManager.getInstance().getCivilian(event.getWhoClicked().getUniqueId());
        ItemStack clickedItem = event.getCurrentItem();
        if (!actions.containsKey(civilian.getUuid())) {
            return;
        }
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            if (!event.isCancelled()) {
                event.setCancelled(true);
            }
            return;
        }
        List<String> actionStrings = actions.get(civilian.getUuid()).get(clickedItem);
        if (actionStrings == null || actionStrings.isEmpty()) {
            if (!event.isCancelled()) {
                event.setCancelled(true);
            }
            return;
        }
        boolean shouldCancel = false;
        for (String actionString : actionStrings) {
            shouldCancel = doActionAndCancel(civilian, actionString, clickedItem) | shouldCancel;
        }
        if (!event.isCancelled()) {
            event.setCancelled(true);
        }
    }

    public boolean doActionAndCancel(Civilian civilian, String actionString, ItemStack itemStack) {
        Player player = Bukkit.getPlayer(civilian.getUuid());
        if (player == null) {
            return true;
        }
        if (actionString.equals("print-tutorial")) {
            TutorialManager.getInstance().printTutorial(player, civilian);
        } else if (actionString.equals("close")) {
            MenuManager.clearHistory(civilian.getUuid());
            player.closeInventory();
        } else if ("clear-history".equals(actionString)) {
            MenuManager.clearHistory(civilian.getUuid());
        } else if (actionString.startsWith("message:")) {
            String messageKey = actionString.split(":")[1];
            player.sendMessage(Civs.getPrefix() + LocaleManager.getInstance().getTranslationWithPlaceholders(player,
                    messageKey));
        } else if ("refresh".equals(actionString)) {
            MenuManager.getInstance().refreshMenu(civilian);
        } else if ("back".equals(actionString)) {
            MenuManager.getInstance().goBack(civilian.getUuid());
        } else if (actionString.startsWith("menu:")) {
            actionString = replaceVariables(civilian, itemStack, actionString);
            String menuString = actionString.replace("menu:", "");
            MenuManager.openMenuFromString(civilian, menuString);
        } else if (actionString.startsWith("command:")) {
            actionString = replaceVariables(civilian, itemStack, actionString);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(civilian.getUuid());
            CommandUtil.performCommand(offlinePlayer, actionString
                    .replace("command:", ""));
        } else if (actionString.startsWith("permission:")) {
            actionString = replaceVariables(civilian, itemStack, actionString);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(civilian.getUuid());
            PermissionUtil.applyPermission(offlinePlayer, actionString
                    .replace("permission:", ""));
        }
        return true;
    }

    private String stringifyData(String key, Object data) {
        if (key.equals("town")) {
            Town town = (Town) data;
            return town.getName();
        } else if (key.equals("alliance")) {
            Alliance alliance = (Alliance) data;
            return alliance.getName();
        } else if (key.equals("region")) {
            Region region = (Region) data;
            return region.getId();
        } else if (key.equals("regionType")) {
            RegionType regionType = (RegionType) data;
            return regionType.getProcessedName();
        } else if (key.equals("townType")) {
            TownType townType = (TownType) data;
            return townType.getProcessedName();
        } else if (key.equals("uuid")) {
            return ((UUID) data).toString();
        } else if (data instanceof String) {
            return (String) data;
        } else {
            return "";
        }
    }

    private String replaceVariables(Civilian civilian, ItemStack clickedItem, String actionString) {
        if (clickedItem.getItemMeta() != null) {
            actionString = actionString.replaceAll("\\$itemName\\$",
                    clickedItem.getItemMeta().getDisplayName());
        }
        Map<String, Object> data = MenuManager.getAllData(civilian.getUuid());
        for (String key : data.keySet()) {
            if (!actionString.contains("$" + key + "$")) {
                continue;
            }
            String replaceString = stringifyData(key, data.get(key));
            actionString = actionString.replaceAll("\\$" + key + "\\$", replaceString);
        }
        return actionString;
    }

    public void onCloseMenu(Civilian civilian, Inventory inventory) {
        // Do nothing normally
    }
}

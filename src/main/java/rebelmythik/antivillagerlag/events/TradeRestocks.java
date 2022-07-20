package rebelmythik.antivillagerlag.events;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import rebelmythik.antivillagerlag.AntiVillagerLag;
import rebelmythik.antivillagerlag.utils.ColorCode;

import java.util.List;

public class TradeRestocks implements Listener {

    public AntiVillagerLag plugin;
    public long restock1;
    public long restock2;
    ColorCode colorcodes = new ColorCode();

    public TradeRestocks(AntiVillagerLag plugin) {
        this.plugin = plugin;
        restock1 = plugin.getConfig().getLong("RestockTimes.time1");
        restock2 = plugin.getConfig().getLong("RestockTimes.time2");
    }

    public void restock(Villager v) {
        List < MerchantRecipe > recipes = v.getRecipes();
        for (MerchantRecipe r: recipes) {
            r.setUses(0);
        }
    }

    public void setNewTime(Villager v) {
        PersistentDataContainer container = v.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "time");
        container.set(key, PersistentDataType.LONG, v.getWorld().getTime());
    }

    public boolean hasTime(Villager v) {
        PersistentDataContainer container = v.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "time");
        return (container.has(key, PersistentDataType.LONG));
    }

    public long getTime(Villager v) {
        PersistentDataContainer container = v.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "time");
        long time = container.get(key, PersistentDataType.LONG);
        return time;
    }

    public String replaceText(String text, String stuff2cut, String replacement) {
        int index = text.indexOf(stuff2cut);
        String text1 = text.substring(0, index);
        String text2 = text.substring(index + stuff2cut.length());
        String finalText = text1 + replacement + text2;
        return finalText;
    }

    @EventHandler
    public void clickVillager(PlayerInteractEntityEvent e) {
        //is it the villager we want?
        if (!e.getRightClicked().getType().equals(EntityType.VILLAGER)) return;
        Villager vil = (Villager) e.getRightClicked();
        Location loc = vil.getLocation();
        if ((vil.getCustomName() != null) && (!vil.getCustomName().equals(plugin.getConfig().getString("NameThatDisables")))) {

        } else if (!vil.getWorld().getBlockAt(loc.getBlockX(), (loc.getBlockY() - 1), loc.getBlockZ()).getType().equals(Material.getMaterial(plugin.getConfig().getString("BlockThatDisables")))) {
            return;
        }
        Player player = e.getPlayer();
        if (player.hasPermission("avl.restockcooldown.bypass")) {
            restock(vil);
            setNewTime(vil);
            return;
        }

        //If he doesn't have a time, restock
        if (!hasTime(vil)) {
            restock(vil);
            setNewTime(vil);
        }
        //if he does have a time, get it; also create time variables

        long curTick = vil.getWorld().getTime();
        long vilTick = getTime(vil);

        //Check if he should be restocked
        if (curTick >= vilTick) {
            if (curTick >= restock2) {
                if (vilTick <= restock2) {
                    restock(vil);
                    setNewTime(vil);
                    return;
                }
            } else if (curTick >= restock1) {
                if (vilTick <= restock1) {
                    restock(vil);
                    setNewTime(vil);
                    return;
                }
            }
        } else {
            if (curTick >= restock1) {
                restock(vil);
                setNewTime(vil);
                return;
            }
        }
        //check if he gets to see cool-down time
        if (player.hasPermission("avl.message.nextrestock")) {
            long timeTillNextRestock;

            if (curTick >= restock2) {
                timeTillNextRestock = (24000 - curTick) + restock1;
            } else if (curTick >= restock1) {
                timeTillNextRestock = restock2 - curTick;
            } else {
                timeTillNextRestock = restock1 - curTick;
            }

            long totalsec = timeTillNextRestock / 20;
            long sec = totalsec % 60;
            long min = (totalsec - sec) / 60;
            String message = plugin.getConfig().getString("messages.next-restock");

            if (message.contains("%avlrestockmin%")) {
                message = replaceText(message, "%avlrestockmin%", Long.toString(min));
            }

            message = replaceText(message, "%avlrestocksec%", Long.toString(sec));
            player.sendMessage(colorcodes.cm(message));

        }
    }
}
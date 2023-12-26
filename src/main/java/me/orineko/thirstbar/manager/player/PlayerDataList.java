package me.orineko.thirstbar.manager.player;

import me.orineko.pluginspigottools.DataList;
import me.orineko.pluginspigottools.FileManager;
import me.orineko.thirstbar.ThirstBar;
import me.orineko.thirstbar.manager.file.ConfigData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

public class PlayerDataList extends DataList<PlayerData> {

    public PlayerData addData(@Nonnull String name) {
        PlayerData playerData = getData(name);
        if(playerData != null) return playerData;
        playerData = new PlayerData(name);
        getDataList().add(playerData);
        return playerData;
    }

    public PlayerData addData(@Nonnull Player player) {
        return addData(player.getName());
    }

    @Nullable
    public PlayerData getData(@Nonnull String name) {
        return super.getData(d -> d.getName().equals(name));
    }

    public void removeDataPlayersOnline(){
        getDataList().stream().filter(p -> p.getPlayer() != null)
                .forEach(playerData -> {
                    if(ThirstBar.getInstance().getSqlManager().getConnection() == null) {
                        ThirstBar.getInstance().getPlayersFile().set(playerData.getName()+".Thirst", playerData.getThirst());
                    } else {
                        ThirstBar.getInstance().getSqlManager().runSetThirstPlayer(playerData.getName(), playerData.getThirst());
                    }

                    playerData.setThirst(playerData.getThirstMax());

                    playerData.getBossBar().removePlayer(playerData.getPlayer());
                    playerData.disableExecuteReduce();
                    playerData.disableExecuteRefresh();
                    Player player = playerData.getPlayer();
                    if(player != null) playerData.disableStage(player, null);
                });
        if(ThirstBar.getInstance().getSqlManager().getConnection() == null) {
            ThirstBar.getInstance().getPlayersFile().save();
        }
    }

    public void loadData(){
        FileManager file = ThirstBar.getInstance().getPlayersFile();
        if(ThirstBar.getInstance().getSqlManager().getConnection() == null) {
            ConfigurationSection section = file.getConfigurationSection("");
            if(section != null) section.getKeys(false).forEach(name -> {
                PlayerData playerData = addData(name);
                BigDecimal max = new BigDecimal(file.getString(name+".Max", "0"));
                if(max.compareTo(BigDecimal.valueOf(1)) > 0) {
                    max = max.min(BigDecimal.valueOf(Double.MAX_VALUE));
                    playerData.setThirstMax(max.doubleValue());
//                    playerData.refresh();
                }
                playerData.setThirst(file.getDouble(name+".Thirst", playerData.getThirstMax()));
                Player player = Bukkit.getPlayer(playerData.getName());
                if(player != null) {
                    boolean disable = file.getBoolean(name + ".Disable", false);
                    if (disable) playerData.setDisable(true);
                    boolean check1 = false;
                    try {
                        check1 = ConfigData.DISABLED_GAMEMODE.stream().anyMatch(g ->
                                player.getGameMode().equals(GameMode.valueOf(g.toUpperCase())));
                    } catch (IllegalArgumentException ignore) {
                    }
                    boolean check2 = ConfigData.DISABLED_WORLDS.stream().anyMatch(w ->
                            player.getWorld().getName().trim().equalsIgnoreCase(w.trim()));
                    playerData.setDisableAll(check1 || check2);
                    playerData.updateAll(player);
                }
            });
        } else {
            List<List<HashMap<String, Object>>> list = ThirstBar.getInstance().getSqlManager().runGetPlayer();
            list.forEach(row -> {
                HashMap<String, Object> nameObj = row.stream().filter(v ->
                        v.getOrDefault("name", null) != null).findAny().orElse(null);
                HashMap<String, Object> disableObj = row.stream().filter(v ->
                        v.getOrDefault("disable", null) != null).findAny().orElse(null);
                HashMap<String, Object> thirstObj = row.stream().filter(v ->
                        v.getOrDefault("thirst", null) != null).findAny().orElse(null);
                HashMap<String, Object> maxObj = row.stream().filter(v ->
                        v.getOrDefault("max", null) != null).findAny().orElse(null);
                if(nameObj == null || disableObj == null || thirstObj == null || maxObj == null) return;
                String name = (String) nameObj.getOrDefault("name", null);
                boolean disable = (int) nameObj.getOrDefault("disable", 0) == 1;
                double thirst = (double) thirstObj.getOrDefault("thirst", -1);
                double max = (double) maxObj.getOrDefault("max", 0);
                if(name == null) return;
                PlayerData playerData = addData(name);
                if(disable) playerData.setDisable(true);
                if(thirst > 0) playerData.setThirst(thirst);
                if(max > 0){
                    playerData.setThirstMax(max);
//                    playerData.refresh();
                }
            });
        }

    }
}

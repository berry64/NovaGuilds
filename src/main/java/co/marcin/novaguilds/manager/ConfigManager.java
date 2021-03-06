/*
 *     NovaGuilds - Bukkit plugin
 *     Copyright (C) 2015 Marcin (CTRL) Wieczorek
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package co.marcin.novaguilds.manager;

import co.marcin.novaguilds.NovaGuilds;
import co.marcin.novaguilds.enums.Config;
import co.marcin.novaguilds.enums.DataStorageType;
import co.marcin.novaguilds.util.ItemStackUtils;
import co.marcin.novaguilds.util.LoggerUtils;
import co.marcin.novaguilds.util.StringUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
	private final NovaGuilds plugin;
	private FileConfiguration config;

	private DataStorageType primaryDataStorageType;
	private DataStorageType secondaryDataStorageType;
	private DataStorageType dataStorageType;

	private boolean useVanishNoPacket = true;

	private final List<PotionEffectType> guildEffects = new ArrayList<>();

	private final Map<Config, Object> cache = new HashMap<>();

	public static final Map<String, String> essentialsLocale = new HashMap<String, String>(){{
		put("en","en-en");
		put("pl","pl-pl");
		put("de","de-de");
		put("zh","zh-cn");
	}};

	public ConfigManager(NovaGuilds novaGuilds) {
		plugin = novaGuilds;
		NovaGuilds.getInstance().setConfigManager(this);
		reload();
		LoggerUtils.info("Enabled");
	}

	public void reload() {
		cache.clear();

		if(!new File(plugin.getDataFolder(),"config.yml").exists()) {
			LoggerUtils.info("Creating default config...");
			plugin.saveDefaultConfig();
		}

		plugin.reloadConfig();
		config = plugin.getConfig();

		if(Config.USETITLES.getBoolean()) {
			if(!plugin.getServer().getBukkitVersion().startsWith("1.8")) {
				Config.USETITLES.set(false);
				LoggerUtils.error("You can't use Titles with Bukkit older than 1.8");
			}
		}

		String primaryDataStorageTypeString = Config.DATASTORAGE_PRIMARY.getString().toUpperCase();
		String secondaryDataStorageTypeString = Config.DATASTORAGE_SECONDARY.getString().toUpperCase();

		boolean primaryValid = false;
		boolean secondaryValid = false;

		if(primaryDataStorageTypeString.equals(secondaryDataStorageTypeString)) {
			LoggerUtils.error("Primary and secondary data storage types cannot be the same!");
			LoggerUtils.error("Resetting to defaults. (MySQL/Flat)");
			primaryDataStorageTypeString = DataStorageType.MYSQL.name();
			secondaryDataStorageTypeString = DataStorageType.FLAT.name();
		}

		for(DataStorageType dst : DataStorageType.values()) {
			if(dst.name().equals(primaryDataStorageTypeString)) {
				primaryValid = true;
			}

			if(dst.name().equals(secondaryDataStorageTypeString)) {
				secondaryValid = true;
			}
		}

		if(!primaryValid || !secondaryValid) {
			LoggerUtils.error("Not valid Data Storage Types.");
			LoggerUtils.error("Resetting to defaults. (MySQL/Flat)");
			primaryDataStorageTypeString = DataStorageType.MYSQL.name();
			secondaryDataStorageTypeString = DataStorageType.FLAT.name();
		}

		if(primaryDataStorageTypeString.equalsIgnoreCase("sqlite") && !Config.DEBUG.getBoolean()) {
			primaryDataStorageTypeString = DataStorageType.MYSQL.name();
			LoggerUtils.error("Please enable debug mode to use SQLite storage.");
		}

		primaryDataStorageType = DataStorageType.valueOf(primaryDataStorageTypeString);
		secondaryDataStorageType = DataStorageType.valueOf(secondaryDataStorageTypeString);
		setToPrimaryDataStorageType();
		LoggerUtils.info("Data storage: Primary: "+primaryDataStorageType.name()+", Secondary: "+secondaryDataStorageType.name());

		//Effects
		List<String> guildEffectsString = Config.GUILD_EFFECT_LIST.getStringList();
		for(String effect : guildEffectsString) {
			PotionEffectType effectType = PotionEffectType.getByName(effect);
			if(effectType != null) {
				guildEffects.add(effectType);
			}
		}

		//Check time values
		if(Config.LIVEREGENERATION_TASKINTERVAL.getSeconds() < 60) {
			LoggerUtils.error("Live regeneration task interval can't be shorter than 60 seconds.");
			Config.LIVEREGENERATION_TASKINTERVAL.set("60s");
		}

		if(Config.CLEANUP_INTERVAL.getSeconds() < 60) {
			LoggerUtils.error("Cleanup interval can't be shorter than 60 seconds.");
			Config.CLEANUP_INTERVAL.set("60s");
		}

		if(Config.SAVEINTERVAL.getSeconds() < 60) {
			LoggerUtils.error("Save interval can't be shorter than 60 seconds.");
			Config.SAVEINTERVAL.set("60s");
		}
	}

	//getters
	public DataStorageType getDataStorageType() {
		return dataStorageType;
	}

	public List<PotionEffectType> getGuildEffects() {
		return guildEffects;
	}

	public boolean useVanishNoPacket() {
		return useVanishNoPacket;
	}

	public FileConfiguration getConfig() {
		return config;
	}

	//setters
	public void disableVanishNoPacket() {
		useVanishNoPacket = false;
	}

	public void setToSecondaryDataStorageType() {
		dataStorageType = secondaryDataStorageType;
	}

	public void setToPrimaryDataStorageType() {
		dataStorageType = primaryDataStorageType;
	}

	//Cache
	public Object getEnumConfig(Config c) {
		return cache.get(c);
	}

	public boolean isInCache(Config c) {
		return cache.containsKey(c);
	}

	public void putInCache(Config c, Object o) {
		if(!cache.containsKey(c)) {
			cache.put(c, o);
		}
	}

	public void removeFromCache(Config c) {
		if(cache.containsKey(c)) {
			cache.remove(c);
		}
	}

	//methods from enum
	public String getString(String path) {
		return config.getString(path) == null ? "" : config.getString(path);
	}

	public List<String> getStringList(String path) {
		return config.getStringList(path) == null ? new ArrayList<String>() : config.getStringList(path);
	}

	public long getLong(String path) {
		return config.getLong(path);
	}

	public int getInt(String path) {
		return config.getInt(path);
	}

	public double getDouble(String path) {
		return config.getDouble(path);
	}

	public boolean getBoolean(String path) {
		return config.getBoolean(path);
	}

	public int getSeconds(String path) {
		return StringUtils.StringToSeconds(getString(path));
	}

	public ItemStack getItemStack(String path) {
		return ItemStackUtils.stringToItemStack(getString(path));
	}

	public Material getMaterial(String path) {
		return Material.getMaterial((getString(path).contains(":")?org.apache.commons.lang.StringUtils.split(getString(path), ':')[0]:getString(path)).toUpperCase());
	}

	//STONE:1
	public byte getMaterialData(String path) {
		return Byte.valueOf(getString(path).contains(":")?org.apache.commons.lang.StringUtils.split(getString(path), ':')[1]:"0");
	}

	public List<ItemStack> getItemStackList(String path) {
		List<String> stringList = getStringList(path);
		List<ItemStack> itemStackList = new ArrayList<>();

		for(String string : stringList) {
			ItemStack is = ItemStackUtils.stringToItemStack(string);

			if(is != null) {
				itemStackList.add(is);
			}
		}

		return itemStackList;
	}

	public List<Material> getMaterialList(String path) {
		List<String> stringList = getStringList(path);
		List<Material> materialList = new ArrayList<>();

		for(String string : stringList) {
			Material material = Material.getMaterial(string);
			if(material != null) {
				materialList.add(material);
			}
		}

		return materialList;
	}

	public void set(String path, Object obj) {
		config.set(path, obj);
		removeFromCache(Config.fromPath(path));
	}
}

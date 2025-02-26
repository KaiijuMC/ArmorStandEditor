/*
 * ArmorStandEditor: Bukkit plugin to allow editing armor stand attributes
 * Copyright (C) 2016-2023  RypoFalem
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.rypofalem.armorstandeditor;

import io.github.rypofalem.armorstandeditor.language.Language;
import com.jeff_media.updatechecker.*;
import io.github.rypofalem.armorstandeditor.Metrics.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public class ArmorStandEditorPlugin extends JavaPlugin{

    //!!! DO NOT REMOVE THESE UNDER ANY CIRCUMSTANCES - Required for BStats and UpdateChecker !!!
    public static final int SPIGOT_RESOURCE_ID = 94503;  //Used for Update Checker
    private static final int PLUGIN_ID = 12668;		     //Used for BStats Metrics

    private NamespacedKey iconKey;
    private static ArmorStandEditorPlugin instance;
    private Language lang;
    
    //Server Version Detection: Paper or Spigot and Invalid NMS Version
    String nmsVersion;
    String languageFolderLocation = "lang/";
    String warningMCVer = "Minecraft Version: ";
    public boolean hasSpigot = false;
    public boolean hasPaper = false;
    String nmsVersionNotLatest = null;
    PluginDescriptionFile pdfFile = this.getDescription();
    static final String SEPARATOR_FIELD = "================================";

    public PlayerEditorManager editorManager;
    
    //Output for Updates
    boolean opUpdateNotification = false;
    boolean runTheUpdateChecker = false;
    double updateCheckerInterval;

    //Edit Tool Information
    Material editTool;
    String toolType;
    int editToolData = Integer.MIN_VALUE;
    boolean requireToolData = false;
    boolean requireToolName = false;
    String editToolName = null;
    boolean requireToolLore = false;
    String editToolLore = null;
    boolean allowCustomModelData = false;
    Integer customModelDataInt = Integer.MIN_VALUE;
    
    //GUI Settings
    boolean requireSneaking = false;
    boolean sendToActionBar = true;
    
    //Armor Stand Specific Settings
    double coarseRot;
    double fineRot;
    boolean glowItemFrames = false;
    boolean invisibleItemFrames = true;
    boolean armorStandVisibility = true;

    //Glow Entity Colors
    public Scoreboard scoreboard;
    public Team team;
    String lockedTeam = "ASLocked";

    private static ArmorStandEditorPlugin plugin;

    public ArmorStandEditorPlugin(){
        instance = this;
    }

    @Override
    public void onEnable() {

        if (!Scheduler.isFolia())
            scoreboard = Objects.requireNonNull(this.getServer().getScoreboardManager()).getMainScoreboard();

        //Get NMS Version
        nmsVersion = getNmsVersion();

        //Load Messages in Console
        getLogger().info("======= ArmorStandEditor =======");
        getLogger().info("Plugin Version: " + pdfFile.getVersion());

        //Minimum Version Check - No Lower than 1.13-API. Will be tuned out in the future
        if (    nmsVersion.startsWith("v1_8")  ||
                nmsVersion.startsWith("v1_9")  ||
                nmsVersion.startsWith("v1_10") ||
                nmsVersion.startsWith("v1_11") ||
                nmsVersion.startsWith("v1_12") ||
                nmsVersion.startsWith("v1_13")){
            getLogger().log(Level.WARNING,"Minecraft Version: {0} is unsupported. Please Update Immediately. Loading failed.",nmsVersion);
            getLogger().info(SEPARATOR_FIELD);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        //Also Warn People to Update if using nmsVersion lower than latest
        if (    nmsVersion.startsWith("v1_14") ||
                nmsVersion.startsWith("v1_15") ||
                nmsVersion.startsWith("v1_16") ||
                nmsVersion.startsWith("v1_17") ||
                nmsVersion.startsWith("v1_18") ){
            getLogger().log(Level.WARNING,"Minecraft Version: {0} is supported, but not latest.",nmsVersion);
            getLogger().log(Level.WARNING, "ArmorStandEditor will still work on your current version. Loading Continuing.");
        } else {
            getLogger().log(Level.INFO, "Minecraft Version: {0} is supported. Loading Continuing.",nmsVersion);
        }
        //Spigot Check
        hasSpigot = getHasSpigot();
        hasPaper = getHasPaper();

        //If Paper and Spigot are both FALSE - Disable the plugin
        if (!hasPaper && !hasSpigot){
            getLogger().severe("This plugin requires either Paper, Spigot or one of its forks to run. This is not an error, please do not report this!");
            getServer().getPluginManager().disablePlugin(this);
            getLogger().info(SEPARATOR_FIELD);
            return;
        } else {
            if (hasSpigot) {
                getLogger().log(Level.INFO,"SpigotMC: {0}",hasSpigot);
            } else {
                getLogger().log(Level.INFO,"PaperMC: {0}",hasPaper);
            }
        }

        getServer().getPluginManager().enablePlugin(this);
        if (!Scheduler.isFolia()) registerScoreboards(scoreboard);
        getLogger().info(SEPARATOR_FIELD);

        //saveResource doesn't accept File.separator on Windows, need to hardcode unix separator "/" instead
        updateConfig("", "config.yml");
        updateConfig(languageFolderLocation, "de_DE.yml");
        updateConfig(languageFolderLocation, "es_ES.yml");
        updateConfig(languageFolderLocation, "fr_FR.yml");
        updateConfig(languageFolderLocation, "ja_JP.yml");
        updateConfig(languageFolderLocation, "nl_NL.yml");
        updateConfig(languageFolderLocation, "pl_PL.yml");
        updateConfig(languageFolderLocation, "pt_BR.yml");
        updateConfig(languageFolderLocation, "ro_RO.yml");
        updateConfig(languageFolderLocation, "ru_RU.yml");
        updateConfig(languageFolderLocation, "test_NA.yml");
        updateConfig(languageFolderLocation, "uk_UA.yml");
        updateConfig(languageFolderLocation, "zh_CN.yml");

        //English is the default language and needs to be unaltered to so that there is always a backup message string
        saveResource("lang/en_US.yml", true);
        lang = new Language(getConfig().getString("lang"), this);

        //Rotation
        coarseRot = getConfig().getDouble("coarse");
        fineRot = getConfig().getDouble("fine");

        //Set Tool to be used in game
        toolType = getConfig().getString("tool");
        if (toolType != null) {
            editTool = Material.getMaterial(toolType); //Ignore Warning
        } else {
            getLogger().severe("Unable to get Tool for Use with Plugin. Unable to continue!");
            getLogger().info(SEPARATOR_FIELD);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        //Do we require a custom tool name?
        requireToolName = getConfig().getBoolean("requireToolName", false);
        if(requireToolName){
            editToolName = getConfig().getString("toolName", null);
            if(editToolName != null) editToolName = ChatColor.translateAlternateColorCodes('&', editToolName);
        }

        //Custom Model Data
        allowCustomModelData = getConfig().getBoolean("allowCustomModelData", false);

        if(allowCustomModelData){
            customModelDataInt = getConfig().getInt("customModelDataInt", Integer.MIN_VALUE);
        }

        //ArmorStandVisibility Node
        armorStandVisibility = getConfig().getBoolean("armorStandVisibility", true);

        //Is there NBT Required for the tool
        requireToolData = getConfig().getBoolean("requireToolData", false);

        if(requireToolData) {
            editToolData = getConfig().getInt("toolData", Integer.MIN_VALUE);
        }

        requireToolLore = getConfig().getBoolean("requireToolLore", false);

        if(requireToolLore) {
            editToolLore = getConfig().getString("toolLore", null);
            if(editToolLore != null) editToolLore = ChatColor.translateAlternateColorCodes('&', editToolLore);
        }

        //Require Sneaking - Wolfst0rm/ArmorStandEditor#17
        requireSneaking = getConfig().getBoolean("requireSneaking", false);

        //Send Messages to Action Bar
        sendToActionBar = getConfig().getBoolean("sendMessagesToActionBar", true);

        //All ItemFrame Stuff
        glowItemFrames = getConfig().getBoolean("glowingItemFrame", true);
        invisibleItemFrames = getConfig().getBoolean("invisibleItemFrames", true);

        //Add ability to enable ot Disable the running of the Updater
        runTheUpdateChecker = getConfig().getBoolean("runTheUpdateChecker", true);

        //Add Ability to check for UpdatePerms that Notify Ops - https://github.com/Wolfieheart/ArmorStandEditor/issues/86
        opUpdateNotification = getConfig().getBoolean("opUpdateNotification", true);
        updateCheckerInterval = getConfig().getDouble("updateCheckerInterval", 24);

        //Run UpdateChecker - Reports out to Console on Startup ONLY!
        if(!Scheduler.isFolia() && runTheUpdateChecker) {

            if(opUpdateNotification){
                runUpdateCheckerWithOPNotifyOnJoinEnabled();
            } else {
                runUpdateCheckerConsoleUpdateCheck();
            }

        }

        //Get Metrics from bStats
        getMetrics();

        editorManager = new PlayerEditorManager(this);
        CommandEx execute = new CommandEx(this);

        //CommandExecution and TabCompletion
        Objects.requireNonNull(getCommand("ase")).setExecutor(execute);
        Objects.requireNonNull(getCommand("ase")).setTabCompleter(execute);

        getServer().getPluginManager().registerEvents(editorManager, this);

    }

    private void runUpdateCheckerConsoleUpdateCheck() {
        if (getArmorStandEditorVersion().contains(".x")) {
            getLogger().warning("Note from the development team: ");
            getLogger().warning("It appears that you are using the development version of ArmorStandEditor");
            getLogger().warning("This version can be unstable and is not recommended for Production Environments.");
            getLogger().warning("Please, report bugs to: https://github.com/Wolfieheart/ArmorStandEditor . ");
            getLogger().warning("This warning is intended to be displayed when using a Dev build and is NOT A BUG!");
            getLogger().info("Update Checker does not work on Development Builds.");
        } else {
            new UpdateChecker(this, UpdateCheckSource.SPIGET, "" + SPIGOT_RESOURCE_ID + "")
                    .setDownloadLink("https://www.spigotmc.org/resources/armorstandeditor-reborn.94503/")
                    .setChangelogLink("https://www.spigotmc.org/resources/armorstandeditor-reborn.94503/history")
                    .setColoredConsoleOutput(true)
                    .setUserAgent(new UserAgentBuilder().addPluginNameAndVersion().addServerVersion())
                    .checkEveryXHours(updateCheckerInterval)
                    .checkNow();
        }
    }

    private void runUpdateCheckerWithOPNotifyOnJoinEnabled() { 
        if (getArmorStandEditorVersion().contains(".x")) {
            getLogger().warning("Note from the development team: ");
            getLogger().warning("It appears that you are using the development version of ArmorStandEditor");
            getLogger().warning("This version can be unstable and is not recommended for Production Environments.");
            getLogger().warning("Please, report bugs to: https://github.com/Wolfieheart/ArmorStandEditor . ");
            getLogger().warning("This warning is intended to be displayed when using a Dev build and is NOT A BUG!");
            getLogger().info("Update Checker does not work on Development Builds.");
        } else {
            new UpdateChecker(this, UpdateCheckSource.SPIGET, "" + SPIGOT_RESOURCE_ID + "")
                    .setDownloadLink("https://www.spigotmc.org/resources/armorstandeditor-reborn.94503/")
                    .setChangelogLink("https://www.spigotmc.org/resources/armorstandeditor-reborn.94503/history")
                    .setColoredConsoleOutput(true)
                    .setNotifyOpsOnJoin(true)
                    .setUserAgent(new UserAgentBuilder().addPluginNameAndVersion().addServerVersion())
                    .checkEveryXHours(updateCheckerInterval)
                    .checkNow();
        }
    }

    //Implement Glow Effects for Wolfstorm/ArmorStandEditor-Issues#5 - Add Disable Slots with Different Glow than Default
    private void registerScoreboards(Scoreboard scoreboard) {
        getLogger().info("Registering Scoreboards required for Glowing Effects");

        //Fix for Scoreboard Issue reported by Starnos - Wolfst0rm/ArmorStandEditor-Issues/issues/18
        if (scoreboard.getTeam(lockedTeam) == null) {
            scoreboard.registerNewTeam(lockedTeam);
            Objects.requireNonNull(scoreboard.getTeam(lockedTeam)).setColor(ChatColor.RED);
        } else {
            getLogger().info("Scoreboard for ASLocked Already exists. Continuing to load");
        }
    }

    private void unregisterScoreboards(Scoreboard scoreboard) {
        getLogger().info("Removing Scoreboards required for Glowing Effects");

        team = scoreboard.getTeam(lockedTeam);
        if(team != null) { //Basic Sanity Check to ensure that the team is there
            team.unregister();
        } else{
            getLogger().severe("Team Already Appears to be removed. Please do not do this manually!");
        }
    }

    private void updateConfig(String folder, String config) {
        if(!new File(getDataFolder() + File.separator + folder + config).exists()){
            saveResource(folder  + config, false);
        }
    }

    @Override
    public void onDisable(){
        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(player.getOpenInventory().getTopInventory().getHolder() == editorManager.getMenuHolder()) player.closeInventory();
        }

        if (!Scheduler.isFolia()) {
            scoreboard = Objects.requireNonNull(this.getServer().getScoreboardManager()).getMainScoreboard();
            unregisterScoreboards(scoreboard);
        }
    }

    public String getNmsVersion(){
        return this.getServer().getClass().getPackage().getName().replace(".",",").split(",")[3];
    }

    public boolean getHasSpigot(){
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            nmsVersionNotLatest = "SpigotMC ASAP.";
            return true;
        } catch (ClassNotFoundException e){
            nmsVersionNotLatest = "";
            return false;
        }
    }

    public boolean getHasPaper(){
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            nmsVersionNotLatest = "SpigotMC ASAP.";
            return true;
        } catch (ClassNotFoundException e){
            nmsVersionNotLatest = "";
            return false;
        }
    }

    public String getArmorStandEditorVersion(){ return getConfig().getString("version"); }

    public boolean getArmorStandVisibility(){
        return getConfig().getBoolean("armorStandVisibility");
    }

    public boolean getItemFrameVisibility(){
        return getConfig().getBoolean("invisibleItemFrames");
    }

    public Language getLang(){
        return lang;
    }

    public boolean getAllowCustomModelData() {
        return this.getConfig().getBoolean("allowCustomModelData");
    }

    public Material getEditTool() {
        return this.editTool;
    }

    public boolean getRunTheUpdateChecker() {
        return this.getConfig().getBoolean("runTheUpdateChecker");
    }

    public Integer getCustomModelDataInt() { return this.getConfig().getInt("customModelDataInt"); }

    public boolean isEditTool(ItemStack itemStk){
        if (itemStk == null) { return false; }
        if (editTool != itemStk.getType()) { return false; }

        ItemMeta itemMeta = itemStk.getItemMeta();
        if(itemMeta == null) return false;

        //FIX: Depreciated Stack for getDurability
        if (requireToolData){
            Damageable d1 = (Damageable) itemMeta; //Get the Damageable Options for itemStk
            if (d1 != null) { //We do this to prevent NullPointers
                if (d1.getDamage() != (short) editToolData) { return false; }
            }
        }

        if(requireToolName && editToolName != null){
            if(!itemStk.hasItemMeta()) { return false; }

            //Get the name of the Edit Tool - If Null, return false
            String itemName = itemMeta.getDisplayName();

            //If the name of the Edit Tool is not the Name specified in Config then Return false
            if(!itemName.equals(editToolName)) { return false; }

        }

        if(requireToolLore && editToolLore != null){

            //If the ItemStack does not have Metadata then we return false
            if(!itemStk.hasItemMeta()) { return false; }

            //Get the lore of the Item and if it is null - Return False
            String itemLore = String.valueOf(itemMeta.getLore().get(0));

            //If the Item does not have Lore - Return False
            boolean hasTheItemLore = itemMeta.hasLore();
            if (!hasTheItemLore)  { return false; }

            //Item the first thing in the ItemLore List does not Equal the Config Value "editToolLore" - return false
            if (!itemLore.equalsIgnoreCase(editToolLore))  { return false; } //Does not need simplified - IntelliJ likes to complain here

        }

        if (allowCustomModelData && customModelDataInt != null) {
            //If the ItemStack does not have Metadata then we return false
            if(!itemStk.hasItemMeta()) { return false; }
            Integer itemCustomModel = itemMeta.getCustomModelData();
            return itemCustomModel.equals(customModelDataInt);
        }
        return true;
    }

    public static ArmorStandEditorPlugin instance(){
        return instance;
    }

    //Metrics/bStats Support
    private void getMetrics(){

        Metrics metrics = new Metrics(this, PLUGIN_ID);

        //RequireToolLore Metric
        metrics.addCustomChart(new SimplePie("tool_lore_enabled", () -> getConfig().getString("requireToolLore")));

        //RequireToolData
        metrics.addCustomChart(new SimplePie("tool_data_enabled", () -> getConfig().getString("requireToolData")));

        //Send Messages to ActionBar
        metrics.addCustomChart(new SimplePie("action_bar_messages", () -> getConfig().getString("sendMessagesToActionBar")));

        //Check for Sneaking
        metrics.addCustomChart(new SimplePie("require_sneaking", () -> getConfig().getString("requireSneaking")));

        //Language is used
        metrics.addCustomChart(new DrilldownPie("language_used", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();

            String languageUsed = getConfig().getString("lang");
            assert languageUsed != null;

            if (languageUsed.startsWith("nl")) {
                map.put("Dutch", entry);
            } else if (languageUsed.startsWith("de")) {
                map.put("German", entry);
            } else if (languageUsed.startsWith("en")) {
                map.put("English", entry);
            } else if (languageUsed.startsWith("es")) {
                map.put("Spanish", entry);
            } else if (languageUsed.startsWith("fr")) {
                map.put("French", entry);
            } else if (languageUsed.startsWith("ja")) {
                map.put("Japanese", entry);
            } else if (languageUsed.startsWith("pl")) {
                map.put("Polish", entry);
            }else if(languageUsed.startsWith("ru")){ //See PR# 41 by KPidS
                map.put("Russian", entry);
            }else if(languageUsed.startsWith("ro")){
                map.put("Romanian", entry);
            } else if(languageUsed.startsWith("uk")){
                map.put("Ukrainian", entry);
            } else if(languageUsed.startsWith("zh")) {
                map.put("Chinese", entry);
            } else if(languageUsed.startsWith("pt")) {
                map.put("Brazilian", entry);
            } else{
                map.put("Other", entry);
            }
            return map;
        }));

        //ArmorStandInvis Config
        metrics.addCustomChart(new SimplePie("armor_stand_invisibility_usage", () -> getConfig().getString("armorStandVisibility")));

        //ArmorStandInvis Config
        metrics.addCustomChart(new SimplePie("itemframe_invisibility_used", () -> getConfig().getString("invisibleItemFrames")));

        //Add tracking to see who is using Custom Naming in BStats
        metrics.addCustomChart(new SimplePie("custom_toolname_enabled", () -> getConfig().getString("requireToolName")));

        metrics.addCustomChart(new SimplePie("using_the_update_checker", () -> getConfig().getString("runTheUpdateChecker")));
        metrics.addCustomChart(new SimplePie("op_updates", () -> getConfig().getString("opUpdateNotification")));


    }

    public NamespacedKey getIconKey() {
        if(iconKey == null) iconKey = new NamespacedKey(this, "command_icon");
        return iconKey;
    }

}

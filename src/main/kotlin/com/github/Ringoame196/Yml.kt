package com.github.Ringoame196

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File

class Yml(private val plugin: Plugin) {
    fun acquisitionYml(path: String, fileName: String): YamlConfiguration {
        val playerDataFolder = File(plugin.dataFolder, path)
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs()
        }

        val filePath = File(playerDataFolder, "$fileName.yml")
        return YamlConfiguration.loadConfiguration(filePath)
    }
    fun acquisitionShortcutName(yml: YamlConfiguration): MutableList<String> {
        return yml.getKeys(false).toMutableList()
    }
    fun superscription(path: String, fileName: String, yml: YamlConfiguration) {
        val playerDataFolder = File(plugin.dataFolder, path)
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs()
        }

        val file = File(playerDataFolder, "$fileName.yml")
        yml.save(file)
    }
}

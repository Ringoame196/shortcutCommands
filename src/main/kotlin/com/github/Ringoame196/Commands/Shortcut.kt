package com.github.Ringoame196.Commands

import com.github.Ringoame196.Yml
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin

class Shortcut(plugin: Plugin) : CommandExecutor, TabExecutor {
    private val yml = Yml(plugin)
    private val shortcutsFile = yml.acquisitionYml("", "shortcuts")
    // 処理マップ
    private val processingMap = mapOf<String, (args: Array<out String>, sender: CommandSender) -> Any>(
        "make" to { args: Array<out String>, sender: CommandSender ->
            makeShortcut(sender, args)
        },
        "add" to { args: Array<out String>, sender: CommandSender ->
            addCommand(sender, args)
        },
        "remove" to { args: Array<out String>, sender: CommandSender ->
            removeCommand(sender, args)
        },
        "delete" to { args: Array<out String>, sender: CommandSender ->
            deleteShortcut(sender, args)
        },
        "run" to { args: Array<out String>, sender: CommandSender ->
            runCommand(sender, args)
        }
    )
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.size < 2) { return false }
        processingMap[args[0]]?.invoke(args, sender) ?: return false
        return true
    }
    private fun isOP(sender: CommandSender): Boolean {
        return if (!sender.isOp) {
            sender.sendMessage("${ChatColor.RED}権限がありません")
            false
        } else { true }
    }
    private fun acquisitionShortCutKeys(): MutableList<String> {
        return yml.acquisitionShortcutName(shortcutsFile)
    }
    private fun isContainsShortcut(shortcutName: String): Boolean {
        return acquisitionShortCutKeys().contains(shortcutName)
    }
    private fun makeShortcut(sender: CommandSender, args: Array<out String>) {
        if (!isOP(sender)) {
            return
        }
        val shortcutName = args[1]
        if (isContainsShortcut(shortcutName)) {
            sender.sendMessage("${ChatColor.RED}同じ名前のショートカットを作成することはできません")
            return
        }
        shortcutsFile.set(shortcutName, listOf<String>())
        saveFile(sender, shortcutsFile, "ショートカットを作成しました")
    }
    private fun deleteShortcut(sender: CommandSender, args: Array<out String>) {
        val shortcutName = args[1]
        if (!isOP(sender)) {
            return
        }
        if (!isContainsShortcut(shortcutName)) {
            sender.sendMessage("${ChatColor.RED}ショートカットが見つかりませんでした")
            return
        }
        shortcutsFile.set(shortcutName, null)
        saveFile(sender, shortcutsFile, "ショートカットを削除しました")
    }
    private fun addCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size <= 2) { return }
        if (!isOP(sender)) {
            return
        }
        val shortcutName = args[1]
        if (!isContainsShortcut(shortcutName)) {
            sender.sendMessage("${ChatColor.RED}ショートカットが見つかりませんでした")
            return
        }
        val command = args.copyOfRange(2, args.size).joinToString(" ").replace("/", "")

        // ショートカットが存在しない場合は新しいリストを作成
        val commands = shortcutsFile.getList("$shortcutName.commands")?.toMutableList() ?: mutableListOf()
        commands.add(command)

        shortcutsFile.set("$shortcutName.commands", commands)
        saveFile(sender, shortcutsFile, "コマンドを追加しました")
    }
    private fun removeCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size <= 2) { return }
        if (!isOP(sender)) {
            return
        }
        val shortcutName = args[1]
        if (!isContainsShortcut(shortcutName)) {
            sender.sendMessage("${ChatColor.RED}ショートカットが見つかりませんでした")
            return
        }
        val command = args.copyOfRange(2, args.size).joinToString(" ")

        // ショートカットが存在しない場合は新しいリストを作成
        val commands = shortcutsFile.getList("$shortcutName.commands")?.toMutableList() ?: mutableListOf()
        commands.remove(command)

        shortcutsFile.set("$shortcutName.commands", commands)
        saveFile(sender, shortcutsFile, "コマンドを削除しました")
    }
    private fun runCommand(sender: CommandSender, args: Array<out String>) {
        val shortcutName = args[1]
        val commands = shortcutsFile.getList("$shortcutName.commands")
        if (commands == null) {
            sender.sendMessage("${ChatColor.RED}ショートカットが見つかりませんでした")
            return
        }
        for (command in commands) {
            command as String
            // 無限ループ対策
            if (command.contains("shortcut ")) {
                sender.sendMessage("${ChatColor.RED}ショートカット内でショートカットを実行することは禁止されています")
                continue
            }
            val executionCommand = Bukkit.dispatchCommand(sender, command)
            if (!executionCommand) {
                sender.sendMessage("${ChatColor.RED}実行不可能のコマンドが含まれています")
                sender.sendMessage("${ChatColor.RED}実行不能のコマンド：$command")
                continue
            }
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): MutableList<String>? {
        return when (args.size) {
            1 -> processingMap.keys.toMutableList()
            2 -> shortcutName()
            3 -> displayCommand(args)
            else -> mutableListOf()
        }
    }
    private fun displayCommand(args: Array<out String>): MutableList<String>? {
        val subCommand = args[0]
        val shortcutName = args[1]
        return when (subCommand) {
            "add" -> mutableListOf("[コマンド]")
            "remove" -> {
                val commands = shortcutsFile.getList("$shortcutName.commands")?.toMutableList()
                commands?.add("[コマンド]")
                commands as MutableList<String>
            }

            else -> mutableListOf()
        }
    }
    private fun shortcutName(): MutableList<String> {
        val shortcutKeys = acquisitionShortCutKeys()
        val shortcutNames = shortcutKeys.toMutableList()
        shortcutNames.add("[ショートカット名]")
        return shortcutNames
    }
    private fun saveFile(sender: CommandSender, file: YamlConfiguration, successMessages: String) {
        try {
            yml.superscription("", "shortcuts", shortcutsFile)
            sender.sendMessage("${ChatColor.YELLOW}$successMessages")
        } catch (e: Exception) {
            sender.sendMessage("${ChatColor.RED}エラーが起きました(詳細はコンソール)")
            Bukkit.getConsoleSender().sendMessage(e.message)
        }
    }
}

package com.u24game.custommod.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import com.u24game.custommod.module.ModModule;
import com.u24game.custommod.module.ModuleManager;

public class CustomModCommand extends CommandBase {

    private final ModuleManager moduleManager;

    public CustomModCommand(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public String getCommandName() {
        return "custommod";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/custommod [modules|architecture]";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("cmod");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || "modules".equalsIgnoreCase(args[0]) || "architecture".equalsIgnoreCase(args[0])) {
            sender.addChatMessage(new ChatComponentText("CustomMod architecture: modular monolith, institution core + capability modules"));
            for (ModModule module : moduleManager.getModules()) {
                sender.addChatMessage(new ChatComponentText("- [" + module.getCategory() + "] " + module.getDisplayName() + " (" + module.getId() + ")"));
            }
            return;
        }

        sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, new String[] { "modules", "architecture" });
        }
        return new ArrayList<String>();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
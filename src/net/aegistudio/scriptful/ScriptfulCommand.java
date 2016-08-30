package net.aegistudio.scriptful;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public enum ScriptfulCommand {
	UNLOAD(" unload", "unload an already loaded plugin.") {
		public void execute(Scriptful scriptful, CommandSender sender, String label, String[] arguments) {
			if(arguments.length == 0)
				sender.sendMessage("/" + label + hierarch + " <name>");
			else {
				ScriptSurrogator surrogator = scriptful.surrogators.remove(arguments[0]);
				if(surrogator == null) {
					sender.sendMessage(ChatColor.RED + "Specified plugin " + arguments[0] + " not loaded!");
					return;
				}
				try {
					surrogator.callUnload();
					sender.sendMessage("Successfully unloaded plugin " + arguments[0] + "!");
				}
				catch(Exception e) {
					sender.sendMessage(ChatColor.RED + "Error on unloading plugin " + arguments[0] + "!");
					scriptful.getLogger().log(Level.WARNING, "UnloadFail", e);
				}
			}
		}
		
		public List<String> tabComplete(Scriptful scriptful, CommandSender sender, String label, String[] arguments) {
			if(arguments.length == 1) {
				Stream<String> startPoint = scriptful.surrogators.keySet().stream();
				return Arrays.asList(startPoint.filter(
						str -> str.startsWith(arguments.length == 0? "" : arguments[0])).toArray(String[]::new));
			}
			return null;
		}
	},
	LOAD(" load", "load a previously disabled / newly add plugin.") {
		public void execute(Scriptful scriptful, CommandSender sender, String label, String[] arguments) {
			if(arguments.length == 0)
				sender.sendMessage("/" + label + hierarch + " <name>");
			else {
				File target = scriptful.listLoadable().get(arguments[0]);
				if(target == null) {
					sender.sendMessage(ChatColor.RED + "Specified plugin " + arguments[0] + " cannot be loaded!");
					return;
				}
				try {
					scriptful.loadFile(target);
					sender.sendMessage("Successfully loaded plugin " + arguments[0] + "!");
				}
				catch(Exception e) {
					sender.sendMessage(ChatColor.RED + "Error on loading plugin " + arguments[0] + "!");
					scriptful.getLogger().log(Level.WARNING, "LoadFail", e);
				}
			}
		}
		
		public List<String> tabComplete(Scriptful scriptful, CommandSender sender, String label, String[] arguments) {
			if(arguments.length == 1) {
				Stream<String> startPoint = scriptful.listLoadable().keySet().stream()
						.filter(str -> !scriptful.surrogators.containsKey(str));
				return Arrays.asList(startPoint.filter(
						str -> str.startsWith(arguments.length == 0? "" : arguments[0])).toArray(String[]::new));
			}
			return null;
		}
	},
	RELOAD(" reload", "reload an already loaded plugin.") {
		public void execute(Scriptful scriptful, CommandSender sender, String label, String[] arguments) {
			if(arguments.length == 0)
				sender.sendMessage("/" + label + hierarch + " <name>");
			else {
				ScriptSurrogator surrogator = scriptful.surrogators.remove(arguments[0]);
				if(surrogator == null) {
					sender.sendMessage(ChatColor.RED + "Specified plugin " + arguments[0] + " not loaded!");
					return;
				}
				
				File target = scriptful.listLoadable().get(arguments[0]);
				if(target == null) {
					sender.sendMessage(ChatColor.RED + "Specified plugin " + arguments[0] + " has already been removed!");
					return;
				}
				
				try {
					surrogator.callUnload();
					scriptful.loadFile(target);
					sender.sendMessage("Successfully loaded plugin " + arguments[0] + "!");
				}
				catch(Exception e) {
					sender.sendMessage(ChatColor.RED + "Error on reloading plugin " + arguments[0] + "!");
					scriptful.getLogger().log(Level.WARNING, "ReloadFail", e);
				}
			}
		}
		
		public List<String> tabComplete(Scriptful scriptful, CommandSender sender, String label, String[] arguments) {
			return UNLOAD.tabComplete(scriptful, sender, label, arguments);
		}
	},
	EXEC(" exec", "execute code under context of a loaded plugin") {
		public void execute(Scriptful scriptful, CommandSender sender, String label, String[] arguments) {
			if(arguments.length < 2)
				sender.sendMessage("/" + label + hierarch + " <name> <code>");
			else {
				ScriptSurrogator surrogator = scriptful.surrogators.get(arguments[0]);
				if(surrogator == null) {
					sender.sendMessage(ChatColor.RED + "Specified plugin " + arguments[0] + " not loaded!");
					return;
				}
				StringBuilder statement = new StringBuilder(arguments[1]);
				for(int i = 2; i < arguments.length; i ++) {
					statement.append(" ");
					statement.append(arguments[i]);
				}
				
				try {
					sender.sendMessage(arguments[0] + "> " + statement.toString());
					Object result = surrogator.engine.eval(statement.toString());
					if(result != null) sender.sendMessage(result.toString());
				}
				catch(Exception e) {
					sender.sendMessage(ChatColor.RED + e.getMessage());
				}
			}
		}
		
		public List<String> tabComplete(Scriptful scriptful, CommandSender sender, String label, String[] arguments) {
			return UNLOAD.tabComplete(scriptful, sender, label, arguments);
		}
	},
	NONE("", "", LOAD, UNLOAD, RELOAD, EXEC);
	
	public final String hierarch;
	public final String description;
	public final ScriptfulCommand[] subcommands;
	private final Map<String, ScriptfulCommand> subcommandMap = new TreeMap<String, ScriptfulCommand>();
	
	private ScriptfulCommand(String hierarch, String description, ScriptfulCommand... subcommands) {
		this.hierarch = hierarch;
		this.description = description;
		this.subcommands = subcommands;
		for(ScriptfulCommand subcommand : subcommands)
			this.subcommandMap.put(subcommand.name().toLowerCase(), subcommand);
	}
	
	public void execute(Scriptful scriptful, CommandSender sender, String label, String[] arguments) {
		if(arguments.length >= 1) {
			ScriptfulCommand subcommand = subcommandMap.get(arguments[0]);
			if(subcommand != null) {
				String[] newArgs = new String[arguments.length - 1];
				System.arraycopy(arguments, 1, newArgs, 0, newArgs.length);
				subcommand.execute(scriptful, sender, label, newArgs);
				return;
			}
		}
		sender.sendMessage("Listing subcommands for " + ChatColor.AQUA + "/" + label + hierarch + ChatColor.RESET + ":");
		for(ScriptfulCommand subcommand : subcommands)
			sender.sendMessage(ChatColor.AQUA + "/" + label 
					+ ChatColor.BLUE + subcommand.hierarch + ChatColor.RESET 
					+ ": " + subcommand.description);
		sender.sendMessage("This is the last page of subcommands for " 
				+ ChatColor.AQUA + "/" + label + hierarch + ChatColor.RESET + ".");
	}
	
	public List<String> tabComplete(Scriptful scriptful, CommandSender sender, String label, String[] arguments) {
		if(arguments.length >= 1) {
			ScriptfulCommand subcommand = subcommandMap.get(arguments[0]);
			if(subcommand != null) {
				String[] newArgs = new String[arguments.length - 1];
				System.arraycopy(arguments, 1, newArgs, 0, newArgs.length);
				return subcommand.tabComplete(scriptful, sender, label, newArgs);
			}
		}
		String argument = arguments.length == 1? arguments[0] : "";
		return Arrays.asList(subcommandMap.keySet().stream()
				.filter(str -> str.startsWith(argument)).toArray(String[]::new));
	}
}

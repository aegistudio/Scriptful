package net.aegistudio.scriptful;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.script.Invocable;
import javax.script.ScriptEngine;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;

import net.aegistudio.scriptful.abstraction.CommandExecAbstract;
import net.aegistudio.scriptful.abstraction.ListenerAbstract;
import net.aegistudio.scriptful.abstraction.TabCompleterAbstract;
import net.aegistudio.scriptful.execution.FunctionExecutor;
import net.aegistudio.scriptful.execution.MethodExecutor;

public class ScriptSurrogator {
	String name;
	ScriptEngine engine;
	Scriptful parent;
	File dataFolder;
	
	public ScriptSurrogator(String pluginName, ScriptEngine engine, Scriptful parent, File dataFolder) {
		this.name = pluginName;
		this.engine = engine;
		this.parent = parent;
		this.dataFolder = dataFolder;
		
		reloadConfig();
	}
	
	public Server getServer() {
		return parent.getServer();
	}
	
	public File getDataFolder() {
		return dataFolder;
	}
	
	public void callUnload() throws Exception {
		if(unload != null) ((Invocable)engine).invokeFunction(unload);
		
		// Unregister handlers.
		for(ListenerAbstract<FunctionExecutor> function : functions) 
			getHandlerList(function.event).unregister(function);
		for(ListenerAbstract<MethodExecutor> method : methods) 
			getHandlerList(method.event).unregister(method);
		
		// Unregister commands.
		Field knownCommands = getCommandMap().getClass().getDeclaredField("knownCommands");
		knownCommands.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<String, Command> commands = (Map<String, Command>) knownCommands.get(getCommandMap());
		knownCommands.setAccessible(false);
		
		Iterator<Entry<String, Command>> commandIterator = commands.entrySet().iterator();
		for(Entry<String, Command> current = commandIterator.next(); 
				commandIterator.hasNext(); current = commandIterator.next())
			if(current.getKey().startsWith(name + ":")) commandIterator.remove();
		
		this.commands.keySet().forEach(c -> commands.remove(c));
	}
	
	/********************************* Event listener related *******************************************/
	private HandlerList getHandlerList(Class<? extends Event> event) throws Exception {
		return (HandlerList)(event.getMethod("getHandlerList").invoke(null));
	}
	
	public TreeSet<ListenerAbstract<FunctionExecutor>> functions = new TreeSet<>();
	public void registerListener(String name, EventPriority priority, 
			boolean ignoreCancelled, Class<? extends Event> event) {
		
		ListenerAbstract<FunctionExecutor> function = new ListenerAbstract<>(
				new FunctionExecutor((Invocable)engine, name), event);
		if(functions.add(function)) getServer().getPluginManager()
			.registerEvent(event, function, priority, function, parent, ignoreCancelled);
	}
	
	public void unregisterListener(String name, Class<? extends Event> event) throws Exception {
		ListenerAbstract<FunctionExecutor> function = new ListenerAbstract<>(
				new FunctionExecutor((Invocable)engine, name), event);
		if(functions.remove(function)) getHandlerList(event).unregister(function);
	}
	
	public TreeSet<ListenerAbstract<MethodExecutor>> methods = new TreeSet<>();
	public void registerListener(Object instance, String name, EventPriority priority, 
			boolean ignoreCancelled, Class<? extends Event> event) {
		ListenerAbstract<MethodExecutor> method = new ListenerAbstract<>(
				new MethodExecutor((Invocable)engine, instance, name), event);
		if(methods.add(method)) getServer().getPluginManager()
			.registerEvent(event, method, priority, method, parent, ignoreCancelled);
	}
	
	public void unregisterListener(Object instance, String name, Class<? extends Event> event) throws Exception {
		ListenerAbstract<MethodExecutor> method = new ListenerAbstract<>(
				new MethodExecutor((Invocable)engine, instance, name), event);
		if(methods.remove(method)) getHandlerList(event).unregister(method);
	}
	
	@SuppressWarnings("unchecked")
	public void registerListener(Map<String, Object> listener) throws Exception {
		if(!listener.containsKey("event")) 
			throw new Exception("The listener should contain an subscribing event.");

		if(!listener.containsKey("handle"))
			throw new Exception("The listener should contain a handler");
		
		Class<? extends Event> eventClass;
		if(listener.get("event") instanceof Class) eventClass = (Class<? extends Event>) listener.get("event");
		else eventClass = (Class<? extends Event>) Class.forName(listener.get("event").toString());
		
		EventPriority priority = (EventPriority) listener.getOrDefault("priority", EventPriority.NORMAL);
		boolean ignoreCancelled = (boolean) listener.getOrDefault("ignoreCancelled", false);
		
		registerListener(listener, "handle", priority, ignoreCancelled, eventClass);
	}
	
	/********************************* Unload method related ************************************************/
	String unload = null;
	public void setUnload(String hook) {
		this.unload = hook;
	}
	
	/********************************* Yaml configuration related *******************************************/
	YamlConfiguration config;
	public YamlConfiguration getConfig() {
		return config;
	}
	
	public void reloadConfig() {
		File configFile = new File(dataFolder, "config.yml");
		if(configFile.exists()) config = YamlConfiguration.loadConfiguration(configFile);
		else config = new YamlConfiguration();
	}
	
	public void saveConfig() throws IOException {
		config.save(new File(dataFolder, "config.yml"));
	}
	
	/********************************* Command execution related *******************************************/
	TreeMap<String, PluginCommand> commands = new TreeMap<>();
	private CommandMap getCommandMap() throws Exception {
		PluginManager manager = parent.getServer().getPluginManager();
		Field commandMap = manager.getClass().getDeclaredField("commandMap");
		try {
			commandMap.setAccessible(true);
			return (CommandMap) commandMap.get(manager);
		}
		finally {
			commandMap.setAccessible(false);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void registerCommand(Map<String, Object> metadata) throws Exception {
		String name = metadata.get("name").toString();
		if(name == null) throw new Exception("You must specify the name of command.");
		if(commands.containsKey(name)) return;
		
		Constructor<PluginCommand> commandConstructor = 
			(Constructor<PluginCommand>) PluginCommand.class
				.getDeclaredConstructors()[0];
		
		commandConstructor.setAccessible(true);
		PluginCommand command = commandConstructor.newInstance(name, parent);
		commandConstructor.setAccessible(false);
		
		getCommandMap().register(this.name, command);
		
		commands.put(name, command);
		
		if(metadata.containsKey("description")) 
			command.setDescription(metadata.get("description").toString());
		
		if(metadata.containsKey("usage"))
			command.setUsage(metadata.get("usage").toString());
		
		if(metadata.containsKey("permission"))
			command.setPermission(metadata.get("permission").toString());
		
		if(metadata.containsKey("aliases"))
			command.setAliases((List<String>)metadata.get("aliases"));
		
		if(metadata.containsKey("permissionMessage"))
			command.setPermissionMessage(metadata.get("permissionMessage").toString());
		
		if(metadata.containsKey("executor"))
			registerExecutor(name, metadata, "executor");
		
		if(metadata.containsKey("tabCompleter"))
			registerExecutor(name, metadata, "tabCompleter");
	}
	
	public void registerExecutor(String name, String function) throws Exception {
		PluginCommand command = commands.get(name);
		command.setExecutor(new CommandExecAbstract<FunctionExecutor>(
				new FunctionExecutor((Invocable) engine, function)));
	}
	
	public void registerExecutor(String name, Object instance, String function) throws Exception {
		PluginCommand command = commands.get(name);
		command.setExecutor(new CommandExecAbstract<MethodExecutor>(
				new MethodExecutor((Invocable) engine, instance, function)));
	}
	
	public void registerTabCompleter(String name, String function) throws Exception {
		PluginCommand command = commands.get(name);
		command.setTabCompleter(new TabCompleterAbstract<FunctionExecutor>(
				new FunctionExecutor((Invocable) engine, function)));
	}
	
	public void registerTabCompleter(String name, Object instance, String function) throws Exception {
		PluginCommand command = commands.get(name);
		command.setTabCompleter(new TabCompleterAbstract<MethodExecutor>(
				new MethodExecutor((Invocable) engine, instance, function)));
	}
}
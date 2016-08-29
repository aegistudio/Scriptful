package net.aegistudio.scriptful;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import javax.script.Invocable;
import javax.script.ScriptEngine;

import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;

public class ScriptSurrogator {
	ScriptEngine engine;
	Scriptful parent;
	File dataFolder;
	
	public ScriptSurrogator(ScriptEngine engine, Scriptful parent, File dataFolder) {
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
	
	private HandlerList getHandlerList(Class<? extends Event> event) throws Exception {
		return (HandlerList)(event.getMethod("getHandlerList").invoke(null));
	}
	
	public TreeSet<ListenerAbstract<FunctionExecutor>> functions = new TreeSet<>();
	public <T extends Event> void registerListener(String name, EventPriority priority, Class<T> event) {
		ListenerAbstract<FunctionExecutor> function = new ListenerAbstract<FunctionExecutor>(
				new FunctionExecutor((Invocable)engine, name), event);
		if(functions.add(function)) getServer().getPluginManager()
			.registerEvent(event, function, priority, function, parent);
	}
	
	public <T extends Event> void unregisterListener(String name, Class<T> event) throws Exception {
		ListenerAbstract<FunctionExecutor> function = new ListenerAbstract<FunctionExecutor>(
				new FunctionExecutor((Invocable)engine, name), event);
		if(functions.remove(function)) getHandlerList(event).unregister(function);
	}
	
	public TreeSet<ListenerAbstract<MethodExecutor>> methods = new TreeSet<>();
	public <T extends Event> void registerListener(Object instance, String name, EventPriority priority, Class<T> event) {
		ListenerAbstract<MethodExecutor> method = new ListenerAbstract<MethodExecutor>(
				new MethodExecutor((Invocable)engine, instance, name), event);
		if(methods.add(method)) getServer().getPluginManager()
			.registerEvent(event, method, priority, method, parent);
	}
	
	public <T extends Event> void unregisterListener(Object instance, String name, Class<T> event) throws Exception {
		ListenerAbstract<MethodExecutor> method = new ListenerAbstract<MethodExecutor>(
				new MethodExecutor((Invocable)engine, instance, name), event);
		if(methods.remove(method)) getHandlerList(event).unregister(method);
	}
	
	String unload = null;
	public void setUnload(String hook) {
		this.unload = hook;
	}
	
	public void callUnload() throws Exception {
		if(unload != null) ((Invocable)engine).invokeFunction(unload);
		for(ListenerAbstract<FunctionExecutor> function : functions) 
			getHandlerList(function.event).unregister(function);
		for(ListenerAbstract<MethodExecutor> method : methods) 
			getHandlerList(method.event).unregister(method);
	}
	
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
}
package net.aegistudio.scriptful;

import java.io.File;
import java.util.TreeSet;

import javax.script.Invocable;
import javax.script.ScriptEngine;

import org.bukkit.Server;
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
	}
	
	public Server getServer() {
		return parent.getServer();
	}
	
	public File getDataFolder() {
		return dataFolder;
	}
	
	public TreeSet<FunctionListener<?>> functions = new TreeSet<>();
	public <T extends Event> void registerListener(String name, EventPriority priority, Class<T> event) {
		FunctionListener<T> function = new FunctionListener<T>((Invocable)engine, name, event);
		if(functions.add(function)) getServer().getPluginManager()
			.registerEvent(event, function, priority, function, parent);
	}
	
	private <T extends Event> HandlerList getHandlerList(Class<T> event) throws Exception {
		return (HandlerList)(event.getMethod("getHandlerList").invoke(null));
	}
	
	public <T extends Event> void unregisterListener(String name, Class<T> event) throws Exception {
		FunctionListener<T> function = new FunctionListener<T>((Invocable)engine, name, event);
		if(functions.remove(function)) getHandlerList(event).unregister(function);
	}
	
	public TreeSet<MethodListener<?>> methods = new TreeSet<>();
	public <T extends Event> void registerListener(Object instance, String name, EventPriority priority, Class<T> event) {
		MethodListener<T> method = new MethodListener<T>((Invocable) engine, instance, name, event);
		if(methods.add(method)) getServer().getPluginManager()
			.registerEvent(event, method, priority, method, parent);
	}
	
	public <T extends Event> void unregisterListener(Object instance, String name, Class<T> event) throws Exception {
		MethodListener<T> method = new MethodListener<T>((Invocable) engine, instance, name, event);
		if(methods.remove(method)) getHandlerList(event).unregister(method);
	}
	
	String unload = null;
	public void setUnload(String hook) {
		this.unload = hook;
	}
	
	public void callUnload() throws Exception {
		if(unload != null) ((Invocable)engine).invokeFunction(unload);
		for(FunctionListener<?> function : functions) getHandlerList(function.event).unregister(function);
		for(MethodListener<?> method : methods) getHandlerList(method.event).unregister(method);
	}
}

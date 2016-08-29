package net.aegistudio.scriptful;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;

public class Scriptful extends JavaPlugin {
	ScriptEngineManager manager;
	TreeMap<String, ScriptSurrogator> surrogators = new TreeMap<String, ScriptSurrogator>();
	
	public void onLoad() {
		manager = new ScriptEngineManager();
	}
	
	public void onEnable() {
		// Print languages.
		StringBuilder builder = new StringBuilder("Supported languages: ");
		manager.getEngineFactories().forEach(e -> builder.append(e.getLanguageName()).append(", "));
		String result = new String(builder);
		getLogger().log(Level.INFO, result.substring(0, result.length() - 2));
		
		// Make data folder.
		if(!getDataFolder().exists()) getDataFolder().mkdir();
		
		// Read scripts.
		for(File file : getDataFolder().listFiles()) try {
			if(file.isDirectory()) continue;
			
			String fileName = file.getName();
			int i = fileName.lastIndexOf('.');
			String name = fileName.substring(0, i);
			String suffix = fileName.substring(i + 1).toLowerCase();
			
			File dataFolder = new File(getDataFolder(), name);
			if(!dataFolder.exists()) dataFolder.mkdir();
			
			switch(suffix) {
				case "zip":
				case "gzip":
				case "gz":
					makeZip(file, suffix, dataFolder);
				break;
				default:
					makeScript(file, name, suffix, dataFolder);
				break;
			}

		} catch(Exception e) {
			super.getLogger().log(Level.WARNING, "LoadFail", e);
		};
	}
	
	public void onDisable() {
		for(ScriptSurrogator surrogator : surrogators.values()) try {
			surrogator.callUnload();
		} catch(Exception e) {
			super.getLogger().log(Level.WARNING, "UnloadFail", e);
		};
	}
	
	private void setCommon(ScriptEngine engine, ScriptSurrogator surrogator) {
		engine.put("self", surrogator);
		
		TreeMap<String, EventPriority> priority = new TreeMap<String, EventPriority>();
		priority.put("monitor", EventPriority.MONITOR);
		priority.put("lowest", EventPriority.LOWEST);
		priority.put("low", EventPriority.LOW);
		priority.put("normal", EventPriority.NORMAL);
		priority.put("high", EventPriority.HIGH);
		priority.put("highest", EventPriority.HIGHEST);
		engine.put("priority", priority);
	}
	
	private void makeScript(File js, String name, String suffix, File dataFolder) throws Exception {
		ScriptEngine engine = manager.getEngineByExtension(suffix);
		if(engine == null) throw new Exception("No engine for " + suffix);
		
		ScriptSurrogator surrogator = new ScriptSurrogator(engine, this, dataFolder);
		setCommon(engine, surrogator);
		
		engine.eval(new FileReader(js));
		
		surrogators.put(name, surrogator);
	}
	
	@SuppressWarnings("resource")
	private void makeZip(File file, String name, File dataFolder) throws Exception {
		ZipFile zip = new ZipFile(file);
		ZipEntry first = zip.stream().filter(
			e -> e.getName().startsWith("main.") || e.getName().startsWith(name + ".")
		).findFirst().get();
		
		String suffix = first.getName().substring(first.getName().lastIndexOf('.') + 1);
		
		ScriptEngine engine = manager.getEngineByExtension(suffix);
		if(engine == null) throw new Exception("No engine for " + suffix);
		
		ZipSurrogator surrogator = new ZipSurrogator(engine, this, dataFolder, zip);
		setCommon(engine, surrogator);
		
		engine.eval(new InputStreamReader(zip.getInputStream(first)));
		
		surrogators.put(name, surrogator);
	}
}

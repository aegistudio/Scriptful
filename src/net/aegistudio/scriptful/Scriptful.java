package net.aegistudio.scriptful;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;

public class Scriptful extends JavaPlugin {
	TreeMap<String, ScriptSurrogator> surrogators = new TreeMap<>();
	TreeMap<String, ScriptEngineFactory> factories = new TreeMap<>();
	URLClassLoader loader;
	
	public void onLoad() {
		// Load default factories.
		ScriptEngineManager manager = new ScriptEngineManager();
		manager.getEngineFactories().forEach(e -> e.getExtensions().forEach(x -> factories.put(x, e)));
		
		// Load classes.
		ArrayList<URL> url = new ArrayList<>();
		ArrayList<String> forNames = new ArrayList<>();
		for(File file : getDataFolder().listFiles())
			if(file.getName().endsWith(".jar")) try {
				url.add(file.toURI().toURL());

				JarFile jar = new JarFile(file);
				JarEntry service = jar.getJarEntry(
						"META-INF/services/javax.script.ScriptEngineFactory");
				if(jar != null) 
					forNames.add(new BufferedReader(new InputStreamReader(jar.getInputStream(service)))
						.lines().findFirst().get());
				jar.close();
			}
			catch(Exception e) {
				getLogger().log(Level.WARNING, "Fail to load classpath.", e);
			}
		loader = new URLClassLoader(url.toArray(new URL[0]));
		for(String name : forNames) 
			try { 
				getLogger().info("Loading engine " + name + ".");
				ScriptEngineFactory factory = (ScriptEngineFactory) loader.loadClass(name).newInstance();
				factory.getExtensions().forEach(x -> factories.put(x, factory));
				manager.registerEngineName(factory.getEngineName(), factory);
			}
			catch(Exception e) {
				getLogger().log(Level.WARNING, "Fail to load engine " + name + ".", e);
			}
	}
	
	public void onEnable() {
		// Print languages.
		StringBuilder builder = new StringBuilder("Supported languages: ");
		factories.keySet().forEach(e -> builder.append(e).append(", "));
		String result = new String(builder);
		getLogger().log(Level.INFO, result.substring(0, result.length() - 2));
		
		// Make data folder.
		if(!getDataFolder().exists()) getDataFolder().mkdir();
		
		// Read scripts.
		for(File file : getDataFolder().listFiles()) try {
			loadFile(file);
		} catch(Exception e) {
			super.getLogger().log(Level.WARNING, "LoadFail", e);
		};
	}
	
	void loadFile(File file) throws Exception {
		if(file.isDirectory()) return;
		
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
			case "jar":
				// skip.
			break;
			default:
				makeScript(file, name, suffix, dataFolder);
			break;
		}	
	}
	
	public void onDisable() {
		for(ScriptSurrogator surrogator : surrogators.values()) try {
			surrogator.callUnload();
		} catch(Exception e) {
			super.getLogger().log(Level.WARNING, "UnloadFail", e);
		};
		
		try {
			loader.close();
		} catch (IOException e) {
			super.getLogger().log(Level.SEVERE, "Cannot unload classloader.", e);
		}
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
	
	private ScriptEngine newEngine(String suffix) {
		ScriptEngineFactory factory = factories.get(suffix);
		return factory == null? null : factory.getScriptEngine();
	}
	
	private void makeScript(File js, String name, String suffix, File dataFolder) throws Exception {
		ScriptEngine engine = newEngine(suffix);
		if(engine == null) throw new Exception("No engine for " + suffix);
		
		ScriptSurrogator surrogator = new ScriptSurrogator(name, engine, this, dataFolder);
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
		
		ScriptEngine engine = newEngine(suffix);
		if(engine == null) throw new Exception("No engine for " + suffix);
		
		ZipSurrogator surrogator = new ZipSurrogator(name, engine, this, dataFolder, zip);
		setCommon(engine, surrogator);
		
		engine.eval(new InputStreamReader(zip.getInputStream(first)));
		
		surrogators.put(name, surrogator);
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
		ScriptfulCommand.NONE.execute(this, sender, label, arguments);
		return true;
	}
	
	Map<String, File> listLoadable() {
		File[] files = super.getDataFolder().listFiles();
		TreeMap<String, File> result = new TreeMap<>();
		for(File file : files) {
			if(file.isDirectory()) continue;
			if(file.getName().endsWith(".jar")) continue;
			result.put(file.getName().substring(0, 
					file.getName().lastIndexOf('.')), file);
		}
		return result;
	}
	
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] arguments) {
		return ScriptfulCommand.NONE.tabComplete(this, sender, label, arguments);
	}
}

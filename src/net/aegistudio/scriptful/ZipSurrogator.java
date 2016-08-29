package net.aegistudio.scriptful;

import java.io.File;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.script.ScriptEngine;

public class ZipSurrogator extends ScriptSurrogator {
	ZipFile file;
	
	public ZipSurrogator(String pluginName, ScriptEngine engine, Scriptful parent, File dataFolder, ZipFile file) {
		super(pluginName, engine, parent, dataFolder);
		this.file = file;
	}
	
	public ZipEntry find(String entryName) throws Exception{
		ZipEntry entry = file.getEntry(entryName);
		if(entry == null) throw new Exception("Cannot find resource!");
		return entry;
	}
	
	public void include(ZipEntry entry) throws Exception{
		if(entry.isDirectory()) throw new Exception("Cannot include directory!");
		engine.eval(new InputStreamReader(file.getInputStream(entry)));		
	}
	
	public void include(String entryName) throws Exception{
		include(find(entryName));
	}
}

package net.aegistudio.scriptful;

import javax.script.Invocable;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

public class FunctionListener<E extends Event> implements Listener, EventExecutor, 
	Comparable<FunctionListener<?>> {
	
	public final Invocable script;
	public final String function;
	public final Class<E> event;
	
	public FunctionListener(Invocable script, String function, Class<E> event) {
		this.script = script;
		this.function = function;
		this.event = event;
	}
	
	@Override
	public void execute(Listener listener, Event event) throws EventException {
		try {
			script.invokeFunction(function, event);
		}
		catch(Exception e) {
			throw new EventException(e);
		}
	}

	public boolean equals(Object another){ 
		if(!(another instanceof FunctionListener)) return false;
		FunctionListener<?> anotherListener = (FunctionListener<?>) another;
		return this.compareTo(anotherListener) == 0;
	}
	
	public int compareTo(FunctionListener<?> another) {
		int compareFirst = event.hashCode() - another.event.hashCode();
		if(compareFirst != 0) return compareFirst;
		
		int compareSecond = script.hashCode() - another.script.hashCode();
		if(compareSecond != 0) return compareSecond;
		
		return function.compareTo(another.function);
	}
}

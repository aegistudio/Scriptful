package net.aegistudio.scriptful;

import javax.script.Invocable;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

public class MethodListener<E extends Event> implements Listener, EventExecutor,
	Comparable<MethodListener<?>> {
	
	public final Invocable script;
	public final Object instance;
	public final String function;
	public final Class<E> event;
	
	public MethodListener(Invocable script, Object instance, String method, Class<E> event) {
		this.script = script;
		this.instance = instance;
		this.function = method;
		this.event = event;
	}
	
	@Override
	public void execute(Listener listener, Event event) throws EventException {
		try {
			script.invokeMethod(instance, function, event);
		}
		catch(Exception e) {
			throw new EventException(e);
		}
	}

	public boolean equals(Object another){ 
		if(!(another instanceof MethodListener)) return false;
		MethodListener<?> anotherListener = (MethodListener<?>) another;
		return this.compareTo(anotherListener) == 0;
	}
	
	public int compareTo(MethodListener<?> another) {
		int compareFirst = event.hashCode() - another.event.hashCode();
		if(compareFirst != 0) return compareFirst;
		
		int compareSecond = script.hashCode() - another.script.hashCode();
		if(compareSecond != 0) return compareSecond;
		
		int compareThird = instance.hashCode() - another.instance.hashCode();
		if(compareThird != 0) return compareThird;
		
		return function.compareTo(another.function);
	}
}

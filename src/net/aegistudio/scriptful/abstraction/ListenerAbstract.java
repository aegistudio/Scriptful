package net.aegistudio.scriptful.abstraction;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import net.aegistudio.scriptful.execution.Executor;

public class ListenerAbstract<E extends Executor<E>> implements Listener, EventExecutor,
	Comparable<ListenerAbstract<?>> {
	
	public final E executor;
	public final Class<? extends Event> event;
	
	public ListenerAbstract(E executor, Class<? extends Event> event) {
		this.executor = executor;
		this.event = event;
	}
	
	@Override
	public void execute(Listener arg0, Event arg1) throws EventException {
		try {
			if(arg1.getClass() == event)
				executor.execute(arg1);
		} catch (Exception e) {
			throw new EventException(e);
		}
	}

	public boolean equals(Object o) {
		if(!(o instanceof ListenerAbstract)) return false;
		return compareTo((ListenerAbstract<?>)o) == 0;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(ListenerAbstract<?> o) {
		int compareFirst = event.hashCode() - o.event.hashCode();
		if(compareFirst != 0) return compareFirst;
		
		int compareSecond = executor.getClass().hashCode() - o.executor.getClass().hashCode();
		if(compareSecond != 0) return compareSecond;
		
		return executor.compareTo((E)o.executor);
	}
}

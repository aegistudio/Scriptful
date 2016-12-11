package net.aegistudio.scriptful.abstraction;

import org.bukkit.scheduler.BukkitRunnable;

import net.aegistudio.scriptful.Scriptful;
import net.aegistudio.scriptful.execution.Executor;

public class RunnableAbstract<E extends Executor<E>> {
	private final Scriptful parent;
	private final BukkitRunnable wrapped;
	
	public RunnableAbstract(Scriptful parent, E executor) {
		this.parent = parent;
		wrapped = new BukkitRunnable() {
			@Override
			public void run() {
				try {
					executor.execute();
				}
				catch(Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
	
	public void cancel() {
		wrapped.cancel();
	}
	
	public int getTaskId() {
		return wrapped.getTaskId();
	}
	
	public void runTask() {
		wrapped.runTask(parent);
	}
	
	public void runTaskAsynchronously() {
		wrapped.runTaskAsynchronously(parent);
	}
	
	public void runTaskLater(long delay) {
		wrapped.runTaskLater(parent, delay);
	}
	
	public void runTaskLaterAsynchronously(long delay) {
		wrapped.runTaskLaterAsynchronously(parent, delay);
	}
	
	public void runTaskTimer(long delay, long period) {
		wrapped.runTaskTimer(parent, delay, period);
	}
	
	public void runTaskTimerAsynchronously(long delay, long period) {
		wrapped.runTaskTimerAsynchronously(parent, delay, period);
	}
}

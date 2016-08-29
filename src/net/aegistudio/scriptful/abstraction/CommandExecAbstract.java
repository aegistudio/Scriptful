package net.aegistudio.scriptful.abstraction;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.aegistudio.scriptful.execution.Executor;

public class CommandExecAbstract<E extends Executor<E>> implements CommandExecutor,
	Comparable<CommandExecAbstract<?>> {
	
	E executor;
	
	public CommandExecAbstract(E executor) {
		this.executor = executor;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		try {
			return (boolean) executor.execute(sender, command, label, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(CommandExecAbstract<?> o) {
		int compareFirst = executor.getClass().hashCode() - o.executor.getClass().hashCode();
		if(compareFirst != 0) return compareFirst;
		
		return executor.compareTo((E)o.executor);
	}
}

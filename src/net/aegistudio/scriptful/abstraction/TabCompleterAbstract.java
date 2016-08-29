package net.aegistudio.scriptful.abstraction;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import net.aegistudio.scriptful.execution.Executor;

public class TabCompleterAbstract<E extends Executor<E>> implements TabCompleter,
	Comparable<CommandExecAbstract<?>> {
	
	E executor;
	
	public TabCompleterAbstract(E executor) {
		this.executor = executor;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		try {
			return (List<String>) executor.execute(sender, command, label, args);
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

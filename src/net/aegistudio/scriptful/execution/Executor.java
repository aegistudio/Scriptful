package net.aegistudio.scriptful.execution;

public interface Executor<T> extends Comparable<T> {
	public Object execute(Object... arguments) throws Exception;
}

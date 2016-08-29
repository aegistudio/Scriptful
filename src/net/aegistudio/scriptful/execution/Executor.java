package net.aegistudio.scriptful.execution;

public interface Executor<T> extends Comparable<T> {
	public void execute(Object... arguments) throws Exception;
}

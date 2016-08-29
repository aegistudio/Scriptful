package net.aegistudio.scriptful;

public interface Executor<T> extends Comparable<T> {
	public void execute(Object... arguments) throws Exception;
}

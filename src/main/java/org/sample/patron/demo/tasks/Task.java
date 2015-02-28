package org.sample.patron.demo.tasks;

import java.util.Map;

/**
 * Interface for Tasks.
 * 
 * @author nishen
 */
public interface Task
{
	/**
	 * @return list of options and their descriptions.
	 */
	public Map<String, String> getUsageOptions();

	/**
	 * Execute the task.
	 */
	public void run();
}

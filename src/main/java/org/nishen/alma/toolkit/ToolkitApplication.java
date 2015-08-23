package org.nishen.alma.toolkit;

import java.util.Map;

import org.nishen.alma.toolkit.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ToolkitApplication
{
	private static final Logger log = LoggerFactory.getLogger(ToolkitApplication.class);

	private String[] args = null;

	private Map<String, Task> tasks = null;

	@Inject
	private ToolkitApplication(@Named("app.cmdline") final String[] args, Map<String, Task> tasks)
	{
		this.args = args;
		this.tasks = tasks;
	}

	public void run()
	{
		log.debug("task manager execution starting");

		if (args == null || args.length == 0)
		{
			System.out.println("java -jar alma-toolkit.jar task-name [task-options]\n");

			for (String taskname : tasks.keySet())
			{
				Task task = tasks.get(taskname);
				System.out.println("Available task: " + taskname);
				System.out.println("Command line options:");
				for (String s : task.getUsageOptions().keySet())
				{
					System.out.println("\t" + s + "    " + task.getUsageOptions().get(s));
				}
				System.out.println();
			}

			return;
		}

		if (log.isDebugEnabled())
		{
			for (String a : args)
				log.debug("cmdline: {}", a);

			for (String n : tasks.keySet())
				log.debug("task name: {}", n);
		}

		String taskname = args[0].toLowerCase();
		Task task = tasks.get(taskname);
		if (task == null)
		{
			System.out.println("task not available: " + taskname);
			return;
		}

		System.out.println("executing task starting: " + taskname);

		task.run();

		System.out.println("executing task complete: " + taskname);

		log.debug("task manager execution complete");
	}
}

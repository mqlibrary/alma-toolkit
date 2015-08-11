package org.nishen.alma.toolkit;

import java.util.Properties;

import org.nishen.alma.toolkit.tasks.Task;
import org.nishen.alma.toolkit.tasks.TaskFixUserIdentifiers;
import org.nishen.alma.toolkit.tasks.TaskListUsers;
import org.nishen.alma.toolkit.tasks.TaskSetUserPurgeDates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TaskManager class manages the execution of a task.
 * 
 * <p>
 * A task can be registered here and executed via command line.
 * 
 * @author nishen
 */
public class TaskManager
{
	private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

	private String[] args = null;
	private Properties config = null;

	// Disable the default empty constructor. We just want to use the
	// parameterized one.
	@SuppressWarnings("unused")
	private TaskManager()
	{}

	/**
	 * Constructor for the TaskManager
	 * 
	 * @param args command line args passed in.
	 * @param config the properties loaded in at run time.
	 */
	public TaskManager(String[] args, Properties config)
	{
		this.args = args;
		this.config = config;

		log.debug("initialised taskmanager");
	}

	/**
	 * Start the Task Manager.
	 */
	public void run()
	{
		Task task = null;
		if (args != null && args.length > 0)
			for (String arg : args)
				switch (arg.toLowerCase())
				{
					case "listusers":
						task = new TaskListUsers(args, config);
						log.debug("created task: listusers");
						break;
					case "fixusers":
						task = new TaskFixUserIdentifiers(args, config);
						log.debug("created task: fixusers");
						break;
					case "setuserpurgedates":
						task = new TaskSetUserPurgeDates(args, config);
						log.debug("created task: setuserpurgedates");
						break;
				}

		if (task != null)
			task.run();
	}
}

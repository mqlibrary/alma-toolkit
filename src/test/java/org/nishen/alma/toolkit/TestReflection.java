package org.nishen.alma.toolkit;

import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nishen.alma.toolkit.tasks.Task;
import org.nishen.alma.toolkit.tasks.TaskListUsers;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestReflection
{
	private static final Logger log = LoggerFactory.getLogger(TestReflection.class);

	private static Reflections r;

	@BeforeClass
	public static void setup()
	{
		r = new Reflections("org.nishen.alma.toolkit.tasks");
	}

	@Test
	public void TestCheckTaskClasses()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		Set<Class<? extends Task>> tasks = r.getSubTypesOf(Task.class);
		Assert.assertTrue("task does not exist: ListUsers", tasks.contains(TaskListUsers.class));

		if (log.isDebugEnabled())
			for (Class<? extends Task> c : tasks)
				log.debug("task class: {}", c.getName());
	}
}

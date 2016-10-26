package org.nishen.alma.toolkit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * This application is a framework for executing tasks. Add your custom tasks
 * and you can execute them via a command line interface.
 * 
 * <p>
 * This is a demo providing a couple of task examples.
 * 
 * @author nishen
 * 
 */
public class ToolkitLauncher
{
	private static final Logger log = LoggerFactory.getLogger(ToolkitLauncher.class);

	public static void main(String[] args)
	{
		Map<String, String> options = null;

		try
		{
			options = parseArgs(args);
		}
		catch (Exception e)
		{
			System.out.println("unable to process command line: " + e.getMessage());
			log.error("error processing command line: {}", e.getMessage());
			printUsage();
		}

		if (options.containsKey("h"))
		{
			printUsage();
			return;
		}

		// list for injector modules
		List<Module> modules = new ArrayList<Module>();

		// module (main configuration)
		modules.add(new ToolkitModule(args));

		// create the injector
		log.debug("creating injector");
		Injector injector = Guice.createInjector(modules);

		// initialise the application object.
		log.debug("creating application");
		ToolkitApplication app = injector.getInstance(ToolkitApplication.class);

		// execute the application
		Calendar timeStart = Calendar.getInstance();
		log.info("executing: [{}]", timeStart.getTime());

		app.run();

		Calendar timeEnd = Calendar.getInstance();
		log.info("execution complete: [{}]", timeEnd.getTime());

		long diff = (timeEnd.getTimeInMillis() - timeStart.getTimeInMillis()) / 1000;
		log.info("time taken (seconds): {}", diff);
	}

	private static Map<String, String> parseArgs(String[] args) throws Exception
	{
		Map<String, String> options = new HashMap<String, String>();

		if (args.length == 0)
		{
			options.put("h", "true");
			return options;
		}

		for (int x = 0; x < args.length; x++)
		{
			options.put("", "");
			if (args[x].equals("-h"))
			{
				options.put("h", "true");
			}
		}

		return options;
	}

	private static void printUsage()
	{
		System.out.println("java -jar alma-tookit.jar [options]");
	}
}

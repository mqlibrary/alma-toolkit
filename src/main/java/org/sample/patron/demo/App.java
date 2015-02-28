package org.sample.patron.demo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class App
{
	private static final Logger log = LoggerFactory.getLogger(App.class);

	private static final String CONFIG = "app.properties";

	public static void main(String[] args)
	{

		// load a properties file (default current directory called app.properties
		// or via system property on command line).

		String configFilename = CONFIG;

		if (System.getProperty("config") != null)
			configFilename = System.getProperty("config");

		File configFile = new File(configFilename);

		Properties config = new Properties();
		try
		{
			if (!configFile.exists() || !configFile.canRead())
				throw new IOException("cannot read file");

			config.load(new FileReader(configFile));
		}
		catch (IOException e)
		{
			log.error("unable to load configuration: {}", configFile.getAbsoluteFile(), e);
			return;
		}

		// once config file is loaded, create a task manager object and start it up.

		TaskManager taskManager = new TaskManager(args, config);

		taskManager.run();
	}
}

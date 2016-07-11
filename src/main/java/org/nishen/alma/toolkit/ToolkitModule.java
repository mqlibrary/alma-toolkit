package org.nishen.alma.toolkit;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.nishen.alma.toolkit.tasks.Task;
import org.nishen.alma.toolkit.util.AlmaAuthHeaderFilter;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.WebClient;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class ToolkitModule extends AbstractModule
{
	private static final Logger log = LoggerFactory.getLogger(ToolkitModule.class);

	private static final String CONFIG_FILE = "app.properties";

	private static final String DEFAULT_PACKAGE = "org.nishen.alma.toolkit.tasks";

	private static final Properties config = new Properties();

	private static String[] args;

	private WebTarget almaTarget = null;

	public ToolkitModule(final String[] args)
	{
		ToolkitModule.args = args;
	}

	@Override
	protected void configure()
	{
		String configFilename = CONFIG_FILE;
		if (System.getProperty("config") != null)
			configFilename = System.getProperty("config");

		File configFile = new File(configFilename);
		try
		{
			if (!configFile.exists() || !configFile.canRead())
				throw new IOException("cannot read config file: " + configFile.getAbsolutePath());

			config.load(new FileReader(configFile));

			if (log.isDebugEnabled())
				for (String k : config.stringPropertyNames())
					log.debug("{}: {}={}", new Object[] { CONFIG_FILE, k, config.getProperty(k) });
		}
		catch (IOException e)
		{
			log.error("unable to load configuration: {}", configFile.getAbsoluteFile(), e);
			return;
		}

		// bind instances
		TypeLiteral<String[]> argsType = new TypeLiteral<String[]>() {};
		bind(argsType).annotatedWith(Names.named("app.cmdline")).toInstance(args);
		bind(Properties.class).annotatedWith(Names.named("app.config")).toInstance(config);

		// bind task classes
		MapBinder<String, Task> tasks = MapBinder.newMapBinder(binder(), String.class, Task.class);
		String[] packages = config.getProperty("task.packages", DEFAULT_PACKAGE).split(",");
		for (String pkg : packages)
		{
			Reflections r = new Reflections(pkg);
			Set<Class<? extends Task>> taskList = r.getSubTypesOf(Task.class);
			for (Class<? extends Task> t : taskList)
			{
				try
				{
					Method m = t.getDeclaredMethod("getTaskName");
					String taskName = (String) m.invoke(null);
					tasks.addBinding(taskName.toLowerCase()).to(t).in(Scopes.SINGLETON);

					if (log.isDebugEnabled())
						log.debug("added task: {}", t.getName());
				}
				catch (Exception e)
				{
					log.error("Task does not conform to interface: {}", t.getName());
					log.debug("{}", e.getMessage(), e);
				}
			}
		}
	}

	@Provides
	@Named("ws.url.alma")
	protected WebTarget provideWebTargetAlma()
	{
		if (almaTarget == null)
		{
			Client client = ClientBuilder.newClient();
			client.register(new AlmaAuthHeaderFilter(config.getProperty("ws.url.alma.key")));
			almaTarget = client.target(config.getProperty("ws.url.alma"));
		}

		return almaTarget;
	}

	@Provides
	protected WebClient provideWebClient()
	{
		WebClient webClient = new WebClient();

		webClient.getOptions().setActiveXNative(false);
		webClient.getOptions().setJavaScriptEnabled(false);
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setUseInsecureSSL(true);

		return webClient;
	}
}

package org.sample.patron.demo.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.sample.patron.demo.entity.User;
import org.sample.patron.demo.entity.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task to correct the Identifiers of a User record.
 * 
 * @author nishen
 */
public class TaskFixUserIdentifiers implements Task
{
	private static final Logger log = LoggerFactory.getLogger(TaskFixUserIdentifiers.class);

	private Properties config = null;

	private String userListFilename = null;

	private String identifiersFilename = null;

	// Disable the default empty constructor. We just want to use the
	// parameterized one.
	@SuppressWarnings("unused")
	private TaskFixUserIdentifiers()
	{}

	/**
	 * @param args command line args passed in.
	 * @param config the properties loaded in at run time.
	 */
	public TaskFixUserIdentifiers(String[] args, Properties config)
	{
		this.config = config;

		// extract relevant command line arguments
		if (args != null && args.length > 0)
			for (int x = 0; x < args.length; x++)
			{
				if (args[x].equals("-ids"))
				{
					if (args.length > (x + 1))
						identifiersFilename = args[++x];
				}
				else if (args[x].equals("-users"))
				{
					if (args.length > (x + 1))
						userListFilename = args[++x];
				}
			}

		log.debug("initialised {}", this.getClass().getCanonicalName());
	}

	@Override
	public void run()
	{
		log.info("executing task: {}", this.getClass().getSimpleName());

		Map<String, String> identifiers = null;
		List<String> userList = null;

		if (userListFilename == null)
		{
			log.error("user list filename required.");
			return;
		}

		if (identifiersFilename == null)
		{
			log.error("identifiers filename required.");
			return;
		}

		try
		{
			File userListFile = new File(userListFilename);
			userList = getUserList(userListFile);

			File idFile = new File(identifiersFilename);
			identifiers = getIdentifiers(idFile);
		}
		catch (FileNotFoundException nfe)
		{
			log.error("cannot find or open file: {}", nfe.getMessage(), nfe);
			return;
		}

		String url = config.getProperty("ws.url.alma");
		String key = config.getProperty("ws.url.alma.key");
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(url);
		target = target.path("users").queryParam("apikey", key);
		
		for (String oldPrimaryId : userList)
		{
			String newPrimaryId = identifiers.get(oldPrimaryId.toLowerCase());
		}
		
		
		
		// we avoid dealing with any XML/JSON. We just make the call.
		Users users = target.request(MediaType.APPLICATION_XML_TYPE).get(Users.class);
		for (User user : users.getUser())
		{
			// extract the information into an object for displaying purposes.
			Object[] param = new Object[4];
			param[0] = user.getPrimaryId();
			param[1] = user.getFirstName();
			param[2] = user.getLastName();
			param[3] = user.getStatus().getValue();

			log.info("[{}] : {} {} ({})", param);
		}
	}

	@Override
	public Map<String, String> getUsageOptions()
	{
		Map<String, String> options = new HashMap<String, String>();

		options.put("-limit x", "limit the number of results to return");
		options.put("-offset x", "starting index of results to return");

		return options;
	}

	private Map<String, String> getIdentifiers(File file) throws FileNotFoundException
	{
		Map<String, String> result = new HashMap<String, String>(600000);

		Scanner scanner = null;
		scanner = new Scanner(file);
		scanner.nextLine(); // header line
		while (scanner.hasNextLine())
		{
			String line = scanner.nextLine();
			if (line == null || "".equals(line))
				continue;

			String[] parts = line.split(",");
			result.put(parts[0].toLowerCase(), parts[1]);
		}
		scanner.close();

		return result;
	}

	private List<String> getUserList(File file) throws FileNotFoundException
	{
		List<String> result = new ArrayList<String>(50000);

		Scanner scanner = null;
		scanner = new Scanner(file);
		scanner.nextLine(); // header line
		while (scanner.hasNextLine())
		{
			String line = scanner.nextLine();
			if (line == null || "".equals(line))
				continue;

			result.add(line);
		}
		scanner.close();

		return result;
	}
}

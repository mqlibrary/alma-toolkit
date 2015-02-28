package org.sample.patron.demo.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.sample.patron.demo.entity.User;
import org.sample.patron.demo.entity.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A sample task using the Alma API. This is a simple task that gets a list of
 * users and provides their primaryId, name and status.
 * 
 * <p>
 * The optional parameters that can be provided are limit and offset. Otherwise,
 * the defaults are 100 and 0 respectively.
 * 
 * @author nishen
 */
public class TaskListUsers implements Task
{
	private static final Logger log = LoggerFactory.getLogger(TaskListUsers.class);

	private Properties config = null;

	private String limit = "100";
	private String offset = "0";

	// Disable the default empty constructor. We just want to use the
	// parameterized one.
	@SuppressWarnings("unused")
	private TaskListUsers()
	{}

	/**
	 * @param args command line args passed in.
	 * @param config the properties loaded in at run time.
	 */
	public TaskListUsers(String[] args, Properties config)
	{
		this.config = config;

		// extract relevant command line arguments
		if (args != null && args.length > 0)
			for (int x = 0; x < args.length; x++)
			{
				if (args[x].equals("-limit"))
				{
					if (args.length > (x + 1))
						limit = args[++x];
				}
				else if (args[x].equals("-offset"))
				{
					if (args.length > (x + 1))
						offset = args[++x];
				}
			}

		log.debug("initialised tasklistusers");
	}

	@Override
	public void run()
	{
		log.info("executing task: {}", this.getClass().getSimpleName());

		// ensure we have a proper number.
		try
		{
			Integer.parseInt(limit);
			Integer.parseInt(offset);
		}
		catch (NumberFormatException nfe)
		{
			log.error("option specified is not an integer: {}", limit, nfe);
			return;
		}

		// this is the URL to the Alma API endpoint
		String url = config.getProperty("ws.url.alma");

		// this is the apikey we will be using
		String key = config.getProperty("ws.url.alma.key");

		// instantiate a REST client.
		Client client = ClientBuilder.newClient();

		// instantiate an endpoint - this is the base of the URL:
		// https://gateway-hostname/almaws/version/
		// or: https://api-ap.hosted.exlibrisgroup.com/almaws/v1
		WebTarget target = client.target(url);

		// the 'path' specifies the resource we are targeting e.g. 'users'.
		// this will result in: https://api-ap.hosted.exlibrisgroup.com/almaws/v1/users
		// we are also specifying some parameters: apikey, limit and offset
		// this results in: https://api-ap.hosted.exlibrisgroup.com/almaws/v1/users?apikey=xxx&limit=100&offset=0
		target = target.path("users").queryParam("apikey", key).queryParam("limit", limit).queryParam("offset", offset);

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
}

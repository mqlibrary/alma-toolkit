package org.nishen.alma.toolkit.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.nishen.alma.toolkit.entity.user.User;
import org.nishen.alma.toolkit.entity.user.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

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

	private Provider<WebTarget> webTargetProvider;

	private String limit = "100";

	private String offset = "0";

	private static final String TASKNAME = "listUsers";

	public static String getTaskName()
	{
		return TASKNAME;
	}

	@Inject
	private TaskListUsers(@Named("app.cmdline") final String[] args, @Named("app.config") final Properties config,
	                      @Named("ws.url.alma") Provider<WebTarget> webTargetProvider)
	{
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

		this.webTargetProvider = webTargetProvider;

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

		// instantiate an endpoint - this is the base of the URL:
		// https://gateway-hostname/almaws/version/
		// or: https://api-ap.hosted.exlibrisgroup.com/almaws/v1
		WebTarget target = webTargetProvider.get();

		// the 'path' specifies the resource we are targeting e.g. 'users'.
		// this will result in: https://api-ap.hosted.exlibrisgroup.com/almaws/v1/users
		// we are also specifying some parameters: limit and offset (apikey preconfigured)
		// this results in: https://api-ap.hosted.exlibrisgroup.com/almaws/v1/users?apikey=xxx&limit=100&offset=0
		target = target.path("users").queryParam("limit", limit).queryParam("offset", offset);

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

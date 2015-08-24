package org.nishen.alma.toolkit.tasks;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * A task to pull a report from Alma Analytics. The row limitation of report exports is
 * just over 60k records. This means that any report more than 60k rows will need to be
 * structured in a way that splits the results up across the limitation boundaries.
 * This can be tedious. It is possible to use the Alma API (Analytics) to fetch a report
 * from Alma Analytics. This method bypasses the limitation. However, there is additional
 * work that needs to be done.
 * 
 * 
 * @author nishen
 */
public class TaskFetchReport implements Task
{
	private static final Logger log = LoggerFactory.getLogger(TaskFetchReport.class);

	private static final String TASKNAME = "fetchReport";

	private Provider<WebTarget> webTargetProvider;

	private String title = "/users/1024839030002171_2171_d/User Analysis/User OneID Listing";

	private String limit = "1000";

	@Inject
	private TaskFetchReport(@Named("app.cmdline") final String[] args, @Named("app.config") final Properties config,
	                        @Named("ws.url.alma") Provider<WebTarget> webTargetProvider)
	{
		// extract relevant command line arguments
		if (args != null && args.length > 0)
			for (int x = 0; x < args.length; x++)
			{
				if (args[x].equals("-title"))
				{
					if (args.length > (x + 1))
						title = args[++x];
				}
				else if (args[x].equals("-limit"))
				{
					if (args.length > (x + 1))
						limit = args[++x];
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
		}
		catch (NumberFormatException nfe)
		{
			log.error("option specified is not an integer: {}", limit, nfe);
			return;
		}

		// get an instance of the web client.
		WebTarget target = webTargetProvider.get();

		// the 'path' specifies the resource we are targeting e.g. 'users'.
		// this will result in: https://api-ap.hosted.exlibrisgroup.com/almaws/v1/users
		// we are also specifying some parameters: limit and offset (apikey preconfigured)
		// this results in: https://api-ap.hosted.exlibrisgroup.com/almaws/v1/users?apikey=xxx&limit=100&offset=0
		String path = null;
		try
		{
			path = URLEncoder.encode(title, "UTF-8");
			path = "%2Fshared%2FMacquarie%20University%2FReports%2FAJS_patron_group_count";
		}
		catch (UnsupportedEncodingException uee)
		{
			log.error("unable to process report title: {}", title);
			return;
		}

		target = target.path("analytics/reports").queryParam("limit", limit).queryParam("path", path);

		// we avoid dealing with any XML/JSON. We just make the call.
		String report = target.request(MediaType.APPLICATION_XML_TYPE).get(String.class);
		log.info("\n{}\n", report);
	}

	@Override
	public Map<String, String> getUsageOptions()
	{
		Map<String, String> options = new HashMap<String, String>();

		options.put("-title x", "the name of the report to fetch (use double quotes if there are spaces)");

		return options;
	}

	public static String getTaskName()
	{
		return TASKNAME;
	}
}

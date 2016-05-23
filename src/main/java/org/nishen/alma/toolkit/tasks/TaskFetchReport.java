package org.nishen.alma.toolkit.tasks;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.nishen.alma.toolkit.entity.report.QueryResultType;
import org.nishen.alma.toolkit.entity.report.ReportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

	private String title = null;

	private String csv = null;

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
				if (args[x].equals("-csv"))
				{
					if (args.length > (x + 1))
						csv = args[++x];
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

		if (title == null)
		{
			log.error("title required");
			return;
		}

		if (csv == null)
		{
			log.error("csv required");
			return;
		}

		// get an instance of the web client.
		WebTarget target = webTargetProvider.get();

		// the 'path' specifies the resource we are targeting e.g. 'users'.
		// this will result in: https://api-ap.hosted.exlibrisgroup.com/almaws/v1/users
		// we are also specifying some parameters: limit and offset (apikey preconfigured)
		// this results in: https://api-ap.hosted.exlibrisgroup.com/almaws/v1/users?apikey=xxx&limit=100&offset=0
		String path = title;
		// path = "/users/1024839030002171_2171_d/User Analysis/Loan Data";

		target = target.path("analytics/reports").queryParam("limit", limit).queryParam("path", path);

		try (CSVPrinter printer = new CSVPrinter(new FileWriter(csv), CSVFormat.EXCEL);)
		{
			String token = null;
			boolean finished = false;
			while (!finished)
			{
				if (token != null)
					target = target.queryParam("token", token);

				// we avoid dealing with any XML/JSON. We just make the call.
				ReportType report = target.request(MediaType.APPLICATION_XML_TYPE).get(ReportType.class);
				QueryResultType result = report.getQueryResult();
				finished = result.isIsFinished();
				token = result.getResumptionToken();

				Element resultXml = (Element) result.getAny();
				NodeList rows = resultXml.getElementsByTagName("Row");
				if (rows.getLength() == 0)
				{
					try
					{
						Thread.sleep(5000);
					}
					catch (InterruptedException ie)
					{
						log.debug("sleep interrupted.");
					}
				}

				for (int x = 0; x < rows.getLength(); x++)
				{
					List<String> csvRow = new ArrayList<String>();

					Element row = (Element) rows.item(x);
					NodeList cols = row.getChildNodes();
					for (int y = 0; y < cols.getLength(); y++)
					{
						Node node = cols.item(y);
						if (node.getNodeType() == Node.ELEMENT_NODE)
						{
							Element col = (Element) cols.item(y);
							csvRow.add(col.getFirstChild().getNodeValue());
							log.debug("column: {}", col.getNodeName());
						}
					}
					printer.printRecord(csvRow);
				}
			}

			printer.flush();
			printer.close();
		}
		catch (IOException ioe)
		{
			log.error("unable to write records to csv file: {}", csv, ioe);
		}
	}

	@Override
	public Map<String, String> getUsageOptions()
	{
		Map<String, String> options = new HashMap<String, String>();

		options.put("-title \"x\"", "the name of the report to fetch (use double quotes if there are spaces)");
		options.put("-csv", "CSV output file");

		return options;
	}

	public static String getTaskName()
	{
		return TASKNAME;
	}
}

package org.nishen.alma.toolkit.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException;
import org.nishen.alma.toolkit.entity.user.ObjectFactory;
import org.nishen.alma.toolkit.entity.user.User;
import org.nishen.alma.toolkit.entity.ws.Error;
import org.nishen.alma.toolkit.entity.ws.WebServiceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Task to set the expiry and purge dates of a User.
 * 
 * @author nishen
 */
public class TaskSetUserPurgeDates implements Task
{
	private static final Logger log = LoggerFactory.getLogger(TaskSetUserPurgeDates.class);

	private static final String TASKNAME = "setUserPurgeDates";

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	private Provider<WebTarget> webTargetProvider;

	private String userListFilename = null;

	private Date expiryDate = null;

	private Date purgeDate = null;

	private ObjectFactory of = new ObjectFactory();

	/**
	 * @param args command line args passed in.
	 * @param config the properties loaded in at run time.
	 */
	@Inject
	private TaskSetUserPurgeDates(@Named("app.cmdline") final String[] args,
	                              @Named("app.config") final Properties config,
	                              @Named("ws.url.alma") Provider<WebTarget> webTargetProvider)
	{
		// extract relevant command line arguments
		if (args != null && args.length > 0)
			for (int x = 0; x < args.length; x++)
			{
				if (args[x].equals("-expiry"))
				{
					String date = null;
					if (args.length > (x + 1))
					{
						try
						{
							date = args[++x];
							expiryDate = sdf.parse(date);
						}
						catch (ParseException pe)
						{
							log.error("unable to parse expirydate: {}", date);
						}
					}
				}
				else if (args[x].equals("-purge"))
				{
					String date = null;
					if (args.length > (x + 1))
					{
						try
						{
							date = args[++x];
							purgeDate = sdf.parse(date);
						}
						catch (ParseException pe)
						{
							log.error("unable to parse purge date: {}", date);
						}
					}
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

		List<String> userList = null;

		if (expiryDate == null)
		{
			log.error("expiry date required.");
			return;
		}

		if (purgeDate == null)
		{
			log.error("purge date required.");
			return;
		}

		log.info("expiry date: {}", expiryDate);
		log.info("purge date:  {}", purgeDate);

		try
		{
			File userListFile = new File(userListFilename);
			userList = getUserList(userListFile);
		}
		catch (FileNotFoundException nfe)
		{
			log.error("cannot find or open file: {}", nfe.getMessage(), nfe);
			return;
		}

		WebTarget target = webTargetProvider.get();
		target = target.path("users");

		for (String primaryId : userList)
		{
			try
			{
				log.info("processing user: {}", primaryId);

				WebTarget t = target.path(primaryId);
				User user = t.request(MediaType.APPLICATION_XML_TYPE).get(User.class);
				log.info("fetched user: {}", user.getPrimaryId());

				user.setExpiryDate(makeDate(expiryDate));
				user.setPurgeDate(makeDate(purgeDate));

				JAXBElement<User> jaxbUser = of.createUser(user);
				user = t.request(MediaType.APPLICATION_XML).put(Entity.entity(jaxbUser, MediaType.APPLICATION_XML),
				                                                User.class);

				log.info("updated user [{}] - {}/{}", user.getPrimaryId(), user.getExpiryDate(), user.getPurgeDate());
			}
			catch (ClientErrorException cee)
			{
				try
				{
					WebServiceResult error = cee.getResponse().readEntity(WebServiceResult.class);
					for (Error e : error.getErrorList().getError())
					{
						String mesg = e.getErrorMessage();
						if (mesg != null)
							mesg = mesg.replaceAll("\n", " ");
					}
				}
				catch (MessageBodyProviderNotFoundException e)
				{
					String error = cee.getResponse().readEntity(String.class);
					Object[] args = new Object[] { primaryId, error };
					log.error("TaskFixUserIdentifiers[{}] error: {}", args);
				}
			}
			catch (ServerErrorException see)
			{
				Object[] args = new Object[] { primaryId, see.getResponse().getStatusInfo().getStatusCode(),
				                               see.getMessage() };
				log.error("TaskFixUserIdentifiers[{}] {}: {}", args);
			}
			catch (Exception e)
			{
				Object[] args = new Object[] { primaryId, e.getMessage(), e };
				log.error("TaskFixUserIdentifiers[{}]: {}", args);
			}
		}
	}

	@Override
	public Map<String, String> getUsageOptions()
	{
		Map<String, String> options = new HashMap<String, String>();

		options.put("-expiry YYYY-MM-DD", "expiry date");
		options.put("-purge YYYY-MM-DD", "purge date");
		options.put("-users filename", "list of user identifiers to process");

		return options;
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

			String[] parts = line.split(",");
			if (parts == null || parts.length < 1)
				continue;

			result.add(parts[0]);
		}
		scanner.close();

		return result;
	}

	private static XMLGregorianCalendar makeDate(Date date)
	{
		DatatypeFactory dtf = null;
		try
		{
			dtf = DatatypeFactory.newInstance();
		}
		catch (DatatypeConfigurationException e)
		{
			log.warn("unable to init DatatypeFactory: {}", e.getMessage());
		}

		Calendar d = Calendar.getInstance();
		d.setTime(date);
		log.trace("makeDate [from database]: {}", d.toString());

		XMLGregorianCalendar c = dtf.newXMLGregorianCalendar();

		c.setYear(d.get(Calendar.YEAR));
		c.setMonth(d.get(Calendar.MONTH) + 1);
		c.setDay(d.get(Calendar.DAY_OF_MONTH));
		c.setTimezone(0);
		c.normalize();
		log.trace("makeDate [xml gregorian]: {}", c.toString());

		return c;
	}

	public static String getTaskName()
	{
		return TASKNAME;
	}
}

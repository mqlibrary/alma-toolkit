package org.sample.patron.demo.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;

import org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException;
import org.sample.patron.demo.entity.ObjectFactory;
import org.sample.patron.demo.entity.Error;
import org.sample.patron.demo.entity.User;
import org.sample.patron.demo.entity.UserIdentifier;
import org.sample.patron.demo.entity.UserIdentifier.IdType;
import org.sample.patron.demo.entity.WebServiceResult;
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

	private String cnFilename = null;

	private ObjectFactory of = new ObjectFactory();

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
				else if (args[x].equals("-cns"))
				{
					if (args.length > (x + 1))
						cnFilename = args[++x];
				}
			}

		log.debug("initialised {}", this.getClass().getCanonicalName());
	}

	@Override
	public void run()
	{
		log.info("executing task: {}", this.getClass().getSimpleName());

		Map<String, String> identifiers = null;
		Map<String, String> cns = null;
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

		if (cnFilename == null)
		{
			log.error("cn filename required.");
			return;
		}

		try
		{
			File userListFile = new File(userListFilename);
			userList = getUserList(userListFile);

			File idFile = new File(identifiersFilename);
			identifiers = getIdentifiers(idFile);

			File cnFile = new File(cnFilename);
			cns = getCnCaseMapping(cnFile);
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
			try
			{
				log.info("processing user: {}", oldPrimaryId);

				String newPrimaryId = identifiers.get(oldPrimaryId.toLowerCase());
				if (newPrimaryId == null)
				{
					log.warn("user partyId not found: {}", oldPrimaryId);
					continue;
				}

				String oneId = cns.get(oldPrimaryId.toLowerCase());
				if (oneId == null)
					oneId = oldPrimaryId;

				WebTarget t = target.path(oldPrimaryId);
				User user = t.request(MediaType.APPLICATION_XML_TYPE).get(User.class);
				log.info("fetched user: {}", user.getPrimaryId());

				// remove PARTY_ID, ONE_ID, INSTITUTION_ID
				boolean hasPartyId = false;
				List<UserIdentifier> idsToRemove = new ArrayList<UserIdentifier>(5);
				for (UserIdentifier i : user.getUserIdentifiers().getUserIdentifier())
				{
					if ("PARTY_ID".equals(i.getIdType().getValue()))
					{
						hasPartyId = true;
						idsToRemove.add(i);
					}

					if ("ONE_ID".equals(i.getIdType().getValue()))
						idsToRemove.add(i);

					if ("INST_ID".equals(i.getIdType().getValue()))
						idsToRemove.add(i);

					// log.info("identifiers [{}]: {}", i.getIdType().getValue(), i.getValue());
				}

				for (UserIdentifier i : idsToRemove)
					user.getUserIdentifiers().getUserIdentifier().remove(i);

				JAXBElement<User> jaxbUser = null;

				// save with oldPrimaryId to eliminate PartyId
				if (hasPartyId)
				{
					jaxbUser = of.createUser(user);
					user = t.request(MediaType.APPLICATION_XML).put(Entity.entity(jaxbUser, MediaType.APPLICATION_XML), User.class);
					log.info("updated user - removed PartyID: {}", user.getPrimaryId());
				}

				// add new primaryId, oneId
				IdType idType = of.createUserIdentifierIdType();
				idType.setValue("ONE_ID");
				idType.setDesc("ONE_ID");

				UserIdentifier userIdentifier = of.createUserIdentifier();
				userIdentifier.setIdType(idType);
				userIdentifier.setStatus("ACTIVE");
				userIdentifier.setValue(oneId);

				user.getUserIdentifiers().getUserIdentifier().add(userIdentifier);
				user.setPrimaryId(newPrimaryId);

				// save with oldPrimaryId
				jaxbUser = of.createUser(user);
				user = t.request(MediaType.APPLICATION_XML).put(Entity.entity(jaxbUser, MediaType.APPLICATION_XML), User.class);
				log.info("updated user - changed PrimaryID: {} -> {}", oldPrimaryId, user.getPrimaryId());
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
					Object[] args = new Object[] { oldPrimaryId, error };
					log.error("getPatron[{}] error: {}", args);
				}
			}
			catch (ServerErrorException see)
			{
				Object[] args = new Object[] { oldPrimaryId, see.getResponse().getStatusInfo().getStatusCode(),
												see.getMessage() };
				log.error("getPatron[{}] {}: {}", args);
			}
			catch (Exception e)
			{
				Object[] args = new Object[] { oldPrimaryId, e.getMessage(), e };
				log.error("getPatron[{}]: {}", args);
			}
		}
	}

	@Override
	public Map<String, String> getUsageOptions()
	{
		Map<String, String> options = new HashMap<String, String>();

		options.put("-cns filename", "Common Names from AD");
		options.put("-users filename", "list of user identifiers to process");
		options.put("-ids filename", "list of OneID mappings to PartyID");

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
			if (parts == null || parts.length < 2)
				continue;

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

			String[] parts = line.split(",");
			if (parts == null || parts.length < 1)
				continue;

			result.add(parts[0]);
		}
		scanner.close();

		return result;
	}

	private Map<String, String> getCnCaseMapping(File file) throws FileNotFoundException
	{
		Map<String, String> result = new HashMap<String, String>(300000);

		Scanner scanner = null;
		scanner = new Scanner(file);
		scanner.nextLine(); // header line
		while (scanner.hasNextLine())
		{
			String line = scanner.nextLine();
			if (line == null || "".equals(line))
				continue;

			result.put(line.toLowerCase(), line);
		}
		scanner.close();

		return result;
	}
}

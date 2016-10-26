package org.nishen.alma.toolkit.tasks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.nishen.alma.toolkit.entity.resreq.UserResourceSharingRequest;
import org.nishen.alma.toolkit.entity.resreq.UserResourceSharingRequests;
import org.nishen.alma.toolkit.util.MailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * A task to check whether there are any urgent resource requests enqueued. If so,
 * send an alert email to library staff.
 * 
 * 
 * @author nishen
 */
public class TaskCheckResourceRequests implements Task
{
	private static final Logger log = LoggerFactory.getLogger(TaskCheckResourceRequests.class);

	private static final String TASKNAME = "checkResourceRequests";

	private Provider<WebTarget> webTargetProvider;

	private Properties config;

	private String institutionCode;

	@Inject
	private TaskCheckResourceRequests(@Named("app.cmdline") final String[] args,
	                                  @Named("app.config") final Properties config,
	                                  @Named("ws.url.alma") Provider<WebTarget> webTargetProvider)
	{
		this.webTargetProvider = webTargetProvider;

		this.config = config;

		this.institutionCode = config.getProperty("resreq.alma.institution.code");

		log.debug("initialised tasklistusers");
	}

	@Override
	public void run()
	{
		log.info("executing task: {}", this.getClass().getSimpleName());

		log.debug("code: {}", this.institutionCode);

		String host = config.getProperty("resreq.mail.host");
		String from = config.getProperty("resreq.mail.from");
		String name = config.getProperty("resreq.mail.name");
		String addr = config.getProperty("resreq.mail.to");
		String subj = config.getProperty("resreq.mail.subj");

		// get an instance of the web client.
		WebTarget target = webTargetProvider.get();

		target = target.path("task-lists/rs/lending-requests").queryParam("library", institutionCode);

		UserResourceSharingRequests requests = target.request(MediaType.APPLICATION_XML_TYPE)
		                                             .get(UserResourceSharingRequests.class);

		List<UserResourceSharingRequest> requestList = new ArrayList<UserResourceSharingRequest>();
		for (UserResourceSharingRequest r : requests.getUserResourceSharingRequest())
		{
			log.info("level of service [{}]: {}", r.getLevelOfService().getValue(), r.getTitle());
			if ("RUSH_LOCAL".equals(r.getLevelOfService().getValue())
			    || "EXPRESS_LOCAL".equals(r.getLevelOfService().getValue()))
				requestList.add(r);
		}

		if (requestList.size() == 0)
			return;

		StringBuilder mesg = new StringBuilder();
		mesg.append("Dear ILL Librarian,\n\n");
		mesg.append("There are urgent requests requiring your attention:\n\n");

		for (UserResourceSharingRequest r : requestList)
		{
			mesg.append("EXTERNAL ID: ").append(r.getExternalId()).append("\n");
			mesg.append("TITLE: ").append(r.getTitle()).append("\n");
			if (r.getCallNumber() != null && !"".equals(r.getCallNumber()))
				mesg.append("CALL NUMBER: ").append(r.getCallNumber()).append("\n");
			Calendar cal = Calendar.getInstance();
			cal.setTime(r.getCreatedTime().toGregorianCalendar().getTime());
			mesg.append("CREATED AT: ").append(cal.getTime()).append("\n");
			mesg.append("--\n\n");
		}

		mesg.append("\n");
		mesg.append("Thanks,\n");
		mesg.append("User Resource Request Watcher\n\n");

		try
		{
			MailUtil.sendEmails(host, from, name, addr, subj, mesg.toString());
		}
		catch (Exception e)
		{
			log.error("failed to send email: {}", mesg.toString(), e);
		}
	}

	@Override
	public Map<String, String> getUsageOptions()
	{
		return new HashMap<String, String>();
	}

	public static String getTaskName()
	{
		return TASKNAME;
	}
}

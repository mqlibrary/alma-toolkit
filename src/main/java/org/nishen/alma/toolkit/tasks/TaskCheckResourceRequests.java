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

		UserResourceSharingRequests requests =
		        target.request(MediaType.APPLICATION_XML_TYPE).get(UserResourceSharingRequests.class);

		List<UserResourceSharingRequest> requestList = new ArrayList<UserResourceSharingRequest>();
		for (UserResourceSharingRequest r : requests.getUserResourceSharingRequest())
		{
			String levelOfService = r.getLevelOfService().getValue();
			String status = r.getStatus().getValue();

			log.debug("level of service [{}]: {}", levelOfService, r.getTitle());

			if (("REQUEST_CREATED_LEND".equals(status) || "LOCATE_FAILED".equals(status)) &&
			    ("RUSH_LOCAL".equals(levelOfService) || "EXPRESS_LOCAL".equals(levelOfService)))
				requestList.add(r);
		}

		if (requestList.size() == 0)
		{
			log.info("no urgent requests...");
			return;
		}

		StringBuilder mesg = new StringBuilder();
		mesg.append("Dear ILL Librarian,").append("<br/><br/>");
		mesg.append("There are urgent requests requiring your attention:").append("<br/><br/>");

		mesg.append("<style type=\"text/css\">\n");
		mesg.append("table th { text-align: right; width: 120px; color: white; ");
		mesg.append("background: #770000; padding-right: 5px; }");
		mesg.append("\n</style>\n\n");

		mesg.append("<table style=\"width: 100%; text-align: left;\">");
		for (UserResourceSharingRequest r : requestList)
		{
			Calendar created = Calendar.getInstance();
			created.setTime(r.getCreatedTime().toGregorianCalendar().getTime());

			Calendar modified = Calendar.getInstance();
			modified.setTime(r.getLastModifiedTime().toGregorianCalendar().getTime());

			mesg.append("<tr>");
			mesg.append("<th>").append("EXTERNAL ID").append("</th>");
			mesg.append("<td>").append(r.getExternalId()).append("</td>");
			mesg.append("</tr>");

			mesg.append("<tr>");
			mesg.append("<th>").append("CREATED").append("</th>");
			mesg.append("<td>").append(created.getTime()).append("</td>");
			mesg.append("</tr>");

			mesg.append("<tr>");
			mesg.append("<th>").append("MODIFIED").append("</th>");
			mesg.append("<td>").append(modified.getTime()).append("</td>");
			mesg.append("</tr>");

			mesg.append("<tr>");
			mesg.append("<th>").append("STATUS").append("</th>");
			mesg.append("<td>").append(r.getStatus().getValue()).append("</td>");
			mesg.append("</tr>");

			mesg.append("<tr>");
			mesg.append("<th>").append("SERVICE LEVEL").append("</th>");
			mesg.append("<td>").append(r.getLevelOfService().getValue()).append("</td>");
			mesg.append("</tr>");

			mesg.append("<tr>");
			mesg.append("<th>").append("TITLE").append("</th>");
			mesg.append("<td>").append(r.getTitle()).append("</td>");
			mesg.append("</tr>");

			if (r.getCallNumber() != null && !"".equals(r.getCallNumber()))
			{
				mesg.append("<tr>");
				mesg.append("<th>").append("CALL NUMBER").append("</th>");
				mesg.append("<td>").append(r.getCallNumber()).append("</td>");
				mesg.append("</tr>");
			}

			mesg.append("<tr><td colspan=\"2\">&nbsp</td></tr>");
		}
		mesg.append("</table>");

		mesg.append("<br/>");
		mesg.append("Thanks,").append("<br/>");
		mesg.append("ILL Alert Bot").append("<br/><br/>");

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

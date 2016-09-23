package org.nishen.alma.toolkit.util;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailUtil
{
	private static final Logger log = LoggerFactory.getLogger(MailUtil.class);

	public static void sendEmails(String host, String from, String name, String addr, String subj,
	                              String mesg) throws Exception
	{
		Properties mailConfig = System.getProperties();
		mailConfig.put("mail.smtp.host", host);

		Session session = Session.getInstance(mailConfig, null);
		session.setDebug(false);

		// Construct the message
		Message msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(from, name));

		String[] addressList = addr.split(";");
		for (int x = 0; x < addressList.length; x++)
		{
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(addressList[x]));
			log.debug("adding mail.to: {}", addressList[x]);
		}

		msg.setSubject(subj);
		msg.setText(mesg);

		// Send the message
		Transport.send(msg);
	}
}

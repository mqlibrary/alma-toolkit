package org.nishen.alma.toolkit.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.nishen.alma.toolkit.entity.partners.Address;
import org.nishen.alma.toolkit.entity.partners.Address.AddressTypes;
import org.nishen.alma.toolkit.entity.partners.Address.Country;
import org.nishen.alma.toolkit.entity.partners.Addresses;
import org.nishen.alma.toolkit.entity.partners.ContactInfo;
import org.nishen.alma.toolkit.entity.partners.Email;
import org.nishen.alma.toolkit.entity.partners.Email.EmailTypes;
import org.nishen.alma.toolkit.entity.partners.Emails;
import org.nishen.alma.toolkit.entity.partners.IsoDetails;
import org.nishen.alma.toolkit.entity.partners.Notes;
import org.nishen.alma.toolkit.entity.partners.ObjectFactory;
import org.nishen.alma.toolkit.entity.partners.Partner;
import org.nishen.alma.toolkit.entity.partners.PartnerDetails;
import org.nishen.alma.toolkit.entity.partners.PartnerDetails.LocateProfile;
import org.nishen.alma.toolkit.entity.partners.PartnerDetails.SystemType;
import org.nishen.alma.toolkit.entity.partners.Partners;
import org.nishen.alma.toolkit.entity.partners.Phone;
import org.nishen.alma.toolkit.entity.partners.Phone.PhoneTypes;
import org.nishen.alma.toolkit.entity.partners.Phones;
import org.nishen.alma.toolkit.entity.partners.ProfileDetails;
import org.nishen.alma.toolkit.entity.partners.ProfileType;
import org.nishen.alma.toolkit.entity.partners.RequestExpiryType;
import org.nishen.alma.toolkit.entity.partners.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
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
public class TaskUpdateResourcePartners implements Task
{
	private static final Logger log = LoggerFactory.getLogger(TaskUpdateResourcePartners.class);

	private static final String TASKNAME = "updateResourcePartners";

	private static final ObjectFactory of = new ObjectFactory();

	private Properties config;

	private Provider<WebTarget> webTargetProviderAlma;

	private Provider<WebClient> webClientProvider;

	private String laddUrl;

	private String tepunaUrl;

	@Inject
	private TaskUpdateResourcePartners(@Named("app.cmdline") final String[] args,
	                                   @Named("app.config") final Properties config,
	                                   @Named("ws.url.alma") Provider<WebTarget> webTargetProviderAlma,
	                                   Provider<WebClient> webClientProvider)
	{
		this.config = config;

		this.laddUrl = config.getProperty("ws.url.ladd");

		this.tepunaUrl = config.getProperty("ws.url.tepuna");

		this.webTargetProviderAlma = webTargetProviderAlma;

		this.webClientProvider = webClientProvider;

		log.debug("initialised taskupdateresourcepartners");
	}

	@Override
	public void run()
	{
		log.info("executing task: {}", this.getClass().getSimpleName());

		Map<String, Partner> almaPartners = getAlmaPartners();

		Map<String, Partner> partners = getLaddPartners();
		partners.putAll(getTepunaPartners());

		for (String s : partners.keySet())
		{
			Partner p = partners.get(s);
			Partner ap = almaPartners.get(s);

			if (ap == null)
			{
				addPartner(p);
			}
			else if (!isEqual(p, ap))
			{
				updatePartner(p);
			}
		}
	}

	public Partner getAlmaPartner(String nuc)
	{
		WebTarget t = webTargetProviderAlma.get().path("partners").path(nuc);
		Partner result = t.request(MediaType.APPLICATION_XML).get(Partner.class);

		return result;
	}

	public Map<String, Partner> getAlmaPartners()
	{
		Map<String, Partner> partnerMap = new HashMap<String, Partner>();

		WebTarget target = webTargetProviderAlma.get().path("partners");

		long limit = 100;
		long offset = 0;
		long total = -1;
		long count = 0;

		do
		{
			WebTarget t = target.queryParam("limit", limit).queryParam("offset", offset);
			Partners result = t.request(MediaType.APPLICATION_XML).get(Partners.class);

			if (total < 0)
				total = result.getTotalRecordCount();
			count += result.getPartner().size();
			offset += limit;

			for (Partner p : result.getPartner())
				partnerMap.put(p.getPartnerDetails().getCode(), p);

			log.debug("getAlmaPartners [count/total/offset]: {}/{}/{}", count, total, offset);
		}
		while (count < total);

		return partnerMap;
	}

	public Map<String, Partner> getLaddPartners()
	{
		Map<String, Partner> result = new HashMap<String, Partner>();

		String institutionCode = config.getProperty("ladd.institution.code");

		WebClient webClient = webClientProvider.get();
		HtmlPage page = null;
		try
		{
			page = webClient.getPage(laddUrl);
		}
		catch (IOException e)
		{
			log.error("unable to acquire page: {}", laddUrl);
			return result;
		}

		HtmlTable table = (HtmlTable) page.getElementById("suspension");
		for (HtmlTableRow row : table.getRows())
		{
			String nuc = row.getCell(0).asText();

			if ("NUC symbol".equals(nuc) || institutionCode.equals(nuc))
			{
				log.debug("skipping nuc: {}", nuc);
				continue;
			}

			String org = row.getCell(1).asText();
			String prefix = "NLA";
			boolean suspended = "Suspended".equals(row.getCell(3).asText());

			Partner partner = new Partner();
			partner.setLink("https://api-ap.hosted.exlibrisgroup.com/almaws/v1/partners/" + nuc);

			PartnerDetails partnerDetails = new PartnerDetails();
			partner.setPartnerDetails(partnerDetails);

			ProfileDetails profileDetails = new ProfileDetails();
			partnerDetails.setProfileDetails(profileDetails);

			profileDetails.setProfileType(ProfileType.ISO);

			RequestExpiryType requestExpiryType = new RequestExpiryType();
			requestExpiryType.setValue("INTEREST_DATE");
			requestExpiryType.setDesc("Expire by interest date");

			IsoDetails isoDetails = new IsoDetails();
			profileDetails.setIsoDetails(isoDetails);

			isoDetails.setAlternativeDocumentDelivery(false);
			isoDetails.setIllServer(config.getProperty("alma.ill.server"));
			isoDetails.setIllPort(Integer.parseInt(config.getProperty("alma.ill.port")));
			isoDetails.setIsoSymbol(prefix + ":" + nuc);
			isoDetails.setSendRequesterInformation(false);
			isoDetails.setSharedBarcodes(true);
			isoDetails.setRequestExpiryType(requestExpiryType);

			SystemType systemType = new SystemType();
			systemType.setValue("LADD");
			systemType.setDesc("LADD");

			LocateProfile locateProfile = new LocateProfile();
			locateProfile.setValue("LADD");
			locateProfile.setDesc("LADD Locate Profile");

			partnerDetails.setStatus(suspended ? Status.INACTIVE : Status.ACTIVE);
			partnerDetails.setCode(nuc);
			partnerDetails.setName(org);
			partnerDetails.setSystemType(systemType);
			partnerDetails.setAvgSupplyTime(4);
			partnerDetails.setDeliveryDelay(4);
			partnerDetails.setCurrency("AUD");
			partnerDetails.setBorrowingSupported(true);
			partnerDetails.setBorrowingWorkflow("LADD_Borrowing");
			partnerDetails.setLendingSupported(true);
			partnerDetails.setLendingWorkflow("LADD_Lending");
			partnerDetails.setLocateProfile(locateProfile);
			partnerDetails.setHoldingCode(nuc);

			ContactInfo contactInfo = new ContactInfo();
			partner.setContactInfo(contactInfo);

			Addresses addresses = new Addresses();
			contactInfo.setAddresses(addresses);

			Emails emails = new Emails();
			contactInfo.setEmails(emails);

			Phones phones = new Phones();
			contactInfo.setPhones(phones);

			Notes notes = new Notes();
			partner.setNotes(notes);

			result.put(partner.getPartnerDetails().getCode(), partner);
		}

		return result;
	}

	public Map<String, Partner> getTepunaPartners()
	{
		Map<String, Partner> result = new HashMap<String, Partner>();

		String institutionCode = config.getProperty("ladd.institution.code");

		WebClient webClient = webClientProvider.get();
		TextPage page = null;
		try
		{
			page = webClient.getPage(tepunaUrl);
		}
		catch (IOException e)
		{
			log.error("unable to acquire page: {}", tepunaUrl);
			return result;
		}

		log.debug("{}", page.getContent());

		try (CSVParser parser = CSVParser.parse(page.getContent(), CSVFormat.DEFAULT.withHeader()))
		{
			for (CSVRecord record : parser)
			{
				String nuc = record.get(0);

				if ("NUC symbol".equals(nuc) || institutionCode.equals(nuc))
				{
					log.debug("skipping nuc: {}", nuc);
					continue;
				}

				String org = record.get(2);
				String prefix = "NLNZ";

				Partner partner = new Partner();

				partner.setLink("https://api-ap.hosted.exlibrisgroup.com/almaws/v1/partners/" + nuc);

				PartnerDetails partnerDetails = new PartnerDetails();
				partner.setPartnerDetails(partnerDetails);

				ProfileDetails profileDetails = new ProfileDetails();
				partnerDetails.setProfileDetails(profileDetails);

				profileDetails.setProfileType(ProfileType.ISO);

				RequestExpiryType requestExpiryType = new RequestExpiryType();
				requestExpiryType.setValue("INTEREST_DATE");
				requestExpiryType.setDesc("Expire by interest date");

				IsoDetails isoDetails = new IsoDetails();
				profileDetails.setIsoDetails(isoDetails);

				isoDetails.setAlternativeDocumentDelivery(false);
				isoDetails.setIllServer(config.getProperty("alma.ill.server"));
				isoDetails.setIllPort(Integer.parseInt(config.getProperty("alma.ill.port")));
				isoDetails.setIsoSymbol(prefix + ":" + nuc);
				isoDetails.setSendRequesterInformation(false);
				isoDetails.setSharedBarcodes(true);
				isoDetails.setRequestExpiryType(requestExpiryType);

				SystemType systemType = new SystemType();
				systemType.setValue("LADD");
				systemType.setDesc("LADD");

				LocateProfile locateProfile = new LocateProfile();
				locateProfile.setValue("LADD");
				locateProfile.setDesc("LADD Locate Profile");

				partnerDetails.setStatus(Status.ACTIVE);
				partnerDetails.setCode(nuc);
				partnerDetails.setName(org);
				partnerDetails.setSystemType(systemType);
				partnerDetails.setAvgSupplyTime(4);
				partnerDetails.setDeliveryDelay(4);
				partnerDetails.setCurrency("AUD");
				partnerDetails.setBorrowingSupported(true);
				partnerDetails.setBorrowingWorkflow("LADD_Borrowing");
				partnerDetails.setLendingSupported(true);
				partnerDetails.setLendingWorkflow("LADD_Lending");
				partnerDetails.setLocateProfile(locateProfile);
				partnerDetails.setHoldingCode(nuc);

				ContactInfo contactInfo = new ContactInfo();
				partner.setContactInfo(contactInfo);

				Addresses addresses = new Addresses();
				contactInfo.setAddresses(addresses);

				String s = record.get(5);
				if (s == null || "".equals(s.trim()))
					s = record.get(4);

				if (s != null && !"".equals(s.trim()))
				{
					Address address = getAddress(s);
					address.setPreferred(true);
					address.setAddressTypes(new AddressTypes());
					address.getAddressTypes().getAddressType().add("ALL");
					addresses.getAddress().add(address);

					log.debug("nuc/address [{}]: {}", nuc, address);
				}

				Emails emails = new Emails();
				contactInfo.setEmails(emails);

				s = record.get(6);
				if (s != null && !"".equals(s.trim()))
				{
					Email email = new Email();
					email.setEmailTypes(new EmailTypes());
					email.setEmailAddress(s);
					email.setPreferred(true);
					email.setDescription("Primary Email Address");
					email.getEmailTypes().getEmailType().add("ALL");
					emails.getEmail().add(email);

					log.debug("nuc/email1 [{}]: {}", nuc, email);
				}

				s = record.get(13);
				if (s != null && !"".equals(s.trim()))
				{
					Email email = new Email();
					email.setEmailTypes(new EmailTypes());
					email.setEmailAddress(s);
					email.setPreferred(true);
					String m = record.get(12);
					if (m != null && !"".equals(m))
						email.setDescription("Manager Email Address: " + m);
					else
						email.setDescription("Manager Email Address");
					email.getEmailTypes().getEmailType().add("ALL");
					emails.getEmail().add(email);

					log.debug("nuc/email2 [{}]: {}", nuc, email);
				}

				Phones phones = new Phones();
				contactInfo.setPhones(phones);
				s = record.get(15);
				if (s == null || "".equals(s.trim()))
					s = record.get(7);

				if (s != null && !"".equals(s.trim()))
				{
					Phone phone = new Phone();
					phone.setPhoneTypes(new PhoneTypes());
					phone.setPhoneNumber(s);
					phone.setPreferred(true);
					phone.setPreferredSMS(false);
					phone.getPhoneTypes().getPhoneType().add("ALL");
					phones.getPhone().add(phone);

					log.debug("nuc/phone [{}]: {}", nuc, phone);
				}

				Notes notes = new Notes();
				partner.setNotes(notes);

				result.put(partner.getPartnerDetails().getCode(), partner);
			}
		}
		catch (IOException ioe)
		{
			log.error("unable to parse data: {}", tepunaUrl);
		}

		return result;
	}

	public void addPartner(Partner partner)
	{
		persistPartner(partner, false);
	}

	public void updatePartner(Partner partner)
	{
		persistPartner(partner, true);
	}

	private void persistPartner(Partner partner, boolean replace)
	{
		String m = MediaType.APPLICATION_XML;

		Partner result = null;

		WebTarget t = webTargetProviderAlma.get().path("partners");

		JAXBElement<Partner> p = of.createPartner(partner);

		String code = partner.getPartnerDetails().getCode();
		try
		{
			if (replace)
			{
				result = t.path(code).request(m).put(Entity.entity(p, m), Partner.class);
			}
			else
			{
				result = t.request(m).post(Entity.entity(p, m), Partner.class);
			}
		}
		catch (Exception e)
		{
			log.error("error adding partner:\n{}", result, e);
		}

		log.debug("result:\n{}", result);
	}

	public Address getAddress(String s)
	{
		Address address = new Address();

		if (s == null || s.trim().length() == 0)
			return address;

		List<String> tmpl = Arrays.asList(s.split(" *, *"));
		List<String> addr = new ArrayList<String>();
		for (String tli : tmpl)
			if (tli != null && !"".equals(tli))
				addr.add(0, tli);

		if (addr.size() == 0)
			return address;

		address.setLine1(addr.get(addr.size() - 1));

		Country country = new Country();
		switch (addr.get(0))
		{
			case "Australia":
				country.setValue("AUS");
				country.setDesc("Australia");
				addr.remove(0);
				address.setCountry(country);
				break;

			case "New Zealand":
				country.setValue("NZL");
				country.setDesc("New Zealand");
				addr.remove(0);
				address.setCountry(country);
				break;

			default:
		}

		if (addr.size() == 0)
			return address;

		if (addr.get(0).matches("\\d{4}"))
		{
			address.setPostalCode(addr.get(0));
			addr.remove(0);

			if (addr.size() == 0)
				return address;

			address.setCity(addr.get(0));
			addr.remove(0);

			if (addr.size() == 0)
				return address;
		}
		else
		{
			address.setCity(addr.get(0));
			addr.remove(0);

			if (addr.size() == 0)
				return address;
		}

		Collections.reverse(addr);

		address.setLine1(addr.get(0));
		addr.remove(0);
		if (addr.size() == 0)
			return address;

		address.setLine2(addr.get(0));
		addr.remove(0);
		if (addr.size() == 0)
			return address;

		address.setLine3(addr.get(0));
		addr.remove(0);
		if (addr.size() == 0)
			return address;

		address.setLine4(addr.get(0));
		addr.remove(0);
		if (addr.size() == 0)
			return address;

		address.setLine5(addr.get(0));
		addr.remove(0);

		return address;
	}

	public boolean isEqual(Partner a, Partner b)
	{
		if (a == null && b == null)
			return true;

		if (a == null || b == null)
			return false;

		if (a.getContactInfo() != null)
			if (a.getContactInfo().getAddresses() != null)
				for (Address address : a.getContactInfo().getAddresses().getAddress())
					address.setStartDate(null);

		if (b.getContactInfo() != null)
			if (b.getContactInfo().getAddresses() != null)
				for (Address address : b.getContactInfo().getAddresses().getAddress())
					address.setStartDate(null);

		return a.equals(b);
	}

	@Override
	public Map<String, String> getUsageOptions()
	{
		Map<String, String> options = new HashMap<String, String>();

		return options;
	}

	public static String getTaskName()
	{
		return TASKNAME;
	}
}

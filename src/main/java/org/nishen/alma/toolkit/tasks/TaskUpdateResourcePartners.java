package org.nishen.alma.toolkit.tasks;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;

import org.nishen.alma.toolkit.entity.ResourcePartner;
import org.nishen.alma.toolkit.entity.partners.Addresses;
import org.nishen.alma.toolkit.entity.partners.ContactInfo;
import org.nishen.alma.toolkit.entity.partners.Emails;
import org.nishen.alma.toolkit.entity.partners.IsoDetails;
import org.nishen.alma.toolkit.entity.partners.Notes;
import org.nishen.alma.toolkit.entity.partners.ObjectFactory;
import org.nishen.alma.toolkit.entity.partners.Partner;
import org.nishen.alma.toolkit.entity.partners.PartnerDetails;
import org.nishen.alma.toolkit.entity.partners.PartnerDetails.LocateProfile;
import org.nishen.alma.toolkit.entity.partners.PartnerDetails.SystemType;
import org.nishen.alma.toolkit.entity.partners.Partners;
import org.nishen.alma.toolkit.entity.partners.Phones;
import org.nishen.alma.toolkit.entity.partners.ProfileDetails;
import org.nishen.alma.toolkit.entity.partners.ProfileType;
import org.nishen.alma.toolkit.entity.partners.RequestExpiryType;
import org.nishen.alma.toolkit.entity.partners.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");

	private static final ObjectFactory of = new ObjectFactory();

	private Properties config;

	private Provider<WebTarget> webTargetProviderAlma;

	private Provider<WebClient> webClientProvider;

	private String laddUrl;

	@Inject
	private TaskUpdateResourcePartners(@Named("app.cmdline") final String[] args,
	                                   @Named("app.config") final Properties config,
	                                   @Named("ws.url.alma") Provider<WebTarget> webTargetProviderAlma,
	                                   Provider<WebClient> webClientProvider)
	{
		this.config = config;

		this.laddUrl = config.getProperty("ws.url.ladd");

		this.webTargetProviderAlma = webTargetProviderAlma;

		this.webClientProvider = webClientProvider;

		log.debug("initialised taskupdateresourcepartners");
	}

	@Override
	public void run()
	{
		log.info("executing task: {}", this.getClass().getSimpleName());

		Map<String, Partner> almaPartners = getAlmaPartners();

		Map<String, Partner> laddPartners = getLaddPartners();

		for (String s : laddPartners.keySet())
		{
			Partner lp = laddPartners.get(s);
			Partner ap = almaPartners.get(s);

			if (ap == null)
			{
				addPartner(lp);
			}
			else if (!lp.equals(ap))
			{
				updatePartner(lp);
			}
		}
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

			ResourcePartner library = new ResourcePartner();
			library.setNuc(row.getCell(0).asText());
			library.setOrganisation(row.getCell(1).asText());
			library.setIsoill("ISO ILL".equals(row.getCell(2).asText()));
			library.setSuspended("Suspended".equals(row.getCell(3).asText()));

			String df = row.getCell(4).asText();
			String dt = row.getCell(5).asText();
			try
			{
				if (df != null && !"".equals(df.trim()))
					library.setSuspendedFrom(sdf.parse(df));

				if (dt != null && !"".equals(dt.trim()))
					library.setSuspendedTo(sdf.parse(dt));
			}
			catch (ParseException pe)
			{
				log.error("unable to parse dates: [{}] or [{}]", df, dt);
			}

			log.debug("library: {}", library);

			Partner p = makePartner(library);

			result.put(p.getPartnerDetails().getCode(), p);
		}

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
		if (replace)
		{
			result = t.path(code).request(m).put(Entity.entity(p, m), Partner.class);
		}
		else
		{
			result = t.request(m).post(Entity.entity(p, m), Partner.class);
		}

		log.debug("result:\n{}", result);
	}

	private Partner makePartner(ResourcePartner rp)
	{
		Partner partner = new Partner();
		partner.setLink("https://api-ap.hosted.exlibrisgroup.com/almaws/v1/partners/" + rp.getNuc());

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
		isoDetails.setIsoSymbol("NLA:" + rp.getNuc());
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
		partnerDetails.setCode(rp.getNuc());
		partnerDetails.setName(rp.getOrganisation());
		partnerDetails.setSystemType(systemType);
		partnerDetails.setAvgSupplyTime(4);
		partnerDetails.setDeliveryDelay(4);
		partnerDetails.setCurrency("AUD");
		partnerDetails.setBorrowingSupported(true);
		partnerDetails.setBorrowingWorkflow("LADD_Borrowing");
		partnerDetails.setLendingSupported(true);
		partnerDetails.setLendingWorkflow("LADD_Lending");
		partnerDetails.setLocateProfile(locateProfile);
		partnerDetails.setHoldingCode(rp.getNuc());

		ContactInfo contactInfo = new ContactInfo();
		partner.setContactInfo(contactInfo);

		contactInfo.setAddresses(new Addresses());
		contactInfo.setEmails(new Emails());
		contactInfo.setPhones(new Phones());

		Notes notes = new Notes();
		partner.setNotes(notes);

		return partner;
	}

	public boolean isEqual(Partner a, Partner b)
	{
		if (a == null && b == null)
			return true;

		if (a == null || b == null)
			return false;

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

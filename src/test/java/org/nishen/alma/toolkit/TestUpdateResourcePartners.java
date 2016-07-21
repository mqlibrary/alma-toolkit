package org.nishen.alma.toolkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nishen.alma.toolkit.entity.partners.Partner;
import org.nishen.alma.toolkit.tasks.TaskUpdateResourcePartners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class TestUpdateResourcePartners
{
	private static final Logger log = LoggerFactory.getLogger(TestUpdateResourcePartners.class);

	private static Map<String, Partner> almaPartners = null;

	private static TaskUpdateResourcePartners task = null;

	@BeforeClass
	public static void setup()
	{
		List<Module> modules = new ArrayList<Module>();
		modules.add(new ToolkitModule(new String[0]));

		Injector injector = Guice.createInjector(modules);

		try
		{
			task = injector.getInstance(TaskUpdateResourcePartners.class);
			almaPartners = task.getAlmaPartners();
		}
		catch (Exception e)
		{
			log.error("", e);
		}
	}

	@Test
	public void TestGetAlmaPartner()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		Partner partner = task.getAlmaPartner("ATU");

		Assert.assertNotNull(partner);
	}

	@Test
	public void TestGetAlmaPartners()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		Map<String, Partner> partners = task.getAlmaPartners();

		Assert.assertNotNull(partners);
		Assert.assertTrue(partners.size() > 5);

		log.debug("partners count: {}", partners.size());
	}

	@Test
	public void TestGetLaddPartners()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		Map<String, Partner> tepunaPartners = task.getLaddPartners();

		Assert.assertTrue(tepunaPartners != null && tepunaPartners.size() > 10);
	}

	@Test
	public void TestCheckLaddPartnerCompare()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		Map<String, Partner> laddPartners = task.getLaddPartners();

		Partner ap = almaPartners.get("AACOM");
		log.debug("{}", ap);

		Partner lp = laddPartners.get("AACOM");
		log.debug("{}", lp);

		Assert.assertTrue(task.isEqual(ap, lp));
	}

	@Test
	public void TestCheckTepunaPartnerCompare()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		Map<String, Partner> tepunaPartners = task.getTepunaPartners();

		Partner ap = almaPartners.get("WLT");
		log.debug("{}", ap);

		Partner tp = tepunaPartners.get("WLT");
		log.debug("{}", tp);

		Assert.assertTrue(task.isEqual(ap, tp));
	}

	@Test
	public void TestGetTepunaPartners()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		Map<String, Partner> tepunaPartners = task.getTepunaPartners();

		Assert.assertTrue(tepunaPartners != null && tepunaPartners.size() > 10);
	}

	@Test
	public void TestGetTepunaAddress()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		Map<String, Partner> tepunaPartners = task.getTepunaPartners();

		Assert.assertTrue(tepunaPartners != null && tepunaPartners.size() > 10);

		Partner p = tepunaPartners.get("ATU");
		Assert.assertEquals("Auckland University of Technology Library", p.getPartnerDetails().getName());
		Assert.assertEquals("+64 9 921 9999 x 8662", p.getContactInfo().getPhones().getPhone().get(0).getPhoneNumber());
		Assert.assertEquals("Private Bag 92006", p.getContactInfo().getAddresses().getAddress().get(0).getLine1());
	}
}

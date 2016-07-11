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
		}
		catch (Exception e)
		{
			log.error("", e);
		}
	}

	@Test
	public void TestCheckPartnerCompare()
	{
		log.debug("running test: {}", Arrays.asList(new Throwable().getStackTrace()).get(0).getMethodName());

		Map<String, Partner> almaPartners = task.getAlmaPartners();

		Map<String, Partner> laddPartners = task.getLaddPartners();

		Partner ap = almaPartners.get("AACOM");
		log.debug("{}", ap);

		Partner lp = laddPartners.get("AACOM");
		log.debug("{}", lp);

		Assert.assertTrue(task.isEqual(ap, lp));
	}
}

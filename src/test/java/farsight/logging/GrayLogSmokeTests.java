package farsight.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import farsight.logging.config.LogEntity;

@Disabled // remove if this test should run!
public class GrayLogSmokeTests {

	static {
		// Add package to log4j PluginManager (this would be done by jar Manifest in
		// production!)
		PluginManager.addPackage("esb.framework.logging.log4jPlugin");
	}

	@Test
	public void testGrayLog() {
		LoggingFrontend lf = new LoggingFrontend("src/test/resources/logging/runtime.Graylog.conf.xml");
		lf.checkInitialized();
		lf.setup(new LogEntity("service", Level.INFO, "log/test.log", null, null));
		lf.getLogger("service").info("Das ist ein Test!");
	}

}

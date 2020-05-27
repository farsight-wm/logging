package farsight.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.softwareag.util.IDataMap;

import farsight.logging.graylog.GelfMessage;
import farsight.logging.tools.FakeGraylog;

public class IntegrationTestFakeGraylog {
	
	static {
		//Add package to log4j PluginManager (this would be done by jar Manifest in production!) 
		PluginManager.addPackage("esb.framework.logging.log4jPlugin");
	}
	
	private static FakeGraylog graylog = new FakeGraylog();
	private static LoggingFrontend lf;
	
	@BeforeAll
	public static void startFakeGraylog() {
		graylog.start();
		lf = new LoggingFrontend("src/test/resources/logging/runtime.fakeGraylog.conf.xml");
	}
	
	@AfterAll
	public static void stopFakeGraylog() {
		graylog.stop();
	}
	
	private void yield() {
		try {
			//let network/threads time to finish writing logs
			Thread.sleep(10);
			Thread.yield();
		} catch (InterruptedException e1) {
			//ignore
		}
	}

	@Test
	public void testSimpleMessage() {
		graylog.reset();
		lf.getLogger("service").info("Das ist ein Test!");
		yield();
		assertThat(graylog.getMessages()).as("has one entry").hasSize(1);
		assertThat(graylog.getMessages().get(0)).as("contains log message").contains("Das ist ein Test!");
	}
	
	@Test
	public void testMessageWithMetadata() {
		graylog.reset();
		IDataMap data = new IDataMap();
		data.put("important", "stuff");
		lf.getLogger("service").info(GelfMessage.create("TestMessage!", "some:caller", data.getIData(), null));
		yield();
		assertThat(graylog.getMessages()).as("has one entry").hasSize(1);
		
		String message = graylog.getMessages().get(0);
		//GELF pattern sends business data as "_<key>":"<value>"
		assertThat(message).as("contains important stuff").contains("\"_important\":\"stuff\"");		
	}
	
	
}

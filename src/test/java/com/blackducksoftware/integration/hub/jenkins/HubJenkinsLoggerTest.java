package com.blackducksoftware.integration.hub.jenkins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.blackducksoftware.integration.hub.jenkins.utils.StoredPrintStream;
import com.blackducksoftware.integration.hub.jenkins.utils.TestBuildListener;
import com.blackducksoftware.integration.hub.logging.LogLevel;

import hudson.EnvVars;

public class HubJenkinsLoggerTest {
	private List<String> expectedMessages;
	private StoredPrintStream storedStream;
	private HubJenkinsLogger logger;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		expectedMessages = new ArrayList<String>();
		storedStream = new StoredPrintStream();

		logger = new HubJenkinsLogger(new TestBuildListener(storedStream));
	}

	@After
	public void tearDown() {
		final List<String> outputList = storedStream.getOutputList();
		final String outputString = storedStream.getOutputString();
		assertEquals("Too many/not enough messages expected: \n" + outputString, expectedMessages.size(),
				outputList.size());

		for (final String expectedMessage : expectedMessages) {
			assertTrue("Did not find the expected message : " + expectedMessage,
					outputString.contains(expectedMessage));
		}
	}

	@Test
	public void testSetLogLevelWithVariables() {
		final EnvVars variables = new EnvVars();
		logger.setLogLevel(variables);
		assertEquals(LogLevel.INFO, logger.getLogLevel());

		variables.put("HUB_LOG_LEVEL", "FAKE");
		logger.setLogLevel(variables);
		assertEquals(LogLevel.INFO, logger.getLogLevel());

		variables.put("HUB_LOG_LEVEL", "error");
		logger.setLogLevel(variables);
		assertEquals(LogLevel.ERROR, logger.getLogLevel());

		variables.put("HUB_LOG_LEVEL", "erRor");
		logger.setLogLevel(variables);
		assertEquals(LogLevel.ERROR, logger.getLogLevel());

		variables.put("HUB_LOG_LEVEL", "OFF");
		logger.setLogLevel(variables);
		assertEquals(LogLevel.OFF, logger.getLogLevel());

		variables.put("HUB_LOG_LEVEL", "ERROR");
		logger.setLogLevel(variables);
		assertEquals(LogLevel.ERROR, logger.getLogLevel());

		variables.put("HUB_LOG_LEVEL", "WARN");
		logger.setLogLevel(variables);
		assertEquals(LogLevel.WARN, logger.getLogLevel());

		variables.put("HUB_LOG_LEVEL", "INFO");
		logger.setLogLevel(variables);
		assertEquals(LogLevel.INFO, logger.getLogLevel());

		variables.put("HUB_LOG_LEVEL", "DEBUG");
		logger.setLogLevel(variables);
		assertEquals(LogLevel.DEBUG, logger.getLogLevel());

		variables.put("HUB_LOG_LEVEL", "TRACE");
		logger.setLogLevel(variables);
		assertEquals(LogLevel.TRACE, logger.getLogLevel());
	}

	@Test
	public void testOff() {
		logger.setLogLevel(LogLevel.OFF);
		logger.error("ERROR Test 1");
		logger.error("ERROR Test 2", new Exception("ERROR EXCEPTION Test 1"));
		logger.error(new Exception("ERROR EXCEPTION Test 2"));
		logger.warn("WARN Test 1");
		logger.info("INFO Test 1");
		logger.debug("DEBUG Test 1");
		logger.debug("DEBUG Test 2", new Exception("DEBUG EXCEPTION Test 1"));
		logger.trace("TRACE Test 1");
		logger.trace("TRACE Test 2", new Exception("TRACE EXCEPTION Test 1"));
	}

	@Test
	public void testError() {
		expectedMessages.add("ERROR Test 1");
		expectedMessages.add("ERROR Test 2");
		expectedMessages.add("ERROR EXCEPTION Test 1");
		expectedMessages.add("ERROR EXCEPTION Test 2");
		logger.setLogLevel(LogLevel.ERROR);
		logger.error("ERROR Test 1");
		logger.error("ERROR Test 2", new Exception("ERROR EXCEPTION Test 1"));
		logger.error(new Exception("ERROR EXCEPTION Test 2"));
		logger.warn("WARN Test 1");
		logger.info("INFO Test 1");
		logger.debug("DEBUG Test 1");
		logger.debug("DEBUG Test 2", new Exception("DEBUG EXCEPTION Test 1"));
		logger.trace("TRACE Test 1");
		logger.trace("TRACE Test 2", new Exception("TRACE EXCEPTION Test 1"));
	}

	@Test
	public void testWarn() {
		expectedMessages.add("ERROR Test 1");
		expectedMessages.add("ERROR Test 2");
		expectedMessages.add("ERROR EXCEPTION Test 1");
		expectedMessages.add("ERROR EXCEPTION Test 2");
		expectedMessages.add("WARN Test 1");
		logger.setLogLevel(LogLevel.WARN);
		logger.error("ERROR Test 1");
		logger.error("ERROR Test 2", new Exception("ERROR EXCEPTION Test 1"));
		logger.error(new Exception("ERROR EXCEPTION Test 2"));
		logger.warn("WARN Test 1");
		logger.info("INFO Test 1");
		logger.debug("DEBUG Test 1");
		logger.debug("DEBUG Test 2", new Exception("DEBUG EXCEPTION Test 1"));
		logger.trace("TRACE Test 1");
		logger.trace("TRACE Test 2", new Exception("TRACE EXCEPTION Test 1"));
	}

	@Test
	public void testInfo() {
		expectedMessages.add("ERROR Test 1");
		expectedMessages.add("ERROR Test 2");
		expectedMessages.add("ERROR EXCEPTION Test 1");
		expectedMessages.add("ERROR EXCEPTION Test 2");
		expectedMessages.add("WARN Test 1");
		expectedMessages.add("INFO Test 1");
		logger.setLogLevel(LogLevel.INFO);
		logger.error("ERROR Test 1");
		logger.error("ERROR Test 2", new Exception("ERROR EXCEPTION Test 1"));
		logger.error(new Exception("ERROR EXCEPTION Test 2"));
		logger.warn("WARN Test 1");
		logger.info("INFO Test 1");
		logger.debug("DEBUG Test 1");
		logger.debug("DEBUG Test 2", new Exception("DEBUG EXCEPTION Test 1"));
		logger.trace("TRACE Test 1");
		logger.trace("TRACE Test 2", new Exception("TRACE EXCEPTION Test 1"));
	}

	@Test
	public void testDebug() {
		expectedMessages.add("ERROR Test 1");
		expectedMessages.add("ERROR Test 2");
		expectedMessages.add("ERROR EXCEPTION Test 1");
		expectedMessages.add("ERROR EXCEPTION Test 2");
		expectedMessages.add("WARN Test 1");
		expectedMessages.add("INFO Test 1");
		expectedMessages.add("DEBUG Test 1");
		expectedMessages.add("DEBUG Test 2");
		expectedMessages.add("DEBUG EXCEPTION Test 1");
		logger.setLogLevel(LogLevel.DEBUG);
		logger.error("ERROR Test 1");
		logger.error("ERROR Test 2", new Exception("ERROR EXCEPTION Test 1"));
		logger.error(new Exception("ERROR EXCEPTION Test 2"));
		logger.warn("WARN Test 1");
		logger.info("INFO Test 1");
		logger.debug("DEBUG Test 1");
		logger.debug("DEBUG Test 2", new Exception("DEBUG EXCEPTION Test 1"));
		logger.trace("TRACE Test 1");
		logger.trace("TRACE Test 2", new Exception("TRACE EXCEPTION Test 1"));
	}

	@Test
	public void testTrace() {
		expectedMessages.add("ERROR Test 1");
		expectedMessages.add("ERROR Test 2");
		expectedMessages.add("ERROR EXCEPTION Test 1");
		expectedMessages.add("ERROR EXCEPTION Test 2");
		expectedMessages.add("WARN Test 1");
		expectedMessages.add("INFO Test 1");
		expectedMessages.add("DEBUG Test 1");
		expectedMessages.add("DEBUG Test 2");
		expectedMessages.add("DEBUG EXCEPTION Test 1");
		expectedMessages.add("TRACE Test 1");
		expectedMessages.add("TRACE Test 2");
		expectedMessages.add("TRACE EXCEPTION Test 1");
		logger.setLogLevel(LogLevel.TRACE);
		logger.error("ERROR Test 1");
		logger.error("ERROR Test 2", new Exception("ERROR EXCEPTION Test 1"));
		logger.error(new Exception("ERROR EXCEPTION Test 2"));
		logger.warn("WARN Test 1");
		logger.info("INFO Test 1");
		logger.debug("DEBUG Test 1");
		logger.debug("DEBUG Test 2", new Exception("DEBUG EXCEPTION Test 1"));
		logger.trace("TRACE Test 1");
		logger.trace("TRACE Test 2", new Exception("TRACE EXCEPTION Test 1"));
	}

}

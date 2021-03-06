/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.parsing;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.RunInfo;
import org.perfcake.common.BoundPeriod;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.message.Message;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.generator.DefaultMessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.model.ScenarioFactory;
import org.perfcake.parser.ScenarioParser;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.destinations.DummyDestination;
import org.perfcake.reporting.reporters.DummyReporter;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.reporting.reporters.WarmUpReporter;
import org.perfcake.validation.MessageValidator;
import org.perfcake.validation.ValidatorManager;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ScenarioParserTest {
   private ScenarioFactory scenarioFactory, noValidationScenarioFactory, noMessagesScenarioFactory;

   private static final int THREADS = 10;
   private static final String MESSAGE1_CONTENT = "Stupid is as supid does! :)";
   private static final String MESSAGE2_CONTENT = "I'm the fish!";
   private static final String SENDER_CLASS = "org.perfcake.message.sender.HTTPSender";
   private static final String FILTERED_PROPERTY_VALUE = "filtered-property-value";
   private static final String DEFAULT_PROPERTY_VALUE = "default-property-value";

   @BeforeClass
   public void prepareScenarioParser() throws PerfCakeException, URISyntaxException, IOException {
      System.setProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY, getClass().getResource("/messages").getPath());
      System.setProperty("test.filtered.property", FILTERED_PROPERTY_VALUE);
      scenarioFactory = new ScenarioFactory(new ScenarioParser(getClass().getResource("/scenarios/test-scenario.xml")).parse());
      noValidationScenarioFactory = new ScenarioFactory(new ScenarioParser(getClass().getResource("/scenarios/test-scenario-no-validation.xml")).parse());
      noMessagesScenarioFactory = new ScenarioFactory(new ScenarioParser(getClass().getResource("/scenarios/test-scenario-no-messages.xml")).parse());
   }

   @Test
   public void parseScenarioPropertiesTest() {
      try {
         final Properties scenarioProperties = scenarioFactory.parseScenarioProperties();
         Assert.assertEquals(scenarioProperties.get("quickstartName"), "testQS", "quickstartName property");
         Assert.assertEquals(scenarioProperties.get("filteredProperty"), FILTERED_PROPERTY_VALUE, "filteredProperty property");
         Assert.assertEquals(scenarioProperties.get("defaultProperty"), DEFAULT_PROPERTY_VALUE, "defaultProperty property");
      } catch (final PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void parseSenderTest() {
      try {
         final MessageSenderManager senderManager = scenarioFactory.parseSender(THREADS);
         Assert.assertEquals(senderManager.getSenderClass(), SENDER_CLASS, "senderClass");
         Assert.assertEquals(senderManager.getSenderPoolSize(), THREADS, "senderPoolSize");
         // TODO: add assertions on a sender
      } catch (final PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void parseGeneratorTest() {
      try {
         AbstractMessageGenerator generator = scenarioFactory.parseGenerator();
         Assert.assertTrue(generator instanceof DefaultMessageGenerator, "The generator is not an instance of " + DefaultMessageGenerator.class.getName());
         DefaultMessageGenerator dmg = (DefaultMessageGenerator) generator;
         dmg.setRunInfo(new RunInfo(new Period(PeriodType.TIME, 30L)));
         Assert.assertEquals(dmg.getThreads(), THREADS, "threads");
         Assert.assertEquals(dmg.getThreadQueueSize(), 5000);
      } catch (PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void parseMessagesTest() {
      try {
         // Message store
         ValidatorManager validatorManager = scenarioFactory.parseValidation();
         List<MessageTemplate> messageStore = scenarioFactory.parseMessages(validatorManager);
         Assert.assertEquals(messageStore.size(), 3);

         // Message 1
         final MessageTemplate mts1 = messageStore.get(0);
         Assert.assertEquals(mts1.getMultiplicity(), new Long(10), "message1 multiplicity");
         final Message m1 = mts1.getMessage();
         // Message 1 content
         Assert.assertEquals(m1.getPayload(), MESSAGE1_CONTENT, "message1 content");
         // Message 1 headers
         final Properties headers1 = m1.getHeaders();
         Assert.assertEquals(headers1.size(), 3, "message1 headers count");
         Assert.assertEquals(headers1.get("m_header1"), "m_h_value1", "message1 header1");
         Assert.assertEquals(headers1.get("m_header2"), "m_h_value2", "message1 header2");
         Assert.assertEquals(headers1.get("m_header3"), "m_h_value3", "message1 header3");
         // Message 1 properties
         final Properties properties1 = m1.getProperties();
         Assert.assertEquals(properties1.size(), 3, "message1 properties count");
         Assert.assertEquals(properties1.get("m_property1"), "m_p_value1", "message1 property1");
         Assert.assertEquals(properties1.get("m_property2"), "m_p_value2", "message1 property2");
         Assert.assertEquals(properties1.get("m_property3"), "m_p_value3", "message1 property3");
         // Message 1 validatorIds
         final List<MessageValidator> validatorsList1 = mts1.getValidators();
         Assert.assertEquals(validatorsList1.size(), 2, "message1 validatorIdList size");
         Assert.assertTrue(validatorsList1.get(0).isValid(new Message("Hello, this is Stupid validator")));
         Assert.assertFalse(validatorsList1.get(0).isValid(new Message("Hello, this is Smart validator")));
         Assert.assertTrue(validatorsList1.get(1).isValid(new Message("Hello, this is happy validator :)")));
         Assert.assertFalse(validatorsList1.get(1).isValid(new Message("Hello, this is sad validator :(")));

         // Message 2
         final MessageTemplate mts2 = messageStore.get(1);
         Assert.assertEquals(mts2.getMultiplicity(), new Long(1), "message2 multiplicity");
         final Message m2 = mts2.getMessage();
         // Message 2 content
         Assert.assertEquals(m2.getPayload(), MESSAGE2_CONTENT, "message2 content");
         // Message 2 headers
         final Properties headers2 = m2.getHeaders();
         Assert.assertEquals(headers2.size(), 0, "message2 headers count");
         // Message 2 properties
         final Properties properties2 = m2.getProperties();
         Assert.assertEquals(properties2.size(), 0, "message2 properties count");
         // Message 2 validatorIds
         final List<MessageValidator> validatorsList2 = mts2.getValidators();
         Assert.assertEquals(validatorsList2.size(), 1, "message2 validatorIdList size");
         Assert.assertTrue(validatorsList2.get(0).isValid(new Message("Go for fishing!")));
         Assert.assertFalse(validatorsList2.get(0).isValid(new Message("Go for mushroom picking! There are no Fish.")));

         // Message 3
         final MessageTemplate mts3 = messageStore.get(2);
         final Message m3 = mts3.getMessage();
         Assert.assertNotNull(m3, "message 3 instance");
         Assert.assertNull(m3.getPayload(), "message 3 payload");
         Assert.assertEquals(m3.getHeaders().size(), 1, "message 3 header count");
         Assert.assertEquals(m3.getHeader("h3_name"), "h3_value", "message 3 header value");

         // Messages section is optional
         validatorManager = noMessagesScenarioFactory.parseValidation();
         final List<MessageTemplate> emptyMessageStore = noMessagesScenarioFactory.parseMessages(validatorManager);
         Assert.assertTrue(emptyMessageStore.isEmpty(), "empty message store with no messages in scenario");

      } catch (final PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void parseReportingTest() {
      try {
         final ReportManager reportManager = scenarioFactory.parseReporting();
         Assert.assertNotNull(reportManager);
         Assert.assertEquals(reportManager.getReporters().size(), 2, "reportManager's number of reporters");
         final String DUMMY_REPORTER_KEY = "dummy";
         final String WARM_UP_REPORTER_KEY = "warmup";

         Map<String, Reporter> reportersMap = new HashMap<>();
         for (Reporter reporter : reportManager.getReporters()) {
            if (reporter instanceof DummyReporter) {
               reportersMap.put(DUMMY_REPORTER_KEY, reporter);
            } else if (reporter instanceof WarmUpReporter) {
               reportersMap.put(WARM_UP_REPORTER_KEY, reporter);
            } else {
               Assert.fail("The reporter should be an instance of either " + DummyReporter.class.getCanonicalName() + " or " + WarmUpReporter.class.getCanonicalName() + ", but it is an instance of " + reporter.getClass().getCanonicalName() + "!");
            }
         }

         Reporter reporter = reportersMap.get(DUMMY_REPORTER_KEY);
         Assert.assertEquals(reporter.getDestinations().size(), 1, "reporter's number of destinations");
         Destination destination = reporter.getDestinations().iterator().next();
         Assert.assertTrue(destination instanceof DummyDestination, "destination's class");
         Assert.assertEquals(((DummyDestination) destination).getProperty(), "dummy_p_value", "destination's property value");
         Assert.assertEquals(((DummyDestination) destination).getProperty2(), "dummy_p2_value", "destination's property2 value");
         int assertedPeriodCount = 0;
         Assert.assertEquals(reporter.getReportingPeriods().size(), 3);
         for (final BoundPeriod<Destination> period : reporter.getReportingPeriods()) {
            switch (period.getPeriodType()) {
               case TIME:
                  Assert.assertEquals(period.getPeriod(), 2000, "time period's value");
                  assertedPeriodCount++;
                  break;
               case ITERATION:
                  Assert.assertEquals(period.getPeriod(), 100, "iteration period's value");
                  assertedPeriodCount++;
                  break;
               case PERCENTAGE:
                  Assert.assertEquals(period.getPeriod(), 50, "percentage period's value");
                  assertedPeriodCount++;
                  break;
            }
         }
         Assert.assertEquals(assertedPeriodCount, 3, "number of period asserted");

         Reporter warmUpReporter = reportersMap.get(WARM_UP_REPORTER_KEY);
         Assert.assertTrue(warmUpReporter instanceof WarmUpReporter, "reporter's class");
         Assert.assertEquals(warmUpReporter.getDestinations().size(), 0, "reporter's number of destinations");
         Assert.assertEquals(((WarmUpReporter) warmUpReporter).getMinimalWarmUpCount(), 12345, "reporter's minimal warmup count");
         Assert.assertEquals(((WarmUpReporter) warmUpReporter).getMinimalWarmUpDuration(), 15000, "reporter's minimal warmup duration");
         Assert.assertEquals(((WarmUpReporter) warmUpReporter).getAbsoluteThreshold(), 0.2d, "reporter's absolute threshold");
         Assert.assertEquals(((WarmUpReporter) warmUpReporter).getRelativeThreshold(), 1d, "reporter's relative threshold");

      } catch (PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void parseValidationTest() {
      try {
         scenarioFactory.parseValidation();
         // TODO: add assertions on validation

         // validation is optional
         noValidationScenarioFactory.parseValidation();
      } catch (final PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }
}
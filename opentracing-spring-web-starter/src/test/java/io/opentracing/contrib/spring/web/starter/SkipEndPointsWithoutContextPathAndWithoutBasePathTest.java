/**
 * Copyright 2016-2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.web.starter;

import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

/**
 * @author Gilles Robert
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = SkipEndPointsWithoutContextPathAndWithoutBasePathTest.SpringConfiguration.class,
    properties = {
        "management.endpoints.web.exposure.include:*",
        "opentracing.spring.web.client.enabled=false",
        "management.endpoints.web.base-path:/"
    })
@RunWith(SpringJUnit4ClassRunner.class)
public class SkipEndPointsWithoutContextPathAndWithoutBasePathTest {

  private static final String HELLO = "/hello";
  private static final String HEALTHCARE = "/healthcare";
  private static final String HEALTH = "/health";
  private static final String INFO = "/info";
  private static final String AUDIT_EVENTS = "/auditevents";
  private static final String METRICS = "/metrics";

  @Configuration
  @EnableAutoConfiguration
  public static class SpringConfiguration {

    @Bean
    public MockTracer tracer() {
      return new MockTracer();
    }

    @RestController
    public static class Controller {

      @RequestMapping(HEALTH)
      public String health() {
        return "health";
      }

      @RequestMapping(AUDIT_EVENTS)
      public String auditEvents() {
        return "audit events";
      }

      @RequestMapping(HELLO)
      public String hello() {
        return "hello";
      }

      @RequestMapping(HEALTHCARE)
      public String healthCare() {
        return "health care";
      }

      @RequestMapping(INFO)
      public String info() {
        return "info";
      }
    }
  }

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private MockTracer mockTracer;

  @Test
  public void testSkipHealthEndpoint() {
    invokeEndpoint(HEALTH);
    assertNoSpans();
  }

  @Test
  public void testSkipInfoEndpoint() {
    invokeEndpoint(INFO);
    assertNoSpans();
  }

  @Test
  public void testSkipAuditEventsEndpoint() {
    invokeEndpoint(AUDIT_EVENTS);
    assertNoSpans();
  }

  @Test
  public void testSkipMetricsEndpoint() {
    invokeEndpoint(METRICS + "?abc");
    assertNoSpans();
  }

  @Test
  public void testTraceHelloEndpoint() {
    invokeEndpoint(HELLO);
    assertOneSpan();
  }

  @Test
  public void testTraceHealthCareEndpoint() {
    invokeEndpoint(HEALTHCARE);
    assertOneSpan();
  }

  @After
  public void reset() {
    mockTracer.reset();
  }

  private void invokeEndpoint(String endpoint) {
    testRestTemplate.getForEntity(endpoint, String.class);
  }

  private void assertNoSpans() {
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(0, mockSpans.size());
  }

  private void assertOneSpan() {
    await().until(reportedSpansSize(), equalTo(1));
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(1, mockSpans.size());
  }

  private Callable<Integer> reportedSpansSize() {
    return () -> mockTracer.finishedSpans().size();
  }

}

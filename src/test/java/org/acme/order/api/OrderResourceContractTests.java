package org.acme.order.api;

import static org.junit.jupiter.api.Assertions.*;

import org.acme.order.BaseTest;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import io.github.microcks.testcontainers.MicrocksContainer;
import io.github.microcks.testcontainers.model.TestRequest;
import io.github.microcks.testcontainers.model.TestResult;
import io.github.microcks.testcontainers.model.TestRunnerType;

@QuarkusTest
class OrderResourceContractTests extends BaseTest {

   @Test
   void testOpenAPIContract() throws Exception {
      // Ask for an Open API conformance to be launched.
      TestRequest testRequest = new TestRequest.Builder()
            .serviceId("Order Service API:0.1.0")
            .runnerType(TestRunnerType.OPEN_API_SCHEMA.name())
            .testEndpoint("http://host.testcontainers.internal:" + quarkusHttpPort + "/api")
            .build();

      TestResult testResult = MicrocksContainer.testEndpoint(microcksContainerUrl, testRequest);

      // You may inspect complete response object with following:
      //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(testResult));

      assertTrue(testResult.isSuccess());
      assertEquals(1, testResult.getTestCaseResults().size());
   }
}

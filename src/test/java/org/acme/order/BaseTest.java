package org.acme.order;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BaseTest {

   @ConfigProperty(name= "quarkus.http.test-port")
   protected int quarkusHttpPort;

   @ConfigProperty(name= "kafka.bootstrap.servers")
   protected String kafkaBootstrapServers;

   @ConfigProperty(name= "quarkus.microcks.default.http")
   protected String microcksContainerUrl;
}

package io.github.tonisoler.idaxshell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
    "io.github.tonisoler.idaxshell",
    "es.idynamicsax.idax.config",
    "es.idynamicsax.idax.security",
    "es.idynamicsax.idax.tenant",
    "es.idynamicsax.idax.repository",
    "es.idynamicsax.idax.service.auth",
    "es.idynamicsax.idax.service.audit",
    "es.idynamicsax.idax.service.mail",
    "es.idynamicsax.idax.service.me",
    "es.idynamicsax.idax.service.messaging",
    "es.idynamicsax.idax.service.permission",
    "es.idynamicsax.idax.service.security",
    "es.idynamicsax.idax.service.tenant",
    "es.idynamicsax.idax.alerts"
})
@ConfigurationPropertiesScan(basePackages = {"io.github.tonisoler.idaxshell", "es.idynamicsax.idax"})
@EntityScan("es.idynamicsax.idax")
@EnableJpaRepositories("es.idynamicsax.idax")
public class IdaxShellApplication {
  public static void main(String[] args) { SpringApplication.run(IdaxShellApplication.class, args); }
}

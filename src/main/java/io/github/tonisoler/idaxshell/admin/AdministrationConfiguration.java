package io.github.tonisoler.idaxshell.admin;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AdministrationConfiguration {
  @Bean
  @ConditionalOnMissingBean(CoreAdministrationPort.class)
  CoreAdministrationPort unavailableCoreAdministrationPort() {
    return new UnavailableCoreAdministrationAdapter();
  }
}

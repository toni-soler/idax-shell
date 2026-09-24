package io.github.tonisoler.idaxshell.admin;

import es.idynamicsax.idax.repository.admin.TenantCreateRepository;
import es.idynamicsax.idax.service.admin.TenantOnboardingService;
import es.idynamicsax.idax.service.auth.LocalIdentitySubjectPolicy;
import es.idynamicsax.idax.tenant.AppUserResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

// TenantOnboardingService ships in idax-core but, unlike its sibling admin services, isn't itself
// a registered Spring bean (no class-level @Component/@Service) - every other consumer is expected
// to wire it up explicitly, the same way idax-ledger's OstrisLedgerDeliveryConfig manually
// constructs ServiceTokenProvider. All five constructor dependencies are already real beans in
// this app (JdbcTemplate via Spring Boot autoconfig, the rest confirmed by SecurityConfiguration
// already receiving AppUserResolver/LocalIdentitySubjectPolicy as plain @Bean method parameters,
// and PasswordConfig providing PasswordEncoder), so this is pure wiring, not new behavior.
@Configuration
public class TenantAdminConfiguration {
  @Bean
  TenantOnboardingService tenantOnboardingService(JdbcTemplate jdbcTemplate, AppUserResolver appUserResolver,
      PasswordEncoder passwordEncoder, TenantCreateRepository tenantCreateRepository, LocalIdentitySubjectPolicy subjectPolicy) {
    return new TenantOnboardingService(jdbcTemplate, appUserResolver, passwordEncoder, tenantCreateRepository, subjectPolicy);
  }
}

package io.github.tonisoler.idaxshell.config;

import es.idynamicsax.idax.repository.auth.AuthLocalIdentityLookupRepository;
import es.idynamicsax.idax.security.JwtAuthFilter;
import es.idynamicsax.idax.security.PermissionAuthorizationFilter;
import es.idynamicsax.idax.service.auth.LocalIdentitySubjectPolicy;
import es.idynamicsax.idax.tenant.AppUserResolver;
import es.idynamicsax.idax.tenant.TenantContextFilter;
import es.idynamicsax.idax.tenant.TenantResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
  @Bean SecurityFilterChain security(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
      TenantContextFilter tenantContextFilter, PermissionAuthorizationFilter permissionFilter) throws Exception {
    return http.csrf(csrf -> csrf.disable())
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a -> a
        .requestMatchers("/", "/index.html", "/favicon.ico", "/assets/**", "/generated/**",
          "/logo-*.svg", "/actuator/health/**", "/api/shell/v1/bootstrap/status",
          "/api/shell/v1/auth/**", "/ledger", "/ledger/**", "/ostris", "/ostris/**",
          "/extensions/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
        .anyRequest().authenticated())
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
      .addFilterAfter(tenantContextFilter, JwtAuthFilter.class)
      .addFilterAfter(permissionFilter, TenantContextFilter.class)
      .build();
  }

  @Bean TenantContextFilter tenantContextFilter(TenantResolver tenantResolver,
      AppUserResolver appUserResolver, AuthLocalIdentityLookupRepository identityLookup,
      LocalIdentitySubjectPolicy subjectPolicy) {
    return new TenantContextFilter(tenantResolver, appUserResolver, identityLookup, subjectPolicy);
  }
}

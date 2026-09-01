package io.github.tonisoler.idaxshell.config;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
  @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a -> a
        .requestMatchers("/actuator/health/**", "/api/shell/v1/bootstrap/status").permitAll()
        .anyRequest().authenticated())
      .oauth2ResourceServer(o -> o.jwt(jwt -> {})).build();
  }

  @Bean JwtEncoder jwtEncoder(ShellProperties properties) throws Exception {
    try (InputStream in = properties.jwt().privateKey().getInputStream(); InputStream publicIn = properties.jwt().publicKey().getInputStream()) {
      String pem = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.US_ASCII)
        .replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
      RSAPrivateKey key = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
      String publicPem = new String(publicIn.readAllBytes(), java.nio.charset.StandardCharsets.US_ASCII)
        .replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
      RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicPem)));
      RSAKey jwk = new RSAKey.Builder(publicKey).privateKey(key).keyID("idax-shell").build();
      return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }
  }
}

package io.github.tonisoler.idaxshell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IdaxShellApplication {
  public static void main(String[] args) { SpringApplication.run(IdaxShellApplication.class, args); }
}


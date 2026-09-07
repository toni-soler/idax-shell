package io.github.tonisoler.idaxshell.admin;

public class CoreCapabilityUnavailableException extends RuntimeException {
  public CoreCapabilityUnavailableException() {
    super("A newer compatible IDAX Core Runtime is required for this capability");
  }
}

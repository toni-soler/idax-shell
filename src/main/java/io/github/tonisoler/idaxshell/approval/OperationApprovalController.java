package io.github.tonisoler.idaxshell.approval;

import es.idynamicsax.idax.security.CurrentUser;
import es.idynamicsax.idax.security.approval.OperationApprovalTokenIssuer;
import es.idynamicsax.idax.service.auth.OperationApprovalService;
import es.idynamicsax.idax.service.security.AuthRateLimiterService;
import io.github.tonisoler.idaxshell.auth.LocalAuthenticationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Per-operation step-up for the signed-in user: confirm ONE specific operation with a fresh
 * authenticator code and receive a short-lived approval for exactly that operation. The approval is
 * useless on its own - the service that executes the operation verifies it against what it is
 * about to do and consumes it once. Authenticated only (never under the public /auth/** paths)
 * and scoped to one workspace by its path: the approval is bound to that workspace, and only a
 * member of it can obtain one (the platform filter resolves a path tenant but does not check
 * membership, so this endpoint does);
 * there is no way to obtain an approval for another user, with a recovery code, or without active
 * two-step verification. All rules live in Core's OperationApprovalService.
 */
@RestController
@RequestMapping("/api/shell/v1")
@ConditionalOnProperty(prefix = "idax.shell.local-demo", name = "enabled", havingValue = "true")
public class OperationApprovalController {
  /** Known, non-secret outcomes. A wrong code is 403, not 401: the session itself is still valid. */
  static final Map<String, HttpStatus> OUTCOMES = Map.of(
      "INVALID_TOTP", HttpStatus.FORBIDDEN,
      "MFA_NOT_ENABLED", HttpStatus.CONFLICT,
      "OPERATION_APPROVAL_USER_REQUIRED", HttpStatus.FORBIDDEN,
      "OPERATION_APPROVAL_TENANT_FORBIDDEN", HttpStatus.FORBIDDEN,
      "INVALID_OPERATION_APPROVAL_REQUEST", HttpStatus.BAD_REQUEST);

  private final OperationApprovalService approvals;
  private final LocalAuthenticationService authentication;
  public OperationApprovalController(OperationApprovalService approvals, LocalAuthenticationService authentication) {
    this.approvals = approvals; this.authentication = authentication;
  }

  @PostMapping("/tenants/{tenantId}/operation-approvals")
  public Approval approve(@PathVariable UUID tenantId, @AuthenticationPrincipal CurrentUser currentUser,
                          @Valid @RequestBody ApprovalRequest request) {
    if (currentUser == null || currentUser.isService() || !authentication.isActiveMember(currentUser.getUserId(), tenantId)) {
      throw new IllegalArgumentException("OPERATION_APPROVAL_TENANT_FORBIDDEN");
    }
    // The approval is for the workspace in the path (the one the operation will run in), which
    // may differ from the workspace the session token was issued for.
    CurrentUser inWorkspace = new CurrentUser(currentUser.getUserId(), currentUser.getUsername(), tenantId,
        currentUser.isSuperuser(), currentUser.getRoles(), currentUser.isLocallyAuthenticated());
    OperationApprovalTokenIssuer.Issued issued = approvals.approve(
        inWorkspace, request.action(), request.resource(), request.intentDigest(), request.code());
    return new Approval(issued.token(), issued.expiresAt());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String, String>> refused(IllegalArgumentException exception) {
    String code = exception.getMessage();
    HttpStatus status = code == null ? null : OUTCOMES.get(code);
    if (status == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("code", "OPERATION_APPROVAL_FAILED", "message", "Operation approval failed"));
    }
    return ResponseEntity.status(status).body(Map.of("code", code, "message", code));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("code", "INVALID_OPERATION_APPROVAL_REQUEST", "message", "INVALID_OPERATION_APPROVAL_REQUEST"));
  }

  @ExceptionHandler(AuthRateLimiterService.RateLimitedException.class)
  ResponseEntity<Map<String, String>> rateLimited(AuthRateLimiterService.RateLimitedException exception) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("code", "RATE_LIMITED",
        "message", "Too many attempts", "lockedUntil", exception.lockedUntil.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
  }

  public record ApprovalRequest(
      @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_.:-]{2,63}$") String action,
      @NotBlank @Size(max = 200) String resource,
      @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") String intentDigest,
      @NotBlank @Size(max = 32) String code) {
    @Override public String toString() { return "ApprovalRequest[action=" + action + ", resource=" + resource + ", code=<redacted>]"; }
  }
  public record Approval(String approvalToken, Instant expiresAt) {
    @Override public String toString() { return "Approval[approvalToken=<redacted>, expiresAt=" + expiresAt + "]"; }
  }
}

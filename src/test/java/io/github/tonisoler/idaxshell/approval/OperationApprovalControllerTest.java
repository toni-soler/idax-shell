package io.github.tonisoler.idaxshell.approval;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import es.idynamicsax.idax.security.CurrentUser;
import es.idynamicsax.idax.security.approval.OperationApprovalTokenIssuer;
import es.idynamicsax.idax.service.auth.OperationApprovalService;
import es.idynamicsax.idax.service.security.AuthRateLimiterService;
import io.github.tonisoler.idaxshell.auth.LocalAuthenticationService;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class OperationApprovalControllerTest {
  private static final String DIGEST = "a".repeat(64);
  private final OperationApprovalService approvals = mock(OperationApprovalService.class);
  private final LocalAuthenticationService authentication = mock(LocalAuthenticationService.class);
  private final OperationApprovalController controller = new OperationApprovalController(approvals, authentication);
  private final UUID workspace = UUID.randomUUID();
  private final CurrentUser user = new CurrentUser(UUID.randomUUID(), "local:ana", UUID.randomUUID(), false, Set.of());

  @Test void theApprovalIsRequestedForTheSignedInUserAndTheExactOperation() {
    Instant expiresAt = Instant.now().plusSeconds(120);
    when(authentication.isActiveMember(user.getUserId(), workspace)).thenReturn(true);
    ArgumentCaptor<CurrentUser> principal = ArgumentCaptor.forClass(CurrentUser.class);
    when(approvals.approve(principal.capture(), eq("OSTRIS_CREDENTIAL_ADD"), eq("controller"), eq(DIGEST), eq("123456")))
        .thenReturn(new OperationApprovalTokenIssuer.Issued("token", UUID.randomUUID(), expiresAt));
    var approval = controller.approve(workspace, user,
        new OperationApprovalController.ApprovalRequest("OSTRIS_CREDENTIAL_ADD", "controller", DIGEST, "123456"));
    assertEquals("token", approval.approvalToken());
    assertEquals(expiresAt, approval.expiresAt());
    // bound to the signed-in user and to the workspace of the path, not the token's own workspace
    assertEquals(user.getUserId(), principal.getValue().getUserId());
    assertEquals(workspace, principal.getValue().getTenantId());
  }

  @Test void aNonMemberOfTheWorkspaceGetsNoApprovalAndNoCodeIsEvenChecked() {
    when(authentication.isActiveMember(user.getUserId(), workspace)).thenReturn(false);
    var refused = assertThrows(IllegalArgumentException.class, () -> controller.approve(workspace, user,
        new OperationApprovalController.ApprovalRequest("OSTRIS_CREDENTIAL_ADD", "controller", DIGEST, "123456")));
    assertEquals(HttpStatus.FORBIDDEN, controller.refused(refused).getStatusCode());
    verifyNoInteractions(approvals);
  }

  @Test void aWrongCodeIsForbiddenNotUnauthorizedSoTheSessionIsNotMistakenForExpired() {
    var response = controller.refused(new IllegalArgumentException("INVALID_TOTP"));
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals("INVALID_TOTP", response.getBody().get("code"));
  }

  @Test void missingTwoStepVerificationIsAConflictTheClientCanExplain() {
    var response = controller.refused(new IllegalArgumentException("MFA_NOT_ENABLED"));
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("MFA_NOT_ENABLED", response.getBody().get("code"));
  }

  @Test void unknownFailuresStayOpaque() {
    var response = controller.refused(new IllegalArgumentException("some internal detail"));
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("OPERATION_APPROVAL_FAILED", response.getBody().get("code"));
    assertFalse(response.getBody().toString().contains("internal detail"));
  }

  @Test void lockoutIsReportedWithItsDeadline() {
    var lockedUntil = OffsetDateTime.now().plusMinutes(5);
    var response = controller.rateLimited(new AuthRateLimiterService.RateLimitedException(lockedUntil));
    assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    assertEquals("RATE_LIMITED", response.getBody().get("code"));
  }

  @Test void neitherTheCodeNorTheTokenAppearInStringForms() {
    assertFalse(new OperationApprovalController.ApprovalRequest("OSTRIS_CREDENTIAL_ADD", "controller", DIGEST, "123456").toString().contains("123456"));
    assertFalse(new OperationApprovalController.Approval("secret-token", Instant.now()).toString().contains("secret-token"));
  }
}

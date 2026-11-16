const baseUrl = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");

async function request(path, options = {}) {
  // Every /auth/ step (password, second factor, forced setup) happens before a session exists and
  // must never carry a previous bearer token.
  const isLogin = path.startsWith("/api/shell/v1/auth/");
  const token = isLogin ? null : sessionStorage.getItem("idax.accessToken");
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
  });
  if (!response.ok) {
    const contentType = response.headers.get("content-type") || "";
    const details = contentType.includes("application/json")
      ? await response.json().catch(() => null)
      : null;
    const error = new Error(details?.message || `HTTP ${response.status}`);
    error.status = response.status;
    error.code = details?.code;
    error.lockedUntil = details?.lockedUntil;
    throw error;
  }
  return response.status === 204 ? null : response.json();
}

/** A session exists only once every required step is complete; only then is the token stored. */
function keepSession(session) {
  if (!session?.accessToken) throw new Error("Sign-in did not complete");
  sessionStorage.setItem("idax.accessToken", session.accessToken);
  return session;
}

export const shellApi = {
  session: () => request("/api/shell/v1/session"),
  login: async (credentials) => {
    sessionStorage.removeItem("idax.accessToken");
    const outcome = await request("/api/shell/v1/auth/login", { method: "POST", body: JSON.stringify(credentials) });
    // The password step may not be the last one. The challenge and setup tokens grant nothing but
    // the next step; they are returned to the caller's memory only, never stored.
    if (outcome.status === "MFA_REQUIRED") return { step: "MFA", challengeToken: outcome.challengeToken };
    if (outcome.status === "MFA_SETUP_REQUIRED") return { step: "MFA_SETUP", setupToken: outcome.setupToken };
    return { step: "SESSION", session: keepSession(outcome) };
  },
  verifyMfa: async (challengeToken, code) => keepSession(
    await request("/api/shell/v1/auth/mfa/verify", { method: "POST", body: JSON.stringify({ challengeToken, code }) })),
  startMfaSetup: (setupToken) => request("/api/shell/v1/auth/mfa/setup-required/start", { method: "POST", body: JSON.stringify({ setupToken }) }),
  // The session that comes with the recovery codes is NOT stored here: the caller stores it with
  // enterSession() only once the user has confirmed the codes are saved.
  activateMfaSetup: (setupToken, code) => request("/api/shell/v1/auth/mfa/setup-required/activate", { method: "POST", body: JSON.stringify({ setupToken, code }) }),
  enterSession: (session) => keepSession(session),
  logout: () => { sessionStorage.removeItem("idax.accessToken"); return Promise.resolve(); },
  tenants: () => request("/api/shell/v1/tenants"),
  modules: async () => (await request("/api/shell/v1/extensions")).extensions,
  users: (tenantId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/users`),
  createUser: (tenantId, body) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/users`, { method: "POST", body: JSON.stringify(body) }),
  updateUser: (tenantId, userId, body) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/users/${encodeURIComponent(userId)}`, { method: "PUT", body: JSON.stringify(body) }),
  deleteUser: (tenantId, userId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/users/${encodeURIComponent(userId)}`, { method: "DELETE" }),
  roles: (tenantId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/roles`),
  createRole: (tenantId, body) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/roles`, { method: "POST", body: JSON.stringify(body) }),
  updateRole: (tenantId, roleId, body) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/roles/${encodeURIComponent(roleId)}`, { method: "PUT", body: JSON.stringify(body) }),
  deleteRole: (tenantId, roleId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/roles/${encodeURIComponent(roleId)}`, { method: "DELETE" }),
  permissionCatalog: (tenantId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/roles/catalog`),
  rolePermissions: (tenantId, roleId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/roles/${encodeURIComponent(roleId)}/permissions`),
  saveRolePermissions: (tenantId, roleId, permissions) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/roles/${encodeURIComponent(roleId)}/permissions`, { method: "PUT", body: JSON.stringify(permissions) }),
  roleUsers: (tenantId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/roles/users`),
  saveUserRoles: (tenantId, userId, roleIds) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/roles/users/${encodeURIComponent(userId)}`, { method: "PUT", body: JSON.stringify({ roleIds }) }),
  alerts: (tenantId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/alerts`),
  createAlert: (tenantId, body) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/alerts`, { method: "POST", body: JSON.stringify(body) }),
  updateAlert: (tenantId, alertId, body) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/alerts/${encodeURIComponent(alertId)}`, { method: "PUT", body: JSON.stringify(body) }),
  deleteAlert: (tenantId, alertId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/alerts/${encodeURIComponent(alertId)}`, { method: "DELETE" }),
  runAlert: (tenantId, alertId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/alerts/${encodeURIComponent(alertId)}/run-now`, { method: "POST" }),
  alertDefinitions: (tenantId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/core-alerts`),
  savedFilters: (tenantId, resource) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/saved-filters/${encodeURIComponent(resource)}`),
  // Platform-level, not scoped to any single tenantId - creating a tenant needs no existing
  // tenant context, and only a platform superuser may call these (see TenantAdminController).
  platformTenants: () => request("/api/shell/v1/platform/tenants"),
  createPlatformTenant: (body) => request("/api/shell/v1/platform/tenants", { method: "POST", body: JSON.stringify(body) }),
  setTenantEnabled: (tenantId, enabled) => request(`/api/shell/v1/platform/tenants/${encodeURIComponent(tenantId)}/enabled`, { method: "PUT", body: JSON.stringify({ enabled }) }),
  updateTenantName: (tenantId, name) => request(`/api/shell/v1/platform/tenants/${encodeURIComponent(tenantId)}/name`, { method: "PUT", body: JSON.stringify({ name }) }),
  saveFilters: (tenantId, resource, body) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/saved-filters/${encodeURIComponent(resource)}`, { method: "PUT", body: JSON.stringify(body) }),
};

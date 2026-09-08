const baseUrl = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");

async function request(path, options = {}) {
  const isLogin = path === "/api/shell/v1/auth/login";
  const token = isLogin ? null : sessionStorage.getItem("idax.accessToken");
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
  });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.status === 204 ? null : response.json();
}

export const shellApi = {
  session: () => request("/api/shell/v1/session"),
  login: async (credentials) => {
    sessionStorage.removeItem("idax.accessToken");
    const session = await request("/api/shell/v1/auth/login", { method: "POST", body: JSON.stringify(credentials) });
    if (!session.accessToken) throw new Error(session.status || "Additional authentication is required");
    sessionStorage.setItem("idax.accessToken", session.accessToken);
    return session;
  },
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
  saveFilters: (tenantId, resource, body) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/saved-filters/${encodeURIComponent(resource)}`, { method: "PUT", body: JSON.stringify(body) }),
};

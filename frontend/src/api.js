const baseUrl = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");

async function request(path, options = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    credentials: "include",
    headers: { "Content-Type": "application/json", ...options.headers },
  });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.status === 204 ? null : response.json();
}

export const shellApi = {
  session: () => request("/api/shell/v1/session"),
  login: (credentials) => request("/api/shell/v1/auth/login", { method: "POST", body: JSON.stringify(credentials) }),
  logout: () => request("/api/shell/v1/auth/logout", { method: "POST" }),
  tenants: () => request("/api/shell/v1/tenants"),
  modules: () => request("/api/shell/v1/extensions"),
  users: (tenantId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/users`),
  roles: (tenantId) => request(`/api/shell/v1/tenants/${encodeURIComponent(tenantId)}/roles`),
};

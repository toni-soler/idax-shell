const en = {
  "nav.alerts": "Alerts", "nav.resize": "Resize navigation", "nav.collapse": "Collapse navigation", "nav.expand": "Expand navigation", "alerts.title": "Alerts", "alerts.subtitle": "Turn saved filters into monitored conditions.",
  "users.subtitle": "Manage identities and workspace membership.", "roles.subtitle": "Group permissions without duplicating the Core catalog.",
  "crud.users": "Users", "crud.roles": "Roles", "crud.alerts": "Alerts", "crud.create": "Create", "crud.edit": "Edit", "crud.delete": "Delete", "crud.cancel": "Cancel", "crud.save": "Save", "crud.actions": "Actions", "crud.loading": "Loading…", "crud.empty": "No matching records.", "crud.confirmDelete": "Delete this record?",
  "filters.placeholder": "Filter value", "filters.save": "Save filter", "filters.alert": "Create alert", "filters.name": "Filter name", "filters.add": "Add condition", "filters.remove": "Remove condition", "filters.clear": "Clear",
  "admin.coreRequired": "The secured Core operation could not be completed.",
  "field.displayName": "Display name", "field.email": "Email", "field.role": "Membership role (label only)", "field.password": "Password", "field.enabled": "Enabled", "field.name": "Name", "field.key": "Key", "field.description": "Description", "field.permissionCount": "Permissions", "field.permissions": "Permissions", "field.resource": "Resource", "field.field": "Field", "field.value": "Value", "field.schedule": "Cron schedule", "field.status": "Status", "field.entityKey": "Entity", "field.filters": "Filter JSON", "field.cron": "Cron schedule", "field.channelEmail": "Email channel", "field.channelMessage": "Message channel",
  "field.roles": "Assigned roles", "field.rolesHint": "These roles actually grant this user's permissions. Membership role above is a display label only.",
  "field.permissionsSelected": "selected", "field.permissionsLoadError": "The permission catalog could not be loaded. Saving is disabled until it loads correctly.",
  "tenants.subtitle": "Create and manage isolated workspaces - each one has its own separate data and, for STIR, its own separate economic community.",
  "tenants.name": "Workspace name", "tenants.nameHint": "Shown in the workspace selector, e.g. \"STIR\" or \"STIR - test\".",
  "tenants.code": "Workspace code", "tenants.codeHint": "A short internal identifier, cannot be changed later.",
  "tenants.adminDisplayName": "Administrator display name", "tenants.adminEmail": "Administrator email",
  "tenants.adminPassword": "Administrator password", "tenants.adminPasswordHint": "The first administrator account for this new workspace. Share these credentials securely.",
  "tenants.enable": "Enable", "tenants.disable": "Disable",
  "state.enabled": "Enabled", "state.disabled": "Disabled"
};

const es = {
  ...en,
  "nav.alerts": "Alertas", "nav.resize": "Redimensionar navegación", "nav.collapse": "Contraer navegación", "nav.expand": "Expandir navegación", "alerts.title": "Alertas", "alerts.subtitle": "Convierte filtros guardados en condiciones monitorizadas.",
  "users.subtitle": "Gestiona identidades y pertenencia al espacio.", "roles.subtitle": "Agrupa permisos sin duplicar el catálogo de Core.",
  "crud.users": "Usuarios", "crud.roles": "Roles", "crud.alerts": "Alertas", "crud.create": "Crear", "crud.edit": "Editar", "crud.delete": "Eliminar", "crud.cancel": "Cancelar", "crud.save": "Guardar", "crud.actions": "Acciones", "crud.loading": "Cargando…", "crud.empty": "No hay registros coincidentes.", "crud.confirmDelete": "¿Eliminar este registro?",
  "filters.placeholder": "Valor del filtro", "filters.save": "Guardar filtro", "filters.alert": "Crear alerta", "filters.name": "Nombre del filtro", "filters.add": "Añadir condición", "filters.remove": "Eliminar condición", "filters.clear": "Limpiar",
  "admin.coreRequired": "No se pudo completar la operación protegida de Core.",
  "field.displayName": "Nombre visible", "field.email": "Correo electrónico", "field.role": "Rol de pertenencia (solo etiqueta)", "field.password": "Contraseña", "field.enabled": "Activo", "field.name": "Nombre", "field.key": "Clave", "field.description": "Descripción", "field.permissionCount": "Permisos", "field.permissions": "Permisos", "field.resource": "Recurso", "field.field": "Campo", "field.value": "Valor", "field.schedule": "Programación cron", "field.status": "Estado", "field.entityKey": "Entidad", "field.filters": "Filtro JSON", "field.cron": "Programación cron", "field.channelEmail": "Canal de correo", "field.channelMessage": "Canal de mensajes",
  "field.roles": "Roles asignados", "field.rolesHint": "Estos roles son los que realmente conceden los permisos de este usuario. El «Rol de pertenencia» de arriba es solo una etiqueta visual.",
  "field.permissionsSelected": "seleccionados", "field.permissionsLoadError": "No se pudo cargar el catálogo de permisos. El guardado está deshabilitado hasta que cargue correctamente.",
  "tenants.subtitle": "Crea y gestiona espacios de trabajo aislados: cada uno tiene sus propios datos y, en el caso de STIR, su propia comunidad económica separada.",
  "tenants.name": "Nombre del espacio", "tenants.nameHint": "Se muestra en el selector de espacio, por ejemplo «STIR» o «STIR - pruebas».",
  "tenants.code": "Código del espacio", "tenants.codeHint": "Un identificador interno breve; no se puede cambiar después.",
  "tenants.adminDisplayName": "Nombre visible del administrador", "tenants.adminEmail": "Correo del administrador",
  "tenants.adminPassword": "Contraseña del administrador", "tenants.adminPasswordHint": "La primera cuenta administradora de este espacio nuevo. Comparte estas credenciales de forma segura.",
  "tenants.enable": "Activar", "tenants.disable": "Desactivar",
  "state.enabled": "Activo", "state.disabled": "Inactivo"
};

export const adminLocales = { es, en, ca: { ...en }, de: { ...en }, fr: { ...en }, it: { ...en }, pt: { ...en }, jp: { ...en }, eu: { ...en }, gl: { ...en }, "pt-BR": { ...en }, zh: { ...en } };

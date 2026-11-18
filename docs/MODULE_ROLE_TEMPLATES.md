# Module role templates

A module usually needs the same set of permissions for everyone who uses it ("participant",
"reviewer", "moderator"). Without help, every workspace administrator had to build those roles by
hand, and add each permission a new module version introduced. Role templates move that into the
extension manifest: the module says which roles it needs, the Shell keeps them in every workspace,
and new members join with the default ones.

## Declaring templates

In the extension manifest (`idax.shell.extension-manifest`, schema version 1), an extension may
list `roleTemplates`:

```json
{"id": "garden", "name": "Garden", "route": "/garden",
 "roleTemplates": [
   {"key": "member", "name": "Garden member", "description": "Uses the garden.",
    "permissions": ["garden.beds.read", "garden.beds.water"], "defaultForNewMembers": true},
   {"key": "steward", "name": "Garden steward", "permissions": ["garden.beds.manage"]}
 ]}
```

- `key`: lower-case words joined by `-`, unique within the extension.
- `name` (required) and `description`: what administrators see. They may rename the role later.
- `permissions`: Core permission codes. A code that is not in the Core catalog yet is not an error
  (see below).
- `defaultForNewMembers` (default `false`): every new member of a workspace gets this role.

A malformed template stops the Shell from starting, like a malformed route: a wrong template would
grant the wrong permissions in every workspace.

## What the Shell does with them

`RoleTemplateService` keeps one Core role per template in each workspace. The role is identified by
its source (`sourceType` `EXTENSION_TEMPLATE`, `sourceKey` `extensionId:key`), never by its name or
key.

| When | What happens |
| --- | --- |
| A workspace is created (`POST /api/shell/v1/platform/tenants`) | Its template roles are created. Its first administrator gets the default ones. |
| A member is created (`POST /api/shell/v1/tenants/{tenantId}/users`) | Template roles are brought up to date. The member gets the default ones, in the same transaction, on top of any role they already have. |
| The Shell starts, then every `idax.shell.role-templates.reconcile-interval` (default `PT15M`; zero disables it) | Every workspace's template roles are brought up to date. Nobody is given a role. |
| An administrator applies the templates (`POST /api/shell/v1/tenants/{tenantId}/roles/templates/apply`, `system.roles.manage`, body `{"assignToExistingMembers": true}`) | Template roles are brought up to date. With `assignToExistingMembers`, every current member who lacks a default role gets it. |

"Up to date" means:

- a missing template role is created, enabled, with the template's permissions;
- an existing one only ever **gains** permissions: what a newer module version adds is added, what
  an administrator added stays, nothing is removed;
- a permission the Core catalog does not know yet (its module has not registered it) is left out,
  reported in `unavailablePermissions`, and added on a later reconciliation.

## What administrators control

- **Rename** a template role, or change its description: the link to the template is kept. The
  Shell keeps a template role's source on every update whatever the client sends, and refuses
  any other role that claims the `EXTENSION_TEMPLATE` source.
- **Disable** it: a disabled template role is never given to anyone, including new members.
- **Delete** it: refused (`409`). It would be recreated on the next reconciliation. Disable it
  instead.
- **Untick** a default role when creating a member: the creation form shows the default roles
  ticked; whatever the administrator leaves ticked is what the member gets.

`GET /api/shell/v1/tenants/{tenantId}/roles/templates` (`system.roles.read`) shows each template as
it stands in the workspace: its role, whether that role is enabled, and any unavailable permission.
The Roles screen shows which roles come from a module template, and offers to give the module
roles to the current members.

## Boundaries

Templates only ever use the Core's public `RolePermissionService`, in the workspace's own tenant
context, and never write the Core's tables directly. They grant platform permissions. A module
whose decisions belong to its community (for example STIR's governance) must still check that
authority itself: being given a role is never the same as being chosen by the community.

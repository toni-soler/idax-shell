CREATE SCHEMA IF NOT EXISTS idax_shell;
CREATE TABLE IF NOT EXISTS idax_shell.bootstrap_state (
  singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
  completed_at timestamptz NOT NULL,
  tenant_id uuid NOT NULL,
  user_id uuid NOT NULL
);


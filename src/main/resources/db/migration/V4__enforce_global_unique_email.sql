-- Enforce globally unique email addresses.
-- Previously email was only unique per workspace (workspace_id, email).
-- The auth system uses global email lookup, so email must be globally unique.

-- Drop the old workspace-scoped unique index (now redundant)
DROP INDEX IF EXISTS idx_users_workspace_email;

-- Add global unique index on email for active (non-deleted) users
CREATE UNIQUE INDEX idx_users_email_active ON users(email) WHERE is_deleted = FALSE;

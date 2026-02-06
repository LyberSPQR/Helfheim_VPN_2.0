--liquibase formatted sql
--changeset admin:index-users-is-active
CREATE INDEX idx_users_is_active ON users(is_active)
WHERE is_active = TRUE;

--changeset admin:index-users-subscription-expires-at
CREATE INDEX idx_users_subscription_expires_at ON users(subscription_expires_at)
WHERE subscription_expires_at IS NOT NULL;

--changeset admin:index-ip-pool-is_assigned
CREATE INDEX ip_pool_index ON ip_pool(is_assigned)
    WHERE is_assigned = FALSE;
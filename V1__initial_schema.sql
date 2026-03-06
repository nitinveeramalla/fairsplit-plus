-- V1__initial_schema.sql
-- FairSplit+ initial database schema
-- Flyway manages all schema changes — never use ddl-auto: create/update

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    avatar_url      VARCHAR(500),
    password_hash   VARCHAR(255),           -- NULL for OAuth-only users
    provider        VARCHAR(20) DEFAULT 'LOCAL',  -- LOCAL, GOOGLE
    provider_id     VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- ============================================================
-- FRIENDSHIPS  (bidirectional — always store with requester_id < addressee_id)
-- ============================================================
CREATE TABLE friendships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, ACCEPTED, BLOCKED
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (requester_id, addressee_id),
    CHECK (requester_id <> addressee_id)
);

-- ============================================================
-- GROUPS
-- ============================================================
CREATE TABLE groups (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    type            VARCHAR(20) NOT NULL DEFAULT 'OTHER',  -- TRIP, HOME, COUPLE, OTHER
    avatar_url      VARCHAR(500),
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_archived     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE group_members (
    group_id        UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER',  -- ADMIN, MEMBER
    joined_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (group_id, user_id)
);

-- ============================================================
-- EXPENSES
-- ============================================================
CREATE TABLE expenses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id        UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    paid_by         UUID NOT NULL REFERENCES users(id),
    description     VARCHAR(500) NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency        CHAR(3) NOT NULL DEFAULT 'USD',
    amount_usd      NUMERIC(12, 2),          -- normalized for balance calculations
    category        VARCHAR(50) DEFAULT 'OTHER',
    split_type      VARCHAR(20) NOT NULL DEFAULT 'EQUAL',  -- EQUAL, PERCENTAGE, EXACT, SHARES
    receipt_url     VARCHAR(500),
    notes           TEXT,
    expense_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_expenses_group_id ON expenses(group_id);
CREATE INDEX idx_expenses_paid_by ON expenses(paid_by);
CREATE INDEX idx_expenses_expense_date ON expenses(expense_date DESC);

-- ============================================================
-- EXPENSE SPLITS  (how each expense is divided)
-- ============================================================
CREATE TABLE expense_splits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id      UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id),
    owed_amount     NUMERIC(12, 2) NOT NULL CHECK (owed_amount >= 0),
    UNIQUE (expense_id, user_id)
);

CREATE INDEX idx_expense_splits_user_id ON expense_splits(user_id);

-- ============================================================
-- SETTLEMENTS  (recording a payment between two users in a group)
-- ============================================================
CREATE TABLE settlements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id        UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    paid_by         UUID NOT NULL REFERENCES users(id),
    paid_to         UUID NOT NULL REFERENCES users(id),
    amount          NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency        CHAR(3) NOT NULL DEFAULT 'USD',
    payment_method  VARCHAR(50) DEFAULT 'OTHER',  -- CASH, VENMO, ZELLE, BANK, OTHER
    note            VARCHAR(255),
    settled_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by      UUID NOT NULL REFERENCES users(id),
    CHECK (paid_by <> paid_to)
);

CREATE INDEX idx_settlements_group_id ON settlements(group_id);

-- ============================================================
-- EXPENSE AUDIT LOG  (who changed what — required for disputes)
-- ============================================================
CREATE TABLE expense_audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id      UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    changed_by      UUID NOT NULL REFERENCES users(id),
    action          VARCHAR(20) NOT NULL,  -- CREATED, UPDATED, DELETED
    old_snapshot    JSONB,
    new_snapshot    JSONB,
    changed_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

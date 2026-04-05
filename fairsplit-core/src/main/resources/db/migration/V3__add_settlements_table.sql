CREATE TABLE settlements (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id    UUID        NOT NULL REFERENCES groups(id)  ON DELETE CASCADE,
    paid_by     UUID        NOT NULL REFERENCES users(id),
    paid_to     UUID        NOT NULL REFERENCES users(id),
    amount      NUMERIC(12, 2) NOT NULL,
    note        TEXT,
    settled_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_settlements_group_id ON settlements(group_id);
CREATE INDEX idx_settlements_paid_by  ON settlements(paid_by);
CREATE INDEX idx_settlements_paid_to  ON settlements(paid_to);
CREATE TABLE activity_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(50) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_activity_events_group_id ON activity_events(group_id);
CREATE INDEX idx_activity_events_created_at ON activity_events(created_at DESC);
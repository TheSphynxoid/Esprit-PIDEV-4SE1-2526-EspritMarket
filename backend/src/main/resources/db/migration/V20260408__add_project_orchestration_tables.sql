CREATE TABLE IF NOT EXISTS project_milestone (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    details TEXT,
    planned_start_date TIMESTAMP,
    planned_end_date TIMESTAMP,
    actual_start_date TIMESTAMP,
    actual_end_date TIMESTAMP,
    status VARCHAR(40) NOT NULL DEFAULT 'PLANNED',
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_project_milestone_project_id
    ON project_milestone(project_id);

CREATE INDEX IF NOT EXISTS idx_project_milestone_project_order
    ON project_milestone(project_id, sort_order, id)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS project_dependency (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    predecessor_milestone_id BIGINT NOT NULL REFERENCES project_milestone(id) ON DELETE CASCADE,
    successor_milestone_id BIGINT NOT NULL REFERENCES project_milestone(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_project_dependency_not_self
        CHECK (predecessor_milestone_id <> successor_milestone_id),
    CONSTRAINT uk_project_dependency_pair
        UNIQUE (project_id, predecessor_milestone_id, successor_milestone_id)
);

CREATE INDEX IF NOT EXISTS idx_project_dependency_project_id
    ON project_dependency(project_id);

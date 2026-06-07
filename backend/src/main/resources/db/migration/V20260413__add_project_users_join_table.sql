CREATE TABLE IF NOT EXISTS project_users (
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_project_users_user_id ON project_users (user_id);

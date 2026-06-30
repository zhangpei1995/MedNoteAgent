CREATE TABLE IF NOT EXISTS chat_session (
    id TEXT PRIMARY KEY,
    user_id TEXT,
    status TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    ended_at DATETIME
);

CREATE INDEX IF NOT EXISTS idx_chat_session_user_id
    ON chat_session (user_id);

CREATE INDEX IF NOT EXISTS idx_chat_session_created_at
    ON chat_session (created_at);

CREATE TABLE IF NOT EXISTS chat_session_metadata (
    id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL UNIQUE,
    source_turn_id TEXT,
    status TEXT NOT NULL,
    title TEXT,
    consultation_category TEXT,
    recognized_drug_name TEXT,
    instruction_item TEXT,
    knowledge_status TEXT,
    scope_status TEXT,
    understanding_text TEXT,
    metadata_json TEXT,
    error_message TEXT,
    generated_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (session_id) REFERENCES chat_session (id)
);

CREATE INDEX IF NOT EXISTS idx_chat_session_metadata_session_id
    ON chat_session_metadata (session_id);

CREATE TABLE IF NOT EXISTS chat_turn_audit (
    id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    user_input TEXT NOT NULL,
    assistant_output TEXT,
    model_provider TEXT NOT NULL,
    model_name TEXT NOT NULL,
    system_prompt TEXT,
    request_json TEXT,
    response_json TEXT,
    status TEXT NOT NULL,
    error_message TEXT,
    elapsed_ms INTEGER,
    created_at DATETIME NOT NULL,
    completed_at DATETIME,
    FOREIGN KEY (session_id) REFERENCES chat_session (id)
);

CREATE INDEX IF NOT EXISTS idx_chat_turn_audit_session_id
    ON chat_turn_audit (session_id);

CREATE INDEX IF NOT EXISTS idx_chat_turn_audit_created_at
    ON chat_turn_audit (created_at);

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create topics table
CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    path VARCHAR(500),
    depth INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_topics_parent FOREIGN KEY (parent_id) REFERENCES topics(id) ON DELETE CASCADE
);

CREATE INDEX idx_topics_parent_id ON topics(parent_id);
CREATE INDEX idx_topics_path ON topics(path);

-- Create documents table
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    storage_key VARCHAR(1000) NOT NULL,
    topic_id BIGINT NOT NULL,
    version VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ingested_at TIMESTAMP,
    uploaded_by VARCHAR(255),
    error_message TEXT,
    CONSTRAINT fk_documents_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

CREATE INDEX idx_documents_topic_id ON documents(topic_id);
CREATE INDEX idx_documents_status ON documents(status);

-- Create document_sections table
CREATE TABLE document_sections (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    order_index INTEGER NOT NULL,
    start_page INTEGER,
    end_page INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_sections_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_sections_document_id ON document_sections(document_id);

-- Create document_chunks table
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    section_id BIGINT,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    start_page INTEGER,
    end_page INTEGER,
    char_start INTEGER,
    char_end INTEGER,
    checksum VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_chunks_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_document_chunks_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE,
    CONSTRAINT fk_document_chunks_section FOREIGN KEY (section_id) REFERENCES document_sections(id) ON DELETE SET NULL,
    CONSTRAINT uk_document_chunks_document_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_document_chunks_topic_id ON document_chunks(topic_id);
CREATE INDEX idx_document_chunks_section_id ON document_chunks(section_id);

-- Create chunk_embeddings table
CREATE TABLE chunk_embeddings (
    id BIGSERIAL PRIMARY KEY,
    chunk_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    section_id BIGINT,
    chunk_index INTEGER NOT NULL,
    embedding vector(1536),
    embedding_model VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chunk_embeddings_chunk FOREIGN KEY (chunk_id) REFERENCES document_chunks(id) ON DELETE CASCADE,
    CONSTRAINT fk_chunk_embeddings_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_chunk_embeddings_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE,
    CONSTRAINT fk_chunk_embeddings_section FOREIGN KEY (section_id) REFERENCES document_sections(id) ON DELETE SET NULL,
    CONSTRAINT uk_chunk_embeddings_chunk_model UNIQUE (chunk_id, embedding_model)
);

CREATE INDEX idx_chunk_embeddings_chunk_id ON chunk_embeddings(chunk_id);
CREATE INDEX idx_chunk_embeddings_document_id ON chunk_embeddings(document_id);
CREATE INDEX idx_chunk_embeddings_topic_id ON chunk_embeddings(topic_id);
CREATE INDEX idx_chunk_embeddings_section_id ON chunk_embeddings(section_id);

-- Create vector similarity index for efficient similarity search (using cosine distance)
-- Note: The lists parameter should be adjusted based on data size (recommended: rows/1000 for < 1M rows)
CREATE INDEX idx_chunk_embeddings_embedding ON chunk_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Create tests table
CREATE TABLE tests (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    question_count INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    generation_model VARCHAR(100),
    generation_params_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tests_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

CREATE INDEX idx_tests_topic_id ON tests(topic_id);
CREATE INDEX idx_tests_status ON tests(status);

-- Create test_questions table
CREATE TABLE test_questions (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL,
    question_index INTEGER NOT NULL,
    question_text TEXT NOT NULL,
    option_a VARCHAR(1000) NOT NULL,
    option_b VARCHAR(1000) NOT NULL,
    option_c VARCHAR(1000) NOT NULL,
    option_d VARCHAR(1000) NOT NULL,
    correct_option VARCHAR(10) NOT NULL,
    explanation TEXT,
    source_chunk_ids_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_questions_test FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE,
    CONSTRAINT uk_test_questions_test_index UNIQUE (test_id, question_index)
);

CREATE INDEX idx_test_questions_test_id ON test_questions(test_id);

-- Create attempts table
CREATE TABLE attempts (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL,
    user_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    total_questions INTEGER NOT NULL,
    correct_count INTEGER NOT NULL DEFAULT 0,
    wrong_count INTEGER NOT NULL DEFAULT 0,
    score_percent DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    scored_at TIMESTAMP,
    CONSTRAINT fk_attempts_test FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE
);

CREATE INDEX idx_attempts_test_id ON attempts(test_id);
CREATE INDEX idx_attempts_user_id ON attempts(user_id);
CREATE INDEX idx_attempts_status ON attempts(status);

-- Create attempt_answers table
CREATE TABLE attempt_answers (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    chosen_option VARCHAR(10) NOT NULL,
    correct BOOLEAN NOT NULL,
    answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attempt_answers_attempt FOREIGN KEY (attempt_id) REFERENCES attempts(id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_answers_question FOREIGN KEY (question_id) REFERENCES test_questions(id) ON DELETE CASCADE,
    CONSTRAINT uk_attempt_answers_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_attempt_answers_attempt_id ON attempt_answers(attempt_id);
CREATE INDEX idx_attempt_answers_question_id ON attempt_answers(question_id);

-- Create generation_runs table
CREATE TABLE generation_runs (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_template_version VARCHAR(100) NOT NULL,
    retrieved_chunk_ids_json TEXT,
    raw_response TEXT,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_generation_runs_test FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE
);

CREATE INDEX idx_generation_runs_test_id ON generation_runs(test_id);
CREATE INDEX idx_generation_runs_status ON generation_runs(status);

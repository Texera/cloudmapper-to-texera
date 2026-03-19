-- 1. Create a single consolidated ENUM type for all possible states
CREATE TYPE cluster_status AS ENUM (
    'LAUNCH_RECEIVED', 
    'PENDING', 
    'RUNNING', 
    'TERMINATE_RECEIVED', 
    'SHUTTING_DOWN', 
    'TERMINATED', 
    'STOP_RECEIVED', 
    'STOPPING', 
    'STOPPED', 
    'START_RECEIVED', 
    'LAUNCH_FAILED', 
    'TERMINATE_FAILED', 
    'STOP_FAILED', 
    'START_FAILED'
);

-- 2. Create the table
CREATE TABLE IF NOT EXISTS cluster (
    cid SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_id INTEGER NOT NULL CHECK (owner_id >= 0), -- Standard INT + CHECK replaces UNSIGNED
    machine_type VARCHAR(255) NOT NULL,
    number_of_machines INT NOT NULL,
    creation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status cluster_status,
    -- Quoting "user" because it is a reserved keyword in Postgres
    FOREIGN KEY (owner_id) REFERENCES "user" (uid) ON DELETE CASCADE
);

-- 3. Create the cluster_activity table
CREATE TABLE IF NOT EXISTS cluster_activity (
    id SERIAL PRIMARY KEY,
    cluster_id INT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    FOREIGN KEY (cluster_id) REFERENCES cluster (cid) ON DELETE CASCADE
);

-- 4. Create the index separately
CREATE INDEX idx_cluster_activity_cid_start ON cluster_activity (cluster_id, start_time);

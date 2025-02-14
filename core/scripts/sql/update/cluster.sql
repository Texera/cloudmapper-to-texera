use `texera_db`;
DROP TABLE IF EXISTS cluster_activity;
DROP TABLE IF EXISTS cluster;

CREATE TABLE IF NOT EXISTS cluster (
    `cid` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `owner_id` INT UNSIGNED NOT NULL,
    `machine_type` VARCHAR(255) NOT NULL,
    `number_of_machines` INT NOT NULL,
    `creation_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status` ENUM('LAUNCH_RECEIVED', 'PENDING', 'RUNNING', 'TERMINATE_RECEIVED', 'SHUTTING_DOWN', 'TERMINATED', 'STOP_RECEIVED', 'STOPPING', 'STOPPED', 'START_RECEIVED', 'LAUNCH_FAILED', 'TERMINATE_FAILED', 'STOP_FAILED', 'START_FAILED'),
    FOREIGN KEY (owner_id) REFERENCES user (uid) ON DELETE CASCADE
) ENGINE = INNODB;

CREATE TABLE IF NOT EXISTS cluster_activity (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `cluster_id` INT NOT NULL,
    `start_time` TIMESTAMP NOT NULL,
    `end_time` TIMESTAMP NULL,
    FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`cid`) ON DELETE CASCADE,
    INDEX (`cluster_id`, `start_time`)
) ENGINE = INNODB;

use penalty_shootout;
-- Create users table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    points INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'offline',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create matches table
CREATE TABLE matches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player1_id INT NOT NULL,
    player2_id INT NOT NULL,
    winner_id INT,
    end_reason VARCHAR(50),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_player1 (player1_id),
    INDEX idx_player2 (player2_id),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create match_details table
CREATE TABLE match_details (
    id INT AUTO_INCREMENT PRIMARY KEY,
    match_id INT NOT NULL,
    round INT NOT NULL,
    shooter_id INT NOT NULL,
    goalkeeper_id INT NOT NULL,
    shooter_direction VARCHAR(20),
    goalkeeper_direction VARCHAR(20),
    result VARCHAR(20),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    FOREIGN KEY (shooter_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (goalkeeper_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_match (match_id),
    INDEX idx_round (round)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert sample users for testing
INSERT INTO users (username, password, points, status) VALUES
('player1', 'pass123', 100, 'offline'),
('player2', 'pass123', 80, 'offline'),
('testuser', 'test', 50, 'offline'),
('admin', 'admin123', 150, 'offline'),
('demo', 'demo', 75, 'offline');

-- Display success message
SELECT 'Database setup completed successfully!' AS Status;
SELECT COUNT(*) AS 'Total Users Created' FROM users;
SELECT 'You can now run the server and client applications.' AS Message;
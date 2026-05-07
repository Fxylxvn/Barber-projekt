-- ==========================================
-- PostgreSQL Database Initialization Script
-- ==========================================

-- Enable UUID extension (optional, for future use)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    email VARCHAR(100),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    work_start_hour INTEGER,
    work_end_hour INTEGER,
    work_days VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tasks Table
CREATE TABLE IF NOT EXISTS task (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Appointments Table
CREATE TABLE IF NOT EXISTS appointment (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    task_id INTEGER NOT NULL REFERENCES task(id),
    appointment_date TIMESTAMP NOT NULL,
    duration_minutes INTEGER DEFAULT 60,
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_appointment_user ON appointment(user_id);
CREATE INDEX IF NOT EXISTS idx_appointment_task ON appointment(task_id);
CREATE INDEX IF NOT EXISTS idx_appointment_date ON appointment(appointment_date);

-- Insert sample data
INSERT INTO users (username, password, role, name, email, first_name, last_name)
VALUES 
    ('admin1', 'adminpass123', 'ADMIN', 'Admin User', 'admin@barber.local', 'Admin', 'User'),
    ('barber1', 'barberpass123', 'BARBER', 'Tomasz Barber', 'barber@barber.local', 'Tomasz', 'Barberski'),
    ('klient1', 'haslo123', 'KLIENT', 'Jan Kowalski', 'klient@barber.local', 'Jan', 'Kowalski')
ON CONFLICT (username) DO NOTHING;

-- Insert sample tasks
INSERT INTO task (name, description)
VALUES 
    ('Strzyżenie', 'Standardowe strzyżenie włosów'),
    ('Golenie', 'Golenie brody i twarzy'),
    ('Stylizacja', 'Zaawansowana stylizacja fryzury'),
    ('Ubarwianie', 'Farbowanie włosów')
ON CONFLICT DO NOTHING;

-- Grant permissions (optional)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO barber_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO barber_user;

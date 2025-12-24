-- --------------------------------------------
-- SkillTrack Auth-Service schema
-- --------------------------------------------
-- This schema is designed for MySQL 8.0+.
--
-- Notes:
-- - Roles are represented as an application enum (Role) but persisted as a VARCHAR
--   with a foreign key to the roles table for referential integrity.
-- - Passwords must be stored as BCrypt hashes only.
--

CREATE TABLE IF NOT EXISTS roles (
  name VARCHAR(20) NOT NULL,
  description VARCHAR(255) NULL,
  PRIMARY KEY (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(100) NOT NULL,
  email VARCHAR(254) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  role VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  last_login_at DATETIME NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_users_username UNIQUE (username),
  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT fk_users_role FOREIGN KEY (role) REFERENCES roles(name)
) ENGINE=InnoDB;

-- Performance indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

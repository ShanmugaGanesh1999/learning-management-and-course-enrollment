-- --------------------------------------------
-- SkillTrack Auth-Service seed data
-- --------------------------------------------
-- Inserts base roles and a default admin user.
--
-- IMPORTANT:
-- - Replace the admin password hash if you rotate credentials.
-- - The password value MUST be a BCrypt hash of "admin123".
--   If you need to regenerate: use a BCrypt generator (strength 10-12+).
--

INSERT IGNORE INTO roles (name, description) VALUES
  ('STUDENT', 'Can browse courses, enroll, and track progress.'),
  ('INSTRUCTOR', 'Can create and manage their own courses.'),
  ('ADMIN', 'Can oversee users and courses across the platform.');

-- Default admin user (username: admin, password: admin123)
-- NOTE: The hash below is a BCrypt placeholder. Replace with a real BCrypt hash of "admin123".
INSERT IGNORE INTO users (username, password, email, full_name, role, created_at, updated_at, last_login_at)
VALUES (
  'admin',
  '$2a$12$REPLACE_ME_WITH_BCRYPT_HASH_OF_admin123__________________________',
  'admin@example.com',
  'Admin User',
  'ADMIN',
  NOW(),
  NOW(),
  NULL
);

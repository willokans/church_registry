-- Seed data for H2 database
-- This file is loaded when using H2 profile
-- Note: Since ddl-auto=create-drop, data is inserted fresh each time

-- Insert sample tenant
INSERT INTO tenants (id, slug, name, theme, created_at) VALUES
('550e8400-e29b-41d4-a716-446655440000', 'sample-parish', 'Sample Parish', '{"primaryColor": "#0066cc", "logo": "/logo.png"}', CURRENT_TIMESTAMP);

-- Insert permissions
INSERT INTO permissions (key) VALUES
('users.manage'),
('users.view'),
('permissions.grant'),
('sacraments.create'),
('sacraments.update'),
('sacraments.view'),
('settings.edit'),
('audit.view');

-- Insert role permissions
INSERT INTO role_permissions (role, permission_key) VALUES
('SUPER_ADMIN', 'users.manage'),
('SUPER_ADMIN', 'users.view'),
('SUPER_ADMIN', 'permissions.grant'),
('SUPER_ADMIN', 'sacraments.create'),
('SUPER_ADMIN', 'sacraments.update'),
('SUPER_ADMIN', 'sacraments.view'),
('SUPER_ADMIN', 'settings.edit'),
('SUPER_ADMIN', 'audit.view'),
('PARISH_ADMIN', 'users.manage'),
('PARISH_ADMIN', 'users.view'),
('PARISH_ADMIN', 'permissions.grant'),
('PARISH_ADMIN', 'sacraments.create'),
('PARISH_ADMIN', 'sacraments.update'),
('PARISH_ADMIN', 'sacraments.view'),
('PARISH_ADMIN', 'settings.edit'),
('PARISH_ADMIN', 'audit.view'),
('REGISTRAR', 'sacraments.create'),
('REGISTRAR', 'sacraments.update'),
('REGISTRAR', 'sacraments.view'),
('PRIEST', 'sacraments.create'),
('PRIEST', 'sacraments.view'),
('VIEWER', 'sacraments.view'),
('VIEWER', 'users.view');

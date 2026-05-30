CREATE TABLE organization_nodes (
	id INT AUTO_INCREMENT PRIMARY KEY,
	parent_id INT NULL,
	node_type VARCHAR(50) NOT NULL,
	name VARCHAR(150) NOT NULL,
	slug VARCHAR(180) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
	path VARCHAR(1024) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
	depth INT NOT NULL DEFAULT 0,
	sort_order INT NOT NULL DEFAULT 0,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT uq_organization_nodes_path UNIQUE (path),
	CONSTRAINT uq_organization_nodes_parent_slug UNIQUE (parent_id, slug),
	CONSTRAINT fk_organization_nodes_parent FOREIGN KEY (parent_id) REFERENCES organization_nodes(id) ON DELETE RESTRICT
);

CREATE INDEX idx_organization_nodes_parent ON organization_nodes(parent_id);
CREATE INDEX idx_organization_nodes_type ON organization_nodes(node_type);
CREATE INDEX idx_organization_nodes_active ON organization_nodes(is_active);

CREATE TABLE node_contents (
	id INT AUTO_INCREMENT PRIMARY KEY,
	node_id INT NOT NULL,
	summary VARCHAR(255),
	description TEXT,
	contact_email VARCHAR(150),
	location VARCHAR(150),
	metadata_json JSON NULL,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT uq_node_contents_node UNIQUE (node_id),
	CONSTRAINT fk_node_contents_node FOREIGN KEY (node_id) REFERENCES organization_nodes(id) ON DELETE CASCADE
);

CREATE TABLE node_links (
	id INT AUTO_INCREMENT PRIMARY KEY,
	node_id INT NOT NULL,
	label VARCHAR(150) NOT NULL,
	url VARCHAR(2048) NOT NULL,
	category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
	icon VARCHAR(50),
	visibility VARCHAR(30) NOT NULL DEFAULT 'INHERIT',
	sort_order INT NOT NULL DEFAULT 0,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT fk_node_links_node FOREIGN KEY (node_id) REFERENCES organization_nodes(id) ON DELETE CASCADE
);

CREATE INDEX idx_node_links_node ON node_links(node_id);
CREATE INDEX idx_node_links_category ON node_links(category);
CREATE INDEX idx_node_links_active ON node_links(is_active);

CREATE TABLE node_memberships (
	id INT AUTO_INCREMENT PRIMARY KEY,
	node_id INT NOT NULL,
	user_id INT NOT NULL,
	membership_role VARCHAR(50) NOT NULL DEFAULT 'MEMBRE',
	is_primary BOOLEAN NOT NULL DEFAULT FALSE,
	active_from DATETIME NULL,
	active_to DATETIME NULL,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT uq_node_memberships_assignment UNIQUE (node_id, user_id, membership_role),
	CONSTRAINT fk_node_memberships_node FOREIGN KEY (node_id) REFERENCES organization_nodes(id) ON DELETE CASCADE
);

CREATE INDEX idx_node_memberships_user ON node_memberships(user_id);
CREATE INDEX idx_node_memberships_node ON node_memberships(node_id);
CREATE INDEX idx_node_memberships_primary ON node_memberships(is_primary);

CREATE TABLE org_user_roles (
	id INT AUTO_INCREMENT PRIMARY KEY,
	user_id INT NOT NULL,
	role_name VARCHAR(100) NOT NULL,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT uq_org_user_roles UNIQUE (user_id, role_name)
);

CREATE INDEX idx_org_user_roles_user ON org_user_roles(user_id);

CREATE TABLE node_access_rules (
	id INT AUTO_INCREMENT PRIMARY KEY,
	node_id INT NOT NULL,
	effect VARCHAR(10) NOT NULL DEFAULT 'ALLOW',
	subject_type VARCHAR(30) NOT NULL,
	subject_value VARCHAR(100) NOT NULL,
	permission VARCHAR(30) NOT NULL DEFAULT 'READ',
	applies_to_children BOOLEAN NOT NULL DEFAULT TRUE,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT uq_node_access_rules UNIQUE (node_id, effect, subject_type, subject_value, permission),
	CONSTRAINT fk_node_access_rules_node FOREIGN KEY (node_id) REFERENCES organization_nodes(id) ON DELETE CASCADE
);

CREATE INDEX idx_node_access_rules_node ON node_access_rules(node_id);
CREATE INDEX idx_node_access_rules_subject ON node_access_rules(subject_type, subject_value);
CREATE INDEX idx_node_access_rules_permission ON node_access_rules(permission);

CREATE TABLE announcements (
	id INT AUTO_INCREMENT PRIMARY KEY,
	node_id INT NULL,
	scope_type VARCHAR(20) NOT NULL DEFAULT 'GLOBAL',
	title VARCHAR(200) NOT NULL,
	body TEXT NOT NULL,
	severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
	start_at DATETIME NULL,
	end_at DATETIME NULL,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,
	created_by_user_id INT NULL,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT fk_announcements_node FOREIGN KEY (node_id) REFERENCES organization_nodes(id) ON DELETE SET NULL
);

CREATE INDEX idx_announcements_scope ON announcements(scope_type);
CREATE INDEX idx_announcements_node ON announcements(node_id);
CREATE INDEX idx_announcements_active_dates ON announcements(is_active, start_at, end_at);

INSERT INTO organization_nodes (parent_id, node_type, name, slug, path, depth, sort_order, is_active)
VALUES (NULL, 'COMPANY', 'Nexaris', 'nexaris', '/nexaris', 0, 0, TRUE);

INSERT INTO node_contents (node_id, summary, description, contact_email, location, metadata_json)
SELECT id,
	   'Espace principal de navigation de Nexaris',
	   'Page racine entreprise contenant les informations globales, les liens utiles et les subdivisions visibles.',
	   NULL,
	   NULL,
	   JSON_OBJECT('theme', 'default')
FROM organization_nodes
WHERE path = '/nexaris';

INSERT INTO node_access_rules (node_id, effect, subject_type, subject_value, permission, applies_to_children)
SELECT id,
	   'ALLOW',
	   'ROLE',
	   'ROLE_USER',
	   'READ',
	   FALSE
FROM organization_nodes
WHERE path = '/nexaris';

CREATE TABLE org_catalog_entries (
	id           INT AUTO_INCREMENT PRIMARY KEY,
	catalog_type VARCHAR(60)  NOT NULL,
	value        VARCHAR(100) NOT NULL,
	color        VARCHAR(20) NULL,
	CONSTRAINT uq_catalog_type_value UNIQUE (catalog_type, value)
);

-- Catégories de liens par défaut
INSERT INTO org_catalog_entries (catalog_type, value, color) VALUES
	('LINK_CATEGORY', 'DOCS', '#0ea5e9'),
	('LINK_CATEGORY', 'FINANCE', '#f59e0b'),
	('LINK_CATEGORY', 'GENERAL', '#64748b'),
	('LINK_CATEGORY', 'HR', '#ec4899'),
	('LINK_CATEGORY', 'IT', '#3b82f6'),
	('LINK_CATEGORY', 'TOOLS', '#10b981');

-- Sévérités d'annonces par défaut
INSERT INTO org_catalog_entries (catalog_type, value, color) VALUES
	('ANNOUNCEMENT_SEVERITY', 'CRITICAL', '#dc2626'),
	('ANNOUNCEMENT_SEVERITY', 'INCIDENT', '#ef4444'),
	('ANNOUNCEMENT_SEVERITY', 'INFO', '#3b82f6'),
	('ANNOUNCEMENT_SEVERITY', 'MAINTENANCE', '#8b5cf6'),
	('ANNOUNCEMENT_SEVERITY', 'WARNING', '#f59e0b');

-- Types de nœuds organisationnels par défaut
INSERT INTO org_catalog_entries (catalog_type, value, color) VALUES
	('NODE_TYPE', 'DEPARTMENT', '#0ea5e9'),
	('NODE_TYPE', 'DIVISION', '#8b5cf6'),
	('NODE_TYPE', 'ORGANIZATION', '#3b82f6'),
	('NODE_TYPE', 'TEAM', '#10b981'),
	('NODE_TYPE', 'UNIT', '#f59e0b');

-- Rôles de règles d'accès par défaut
INSERT INTO org_catalog_entries (catalog_type, value, color) VALUES
	('ACCESS_RULE_ROLE', 'ROLE_USER', '#3b82f6');


CREATE TABLE membership_role_permissions (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    membership_role VARCHAR(50) NOT NULL,
    permission      VARCHAR(60) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_membership_role_permission UNIQUE (membership_role, permission)
);

INSERT INTO org_catalog_entries (catalog_type, value, color)
SELECT 'MEMBERSHIP_ROLE', 'RESPONSABLE', '#dc2626'
WHERE NOT EXISTS (
	SELECT 1 FROM org_catalog_entries WHERE catalog_type = 'MEMBERSHIP_ROLE' AND value = 'RESPONSABLE'
);

INSERT INTO org_catalog_entries (catalog_type, value, color)
SELECT 'MEMBERSHIP_ROLE', 'GESTIONNAIRE', '#f59e0b'
WHERE NOT EXISTS (
	SELECT 1 FROM org_catalog_entries WHERE catalog_type = 'MEMBERSHIP_ROLE' AND value = 'GESTIONNAIRE'
);

INSERT INTO org_catalog_entries (catalog_type, value, color)
SELECT 'MEMBERSHIP_ROLE', 'EDITEUR', '#0ea5e9'
WHERE NOT EXISTS (
	SELECT 1 FROM org_catalog_entries WHERE catalog_type = 'MEMBERSHIP_ROLE' AND value = 'EDITEUR'
);

INSERT INTO org_catalog_entries (catalog_type, value, color)
SELECT 'MEMBERSHIP_ROLE', 'MEMBRE', '#3b82f6'
WHERE NOT EXISTS (
	SELECT 1 FROM org_catalog_entries WHERE catalog_type = 'MEMBERSHIP_ROLE' AND value = 'MEMBRE'
);

INSERT INTO org_catalog_entries (catalog_type, value, color)
SELECT 'MEMBERSHIP_ROLE', 'LECTEUR', '#94a3b8'
WHERE NOT EXISTS (
	SELECT 1 FROM org_catalog_entries WHERE catalog_type = 'MEMBERSHIP_ROLE' AND value = 'LECTEUR'
);

INSERT INTO membership_role_permissions (membership_role, permission) VALUES
	('RESPONSABLE', 'READ'),
	('RESPONSABLE', 'EDIT_CONTENT'),
	('RESPONSABLE', 'EDIT_LINKS'),
	('RESPONSABLE', 'MANAGE_MEMBERS'),
	('RESPONSABLE', 'MANAGE_ACCESS'),
	('RESPONSABLE', 'MANAGE_ANNOUNCEMENTS'),
	('RESPONSABLE', 'CREATE_CHILD'),
	('RESPONSABLE', 'DELETE_NODE'),
	('GESTIONNAIRE', 'READ'),
	('GESTIONNAIRE', 'EDIT_CONTENT'),
	('GESTIONNAIRE', 'EDIT_LINKS'),
	('GESTIONNAIRE', 'MANAGE_MEMBERS'),
	('GESTIONNAIRE', 'MANAGE_ACCESS'),
	('GESTIONNAIRE', 'MANAGE_ANNOUNCEMENTS'),
	('GESTIONNAIRE', 'CREATE_CHILD'),
	('EDITEUR', 'READ'),
	('EDITEUR', 'EDIT_CONTENT'),
	('EDITEUR', 'EDIT_LINKS'),
	('EDITEUR', 'MANAGE_ANNOUNCEMENTS'),
	('MEMBRE', 'READ'),
	('MEMBRE', 'MANAGE_ANNOUNCEMENTS'),
	('LECTEUR', 'READ');
INSERT INTO org_catalog_entries (catalog_type, value, color)
SELECT 'NODE_TYPE', 'COMPANY', '#3b82f6'
WHERE NOT EXISTS (
    SELECT 1
    FROM org_catalog_entries
    WHERE catalog_type = 'NODE_TYPE'
      AND value = 'COMPANY'
);

-- Every department gets a number from 1 to 99, unique within its organization: it prefixes the personal numbers of its clients.
-- Departments without a number are numbered after the highest existing one in their organization, by id.
UPDATE department d
SET department_number = numbered.department_number
FROM (
    SELECT id,
           (SELECT coalesce(max(department_number), 0) FROM department WHERE organization_id = unnumbered.organization_id)
               + row_number() OVER (PARTITION BY organization_id ORDER BY id) AS department_number
    FROM department unnumbered
    WHERE department_number IS NULL
) numbered
WHERE d.id = numbered.id;

ALTER TABLE department
    ALTER COLUMN department_number SET NOT NULL,
    ADD CONSTRAINT department_number_range CHECK (department_number BETWEEN 1 AND 99);

CREATE UNIQUE INDEX idx_department_number_organization ON department (organization_id, department_number);

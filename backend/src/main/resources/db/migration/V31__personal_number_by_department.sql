-- The personal number becomes text XXYYYY: the two-digit department number and a sequence within the department.
-- Existing clients are renumbered by id within their department. The index is rebuilt afterwards,
-- so an old number cannot clash with a new one halfway through the update.
DROP INDEX idx_client_personal_number_organization;

ALTER TABLE client ALTER COLUMN personal_number TYPE varchar(6) USING NULL;

UPDATE client c
SET personal_number = lpad(d.department_number::text, 2, '0') || lpad(numbered.sequence::text, 4, '0')
FROM (
    SELECT id, row_number() OVER (PARTITION BY department_id ORDER BY id) AS sequence
    FROM client
) numbered,
     department d
WHERE c.id = numbered.id
  AND d.id = c.department_id;

ALTER TABLE client
    ALTER COLUMN personal_number SET NOT NULL,
    ADD CONSTRAINT client_personal_number_format CHECK (personal_number ~ '^[0-9]{6}$');

CREATE UNIQUE INDEX idx_client_personal_number_organization ON client (personal_number, organization_id);

DO $$
DECLARE
    problem text;
BEGIN
    SELECT string_agg('department ' || id || ' (organization ' || organization_id || ') has number ' || department_number, '; ')
    INTO problem
    FROM department
    WHERE department_number NOT BETWEEN 1 AND 99;
    IF problem IS NOT NULL THEN
        RAISE EXCEPTION 'V30: department_number outside 1-99: %', problem;
    END IF;

    SELECT string_agg('organization ' || organization_id || ' number ' || department_number || ' used by ' || cnt || ' departments', '; ')
    INTO problem
    FROM (SELECT organization_id, department_number, count(*) AS cnt
          FROM department
          WHERE department_number IS NOT NULL
          GROUP BY organization_id, department_number
          HAVING count(*) > 1) duplicates;
    IF problem IS NOT NULL THEN
        RAISE EXCEPTION 'V30: duplicate department_number within an organization: %', problem;
    END IF;

    SELECT string_agg('organization ' || organization_id || ' would need number ' || needed, '; ')
    INTO problem
    FROM (SELECT organization_id,
                 coalesce(max(department_number), 0) + count(*) FILTER (WHERE department_number IS NULL) AS needed
          FROM department
          GROUP BY organization_id) totals
    WHERE needed > 99;
    IF problem IS NOT NULL THEN
        RAISE EXCEPTION 'V30: numbering departments without a number would exceed 99: %', problem;
    END IF;
END $$;

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

ALTER TABLE client
    ADD COLUMN title_before           varchar(20),
    ADD COLUMN title_after            varchar(20),
    ADD COLUMN phone2                 varchar(16),
    ADD COLUMN guardian_name          varchar(255),
    ADD COLUMN guardian_contact       text,
    ADD COLUMN care_from              date,
    ADD COLUMN care_to                date,
    ADD COLUMN contract_from          date,
    ADD COLUMN contract_to            date,
    ADD COLUMN contract_indefinite    boolean DEFAULT true,
    ADD COLUMN no_charge              boolean NOT NULL DEFAULT false,
    ADD COLUMN intake_assessment_date date;

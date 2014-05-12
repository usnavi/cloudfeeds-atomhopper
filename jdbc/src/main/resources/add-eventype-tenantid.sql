BEGIN;

ALTER TABLE entries ADD COLUMN eventType text;
ALTER TABLE entries ADD COLUMN tenantId text;

CREATE INDEX eventType_idx on entries( eventTYpe );
CREATE INDEX tenantId_idx on entries( tenantId );

COMMIT;
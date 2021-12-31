DROP TYPE IF EXISTS job_status;

CREATE TYPE job_status AS ENUM ('new', 'initializing', 'initialized', 'running', 'success', 'error');

DROP TABLE IF EXISTS jobs;

CREATE TABLE jobs(
	status job_status not null,
	target_id int,
	payload json,
	updated timestamp not null,
	id SERIAL
);

CREATE OR REPLACE FUNCTION jobs_status_notify()
	RETURNS trigger AS
$$
BEGIN
	PERFORM pg_notify('jobs_status_channel', NEW.id::text);
	RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER jobs_status
	AFTER INSERT OR UPDATE OF status
	ON jobs
	FOR EACH ROW
EXECUTE PROCEDURE jobs_status_notify();
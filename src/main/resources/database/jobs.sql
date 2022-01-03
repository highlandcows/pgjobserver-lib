DROP TYPE IF EXISTS job_status;

CREATE TYPE job_status AS ENUM ('new', 'initializing', 'initialized', 'running', 'success', 'error');

DROP TABLE IF EXISTS jobs;

CREATE TABLE jobs(
  channel_name text not null,
	status job_status not null,
	target_id int,
	payload json,
	updated timestamp not null,
	id SERIAL
);
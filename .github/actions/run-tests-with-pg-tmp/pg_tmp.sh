#!/bin/sh
export PATH=/usr/lib/postgresql/14/bin:$PATH

cd /
su postgres -c "/usr/local/bin/pg_tmp -t"
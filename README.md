![Build Status](https://github.com/highlandcows/pgjobserver-lib/actions/workflows/scala.yml/badge.svg)
[![Publish package to GitHub Packages](https://github.com/highlandcows/pgjobserver-lib/actions/workflows/release.yml/badge.svg?branch=v0.1.0)](https://github.com/highlandcows/pgjobserver-lib/actions/workflows/release.yml)

# highlandcows pgjobserver-lib
This project implements a simple PostreSQL-based job queue library based on
[System design hack: Postgres is a great pub/sub & job server](https://webapp.io/blog/postgres-is-the-answer/) using
[Scala](https://www.scala-lang.org), [Slick](https://scala-slick.org/doc/3.3.3/index.html), and of course,
[PostgreSQL](https://www.postgresql.org/docs/current).

## How does it work?
As discussed in the above blog, it is possible to create a "job" table that, when used with the PostgreSQL
NOTIFY mechanism referenced below, can effect a fairly efficient messaging system between multiple services and/or
threads.

It does require that all services have access to the same database but given PostgreSQL's scalability, this is
not likely to be an issue except at very large scale.

## Details on PostgreSQL NOTIFY
The [NOTIFY](https://www.postgresql.org/docs/current/sql-notify.html) mechanism allows a basic form of 
interprocess communication using a PostgreSQL database. 

# Building/Testing
This project is built using [sbt](https://www.scala-sbt.org/1.x/docs/) and as such can be built by running the
command
```shell
$ sbt compile
```

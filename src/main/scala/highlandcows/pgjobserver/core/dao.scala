package highlandcows.pgjobserver.core

import org.postgresql.PGConnection
import slick.sql.SqlStreamingAction

import scala.concurrent.{ ExecutionContext, Future }

import java.util.concurrent.Executors

object dao {
  import repository.PostgresProfile.api._

  /** Class the encapsulates the `jobs` table. Note that each of the methods takes in an implicit Slick `Database`
    * object as we need that to run the associated action on.
    */
  class JobsDAO() {

    private val jobs = TableQuery[repository.JobsSchema]

    def addJob(job: Job)(implicit db: Database): Future[Job] = {
      val action = (jobs returning jobs) += job
      db.run(action)
    }

    def getJob(id: Int)(implicit db: Database): Future[Option[Job]] = {
      val action = jobs.filter(_.id === id).result.headOption
      db.run(action)
    }

    def getNewJobs()(implicit db: Database): Future[Seq[Job]] = {
      val action = jobs.filter(_.jobStatus === JobStatus.New).result
      db.run(action)
    }

    def updateJob(job: Job)(implicit db: Database): Future[Job] = {
      val action = jobs.filter(_.id === job.id).update(job) andThen jobs.filter(_.id === job.id).result.head
      db.run(action)
    }

    def updateJobStatus(id: Int, jobStatus: JobStatus.Value)(implicit
      db: Database,
      ec: ExecutionContext
    ): Future[Unit] = {
      val q      = for { job <- jobs if job.id === id } yield job.jobStatus
      val action = q.update(jobStatus)
      db.run(action).map {
        case 1 =>
          Future.successful(())
        case n =>
          throw new RuntimeException(s"Updated to job $id to $jobStatus failed: $n rows updated")
      }
    }

  }

  /** Class that encapsulates listening for PostgreSQL notifications. Note that we take in a database when the instance
    * is created as we must use the same connection for all operations, i.e., listening, receiving, and stopping
    * listening. Also note that if the `jobs` table is updated while there is no listener, those events will be
    * effectively lost.
    *
    * @see
    *   https://www.postgresql.org/docs/current/sql-notify.html
    */
  class JobsProcessor(val channelName: String)(implicit db: Database, ec: ExecutionContext) {
    lazy val session: Session = db.createSession()

    /** Start listening for updates to `jobs` table for updates */
    def startJobNotifications(): Future[Unit] = {
      val action = SimpleDBIO[Unit](_ => session.conn.createStatement().execute(s"LISTEN $channelName"))
      db.run(action)
    }

    /** Stop listening for updates to `jobs` table for updates */
    def stopJobNotifications(): Future[Unit] = {
      val action = SimpleDBIO[Unit](_ => session.conn.createStatement().execute(s"UNLISTEN $channelName"))
      db.run(action)
    }

    /** Get the notifications for the channel that we're listening and pass them to the function object we were given to
      * do whatever it is they need to do. Note that the supplied function will be given a collection of the `jobs.id`
      * field.
      */
    def processJobs[T](timeoutMs: Int = 0)(block: Seq[Int] => T): Future[T] = {
      val action = SimpleDBIO(_ =>
        session.conn.asInstanceOf[PGConnection].getNotifications(timeoutMs).map(_.getParameter.toInt).toSeq
      )

      db.run(action).map(block)
    }
  }

  object JobsProcessor {
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

    private def createFunctionSQL(channelName: String, functionName: String): String = {
      // NB: `s` string interpolation expands `$$` to a single `$`, thus the quadruple `$` signs here
      s"""
         |CREATE OR REPLACE FUNCTION $functionName()
         |	RETURNS trigger AS
         |$$$$
         |BEGIN
         |	PERFORM pg_notify('$channelName', NEW.id::text);
         |	RETURN NEW;
         |END;
         |$$$$ LANGUAGE plpgsql;
         |""".stripMargin
    }

    private def createTriggerSQL(channelName: String, functionName: String): String =
      s"""
         |CREATE TRIGGER ${functionName}_status
         |	AFTER INSERT OR UPDATE OF status
         |	ON jobs
         |	FOR EACH ROW
         |  WHEN (NEW.channel_name = '$channelName')
         |EXECUTE PROCEDURE $functionName();
         |""".stripMargin

    private[core] def isNotifyFunctionDefined(functionName: String)(implicit db: Database): Future[Boolean] = {
      val action: SqlStreamingAction[Vector[Int], Int, Effect] = sql"""
        SELECT count(*)
        FROM pg_catalog.pg_namespace n
        JOIN pg_catalog.pg_proc p ON p.pronamespace = n.oid
        WHERE
        p.prokind = 'f' AND n.nspname = 'public' AND p.proname = $functionName""".as[Int]

      db.run(action).map(count => count.head > 0)
    }

    private def createNotifyFunction(channelName: String, functionName: String)(implicit db: Database): Future[Unit] = {
      val actions = DBIO.seq(
        SimpleDBIO[Unit](_.connection.createStatement.execute(createFunctionSQL(channelName, functionName))),
        SimpleDBIO[Unit](_.connection.createStatement.execute(createTriggerSQL(channelName, functionName)))
      )
      db.run(actions)
    }

    /** Create/connect to a `JobsProcessor` object for the current database. We return a `Future` here because we need
      * to install/confirm that we have the specified function and trigger in that database.
      */
    def apply(channelName: String)(implicit db: Database): Future[JobsProcessor] =
      maybeCreateNotifyFunction(channelName, s"${channelName}_notify").map(_ => new JobsProcessor(channelName))

    // Create the notification trigger in the database if it does not already exist.
    private def maybeCreateNotifyFunction(channelName: String, functionName: String)(implicit
      db: Database
    ): Future[Unit] =
      isNotifyFunctionDefined(functionName).flatMap {
        case false =>
          createNotifyFunction(channelName, functionName)
        case true =>
          Future.successful(())
      }
  }
}

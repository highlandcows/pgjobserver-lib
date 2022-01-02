package highlandcows.pgjobserver.core

import org.postgresql.PGConnection
import slick.sql.SqlStreamingAction

import java.util.concurrent.Executors
import scala.concurrent.{ ExecutionContext, Future }

object dao {
  import repository.PostgresProfile.api._

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
      val action = q.update(jobStatus).map(_ => ())
      db.run(action)
    }

  }

  class JobsProcessor(channelName: String)(implicit ec: ExecutionContext) {

    def startJobNotifications()(implicit db: Database): Future[Unit] = {
      val action = SimpleDBIO[Unit](_.connection.createStatement().execute(s"LISTEN $channelName"))
      db.run(action)
    }

    def stopJobNotifications()(implicit db: Database): Future[Unit] = {
      val action = SimpleDBIO[Unit](_.connection.createStatement().execute(s"UNLISTEN $channelName"))
      db.run(action)
    }

    def processJobs[T](timeoutMs: Int = 0)(block: Seq[Int] => T)(implicit db: Database): Future[T] = {
      // Get any pending notifications, irrespective of which channel they occurred on
      val action = SimpleDBIO(_.connection.asInstanceOf[PGConnection].getNotifications(timeoutMs))

      // Get the notifications which are for the channel that we're listening and pass them to the
      // function object we were given to do whatever it is they need to do.
      db.run(action).map(_.toSeq.filter(_.getName == channelName)).map(_.map(_.getParameter.toInt)).map(block)
    }
  }

  object JobsProcessor {
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

    private def createFunctionSQL(channelName: String, functionName: String): String =
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

    private def createTriggerSQL(functionName: String): String =
      s"""
         |CREATE TRIGGER ${functionName}_status
         |	AFTER INSERT OR UPDATE OF status
         |	ON jobs
         |	FOR EACH ROW
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

    private def createNotifyFunction(channelName: String)(implicit db: Database): Future[Unit] = {
      val functionName = s"${channelName}_notify"
      val actions = DBIO.seq(
        SimpleDBIO[Unit](_.connection.createStatement.execute(createFunctionSQL(channelName, functionName))),
        SimpleDBIO[Unit](_.connection.createStatement.execute(createTriggerSQL(functionName)))
      )
      db.run(actions)
    }

    def apply(channelName: String)(implicit db: Database): Future[JobsProcessor] = {
      for {
        notifyDefined <- isNotifyFunctionDefined(channelName)
        _ <-
          if (!notifyDefined) createNotifyFunction(channelName).map(_ => createTriggerSQL(channelName))
          else Future.successful(())
      } yield new JobsProcessor(channelName)
    }
  }
}

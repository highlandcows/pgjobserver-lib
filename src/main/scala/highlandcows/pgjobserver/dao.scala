package highlandcows.pgjobserver

import org.postgresql.PGConnection

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

  class JobProcessor(channelName: String) {
    def startJobNotifications()(implicit db: Database): Future[Unit] = {
      val action = SimpleDBIO[Unit](_.connection.createStatement().execute(s"LISTEN $channelName"))
      db.run(action)
    }

    def stopJobNotifications()(implicit db: Database): Future[Unit] = {
      val action = SimpleDBIO[Unit](_.connection.createStatement().execute("UNLISTEN $channelName"))
      db.run(action)
    }

    def processJobs[T](
      timeoutMs: Int = 0
    )(block: Seq[Int] => T)(implicit db: Database, ec: ExecutionContext): Future[T] = {
      // Get any pending notifications, irrespective of which channel they occurred on
      val action = SimpleDBIO(_.connection.asInstanceOf[PGConnection].getNotifications(timeoutMs))

      // Get the notifications which are for the channel that we're listening and pass them to the
      // function object we were given to do whatever it is they need to do.
      db.run(action).map(_.toSeq.filter(_.getName == channelName)).map(_.map(_.getParameter.toInt)).map(block)
    }
  }
}

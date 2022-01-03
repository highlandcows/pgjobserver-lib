package highlandcows.pgjobserver.core

import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json._

import java.util.concurrent.Executors
import scala.concurrent.{ ExecutionContext, Future }

class JobCreationTests extends FixtureAsyncFlatSpec with testutil.PgTmpDatabaseFixture {
  import testutil.{ randomChannelName, testPayload }

  // We need at least 2 threads otherwise our tests will not run.
  override implicit val executionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  "Jobs repository" should "create a new job" in { implicit db =>
    val jobsDAO = new dao.JobsDAO()
    jobsDAO.addJob(Job(randomChannelName, testPayload)).map { job =>
      job.id should not be 0
      job.jobStatus shouldBe JobStatus.New
    }
  }

  it should "create multiple jobs" in { implicit db =>
    val jobsDAO     = new dao.JobsDAO()
    val channelName = randomChannelName
    Future
      .sequence(
        Seq(
          jobsDAO.addJob(Job(channelName, testPayload)),
          jobsDAO.addJob(Job(channelName, testPayload)),
          jobsDAO.addJob(Job(channelName, testPayload))
        )
      )
      .flatMap { jobs =>
        jobs.size shouldBe 3
        jobs.forall(_.id != 0) shouldBe true
        jobs.forall(_.jobStatus == JobStatus.New) shouldBe true
      }
  }

  it should "update a job" in { implicit db =>
    val jobsDAO = new dao.JobsDAO()
    jobsDAO.addJob(Job(randomChannelName, testPayload)).flatMap { job =>
      jobsDAO
        .updateJobStatus(job.id, JobStatus.Initializing)
        .flatMap(_ =>
          jobsDAO.getJob(job.id).map {
            case Some(updatedJob) =>
              assert(updatedJob.jobStatus === JobStatus.Initializing)
            case None =>
              throw new AssertionError()
          }
        )
    }
  }

  it should "have the expected JSON payload" in { implicit db =>
    val jobsDAO = new dao.JobsDAO()
    jobsDAO.addJob(Job(randomChannelName, testPayload)).map { job =>
      job.payload shouldEqual JsObject(
        Seq(
          "type" -> JsString("testMessage"),
          "data" -> JsObject(Seq("field1" -> JsNumber(100), "field2" -> JsBoolean(true)))
        )
      )
    }
  }

  it should "find all new jobs" in { implicit db =>
    val jobsDAO     = new dao.JobsDAO()
    val channelName = randomChannelName
    Future
      .sequence(List(jobsDAO.addJob(Job(channelName, testPayload)), jobsDAO.addJob(Job(channelName, testPayload))))
      .flatMap { jobs =>
        // We update one job from "New" to "Initializing" so that when we query the new
        // jobs we should only get 1 such job.
        jobsDAO.updateJob(jobs.head.copy(jobStatus = JobStatus.Initializing)).flatMap { _ =>
          jobsDAO.getNewJobs().map { jobs =>
            jobs.size shouldBe 1
          }
        }
      }

  }

}

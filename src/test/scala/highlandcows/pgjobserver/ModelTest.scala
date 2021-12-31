package highlandcows.pgjobserver

import highlandcows.pgjobserver.helpers.DateExt
import highlandcows.testutil
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json._

import scala.concurrent.{ ExecutionContext, Future }

class ModelTest extends FixtureAsyncFlatSpec with testutil.PgTmpDatabaseFixture {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val testPayload: JsValue = JsObject(
    Seq(
      "type" -> JsString("testMessage"),
      "data" -> JsObject(Seq("field1" -> JsNumber(100), "field2" -> JsBoolean(true)))
    )
  )

  "Jobs repository" should "create a new job" in { implicit db =>
    val jobsDAO = new dao.JobsDAO()
    jobsDAO.addJob(Job(testPayload)).map { job =>
      job.id should not be 0
      job.jobStatus shouldBe JobStatus.New
    }
  }

  it should "create multiple jobs" in { implicit db =>
    val jobsDAO = new dao.JobsDAO()
    Future
      .sequence(
        Seq(jobsDAO.addJob(Job(testPayload)), jobsDAO.addJob(Job(testPayload)), jobsDAO.addJob(Job(testPayload)))
      )
      .flatMap { jobs =>
        jobs.size shouldBe 3
        jobs.forall(_.id != 0) shouldBe true
        jobs.forall(_.jobStatus == JobStatus.New) shouldBe true
      }
  }

  it should "update a job" in { implicit db =>
    val jobsDAO = new dao.JobsDAO()
    jobsDAO.addJob(Job(testPayload)).flatMap { job =>
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
    jobsDAO.addJob(Job(testPayload, JobStatus.New, new java.util.Date().asSqlDate)).map { job =>
      job.payload shouldEqual JsObject(
        Seq(
          "type" -> JsString("testMessage"),
          "data" -> JsObject(Seq("field1" -> JsNumber(100), "field2" -> JsBoolean(true)))
        )
      )
    }
  }

  it should "find all new jobs" in { implicit db =>
    val jobsDAO = new dao.JobsDAO()
    Future
      .sequence(List(jobsDAO.addJob(Job(testPayload)), jobsDAO.addJob(Job(testPayload))))
      .flatMap { jobs =>
        jobsDAO.updateJob(jobs.head.copy(jobStatus = JobStatus.Initializing)).flatMap { _ =>
          jobsDAO.getNewJobs().map { jobs =>
            jobs.size shouldBe 1
          }
        }
      }

  }

}

package highlandcows.pgjobserver.core

import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class JobProcessingTests extends FixtureAsyncFlatSpec with testutil.PgTmpDatabaseFixture {
  import dao.JobsProcessor

  // We need at least 2 threads otherwise our tests will not run.
  override implicit val executionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  "Jobs Processor" should "create the necessary function and trigger" in { implicit db =>
    for {
      _ <- JobsProcessor("test_channel")
      f <- JobsProcessor.isNotifyFunctionDefined("test_channel_notify")
    } yield f shouldBe true
  }

  it should "allow us to create multiple JobsProcessor with different channel names" in { implicit db =>
    for {
      _  <- JobsProcessor("test_channel_01")
      f1 <- JobsProcessor.isNotifyFunctionDefined("test_channel_01_notify")
      f2 <- JobsProcessor.isNotifyFunctionDefined("test_channel_02_notify")
      _  <- JobsProcessor("test_channel_02")
      f3 <- JobsProcessor.isNotifyFunctionDefined("test_channel_02_notify")
    } yield f1 && !f2 && f3 shouldBe true
  }

  it should "create 1 notification for 1 job added" in { implicit db =>
    val jobsDAO = new dao.JobsDAO()
    for {
      jobsProcessor <- JobsProcessor("test_channel")
      _             <- jobsProcessor.startJobNotifications()
      job           <- jobsDAO.addJob(Job("test_channel", testutil.testPayload))
      notifs        <- jobsProcessor.processJobs(500)(identity)
    } yield notifs.size == 1 && notifs.head == job.id shouldBe true

  }

  it should "create 1 notification per channel" in { implicit db =>
    val jobsDAO = new dao.JobsDAO()
    for {
      jobProcessor01 <- JobsProcessor("test_channel_01")
      _              <- jobProcessor01.startJobNotifications()
      jobProcessor02 <- JobsProcessor("test_channel_02")
      _              <- jobProcessor02.startJobNotifications()
      job01          <- jobsDAO.addJob(Job("test_channel_01", testutil.testPayload))
      job02          <- jobsDAO.addJob(Job("test_channel_02", testutil.testPayload))
      notifs01       <- jobProcessor01.processJobs(500)(identity)
      notifs02       <- jobProcessor02.processJobs(500)(identity)
    } yield notifs01.size === 1 && notifs02.size == 1 && notifs01.head == job01.id && notifs02.head == job02.id shouldBe true

  }
}

package highlandcows.pgjobserver.core

import highlandcows.pgjobserver.core.dao.JobsProcessor
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class JobProcessingTests extends FixtureAsyncFlatSpec with testutil.PgTmpDatabaseFixture {

  // We need at least 2 threads otherwise our tests will not run.
  override implicit val executionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  "Jobs Processor" should "create the necessary function and trigger" in { implicit db =>
    for {
      _ <- JobsProcessor("test_channel")
      f <- JobsProcessor.isNotifyFunctionDefined("test_channel_notify")
    } yield f shouldBe true
  }
}

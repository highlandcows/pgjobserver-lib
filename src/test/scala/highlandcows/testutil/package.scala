package highlandcows

import org.scalatest.{ FixtureAsyncTestSuite, FutureOutcome }
import org.slf4j.{ Logger, LoggerFactory }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.io.Source
import scala.language.postfixOps

package object testutil {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  import pgjobserver.helpers.URIExt
  import pgjobserver.repository.PostgresProfile.api._

  def startPgTmp(): String = {
    val p = Runtime.getRuntime.exec("pg_tmp -t")
    Source.fromInputStream(p.getInputStream).getLines().mkString
  }

  def createSchema()(implicit db: Database, ec: ExecutionContext): Future[Unit] = {
    val schema = Source.fromInputStream(getClass.getResourceAsStream("/database/jobs.sql")).getLines().mkString("\n")
    val action = SimpleDBIO(_.connection.createStatement().executeUpdate(schema))
    db.run(action).map(_ => logger.info("Created database schema from jobs.sql"))
  }

  def testDatabase(): Database = {
    val pgTmpUrl = new java.net.URI(testutil.startPgTmp()).toJdbcUrl
    val db = Database.forURL(
      pgTmpUrl,
      executor =
        AsyncExecutor(getClass.getSimpleName, minThreads = 10, maxThreads = 10, queueSize = 1000, maxConnections = 10)
    )
    logger.info(s"Connected to database $pgTmpUrl")
    db
  }

  // This mix-in trait allows us to have a separate database for each test.
  trait PgTmpDatabaseFixture extends FixtureAsyncTestSuite {
    override type FixtureParam = Database
    override def withFixture(testCode: OneArgAsyncTest): FutureOutcome = {
      implicit val db: Database = testutil.testDatabase()
      Await.result(testutil.createSchema(), 5 seconds)
      super.withFixture(testCode.toNoArgAsyncTest(db))
    }
  }
}

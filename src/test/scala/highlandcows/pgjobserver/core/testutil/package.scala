package highlandcows.pgjobserver.core

import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.{ FixtureAsyncTestSuite, FutureOutcome }
import org.slf4j.{ Logger, LoggerFactory }
import play.api.libs.json.{ JsBoolean, JsNumber, JsObject, JsString, JsValue }
import net.ceedubs.ficus.Ficus._

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.io.Source
import scala.language.postfixOps
import scala.util.Random

package object testutil {
  val logger: Logger = LoggerFactory.getLogger("testutil")
  val config: Config = ConfigFactory.load().withFallback(ConfigFactory.systemEnvironment()).resolve()

  lazy val rand                 = new Random()
  def randomChannelName: String = s"ch_${rand.nextInt().toHexString}"

  // We need a semi-random payload for storing when we run the tests.
  val testPayload: JsValue = JsObject(
    Seq(
      "type" -> JsString("testMessage"),
      "data" -> JsObject(Seq("field1" -> JsNumber(100), "field2" -> JsBoolean(true)))
    )
  )

  import helpers.URIExt
  import repository.PostgresProfile.api._

  def startPgTmp(): String = {
    val pgTmpCmd = config.as[String]("tests.pgTmpCmd")
    logger.info(s"Running $pgTmpCmd")
    val p      = Runtime.getRuntime.exec(pgTmpCmd)
    val output = Source.fromInputStream(p.getInputStream).getLines().mkString
    p.waitFor()
    if (p.exitValue() != 0) {
      throw new RuntimeException(s"Failed to connect: $output")
    }
    output
  }

  def createSchema()(implicit db: Database, ec: ExecutionContext): Future[Unit] = {
    val schema = Source.fromInputStream(getClass.getResourceAsStream("/database/jobs.sql")).getLines().mkString("\n")
    val action = SimpleDBIO(_.connection.createStatement().executeUpdate(schema)).map(_ => ())
    db.run(action)
  }

  def testDatabase(): Database = {
    val pgTmpUrl = new java.net.URI(testutil.startPgTmp()).toJdbcUrl(config.as[Option[String]]("tests.database.user"))
    val db = Database.forURL(
      pgTmpUrl,
      executor =
        AsyncExecutor(getClass.getSimpleName, minThreads = 10, maxThreads = 10, queueSize = 1000, maxConnections = 10)
    )
    logger.info(s"Connected to database $pgTmpUrl")
    db
  }

  // This mix-in trait allows us to have a separate database for each test.
  trait PgTmpDatabaseFixture { this: FixtureAsyncTestSuite =>
    override type FixtureParam = Database
    override def withFixture(testCode: OneArgAsyncTest): FutureOutcome = {
      implicit val db: Database = testutil.testDatabase()
      Await.result(testutil.createSchema(), 5 seconds)
      withFixture(testCode.toNoArgAsyncTest(db))
    }
  }
}

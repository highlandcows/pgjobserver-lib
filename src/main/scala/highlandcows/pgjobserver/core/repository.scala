package highlandcows.pgjobserver.core

import com.github.tminglei.slickpg._
import play.api.libs.json.JsValue
import slick.basic.Capability
import slick.jdbc.{ JdbcCapabilities, JdbcType }

import java.sql.Date

object repository {
  import JobStatus.JobStatus

  trait PostgresProfile
      extends ExPostgresProfile
      with PgDateSupport
      with PgDate2Support
      with PgEnumSupport
      with PgPlayJsonSupport {

    override val pgjson = "jsonb"

    override protected def computeCapabilities: Set[Capability] =
      super.computeCapabilities + JdbcCapabilities.insertOrUpdate

    override val api: ApiExt = new ApiExt {}

    trait ApiExt extends API with SimpleDateTimeImplicits with DateTimeImplicits with JsonImplicits {
      implicit val jobStatusTypeMapper: JdbcType[JobStatus]           = createEnumJdbcType("JobStatus", JobStatus)
      implicit val jobStatusListTypeMapper: JdbcType[List[JobStatus]] = createEnumListJdbcType("jobStatus", JobStatus)
    }
  }
  object PostgresProfile extends PostgresProfile

  import PostgresProfile.api._
  class JobsSchema(tag: Tag) extends Table[Job](tag, "jobs") {

    def id: Rep[Int]                = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def channelName: Rep[String]    = column[String]("channel_name")
    def payload: Rep[JsValue]       = column[JsValue]("payload")
    def jobStatus: Rep[JobStatus]   = column[JobStatus]("status")
    def jobStatusUpdated: Rep[Date] = column[Date]("updated")
    def targetId: Rep[Option[Int]]  = column[Option[Int]]("target_id")

    def * = (channelName, payload, jobStatus, jobStatusUpdated, targetId, id) <> (Job.tupled, Job.unapply)
  }
}

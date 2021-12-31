package highlandcows.pgjobserver

import com.github.tminglei.slickpg.{
  ExPostgresProfile,
  PgDate2Support,
  PgDateSupport,
  PgEnumSupport,
  PgPlayJsonSupport
}
import play.api.libs.json.JsValue
import slick.basic.Capability
import slick.jdbc.{ JdbcCapabilities, JdbcType }

import java.sql.Date

import JobStatus.JobStatus

object repository {
  trait PostgresProfile
      extends ExPostgresProfile
      with PgDateSupport
      with PgDate2Support
      with PgEnumSupport
      with PgPlayJsonSupport {

    override val pgjson = "jsonb"

    override protected def computeCapabilities: Set[Capability] =
      super.computeCapabilities + JdbcCapabilities.insertOrUpdate

    override val api: API = new API {}

    trait API extends super.API with SimpleDateTimeImplicits with DateTimeImplicits with JsonImplicits {
      implicit val jobStatusTypeMapper: JdbcType[JobStatus]           = createEnumJdbcType("JobStatus", JobStatus)
      implicit val jobStatusListTypeMapper: JdbcType[List[JobStatus]] = createEnumListJdbcType("jobStatus", JobStatus)
    }
  }
  object PostgresProfile extends PostgresProfile

  import PostgresProfile.api._
  class JobsSchema(tag: Tag) extends Table[Job](tag, "jobs") {

    def id: Rep[Int]                = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def payload: Rep[JsValue]       = column[JsValue]("payload")
    def jobStatus: Rep[JobStatus]   = column[JobStatus]("status")
    def jobStatusUpdated: Rep[Date] = column[Date]("updated")
    def targetId: Rep[Option[Int]]  = column[Option[Int]]("target_id")

    def * = (payload, jobStatus, jobStatusUpdated, targetId, id) <> (Job.tupled, Job.unapply)
  }
}

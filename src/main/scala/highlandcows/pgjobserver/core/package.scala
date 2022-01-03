package highlandcows.pgjobserver

import play.api.libs.json.JsValue

package object core {
  import core.helpers.DateExt

  object JobStatus extends Enumeration {
    type JobStatus = Value

    val New: Value          = Value("new")
    val Initializing: Value = Value("initializing")
    val Initialized: Value  = Value("initialized")
    val Running: Value      = Value("running")
    val Success: Value      = Value("success")
    val Error: Value        = Value("error")
  }

  case class Job(
    channelName: String,
    payload: JsValue,
    jobStatus: JobStatus.Value = JobStatus.New,
    jobStatusUpdated: java.sql.Date = new java.util.Date().asSqlDate,
    targetId: Option[Int] = None,
    id: Int = 0
  )
}

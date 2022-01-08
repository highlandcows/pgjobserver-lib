package highlandcows.pgjobserver.core

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.matching.Regex

package object helpers {
  implicit class FutureExt[T](future: Future[T]) {
    def asUnit(implicit ec: ExecutionContext): Future[Unit] = future.map(_ => ())
  }

  implicit class DateExt(date: java.util.Date) {
    def asSqlDate = new java.sql.Date(date.getTime)
  }

  implicit class URIExt(uri: java.net.URI) {
    def toJdbcUrl(user: Option[String] = None): String =
      s"jdbc:${uri.getScheme}://${uri.getHost}:${uri.getPort}${uri.getPath}${user.map(u => s"?user=$u").getOrElse("")}"
  }

  implicit class StringExt(s: String) {
    val snakeCaseRE: Regex = "([a-z])([A-Z])".r

    def toSnakeCase: String = snakeCaseRE.replaceAllIn(s, "$1_$2").toLowerCase
  }

}

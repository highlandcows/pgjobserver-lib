package highlandcows.pgjobserver

import scala.concurrent.{ ExecutionContext, Future }

package object helpers {
  implicit class FutureExt[T](future: Future[T]) {
    def asUnit(implicit ec: ExecutionContext): Future[Unit] = future.map(_ => ())
  }

  implicit class DateExt(date: java.util.Date) {
    def asSqlDate = new java.sql.Date(date.getTime)
  }

  implicit class URIExt(uri: java.net.URI) {
    def toJdbcUrl: String = s"jdbc:${uri.getScheme}://${uri.getHost}:${uri.getPort}${uri.getPath}"
  }

}

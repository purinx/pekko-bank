package bank.util.db

import zio.{Exit, Runtime, Unsafe, ZIO, ZLayer}
import java.sql.Connection
import javax.sql.DataSource
import scala.concurrent.Future

class DBIORunner(dataSource: DataSource, runtime: Runtime[Any]) {

  private val connectionLayer: ZLayer[Any, Throwable, Connection] = ZLayer.scoped {
    ZIO.fromAutoCloseable(ZIO.attempt(dataSource.getConnection)).flatMap { connection =>
      ZIO.acquireReleaseExit(ZIO.attempt {
        connection.setAutoCommit(false)
        connection
      }) { (conn, exit) =>
        exit match {
          case Exit.Success(_) => ZIO.attempt(conn.commit()).orDie
          case Exit.Failure(_) => ZIO.attempt(conn.rollback()).orDie
        }
      }
    }
  }

  def run[E, A](io: ZIO[Connection, E, A]): Future[Either[Throwable, A]] = {
    val program: ZIO[Any, Throwable, A] = io.provide(connectionLayer).mapError {
      case e: Throwable => e
      case e            => new RuntimeException(s"Non-throwable error: $e")
    }
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.runToFuture(program.either)
    }
  }
}

package bank.util.db

import zio.{ZIO, ZLayer, ULayer, IO}
import java.sql.Connection
import javax.sql.DataSource

class DBIORunner(dataSource: DataSource) {

  private val connectionLayer: ULayer[Connection] = ZLayer.scoped {
    for {
      connection <- ZIO
        .fromAutoCloseable(
          ZIO.succeedBlocking(dataSource.getConnection()),
        )
        .withFinalizerExit { (c, exit) =>
          if (exit.isSuccess) ZIO.succeedBlocking(c.commit()) else ZIO.succeedBlocking(c.rollback())
        }
      _ <- ZIO.succeedBlocking(connection.setReadOnly(false))
      _ <- ZIO.succeedBlocking(connection.setAutoCommit(false))
    } yield connection
  }

  def runTx[E, A](io: DBIO[E, A]): IO[E, A] = {
    io.provide(connectionLayer)
  }

}

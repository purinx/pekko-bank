package bank.util.db

import zio.{ZIO, ZLayer, ULayer, IO, Task}
import java.sql.Connection
import javax.sql.DataSource
import doobie.util.transactor.Transactor

class DBIORunner(xa: Transactor[Task]) {

  def runTx[E, A](io: DBIO[E, A]): IO[E, A] = {
    io.provide {
      ZLayer.scoped(xa)
    }
  }

}

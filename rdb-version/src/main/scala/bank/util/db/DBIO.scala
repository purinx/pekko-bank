package bank.util.db

import zio.{Task, ZIO}
import java.sql.Connection
import doobie.implicits._
import doobie.util.transactor.Transactor
import zio.interop.catz._

type DBIO[E, A] = ZIO[Connection, E, A]

object DBIO {
  def transact[A](io: doobie.ConnectionIO[A]): DBIO[Throwable, A] = {
    for {
      conn <- ZIO.service[Connection]
      xa = Transactor.fromConnection[Task](conn, None)
      a <- io.transact(xa)
    } yield a
  }
}

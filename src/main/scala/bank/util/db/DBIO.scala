package bank.util.db

import zio.{Task, ZIO}
import java.sql.Connection

type DBIO[E, A] = ZIO[Connection, E, A]
type DBTask[A]  = ZIO[Connection, Throwable, A]
type DBUIO[A]   = ZIO[Connection, Nothing, A]

object DBIO {

  def withDoobie[A](io: doobie.ConnectionIO[A]): DBTask[A] = {

    import zio.interop.catz.*
    import doobie.util.transactor.Transactor
    import doobie.implicits.*

    for {
      connection <- ZIO.service[Connection]
      a          <- {
        val xa: Transactor[Task] = Transactor.fromConnection[Task](connection, logHandler = None)
        io.transact(xa)
      }
    } yield a
  }

  def withDoobieSucceed[A](io: doobie.ConnectionIO[A]): DBUIO[A] = withDoobie(io).orDie

}

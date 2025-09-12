package bank.util.db

import zio.{Task, ZIO}
import doobie.util.transactor.Transactor
import zio.interop.catz.*

type DBIO[E, A] = ZIO[Transactor[Task], E, A]
type DBTask[A]  = ZIO[Transactor[Task], Throwable, A]
type DBUIO[A]   = ZIO[Transactor[Task], Nothing, A]

object DBIO {

  def withDoobie[A](io: doobie.ConnectionIO[A]): DBTask[A] = {

    import doobie.implicits.*

    for {
      xa <- ZIO.service[Transactor[Task]]
      a  <- {
        io.transact(xa)
      }
    } yield a
  }

  def withDoobieSucceed[A](io: doobie.ConnectionIO[A]): DBUIO[A] = withDoobie(io).orDie

}

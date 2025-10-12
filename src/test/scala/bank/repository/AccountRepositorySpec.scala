package bank.repository

import bank.domain.account.{Account, AccountId}
import bank.util.db.{DBIO, DBIORunner}
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import munit.FunSuite
import zio.{Exit, FiberFailure, Runtime, Unsafe}

import java.sql.Connection
import javax.sql.DataSource

class AccountRepositorySpec extends FunSuite {

  private val repository = AccountRepositoryImpl

  private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.builder().start()
  private val dataSource: DataSource             = embeddedPostgres.getPostgresDatabase()
  private val runner: DBIORunner                 = new DBIORunner(dataSource)

  private val runtime = Runtime.default

  override def beforeAll(): Unit = {
    super.beforeAll()
    initializeSchema()
  }

  override def beforeEach(context: BeforeEach): Unit = {
    initializeSchema()
  }

  override def afterAll(): Unit = {
    try Option(embeddedPostgres).foreach(_.close())
    finally super.afterAll()
  }

  private def initializeSchema(): Unit = {
    val statements = Seq(
      """DROP TABLE IF EXISTS accounts;""",
      """DROP TYPE IF EXISTS account_status;""",
      """CREATE TYPE account_status AS ENUM ('ACTIVE', 'FROZEN', 'CLOSED');""",
      """CREATE TABLE accounts (
        id          uuid PRIMARY KEY,
        owner_name  text NOT NULL,
        currency    char(3) NOT NULL,
        status      account_status NOT NULL,
        version     bigint NOT NULL,
        created_at  timestamptz NOT NULL DEFAULT now()
      );""",
    )

    withConnection { connection =>
      val stmt = connection.createStatement()
      try statements.foreach(sql => stmt.execute(sql))
      finally stmt.close()
    }
  }

  private def withConnection[A](f: Connection => A): A = {
    val connection = dataSource.getConnection()
    try f(connection)
    finally connection.close()
  }

  private def run[E, A](io: DBIO[E, A]): Either[E, A] =
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.run(runner.runTx(io).either) match
        case Exit.Success(value) => value
        case Exit.Failure(cause) => throw FiberFailure(cause)
    }

  private def runOrFail[E, A](io: DBIO[E, A]): A =
    run(io) match
      case Right(value) => value
      case Left(err)    => fail(s"expected success but got error: $err")

  test("all returns an empty list when no accounts exist") {
    val accounts = runOrFail(repository.all())
    assertEquals(accounts, List.empty[Account])
  }

  test("create persists an account and findBy returns the stored record") {
    val account = Account.create("Alice")

    val (count, stored) = runOrFail {
      for {
        inserted <- repository.create(account)
        fetched  <- repository.findBy(account.id)
      } yield (inserted, fetched)
    }

    assertEquals(count, 1)
    assertEquals(stored.id, account.id)
    assertEquals(stored.ownerName, account.ownerName)
    assertEquals(stored.currency, account.currency)
    assertEquals(stored.status, account.status)
    assertEquals(stored.version, account.version)
  }

  test("findBy returns NotFoundError when the account does not exist") {
    val missingId = AccountId.newId()

    val result = run(repository.findBy(missingId))

    assertEquals(result, Left(NotFoundError))
  }

  test("all returns every stored account") {
    val accountA = Account.create("Alice")
    val accountB = Account.create("Bob")

    val accounts = runOrFail {
      for {
        _      <- repository.create(accountA)
        _      <- repository.create(accountB)
        values <- repository.all()
      } yield values
    }

    val ids = accounts.map(_.id).toSet
    assertEquals(ids, Set(accountA.id, accountB.id))
  }
}

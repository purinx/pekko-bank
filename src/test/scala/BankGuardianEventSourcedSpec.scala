package bank

import munit.FunSuite
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import bank.domain.account.AccountId
import com.typesafe.config.ConfigFactory

class BankGuardianEventSourcedSpec extends FunSuite {

  private val testConfig = EventSourcedBehaviorTestKit.config.withFallback(
    ConfigFactory.parseString(
      """
      pekko.actor.allow-java-serialization = on
      pekko.actor.warn-about-java-serializer-usage = off
      """,
    ),
  )
  private val testKit = ActorTestKit(testConfig)

  override def afterAll(): Unit = {
    testKit.shutdownTestKit()
    super.afterAll()
  }

  private def newESKit() =
    EventSourcedBehaviorTestKit[
      BankAccount.Command,
      BankAccount.Event,
      BankAccount.BalanceState,
    ](
      testKit.system,
      BankGuardian.applyEventSourcedBehavior(AccountId.newId()),
    )

  test("入金で残高が増え、イベントと返信が正しい") {
    val es = newESKit()

    val r1 = es.runCommand[BankAccount.OperationResult](reply => BankAccount.Deposit(100, reply))
    assertEquals(r1.reply, BankAccount.OperationSucceeded(100L))
    assertEquals(r1.event, BankAccount.Deposited(100))
    assertEquals(r1.state.currentBalance.balance, 100L)
  }

  test("出金で残高が減り、イベントと返信が正しい") {
    val es = newESKit()

    es.runCommand[BankAccount.OperationResult](reply => BankAccount.Deposit(120, reply))
    val r2 = es.runCommand[BankAccount.OperationResult](reply => BankAccount.Withdraw(20, reply))

    assertEquals(r2.reply, BankAccount.OperationSucceeded(100L))
    assertEquals(r2.event, BankAccount.Withdrew(20))
    assertEquals(r2.state.currentBalance.balance, 100L)
  }

  test("残高不足の出金は失敗しイベントは発生しない") {
    val es = newESKit()

    es.runCommand[BankAccount.OperationResult](reply => BankAccount.Deposit(50, reply))
    val r = es.runCommand[BankAccount.OperationResult](reply => BankAccount.Withdraw(60, reply))

    assert(r.reply.isInstanceOf[BankAccount.OperationFailed])
    assertEquals(r.events.size, 0)
    assertEquals(r.state.currentBalance.balance, 50L)
  }

  test("0以下の入金は失敗しイベントは発生しない") {
    val es = newESKit()

    val r = es.runCommand[BankAccount.OperationResult](reply => BankAccount.Deposit(0, reply))

    assert(r.reply.isInstanceOf[BankAccount.OperationFailed])
    assertEquals(r.events.size, 0)
    assertEquals(r.state.currentBalance.balance, 0L)
  }

  test("残高照会は現在値を返信し、GotBalanceイベントを記録する") {
    val es = newESKit()

    es.runCommand[BankAccount.OperationResult](reply => BankAccount.Deposit(70, reply))
    val r = es.runCommand[BankAccount.CurrentBalance](reply => BankAccount.GetBalance(reply))

    assertEquals(r.reply, BankAccount.CurrentBalance(70L))
    assertEquals(r.event, BankAccount.GotBalance())
    assertEquals(r.state.currentBalance.balance, 70L)
  }
}

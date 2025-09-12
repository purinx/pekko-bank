package bank

import munit.FunSuite
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import bank.domain.account.AccountId
import com.typesafe.config.ConfigFactory
import bank.actor.AccountActor
import bank.actor.BankGuardian

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
      AccountActor.Command,
      BankGuardian.Event,
      AccountActor.BalanceState,
    ](
      testKit.system,
      BankGuardian.applyEventSourcedBehavior(AccountId.newId()),
    )

  test("入金で残高が増え、イベントと返信が正しい") {
    val es = newESKit()

    val r1 = es.runCommand[AccountActor.OperationResult](reply => AccountActor.Deposit(100, reply))
    assertEquals(r1.reply, AccountActor.OperationSucceeded(100L))
    assertEquals(r1.event, BankGuardian.Deposited(100))
    assertEquals(r1.state.currentBalance.balance, 100L)
  }

  test("出金で残高が減り、イベントと返信が正しい") {
    val es = newESKit()

    es.runCommand[AccountActor.OperationResult](reply => AccountActor.Deposit(120, reply))
    val r2 = es.runCommand[AccountActor.OperationResult](reply => AccountActor.Withdraw(20, reply))

    assertEquals(r2.reply, AccountActor.OperationSucceeded(100L))
    assertEquals(r2.event, BankGuardian.Withdrew(20))
    assertEquals(r2.state.currentBalance.balance, 100L)
  }

  test("残高不足の出金は失敗しイベントは発生しない") {
    val es = newESKit()

    es.runCommand[AccountActor.OperationResult](reply => AccountActor.Deposit(50, reply))
    val r = es.runCommand[AccountActor.OperationResult](reply => AccountActor.Withdraw(60, reply))

    assert(r.reply.isInstanceOf[AccountActor.OperationFailed])
    assertEquals(r.events.size, 0)
    assertEquals(r.state.currentBalance.balance, 50L)
  }

  test("0以下の入金は失敗しイベントは発生しない") {
    val es = newESKit()

    val r = es.runCommand[AccountActor.OperationResult](reply => AccountActor.Deposit(0, reply))

    assert(r.reply.isInstanceOf[AccountActor.OperationFailed])
    assertEquals(r.events.size, 0)
    assertEquals(r.state.currentBalance.balance, 0L)
  }

  test("残高照会は現在値を返信し、GotBalanceイベントを記録する") {
    val es = newESKit()

    es.runCommand[AccountActor.OperationResult](reply => AccountActor.Deposit(70, reply))
    val r = es.runCommand[AccountActor.CurrentBalance](reply => AccountActor.GetBalance(reply))

    assertEquals(r.reply, AccountActor.CurrentBalance(70L))
    assertEquals(r.event, BankGuardian.GotBalance())
    assertEquals(r.state.currentBalance.balance, 70L)
  }
}

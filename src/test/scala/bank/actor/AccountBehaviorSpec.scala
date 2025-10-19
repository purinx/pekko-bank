package bank.actor

import bank.actor.AccountBehavior.*
import bank.domain.account.Account
import com.typesafe.config.ConfigFactory
import munit.FunSuite
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit

import java.util.UUID

class AccountBehaviorSpec extends FunSuite {
  private val config =
    EventSourcedBehaviorTestKit.config.withFallback(ConfigFactory.load())

  private val testKit = ActorTestKit("AccountBehaviorSpec", config)

  override def afterAll(): Unit = {
    super.afterAll()
    testKit.shutdownTestKit()
  }

  test("create command succeeds once and rejects duplicates") {
    val accountId       = randomAccountId()
    val behaviorTestKit = behaviorKit(accountId)
    val owner           = "Alice"

    val createdResult  = behaviorTestKit.runCommand[OperationResult](reply => Create(owner, reply))
    val createdAccount = expectAccountCreated(createdResult.reply)
    assertEquals(createdAccount.ownerName, owner)
    assertEquals(createdAccount.id.asString, accountId)

    val duplicateResult = behaviorTestKit.runCommand[OperationResult](reply => Create(owner, reply))
    val duplicateReply  = expectFailed(duplicateResult.reply)
    assertEquals(duplicateReply.reason, "Account already created")
  }

  test("deposit accepts positive amounts and tracks running balance") {
    val behaviorTestKit = behaviorKit(randomAccountId())
    behaviorTestKit.runCommand[OperationResult](reply => Create("Bob", reply))

    val firstDeposit = behaviorTestKit.runCommand[OperationResult](reply => Deposit(100, reply))
    val firstReply   = expectSucceeded(firstDeposit.reply)
    assertEquals(firstReply.balance, BigDecimal(100))
    val firstState = expectCreatedState(firstDeposit.state)
    assertEquals(firstState.balance, BigDecimal(100))

    val secondDeposit = behaviorTestKit.runCommand[OperationResult](reply => Deposit(40, reply))
    val secondReply   = expectSucceeded(secondDeposit.reply)
    assertEquals(secondReply.balance, BigDecimal(140))
    val secondState = expectCreatedState(secondDeposit.state)
    assertEquals(secondState.balance, BigDecimal(140))
  }

  test("deposit rejects non positive amounts") {
    val behaviorTestKit = behaviorKit(randomAccountId())
    behaviorTestKit.runCommand[OperationResult](reply => Create("Carol", reply))

    val zeroDeposit = behaviorTestKit.runCommand[OperationResult](reply => Deposit(0, reply))
    val zeroReply   = expectFailed(zeroDeposit.reply)
    assertEquals(zeroReply.reason, "Amount must be positive")

    val negativeDeposit = behaviorTestKit.runCommand[OperationResult](reply => Deposit(-1, reply))
    val negativeReply   = expectFailed(negativeDeposit.reply)
    assertEquals(negativeReply.reason, "Amount must be positive")
  }

  test("recovery replays persisted events across restarts") {
    val behaviorTestKit = behaviorKit(randomAccountId())
    behaviorTestKit.runCommand[OperationResult](reply => Create("Dave", reply))
    behaviorTestKit.runCommand[OperationResult](reply => Deposit(200, reply))

    behaviorTestKit.restart()

    val withdraw = behaviorTestKit.runCommand[OperationResult](reply => Withdraw(200, reply))
    val reply    = expectSucceeded(withdraw.reply)
    assertEquals(reply.balance, BigDecimal(0))
    val stateAfterWithdraw = expectCreatedState(withdraw.state)
    assertEquals(stateAfterWithdraw.balance, BigDecimal(0))
  }

  test("withdraw fails when amount exceeds available balance") {
    val behaviorTestKit = behaviorKit(randomAccountId())
    behaviorTestKit.runCommand[OperationResult](reply => Create("Eve", reply))
    behaviorTestKit.runCommand[OperationResult](reply => Deposit(50, reply))

    val withdraw = behaviorTestKit.runCommand[OperationResult](reply => Withdraw(100, reply))
    val failed   = expectFailed(withdraw.reply)
    assertEquals(failed.reason, "Insufficient balance")
    val stateAfterAttempt = expectCreatedState(withdraw.state)
    assertEquals(stateAfterAttempt.balance, BigDecimal(50))
  }

  private def behaviorKit(accountId: String) =
    EventSourcedBehaviorTestKit[Command, Event, State](testKit.system, AccountBehavior(accountId))

  private def randomAccountId(): String =
    UUID.randomUUID().toString

  private def expectAccountCreated(result: OperationResult): Account =
    result match {
      case AccountCreated(account) => account
      case other                   => fail(s"Expected AccountCreated but received $other")
    }

  private def expectSucceeded(result: OperationResult): OperationSucceeded =
    result match {
      case s: OperationSucceeded => s
      case other                 => fail(s"Expected OperationSucceeded but received $other")
    }

  private def expectFailed(result: OperationResult): OperationFailed =
    result match {
      case f: OperationFailed => f
      case other              => fail(s"Expected OperationFailed but received $other")
    }

  private def expectCreatedState(state: State): CreatedState =
    state match {
      case created: CreatedState => created
      case other                 => fail(s"Expected CreatedState but received $other")
    }
}

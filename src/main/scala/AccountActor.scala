
import org.apache.pekko.actor.{Actor, Props}

object AccountActor {
  sealed trait Command
  case class Apply(accountInfo: AccountInfo) extends Command
  case class Applied(accountInfo: AccountInfo) extends Event
  case class Rejected(accountInfo: AccountInfo, reason: String) extends Event

  sealed trait Event
  case class Get() extends Command

  def props(accountRepository: AccountRepository): Props =
    Props(new AccountActor(accountRepository))
}

class AccountActor(accountRepository: AccountRepository) extends Actor {

  override def receive: Receive = ???


//  with Stash
//  with ActorLogging {
//    import CredibilityActor._
//    import context.dispatcher
//
//    private val credibilityId = CredibilityId(self.path.name)
//    private var credibilityInformation = CredibilityInformation.empty
//
//    override def preStart(): Unit = {
//      super.preStart()
//
//      credibilityRepository.findCredibility(credibilityId).map {
//        credibilityInfo =>
//          log.info(s"Credibility Information Loaded For ${credibilityId.value}")
//          credibilityInfo
//      }.recover {
//        case _ =>
//          log.info(s"Creating New Credibility Account For ${credibilityId.value}.")
//          CredibilityInformation.empty
//      }.pipeTo(self)
//    }
//
//    override def receive: Receive = initializing
//
//    private def initializing: Receive = {
//      case credibilityInfo: CredibilityInformation =>
//        credibilityInformation = credibilityInfo
//        context.become(running)
//        unstashAll()
//      case _ =>
//        stash()
//    }
//
//    private def running: Receive = {
//      case cmd: ApplyCredibilityAdjustment => handle(cmd)
//      case GetCredibilityInformation() =>
//        log.info(s"Retrieving Credibility Information For ${credibilityId.value}")
//        sender() ! credibilityInformation
//    }
//
//    private def handle(cmd: ApplyCredibilityAdjustment) = {
//      cmd match {
//        case ApplyCredibilityAdjustment(Debit(money)) if money > credibilityInformation.currentTotal =>
//          log.info(s"Insufficient Money For ${credibilityId.value}")
//          sender() ! CredibilityAdjustmentRejected(Debit(money), "Insufficient Money")
//        case ApplyCredibilityAdjustment(adjustment) =>
//          log.info(s"Applying $adjustment for ${credibilityId.value}")
//          credibilityInformation = credibilityInformation.applyAdjustment(adjustment)
//          credibilityRepository.updateCredibility(credibilityId, credibilityInformation).map { _ =>
//            CredibilityAdjustmentApplied(adjustment)
//          }.pipeTo(sender())
//      }
//    }
}



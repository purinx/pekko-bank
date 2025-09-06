import org.apache.pekko.actor.{Actor, ActorRef, Props}
import org.apache.pekko.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId}

class AccountActorSupervisor(accountRepository: AccountRepository)  extends Actor{
  import AccountActorSupervisor.*
  
  protected def createAccountActor(name: String): ActorRef = 
    context.actorOf(AccountActor.props(accountRepository), name)
  

  override def receive: Receive = {
    case Deliver(command, to) =>
      val accountActor = context.child(to.value)
        .getOrElse(createAccountActor(to.value))

      accountActor.forward(command)
  }
}

object AccountActorSupervisor {
  case class Deliver(command: AccountActor.Command, to: AccountId) extends SerializableMessage

  def props(accountRepository: AccountRepository): Props =
    Props(new AccountActorSupervisor(accountRepository))

  val idExtractor: ExtractEntityId = {
    case Deliver(msg, id) => (id.value.toString, msg)
  }

  val shardIdExtractor: ExtractShardId = {
    case Deliver(_, id) =>
      (Math.abs(id.value.hashCode) % 30).toString
  }
}
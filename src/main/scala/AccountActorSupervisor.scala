import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}

object AccountActorSupervisor {
  sealed trait Command
  case class Deliver(command: AccountActor.Command, to: AccountId) extends Command

  val typeKey =
    EntityTypeKey[AccountActor.Command]("Account")

  def apply(accountRepository: AccountRepository): Behavior[Command] =
    Behaviors.setup { context =>
      val sharding = ClusterSharding(context.system)

      sharding.init(Entity(typeKey) { entityContext =>
        AccountActor(
          AccountId(entityContext.entityId),
          accountRepository,
        )
      })

      Behaviors.receiveMessage { case Deliver(command, to) =>
        val entityRef = sharding.entityRefFor(typeKey, to.value)
        entityRef ! command
        Behaviors.same
      }
    }
}

package bank.util.actor

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior, RecipientRef}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.util.Timeout
import scala.concurrent.{Future, Promise}
import java.util.concurrent.TimeoutException
import scala.reflect.ClassTag

object FilterableAskPattern {

  private case class ReceiveTimeout()

  implicit class FilterableAskable[Req](private val ref: RecipientRef[Req]) extends AnyVal {

    def askExpecting[Res: ClassTag](
        createRequest: ActorRef[Res] => Req,
    )(
        predicate: Res => Boolean,
    )(implicit timeout: Timeout, system: ActorSystem[?]): Future[Res] = {

      val promise = Promise[Res]()

      val temporaryActorBehavior: Behavior[Res | ReceiveTimeout] = Behaviors.withTimers { timers =>
        // タイムアウトの設定
        timers.startSingleTimer(ReceiveTimeout(), timeout.duration)

        Behaviors.receiveMessage {
          case response: Res =>
            if (predicate(response)) {
              promise.trySuccess(response)
              Behaviors.stopped // 成功したら停止
            } else {
              Behaviors.same // 条件に合わなければ次のメッセージを待つ
            }
          case ReceiveTimeout() =>
            promise.tryFailure(new TimeoutException(s"Ask timed out on [$ref] after [$timeout]"))
            Behaviors.stopped
        }
      }

      val temporaryActor: ActorRef[Res | ReceiveTimeout] =
        system.systemActorOf(temporaryActorBehavior, s"askExpecting-${java.util.UUID.randomUUID()}")

      val replyTo: ActorRef[Res] = temporaryActor.narrow[Res]

      ref ! createRequest(replyTo)

      promise.future
    }
  }
}

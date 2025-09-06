import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.ma

@main def hello(): Unit =
  println("Hello world!")
  println(msg)

def msg = "I was compiled by Scala 3. :)"


given system: ActorSystem = ActorSystem("Account")

AkkaManagement(system).start()

lazy val accountRepository: AccountRepository = new InMemoryAccountRepositoryImpl(using system.dispatchers)
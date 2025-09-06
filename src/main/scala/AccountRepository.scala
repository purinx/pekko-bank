import org.apache.pekko.Done

import scala.concurrent.{ExecutionContext, Future}

trait AccountRepository {
  def findBy(accountId: AccountId): Future[Option[AccountInfo]]
  def storeAccount(accountId: AccountId, accountInfo: AccountInfo): Future[Done]
}

class InMemoryAccountRepositoryImpl(using ExecutionContext) extends AccountRepository {
  private var data: Map[AccountId, AccountInfo] = Map.empty

  override def findBy(accountId: AccountId): Future[Option[AccountInfo]] = Future {
    data.get(accountId)
  }

  override def storeAccount(accountId: AccountId, accountInfo: AccountInfo): Future[Done] = Future {
    data = data.updated(accountId, accountInfo)
    Done
  }
}

sealed trait AccountLedger {
  def value: Int
}

// 入金
case class Credit(money: Int) extends AccountLedger {
  // TODO: moneyの値がnonNegIntなのを確認する
  override def value: Int = money
}

// 出金
case class Deposit(money: Int) extends AccountLedger {
  // TODO: moneyの値がnegIntなのを確認する
  override def value: Int = money
}

// 口座情報
case class AccountInfo(accountId: AccountId, accountLedgers: Seq[AccountLedger]) {
  // 現在の口座残高情報
  // 出金と入金を畳み込んで計算しているが、本来はプロジェクションを利用するのでミドルウェアからデータを取得するようにする
  private val currentMoney = this.accountLedgers.foldLeft(0)(_ + _.value)

  // 口座に入出金情報を追加する
  def applyLedger(accountLedger: AccountLedger): AccountInfo =
    this.copy(accountId = this.accountId, accountLedgers = this.accountLedgers :+ accountLedger)

}

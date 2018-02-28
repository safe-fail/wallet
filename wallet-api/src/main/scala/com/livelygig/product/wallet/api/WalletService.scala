package com.livelygig.product.content.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.{ Service, ServiceCall }
import com.livelygig.product.shared.models.wallet._
trait WalletService extends Service {

  def ping(): ServiceCall[String, String]

  //Non-secured API's for Mobile App

  def mobileGetETHNetConnected(): ServiceCall[NotUsed, String]

  def mobileGetAccountDetails(publicKey: String): ServiceCall[NotUsed, UserDetails]

  def mobileGetAccountTransactionHistory(publicKey: String): ServiceCall[Seq[ERC20ComplientToken], Seq[TransactionWithSymbol]]

  def mobileGetTransactionStatus(txnHash: String): ServiceCall[NotUsed, String]

  def mobileAccountTokensDetails(publicKey: String): ServiceCall[Seq[ERC20ComplientToken], Seq[ERC20ComplientToken]]

  def mobileGetNonce(publicKey: String): ServiceCall[EtherTransaction, SignedTxnParams]

  def mobileSendSignedTransaction(): ServiceCall[String, String]

  def getETHNetConnected(): ServiceCall[NotUsed, String]

  def descriptor = {
    import Service._
    named("wallet").withCalls(
      namedCall("/api/wallet/ping", ping _),
      pathCall("/api/wallet/ethnet/info", getETHNetConnected _),
      //Client Authenticated API's pathCall for Mobile App
      pathCall("/api/wallet/mobile/ethnet/info", mobileGetETHNetConnected _),
      pathCall("/api/wallet/mobile/:publicKey/account/details", mobileGetAccountDetails _),
      pathCall("/api/wallet/mobile/:publicKey/account/erctoken/details", mobileAccountTokensDetails _),
      pathCall("/api/wallet/mobile/status/:txnHash", mobileGetTransactionStatus _),
      pathCall("/api/wallet/mobile/:publicKey/transactions/", mobileGetAccountTransactionHistory _),
      pathCall("/api/wallet/mobile/:publicKey/nonce/", mobileGetNonce _),
      pathCall("/api//wallet/mobile/signedTxn", mobileSendSignedTransaction _))
  }
}

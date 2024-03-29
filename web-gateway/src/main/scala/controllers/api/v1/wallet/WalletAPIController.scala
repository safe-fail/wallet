package controllers.api.v1.wallet

import com.livelygig.product.content.api.WalletService
import com.livelygig.shared.models.wallet.{ EtherTransaction, SignedTransaction }
import net.ceedubs.ficus.Ficus._
import play.api.libs.json.Json
import play.api.mvc.{ BaseController, ControllerComponents }
import play.api.{ Configuration, Environment, Mode }
import utils.Mocker
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Created by shubham.k on 27-02-2017.
 */

class WalletAPIController(
  walletService: WalletService,
  val controllerComponents: ControllerComponents,
  configuration: Configuration,
  environment: Environment)(implicit val ec: ExecutionContext) extends BaseController with Mocker {

  override val mockdataLocation: String = {
    if (environment.mode == Mode.Dev) {
      "web-gateway/src/main/scala/mockdata"
    } else {
      //s"${System.getProperty("user.home")}/work/livelygig/mockdata"
      "/home/ubuntu/work/livelygig/mockdata"
    }
  }
  val i18ndataLocation: String = {
    if (environment.mode == Mode.Dev) {
      "web-gateway/src/main/scala/i18n/wallet/"
    } else {
      //s"${System.getProperty("user.home")}/work/livelygig/mockdata"
      "/home/ubuntu/work/livelygig/mockdata/languages/"
    }
  }

  private val isMock: Boolean = configuration.underlying.as[Boolean]("appsettings.ismock")

  def mobileGetTransactionHistory(publicKey: String) = Action.async { implicit request =>
    val ercComplientTokenList = getMockWalletTokenListResponse("accountTokensDetails")
    walletService
      .mobileGetAccountTransactionHistory(publicKey)
      .invoke(ercComplientTokenList)
      .map(e => Ok(Json.toJson(e)).withHeaders("Access-Control-Allow-Origin" -> "*"))
  }

  def mobileGetAccountTokenDetails(publicKey: String) = Action.async { implicit request =>
    val ercComplientTokenList = getMockWalletTokenListResponse("accountTokensDetails")
    walletService
      .mobileAccountTokensDetails(publicKey)
      .invoke(ercComplientTokenList)
      .map(e => Ok(Json.toJson(e)).withHeaders("Access-Control-Allow-Origin" -> "*"))
  }

  def mobileGetMarketPrice() = Action.async { implicit request =>
    val currList = getLiveMarketPrices()
    Future(
      Ok(
        Json.toJson(currList)).withHeaders("Access-Control-Allow-Origin" -> "*"))
  }

  def mobileGetNetworkInfo() = Action.async { implicit request =>
    walletService.mobileGetETHNetConnected()
      .invoke()
      .map(e => Ok(e).withHeaders("Access-Control-Allow-Origin" -> "*"))
  }

  def mobileGetTransactionStatus(txnHash: String) = Action.async { implicit request =>
    walletService
      .mobileGetTransactionStatus(txnHash)
      .invoke()
      .map(e => Ok(e).withHeaders("Access-Control-Allow-Origin" -> "*"))
  }

  def language(lang: String) = Action.async { implicit request =>
    val mockdata =
      scala
        .io
        .Source.fromFile(s"${i18ndataLocation + lang}.json", "utf-8").mkString
    Future(Ok(mockdata.toString).withHeaders("Access-Control-Allow-Origin" -> "*"))
  }

  def getEncodedFunction(etherTransaction: EtherTransaction) = {
    walletService
      .mobileGetEncodedFunction()
      .invoke(etherTransaction)
      .map(e => Ok(Json.toJson(e)).withHeaders("Access-Control-Allow-Origin" -> "*"))
  }

  def mobilePost() = Action.async { implicit request =>

    request.body.asJson match {
      case Some(json) => {
        Json.fromJson[EtherTransaction](json).fold(
          _ => Future.successful(BadRequest("Error in parsing json")),
          etherTxnModal => getEncodedFunction(etherTxnModal))
      }
      case None => Future.successful(BadRequest("No Json Found"))
    }

  }

  def mobileSendTransaction() = Action.async { implicit request =>

    request.body.asJson match {
      case Some(json) => {
        Json.fromJson[SignedTransaction](json).fold(
          _ => Future.successful(BadRequest("Error in parsing json")),
          signedTxn => mobilePostTransaction(signedTxn.data))
      }
      case None => Future.successful(BadRequest("No Json Found"))
    }

  }

  def mobilePostTransaction(signedTxn: String) = {
    walletService
      .mobileSendSignedTransaction()
      .invoke(signedTxn)
      .map(e => Ok(e).withHeaders("Access-Control-Allow-Origin" -> "*"))
  }
}

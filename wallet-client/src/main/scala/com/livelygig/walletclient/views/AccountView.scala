package com.livelygig.walletclient.views

import com.livelygig.shared.models.wallet._
import com.livelygig.walletclient.facades.jquery.JQueryFacade.jQuery
import com.livelygig.walletclient.facades.{ Toastr, WalletJS }
import com.livelygig.walletclient.handler.{ GetCurrencies, UpdateAccountTokenList }
import com.livelygig.walletclient.rootmodel.TokenDetailsRootModel
import com.livelygig.walletclient.router.ApplicationRouter.{ Loc, _ }
import com.livelygig.walletclient.services.{ CoreApi, WalletCircuit }
import diode.AnyAction._
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^.{ ^, _ }
import japgolly.scalajs.react.{ BackendScope, Callback, ScalaComponent, _ }
import org.scalajs.dom
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
//import japgolly.scalajs.react.vdom.Implicits._

object AccountView {

  // Toastr options
  Toastr.options.timeOut = 5000; // How long the toast will display without user interaction
  Toastr.options.extendedTimeOut = 60; // How long the toast will display after a user hovers over it
  Toastr.options.closeButton = true
  Toastr.options.positionClass = "toast-top-full-width"

  case class Props(proxy: ModelProxy[Pot[TokenDetailsRootModel]], router: RouterCtl[Loc])

  final case class State(currencySelected: String, coinExchange: CoinExchange)

  final class Backend(t: BackendScope[Props, State]) {
    def getLiveCurrencyUpdate() = {

      CoreApi.mobileGetLivePrices()
        .map(prices =>
          Json.parse(prices)
            .validate[CoinExchange].asEither match {
              case Left(err) => println(err)
              case Right(res) =>
                WalletCircuit.dispatch(GetCurrencies(res))
                t.modState(s => s.copy(
                  coinExchange = res,
                  currencySelected = res
                    .coinExchangeList
                    .find(_.coin.equalsIgnoreCase("ETH"))
                    .get
                    .currencies
                    .find(_.symbol.equalsIgnoreCase(t.state.runNow().currencySelected))
                    .get.symbol))
                .runNow()
            })

    }

    def setCurrencyLocal(currSymbol: String): react.Callback = Callback {
      dom.window.localStorage.setItem("currency", currSymbol)
    }

    def updateCurrency(): Callback = {
      getLiveCurrencyUpdate
      val slctedCurr = if (dom.window.localStorage.getItem("currency") == null) "USD" else dom.window.localStorage.getItem("currency")
      t.modState(s => s.copy(currencySelected = slctedCurr))
    }

    def componentDidMount(props: Props): Callback = {
      jQuery(".select-currency-info").removeClass("active")
      jQuery(".select-currency-info").first().addClass("active")
      setCurrencyLocal(t.state.runNow().currencySelected)
      Callback.when(!props.proxy().isPending)(props.proxy.dispatchCB((UpdateAccountTokenList())))
    }

    def updateURL(loc: String): Callback = {

      t.props.flatMap {
        props =>
          loc match {
            case "SendLoc" => props.router.set(SendLoc)
            case "HistoryLoc" => props.router.set(HistoryLoc)
            case "RequestLoc" => props.router.set(RequestLoc)
          }
      }

    }

    def onItemClicked(e: ReactEventFromHtml): react.Callback = Callback {
      e.preventDefault()
      jQuery(".select-currency-info").removeClass("active")
      if (!jQuery(e.target).is(".select-currency-info")) {
        jQuery(e.target).parents(".select-currency-info").addClass("active")
      } else {
        jQuery(e.target).addClass("active")
      }
    }

    def updateCurrencyState(e: ReactEventFromInput): react.Callback = {
      val newValue = e.target.value
      setCurrencyLocal(newValue)
      t.modState(s => s.copy(currencySelected = newValue))
    }

    //scalastyle: off
    def render(p: Props, s: State): VdomElement = {
      val coinList = s.coinExchange.coinExchangeList

      def createItem(userERCToken: TokenDetails) = {
        val coin = coinList.filter(e => e.coin.equalsIgnoreCase(userERCToken.symbol))
          .flatMap(_.currencies.filter(e => e.symbol == s.currencySelected.toUpperCase))

        <.div(
          ^.className := "row select-currency-info", ^.onClick ==> onItemClicked,
          <.div(
            <.div(
              ^.className := "row left-info",
              <.label(^.className := "col-lg-8 col-md-8 col-sm-8 col-xs-4", userERCToken.tokenName),
              <.span(
                ^.className := "col-lg-2 col-md-2 col-sm-2 col-xs-4 tokenUSDValue ellipseText",
                WalletJS.getnumberFormat(BigDecimal.apply(userERCToken.balance).setScale(2, BigDecimal.RoundingMode.UP).toString)),
              <.span(^.className := "col-lg-2 col-lg-2 col-md-2 col-sm-2 col-xs-4 symbol", s"${userERCToken.symbol}")),
            coin.zipWithIndex.map {
              case (e, index) =>
                <.div(
                  ^.key := index,
                  ^.className := "row",
                  <.span(
                    ^.className := "col-lg-8 col-md-8 col-sm-8 col-xs-4 ellipseText",
                    s"@ ${e.price} ${e.symbol}"),
                  <.span(
                    ^.className := "col-lg-2 col-md-2 col-sm-2 col-xs-4 tokenUSDValue ellipseText",
                    WalletJS.getnumberFormat(BigDecimal.apply(e.price.toDouble * userERCToken.balance.toDouble).setScale(2, BigDecimal.RoundingMode.UP).toString())),
                  <.span(
                    ^.className := "col-lg-2 col-lg-2 col-md-2 col-sm-2 col-xs-4 symbol",
                    e.symbol))
            }.toVdomArray))
      }

      <.div(^.id := "bodyWallet")(
        <.div(
          ^.className := "wallet-information-bottom",
          <.div(
            ^.className := "wallet-information-total",
            <.div(
              ^.className := "row heading-currency",
              <.div(
                ^.className := "col-lg-8 col-md-8 col-sm-8 col-xs-4 hand-cuure-left",
                <.label("total:")),
              <.div(
                ^.className := "col-lg-4 col-md-4 col-sm-4 col-xs-8 select-currency",
                <.label(
                  ^.className := "ellipseText",
                  p.proxy().render(tokenList =>

                    WalletJS.getnumberFormat(BigDecimal.apply(tokenList.accountTokenDetails.map { token =>
                      val coin = coinList.filter(_.coin.equalsIgnoreCase(token.symbol))
                        .flatMap { e => e.currencies.filter(c => c.symbol.equalsIgnoreCase(s.currencySelected)) }
                      token.balance.toDouble * coin.map(_.price).sum
                    }.sum).setScale(2, BigDecimal.RoundingMode.UP).toString))),
                <.select(^.id := "selectOption", ^.onChange ==> updateCurrencyState)(
                  s.coinExchange.coinExchangeList.filter(e => e.coin.equalsIgnoreCase("ETH")).flatMap(_.currencies
                    .map { currency =>
                      if (s.currencySelected.equalsIgnoreCase(currency.symbol)) {
                        <.option(^.value := currency.symbol, currency.symbol)
                      } else {
                        <.option(^.value := currency.symbol, currency.symbol)
                      }
                      /* <.option(^.value := currency.symbol, currency.symbol, ^.selected := "selected")
                        .when(s.currencySelected.equalsIgnoreCase(currency.symbol))
                      <.option(^.value := currency.symbol, currency.symbol)
                        .when(!s.currencySelected.equalsIgnoreCase(currency.symbol))*/
                    }).toVdomArray))),
            p.proxy().render(tokenList =>
              <.div(^.className := "currency-info-scrollbar")(
                tokenList.accountTokenDetails.filter(e => e.symbol.equalsIgnoreCase("ETH") || !e.balance.equalsIgnoreCase("0")) reverseMap createItem: _*)),
            p.proxy().renderFailed(ex => <.div()(
              <.label(^.className := "warn-text", "Error while loading available token list"))),
            p.proxy().renderPending(e =>
              <.div()(
                <.img(^.src := "../assets/images/processing-img.svg", ^.className := "loading-img"))))),
        <.div(
          ^.className := "container btnDefault-container homeButtonContainer",
          <.div(
            ^.className := "row", <.div(^.id := "test"),
            <.div(
              ^.className := "col-lg-12 col-md-12 col-sm-12 col-xs-12",
              <.button(^.`type` := "button", ^.className := "btn btnDefault btn-account-camera goupButton notAlphaV",
                <.i(^.className := "fa fa-camera", VdomAttr("aria-hidden") := "true")),
              <.button(^.`type` := "button", ^.className := "btn btnDefault goupButton", ^.onClick --> updateURL("HistoryLoc") /*p.router.setEH(HistoryLoc)*/ , "Transactions"),
              <.button(^.`type` := "button", ^.className := "btn btnDefault goupButton", ^.onClick --> updateURL("RequestLoc") /* p.router.setEH(RequestLoc)*/ , "Request"),
              <.button(^.`type` := "button", ^.className := "btn btnDefault goupButton", ^.onClick --> updateURL("SendLoc") /*p.router.setEH(SendLoc)*/ , "Send")))))
    }
  }

  val component = ScalaComponent.builder[Props]("AccountView")
    .initialState(State("ETH", CoinExchange(Seq(CurrencyList("", Seq(Currency("", 0, "")))))))
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.updateCurrency())
    .componentDidMount(scope => scope.backend.componentDidMount(scope.props))
    .build

  def apply(props: Props) = component(props)
}

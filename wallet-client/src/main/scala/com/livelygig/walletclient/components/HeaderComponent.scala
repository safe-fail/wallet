package com.livelygig.walletclient.components

import com.livelygig.shared.models.wallet.Account
import com.livelygig.walletclient.facades.Blockies
import com.livelygig.walletclient.facades.jquery.JQueryFacade.jQuery
import com.livelygig.walletclient.handler.ChangeLang
import com.livelygig.walletclient.router.ApplicationRouter.{Loc, _}
import com.livelygig.walletclient.services.{CoreApi, WalletCircuit}
import com.livelygig.walletclient.utils.I18N
import diode.AnyAction._
import diode.ModelRO
import japgolly.scalajs.react
import japgolly.scalajs.react.extra.router.{Resolution, RouterCtl}
import japgolly.scalajs.react.vdom.html_<^.{^, _}
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.util.{Failure, Success}

object HeaderComponent {

  case class Props(c: RouterCtl[Loc], r: Resolution[Loc])

  case class State(lang: js.Dynamic = WalletCircuit.zoom(_.i18n.language).value, ethNetInfo: String = "", walletPublicKey: String = "", selectedAddress: String = WalletCircuit.zoomTo(_.appRootModel.appModel.data.accountInfo.selectedAddress).value)

  def getHeaderName(currentLoc: Loc, state: State) = {
    currentLoc match {
      case LandingLoc => state.lang.selectDynamic("HOME").toString
      case AccountLoc => state.lang.selectDynamic("HOME").toString
      case SendLoc => state.lang.selectDynamic("SEND").toString
      case HistoryLoc => state.lang.selectDynamic("HISTORY").toString
      case RequestLoc => state.lang.selectDynamic("REQUEST").toString
      case IdentitiesLoc => state.lang.selectDynamic("IDENTITIES").toString
      case ManageIdentitiesLoc => state.lang.selectDynamic("MANAGE").toString
      case NotificationLoc => state.lang.selectDynamic("NOTIFICATIONS").toString
      case InitialSetupLoc => state.lang.selectDynamic("INITIAL_SETUP").toString
      case BackupIdentityLoc => state.lang.selectDynamic("BACKUP_IDENTITY").toString
      case BackupAccountLoc => state.lang.selectDynamic("BACKUP_ACCOUNT").toString
      case SetupAccountLoc => state.lang.selectDynamic("SETUP_ACCOUNT").toString
      case TermsOfServiceLoc => state.lang.selectDynamic("TERMS_OF_SERVICE").toString
      case PopulateQRCodeLoc("") => state.lang.selectDynamic("SEND").toString
      case AllAccountsLoc => state.lang.selectDynamic("ALL_ACCOUNTS").toString
      case AddSharedWalletLoc => state.lang.selectDynamic("ADD_SHARED_WALLET").toString
      case MultisigHomeLoc => state.lang.selectDynamic("MULTISIG").toString
      case AddTokenLoc => state.lang.selectDynamic("ADDTOKEN").toString
      case _ => "Send"
    }
  }

  final class Backend(t: BackendScope[Props, State]) {
    def getETHNetInfo() = {
      CoreApi.mobileGetETHNetInfo().map(
        netInfo =>
          t.modState(s => s.copy(ethNetInfo = netInfo)).runNow())
    }

    def isSimpleHeader(page: Loc): Boolean = {
      Seq(InitialSetupLoc, BackupIdentityLoc, BackupAccountLoc, SetupAccountLoc, TermsOfServiceLoc)
        .contains(page)
    }

    def changeLang(lang: String): react.Callback = Callback{
      CoreApi.getLang(lang).onComplete {
        case Success(res) => {
          WalletCircuit.dispatch(ChangeLang(JSON.parse(res)))
        }
        case Failure(_) => println(s"failed to load language for ${lang}")
      }
    }

    def updateLang(reader: ModelRO[js.Dynamic]) = {
      t.modState(s => s.copy(lang = reader.value)).runNow()
    }

    def onSideBarMenuClicked(e: ReactEventFromHtml): react.Callback = Callback{
      val cw = e.target.clientWidth
      val w = jQuery("#mySidenav").width()
      if (cw == w)
        toggleNav()
    }

    def userProfileImg = {
      Blockies.create(js.Dynamic.literal(size = 15, scale = 3, seed = s"DO NOT REMOVE ME OR BLOCKIES WON'T WORK FOR SOME REASON"))
      val str = Blockies.create(js.Dynamic.literal(size = 15, scale = 3, seed = s"0x${WalletCircuit.zoomTo(_.appRootModel.appModel.data.accountInfo.selectedAddress).value}"))
      jQuery("#userProfileImg").prepend(str)
    }

    def componentDidMount(): Callback = {
      userProfileImg
      WalletCircuit.subscribe(WalletCircuit.zoom(_.i18n.language))(e => updateLang(e))
      getETHNetInfo()
      changeLang(I18N.Lang.en_us)
    }

    def toggleNav(): Callback = Callback{
      jQuery("#mySidenav").toggleClass("fullWidth")
      jQuery("#closebtnContainer").toggleClass("active")
      jQuery("#bodyWallet").toggleClass("blurBackground")
      WalletCircuit.subscribe(WalletCircuit.zoomTo(_.appRootModel.appModel.data.accountInfo.selectedAddress)) {
        selected =>
          t.modState(state => state.copy(selectedAddress = selected.value)).runNow()
      }
    }

    def render(props: Props, state: State): VdomElement = {
      val accountInfo = WalletCircuit.zoomTo(_.appRootModel.appModel.data.accountInfo).value
      <.div()(
        <.div(
          ^.className := "wallet-inner-navigation",
          <.div(
            ^.className := "row",
            <.div(
              ^.className := "col-lg-2 col-md-2 col-sm-2 col-xs-2",
              <.span(^.className := "togglebtn", ^.onClick --> toggleNav, "☰"),
              <.div(^.id := "mySidenav", ^.className := "sidenav", ^.onClick ==> onSideBarMenuClicked,
                <.div(
                  ^.id := "closebtnContainer",
                  <.span(^.className := "closebtn", ^.onClick --> toggleNav, "×")),
                <.ul(
                  ^.id := "menu",
                  SidebarMenuComponent(props.c, props.r.page)))),
            <.div(
              ^.className := "col-lg-8 col-md-8 col-sm-8 col-xs-8",
              <.div(
                ^.className := "wallet-information",
                <.a(^.href := "javascript:void()", ^.className := "back-to"),
                <.p(state.ethNetInfo),
                <.h2(state.lang.selectDynamic("WALLET").toString),
                <.span(
                  ^.className := "wallet-page",
                  <.h3(state.lang.selectDynamic("ACCOUNT").toString), <.i(
                    ^.className := "fa fa-arrow-right",
                    VdomAttr("aria-hidden") := "true"),
                  <.h3(

                    accountInfo.accounts
                      .find(_.address == state.selectedAddress).getOrElse(Account("No Address Selected", "No Address Selected")).accountName), <.i(
                    ^.className := "fa fa-arrow-right",
                    VdomAttr("aria-hidden") := "true"),
                  {
                    <.h3(getHeaderName(props.r.page, state))
                  }))),
            <.div(
              ^.className := "col-lg-2 col-md-2 col-sm-2 col-xs-2",
              <.div(
                ^.className := "wallet-user-icon ", /*
                  <.a(
                    ^.href := "#/notification",
                    <.i(^.className := "fa fa-bell-o", VdomAttr("aria-hidden") := "true")),*/
//                <.div(^.id := "userProfileImg", ^.className := "img-userIcon")
              )))))

    }
  }

  val component = ScalaComponent.builder[Props]("Header")
    .initialState(State())
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.componentDidMount())
    .build
}


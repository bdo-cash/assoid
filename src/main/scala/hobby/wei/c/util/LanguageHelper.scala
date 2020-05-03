package hobby.wei.c.util

import java.util.Locale
import android.content.Context
import hobby.chenai.nakam.assoid.R
import hobby.wei.c.core.AbsApp
import hobby.wei.c.persist.{noUser, ModularStorer}

/**
  * @author Wei.Chou
  * @version 1.0, 03/03/2020
  */
// Changing `object` to` class` solves two problems:
// 1. Object's long-term resident memory cannot be destroyed;
// 2. The important thing is to elegantly solve the problem that
// `DEFAULT` cannot be reloaded (after the system language is changed).
class LanguageHelper {
  lazy val DEFAULT = {
    // if this, the string show lowercase.
    // Locale(App.get().getString(R.string.follow_system))
    new Locale("", "", AbsApp.get.getString(R.string.follow_system))
  }

  lazy val ZH_CN = Locale.SIMPLIFIED_CHINESE

  lazy val LOCALES_ALL_SUPPORTED = Seq(
    DEFAULT,
    ZH_CN,
    Locale.ENGLISH
  )
}

object LanguageHelper {
  def isSameWithSetting(context: Context, helper: LanguageHelper, locale: Locale = null) =
    if (locale == null) {
      context.getResources.getConfiguration.locale == loadSelected(helper)
    } else context.getResources.getConfiguration.locale == locale

  def isDefaultLocale(helper: LanguageHelper, locale: Locale = null) =
    if (locale == null) helper.DEFAULT == loadSelected(helper) else helper.DEFAULT == locale

  def changeLanguage(context: Context, helper: LanguageHelper) {
    val locale = loadSelected(helper)
    if (!isSameWithSetting(context, helper, locale)) {
      changeLanguage(
        context,
        if (isDefaultLocale(helper, locale))
          Locale.getDefault()
        else locale
      )
    }
  }

  private def changeLanguage(context: Context, locale: Locale) {
    val configuration = context.getResources.getConfiguration
    configuration.setLocale(locale)
    // not work
    // context.createConfigurationContext(configuration)
    context.getResources.updateConfiguration(configuration, context.getResources.getDisplayMetrics)
  }

  def persistLocale(helper: LanguageHelper, locale: Locale) {
    if (locale == helper.DEFAULT) {
      STATE.clearLanguageLocale()
    } else {
      STATE.storeLanguageLocale(locale.getDisplayName(locale))
    }
  }

  def loadSelected(helper: LanguageHelper): Locale = {
    val stored = STATE.getLanguageLocale(helper.DEFAULT.getDisplayName(helper.DEFAULT))
    helper.LOCALES_ALL_SUPPORTED.filter { it => stored == it.getDisplayName(it) }.head
  }

  def loadSelectedUnion(helper: LanguageHelper): Locale = {
    val selected = loadSelected(helper)
    if (selected == helper.DEFAULT) {
      Locale.getDefault()
    } else selected
  }

  def isLocaleIndicatesZh(helper: LanguageHelper): Boolean = {
    val selected = loadSelected(helper)

    def isDefaultZh: Boolean = {
      val default = Locale.getDefault()
      default.getDisplayName(default).startsWith(Locale.CHINESE.getDisplayName(Locale.CHINESE))
    }

    selected == helper.ZH_CN || selected == helper.DEFAULT && isDefaultZh
  }

  private val MODULAR_LANGUAGE_STORE = "modular.language_store"
  def STATE = ModularStorer.get(noUser(), MODULAR_LANGUAGE_STORE, clearable = false) {
    _.bind(new StateStorer)
  }

  class StateStorer extends ModularStorer {
    private val KEY_LANGUAGE_LOCALE = "language_locale"

    def storeLanguageLocale(displayName: String): Unit = get().storeString(KEY_LANGUAGE_LOCALE, displayName)

    def getLanguageLocale(default: String): String = get().loadString(KEY_LANGUAGE_LOCALE, default)

    def clearLanguageLocale(): Unit = get().remove(KEY_LANGUAGE_LOCALE)
  }
}


/*
 * Copyright (C) 2014-present, Wei Chou(weichou2010@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hobby.wei.c.phone

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import hobby.chenai.nakam.lang.J2S.NonNull
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.wei.c.used.UsedStorer
import hobby.wei.c.util.MD5Utils

/**
  * <pre>
  * 需要权限：
  * `android.permission.READ_PHONE_NUMBERS`,
  * `android.permission.READ_PHONE_STATE`。
  * </pre>
  *
  * @author Wei Chou(weichou2010@gmail.com)
  * @version 1.1, 15/11/2017, 重构旧代码。
  */
object Device {
  case class ScreenSize(width: Int, height: Int)

  def getScreenSize(context: Context) = {
    val dm = context.getResources.getDisplayMetrics
    var w = dm.widthPixels // 获取分辨率宽度
    var h = dm.heightPixels
    if (w > h) {
      val i = w
      w = h
      h = i
    }
    ScreenSize(w, h)
  }

  def getPhoneNumber(context: Context) = {
    val n = getTelephonyManager(context).getLine1Number
    Option(if (n == null || n.length == 0 || n.matches("0*")) null else n)
  }

  def getUniqueId(context: Context) = {
    lazy val stored = UsedStorer.device.getUniqueId
    lazy val deviceId = getTelephonyManager(context).getDeviceId
    lazy val androidId = Settings.Secure.getString(context.getContentResolver, Settings.Secure.ANDROID_ID)
    lazy val macAddress = {
      val info = getWifiManager(context).getConnectionInfo
      if (info.isNull) null else info.getMacAddress
    }
    if (stored.nonNull) stored
    else {
      val id = MD5Utils.toMD5 {
        if (deviceId.nonNull) deviceId
        else if (androidId.nonNull) androidId
        else macAddress
        // TODO: 还有 AdvertisingIdClient$Info#getId,
        // and for analytics use InstanceId#getId,
        // 不过需要 Google Play Service, 待完善。
      }
      UsedStorer.device.saveUniqueId(id)
      id
    }
  }

  def sysVersion = "Android " + Build.VERSION.RELEASE

  /** 设备型号 */
  def brand = Build.BRAND + " " + Build.MODEL

  def cpuAbi = {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) Array(Build.CPU_ABI, Build.CPU_ABI2)
    else Build.SUPPORTED_ABIS
  }.filterNot(_ == Build.UNKNOWN).mkString(", ")

  private def getTelephonyManager(context: Context) = context.getSystemService(Context.TELEPHONY_SERVICE).as[TelephonyManager]

  private def getWifiManager(context: Context) = context.getSystemService(Context.WIFI_SERVICE).as[WifiManager]
}

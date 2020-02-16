/*
 * Copyright (C) 2017-present, Chenai Nakam(chenai.nakam@gmail.com)
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

package hobby.wei.c.core

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.J2S.NonNull
import hobby.wei.c.LOG._

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 22/05/2018;
  *          2.0, 16/02/2020, 更新文档，增加【白名单】相关逻辑。
  */
trait AbsSrvce extends Service with TAG.ClassName {
  @volatile private var mDestroyed = false
  private var mWakeLock: PowerManager#WakeLock = _

  /**
    * 是否保持唤醒（配合白名单机制）。
    * <p>
    * 理论上，如果启用`startForeground()`，应用将不会进入`应用待机模式`；但还是会进入`低电耗模式`，本设置
    * 也将被忽略。因此本设置在常规情况下，只能在非`低电耗模式`且未启用`startForeground()`的情况下[可能]有用。
    * <p>
    * 但我们可以配合【白名单】机制。
    * 系统给我们提供了一个【白名单】，我们可以引导用户把本应用添加到电池优化的白名单。
    * 在低电耗模式和应用待机模式期间，列入白名单的应用【可以使用网络】并【保留部分唤醒锁定】。但仍会受到其它限制。详情参见：
    * https://developer.android.com/training/monitoring-device-state/doze-standby#support_for_other_use_cases
    * <br><br>
    *
    * 相关：
    * <br>
    * 由于 Android 6.0+ 对电源管理启用了`Doze`机制，WakeLock在`低电耗模式`下，不起作用（被忽略）；但`应用待机模式`不受影响。
    * <p>
    * 1. 如果用户未插接设备的电源，在屏幕关闭的情况下，让设备在一段时间内保持不活动状态，那么设备就会进入【低电耗模式】。
    * 应用会受到以下限制（不一一列举，详见链接）：
    * a. 暂停访问网络；
    * b. 系统【忽略唤醒锁定】。
    * ...
    * 注意：如果要使用`AlarmManager`，就只能用`setAlarmClock()`频繁地唤醒设备（因为它能够将设备彻底唤醒）。
    * 因为`setAndAllowWhileIdle()`及`setExactAndAllowWhileIdle()`为每个应用触发闹钟的频率都不能超过每[9]分钟一次。
    * <p>
    * 2.【应用待机模式】允许系统判定应用在用户未主动使用它时是否处于闲置状态。当用户有一段时间未触摸应用时，系统便会作出此判定。但
    * 具有以下特征的将`不`被判定为应用待机模式：
    * a. 用户显式/明确启动应用；
    * b. 应用当前有一个进程位于前台（表现为 Activity 或前台服务`foregroundService`形式，或被另一 Activity 或前台服务占用）；
    * c. 应用生成用户可在锁定屏幕或通知栏中看到的通知；
    * d. 应用是正在使用中的设备管理应用（例如设备策略控制器）。虽然设备管理应用通常在后台运行，但永远不会进入应用待机模式，因为
    * 它们必须保持可用性，以便随时从服务器接收策略。
    * <p>
    * 相关链接；
    * https://developer.android.com/training/monitoring-device-state/doze-standby
    * https://developer.android.com/guide/background/
    * <p>
    * 需要权限`android.permission.WAKE_LOCK`。
    */
  protected val needKeepWake = false

  /**
    * 需要触发`android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` Intent
    * 来触发一个系统对话框。
    * 需要权限`android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`。
    *
    * https://developer.android.com/training/monitoring-device-state/doze-standby#support_for_other_use_cases
    */
  protected def ensureIgnoringBatteryOptimizations(powerManager: PowerManager): Unit = {
    val intent = newIntent4ReqIgnoringBatteryOptimizations
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    getApplicationContext.startActivity(intent)
  }

  final def newIntent4ReqIgnoringBatteryOptimizations = new Intent(
    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse(s"package:$getPackageName"))

  def isDestroyed = mDestroyed

  override def onCreate(): Unit = {
    super.onCreate()
    AbsApp.get.onServiceCreated(this)
    if (needKeepWake) {
      try {
        val powerManager = getSystemService(classOf[PowerManager])
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, className.toString)
        mWakeLock.acquire()
        if (!powerManager.isIgnoringBatteryOptimizations(getPackageName))
          ensureIgnoringBatteryOptimizations(powerManager)
      } catch {
        case ex: Exception => e(ex)
      }
    }
  }

  override def onDestroy(): Unit = {
    mDestroyed = true
    if (mWakeLock.nonNull) {
      mWakeLock.release()
    }
    AbsApp.get.onServiceDestroyed(this)
    super.onDestroy()
  }
}

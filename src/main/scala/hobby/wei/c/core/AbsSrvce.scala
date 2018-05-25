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
import android.content.Context
import android.os.PowerManager
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.J2S.NonNull
import hobby.wei.c.LOG._

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 22/05/2018
  */
trait AbsSrvce extends Service with TAG.ClassName {
  @volatile private var mDestroyed = false
  private var mWakeLock: PowerManager#WakeLock = _

  /**
    * 是否保持唤醒（理论上，如果启用`startForeground()`，应用将不会进入待机状态；而如果用户启用了`低电耗模式`，本设置
    * 也将被忽略。因此本设置只能在非`低电耗模式`且未启用`startForeground()`的情况下可能有用。但为了减少权限的申请，还是推荐启用`startForeground()`）。
    * 需要权限 `android.permission.WAKE_LOCK`。
    */
  protected val needKeepWake = false

  def isDestroyed = mDestroyed

  override def onCreate(): Unit = {
    super.onCreate()
    AbsApp.get[AbsApp].onServiceCreated(this)
    // 需要权限 `android.permission.WAKE_LOCK`
    // 注意：由于Android6.0+对电源管理启用了`Doze`机制，
    // WakeLock在`低电耗模式`下，不起作用（被忽略）；但`应用待机模式`不受影响。
    //
    // 具有以下3个特征将`不`被判定为待机模式：
    // 1. 用户显式启动应用；
    // 2. 应用当前有一个进程位于前台（表现为 Activity 或前台服务形式，或被另一 Activity 或前台服务占用）；
    // 3. 应用生成用户可在锁屏或通知托盘中看到的通知。
    //
    // 因此，startForeground()有助于keep服务避免进入待机。具体参见；
    // https://developer.android.com/training/monitoring-device-state/doze-standby
    try {
      if (needKeepWake) {
        mWakeLock = getSystemService(Context.POWER_SERVICE).asInstanceOf[PowerManager].newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, className.toString)
        mWakeLock.acquire()
      }
    } catch {
      case ex: Exception => e(ex)
    }
  }

  override def onDestroy(): Unit = {
    mDestroyed = true
    if (mWakeLock.nonNull) {
      mWakeLock.release()
    }
    AbsApp.get[AbsApp].onServiceDestroyed(this)
    super.onDestroy()
  }
}

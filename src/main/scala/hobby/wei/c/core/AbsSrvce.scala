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

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 22/05/2018;
  *          2.0, 16/02/2020, 更新文档，增加【白名单】相关逻辑;
  *          2.1, 22/02/2020, 重构。
  */
trait AbsSrvce extends Service with IgnoreBatteryOptReq {
  @volatile private var mDestroyed = false

  /**
    * 是否保持唤醒（配合白名单机制）。
    *
    * 详见 [[IgnoreBatteryOptReq]] 文档。
    * <p>
    * 相关链接；
    * https://developer.android.com/guide/background/
    * <p>
    * 需要权限`android.permission.WAKE_LOCK`。
    */
  protected val needKeepWake = false

  /**
    * 如果要从页面引导该设置，这里应该置为默认值`false`。
    */
  protected val ignoringBatteryOptimization = false

  def isDestroyed = mDestroyed

  override def onCreate(): Unit = {
    super.onCreate()
    AbsApp.get.onServiceCreated(this)
    if (needKeepWake) {
      acquireWakeLock(ignoringBatteryOptimization)
    }
  }

  override def onDestroy(): Unit = {
    mDestroyed = true
    releaseWakeLock()
    AbsApp.get.onServiceDestroyed(this)
    super.onDestroy()
  }
}

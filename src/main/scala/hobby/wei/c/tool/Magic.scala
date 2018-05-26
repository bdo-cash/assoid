/*
 * Copyright (C) 2018-present, Chenai Nakam(chenai.nakam@gmail.com)
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

package hobby.wei.c.tool

import android.os.Handler
import hobby.chenai.nakam.basis.TAG
import hobby.wei.c.LOG._

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 27/05/2018
  */
object Magic extends TAG.ClassName {
  /**
    * 重试一个操作指定的次数，直到成功，或者用完次数。
    *
    * @param delay   延迟多长时间后重试。单位：毫秒。
    * @param times   最多重试多少次。
    * @param action  具体要执行的操作。返回`true`表示成功，结束重试。
    * @param handler 用于延迟`action`的执行器。
    */
  def retryForceful(delay: Int, times: Int = 8)(action: => Boolean)(implicit handler: Handler): Unit = if (times > 0) {
    val f = () => action
    i("retryForceful | delay: %s, times: %s, action => f: %s.", delay, times, f)
    if (!f()) handler.postDelayed(new Runnable {
      override def run(): Unit = retryForceful(delay, times - 1)(f())(handler)
    }, delay)
  }
}

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
import hobby.wei.c.LOG

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 27/05/2018
  */
trait RetryByHandler extends Retry {
  override protected lazy val logger = LOG
  import logger._

  implicit lazy val delayer: Delayer = implicitly[Handler]

  /** 如果在调用`retryForceful()`或`delay()`的时候未传入隐式参数，则可通过重写本方法以获得同样的效果，
    * 推荐使用本方法，但请务必谨慎：确认在任何调用上都确实打算使用这同一个`handler`（对于个别
    * 不使用本`handler`的，可临时手动传入正确的那个）。 */
  implicit protected def delayerHandler: Handler = ???

  implicit class DelayerOf(handler: Handler) extends Delayer {
    override def delay(delayMillis: Int)(action: => Unit): Unit = {
      w("delayAction | delayMillis: %s.", delayMillis)
      handler.postDelayed(new Runnable {
        override def run(): Unit = action
      }, delayMillis)
    }
  }
}

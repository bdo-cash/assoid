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

package hobby.chenai.nakam.assoid

import android.os.Bundle
import android.view.View.OnClickListener
import hobby.chenai.nakam.basis.TAG
import hobby.wei.c.core._
import hobby.wei.c.core.Ctx.%

/**
  * 搭积木的方式构建应用。
  *
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 11/09/2017
  */
abstract class SampleActy extends AbsActy with %[AbsApp] with Ctx.Acty with TypedFindView
  with Keyboard.Acty with EventHost.Acty with TwiceBack with Inject.Acty with ReqCode
  with Secure.Acty with PermissionReq.Acty with TAG.ClassName with OnClickListener {
  // 或者：
  // class SampleActy extends AssoidCompat4ActyV7 with TypedFindView {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    TypedViewHolder.setContentView(this, TR.layout.sample)

    findView(TR.l).getLayoutParams
  }
}

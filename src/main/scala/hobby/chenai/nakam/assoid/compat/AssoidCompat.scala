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

package hobby.chenai.nakam.assoid.compat

import android.app.Activity
import android.content.Context
import hobby.chenai.nakam.lang.TypeBring

/**
  * TypeBring for compat activity.
  *
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 11/09/2017
  */
trait AssoidCompat extends TypeBring[Nothing, AnyRef, AnyRef] {
  require(isInstanceOf[Activity])

  implicit lazy val context: Context = this
}

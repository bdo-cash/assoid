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

import android.support.v7.app.AppCompatActivity
import android.view.View

/**
  * TypeBring and {{{TypedFindView.findViewById[V <: View](id: Int): V}}}
  * compat for `AppCompatActivity`.
  * <p>
  * 注意：要兼容包 `AppCompatActivity`，就必须使用 `Theme.AppCompat`
  * 主题并配置到主工程的 `AndroidManifest.xml` 的
  * {{{<application android:theme="@style/AppTheme">}}} 标签属性。详见：
  * {{{res/values/theme-compat}}}。
  *
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 11/09/2017
  */
class AssoidCompat4ActyV7 extends AppCompatActivity with AssoidCompat {
  // 由于自动生成了特质 TypedFindView 生成了未实现的方法签名 findViewById[V <: View](id: Int): V，
  // 一般情况下，如果页面继承了 android.app.Activity，由于 Activity 中该方法的实现与特质中的方法签名
  // 相同，当追加（with）了特质 TypedFindView，不用手动覆盖该方法便算 impl 了。

  // 但对于 AppCompatActivity 类，其方法签名是 findViewById(id: Int): View，两者被认为不同，即
  // 认为 TypedFindView 的 findViewById 没有 impl，会报错。但如果手动去 impl，对不起，又报错说，两者
  // 在类型擦除后签名相同，冲突。
  // 这特么就蛋疼了。不过这是 Scala 语言的已知 bug。

  // 试了很多方法，均无法在方法签名和类型上做手脚，以至于最终发现了下面的这样一个方法，可以欺骗编译器。

  // 需要注意的是：为了兼容 super[AppCompatActivity]，本类的定义必须是 class 而非 trait。
  protected def findViewById[V <: View](id: Int)(implicit v: V = null): V = super[AppCompatActivity].findViewById(id)
}

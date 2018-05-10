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

package hobby.wei.c.core

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import hobby.chenai.nakam.lang.J2S.NonNull
import hobby.wei.c.Const

/**
  * @author Wei Chou(weichou2010@gmail.com)
  * @version 1.1, 17/11/2017, 重构旧代码。
  */
abstract class AbsActy extends Activity {
  override protected def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    //退出应用，无论是否退出，都应该首先执行上面的代码，确保数据不丢失
    if (data.nonNull) {
      if (data.getBooleanExtra(Const.EXTRA_BACK_CONTINUOUS, false)) {
        backContinuous(resultCode, new Intent().putExtras(data).setData(data.getData))
      } else {
        val name = data.getStringExtra(Const.EXTRA_BACK_TO_NAME)
        if (name != null) {
          if (name.equals(getClass.getName)) data.removeExtra(Const.EXTRA_BACK_TO_NAME)
          else backToInner(resultCode, name, new Intent().putExtras(data).setData(data.getData))
        } else {
          val count = -1 /*以前是--count*/ + data.getIntExtra(Const.EXTRA_BACK_TO_COUNT, -1)
          if (count <= 0) data.removeExtra(Const.EXTRA_BACK_TO_COUNT)
          else backTo(resultCode, count, new Intent().putExtras(data).setData(data.getData))
        }
      }
    }
  }

  override protected def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    AbsApp.get.onActivityCreated(this)
  }

  override protected def onDestroy(): Unit = {
    if (AbsApp.get.onActivityDestroyed(this)) {
      onDestroyToExit()
    }
    super.onDestroy()
  }

  /**
    * 在{@link #onDestroy()}执行期间被回调，且只有在本次<code>onDestroy()</code>之后会退出应用的情况下才会回调，表示之后就退出应用了。
    **/
  protected def onDestroyToExit(): Unit = {}

  private[core] def uniqueObjectId(): String = super.toString //必须是super，不受子类重写干扰

  def backContinuous(): Unit = backContinuous(Activity.RESULT_CANCELED, null)

  /**
    * 给栈中连续的Activity递归地执行finish()操作。注意：本方法需要栈中的Activity可以接收栈顶Activity的返回值才起作用，
    * 参见{@link Activity#onActivityResult(int, int, Intent) onActivityResult(int, int, Intent)}
    * 和{@link Activity#startActivityForResult(Intent, int, Bundle) startActivityForResult(Intent, int, Bundle)}。
    * 注意是连续的，也就是说，栈中的这些Activity自己没有finish()，遇到已经finish()的则中断。
    *
    * @param resultCode 参见{ @link AbsActy#setResult(int, Intent) setResult(int, Intent)}
    * @param data       参见{ @link AbsActy#setResult(int, Intent) setResult(int, Intent)}
    */
  def backContinuous(resultCode: Int, data: Intent): Unit = {
    setResult(resultCode,
      if (data.isNull) new Intent().putExtra(Const.EXTRA_BACK_CONTINUOUS, true)
      else data.putExtra(Const.EXTRA_BACK_CONTINUOUS, true))
    finish()
  }

  def backTo(activityClass: Class[_ <: AbsActy]): Unit = backTo(Activity.RESULT_CANCELED, activityClass, null)

  /**
    * 返回到Activity栈中指定的类所在的Activity。注意：本方法需要栈中的Activity可以接收栈顶Activity的返回值才起作用，
    * 参见{@link Activity#onActivityResult(int, int, Intent) onActivityResult(int, int, Intent)}
    * 和{@link Activity#startActivityForResult(Intent, int, Bundle) startActivityForResult(Intent, int, Bundle)}。
    *
    * @param resultCode    参见{ @link AbsActy#setResult(int, Intent) setResult(int, Intent)}
    * @param activityClass 要返回到的Activity所对应的类
    * @param data          参见{ @link AbsActy#setResult(int, Intent) setResult(int, Intent)}
    */
  def backTo(resultCode: Int, activityClass: Class[_ <: AbsActy], data: Intent): Unit = backToInner(resultCode, activityClass.getName(), data)


  private def backToInner(resultCode: Int, actyClassName: String, data: Intent): Unit = {
    setResult(resultCode,
      if (data.isNull) new Intent().putExtra(Const.EXTRA_BACK_TO_NAME, actyClassName)
      else data.putExtra(Const.EXTRA_BACK_TO_NAME, actyClassName))
    finish()
  }

  def backTo(count: Int): Unit = backTo(Activity.RESULT_CANCELED, count, null)

  /**
    * 返回Activity栈中指定数量的Activity。注意：本方法需要栈中的Activity可以接收栈顶Activity的返回值才起作用，
    * 参见{@link Activity#onActivityResult(int, int, Intent) onActivityResult(int, int, Intent)}
    * 和{@link Activity#startActivityForResult(Intent, int, Bundle) startActivityForResult(Intent, int, Bundle)}。
    *
    * @param resultCode 参见{ @link AbsActy#setResult(int, Intent) setResult(int, Intent)}
    * @param count      要返回的Activity栈数量
    * @param data       参见{ @link AbsActy#setResult(int, Intent) setResult(int, Intent)}
    */
  def backTo(resultCode: Int, count: Int, data: Intent): Unit = {
    setResult(resultCode,
      if (data.isNull) new Intent().putExtra(Const.EXTRA_BACK_TO_COUNT, count)
      else data.putExtra(Const.EXTRA_BACK_TO_COUNT, count))
    finish()
  }
}

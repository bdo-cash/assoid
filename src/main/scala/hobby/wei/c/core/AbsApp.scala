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

import java.util
import java.lang.ref.WeakReference
import android.app.{ActivityManager, Application}
import android.content.pm.PackageManager
import android.os.{Handler, Looper, Process}
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.J2S.{NonNull, WrapIterator}
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.wei.c.LOG._
import hobby.wei.c.used.UsedStorer

import scala.collection.JavaConversions.asScalaBuffer
import scala.util.control.Breaks._

/**
  * @author Wei Chou(weichou2010@gmail.com)
  * @version 1.1, 17/11/2017, 重构旧代码。
  */
object AbsApp {
  private var sInstance: AbsApp = _

  def get[A <: AbsApp]: A = sInstance.ensuring(_.nonNull).as[A]
}

abstract class AbsApp extends Application with TAG.ClassName {
  CrashHandler.startCaughtAllException(false, true)
  AbsApp.sInstance = this

  private val sHandlerRefMap = new util.WeakHashMap[Looper, Handler]

  private var mFirstLaunch = true
  private var mForceExit = false

  def withPackageNamePrefix(name: String) = getPackageName + "." + name

  /**
    * 获取一个全局的与UI线程相关联的Handler. 注意：不可在{@link AbsApp#onCreate()}前调用。
    */
  def mainHandler = getHandler(getMainLooper)

  def getHandler(looper: Looper) = {
    require(looper.nonNull)
    var handler = sHandlerRefMap.get(looper)
    if (handler == null) {
      synchronized {
        handler = sHandlerRefMap.get(looper)
        if (handler == null) {
          handler = new Handler(looper)
          sHandlerRefMap.put(looper, handler)
        }
      }
    }
    handler
  }

  override def onCreate(): Unit = {
    super.onCreate()
    // if(getConfig().isNull) throw new NullPointerException("getConfig() 返回值不能为null。请不要返回Config.get()");
    loadInfos()
  }

  //protected def getConfig(): Config

  /**
    * 退出应用事件回调。
    *
    * @return `true`表示 kill 当前 App 以及其所有后台进程（需要加上权限：android.permission.KILL_BACKGROUND_PROCESSES），`false`则不 kill。
    */
  protected def onExit(firstLaunch: Boolean): Boolean = false

  private def loadInfos(): Unit = {
    mFirstLaunch = isFirstTimeLaunch(0)
  }

  //////////////////////////////////////////////////////////////////////////////////////////

  def finishActies(actyClasses: Class[_]*): Unit = {
    for (clazz <- actyClasses; ref <- mActivitieStack.toSeq) {
      Option(ref.get).foreach { acty =>
        w("[finishActies]acty: %s.", acty.getClass.getSimpleName.s)
        if (clazz.isAssignableFrom(acty.getClass) && acty.getClass.isAssignableFrom(clazz)) {
          w("[finishActies]----finish: %s.", acty.getClass.getSimpleName.s)
          acty.finish()
        }
      }
    }
  }

  /**
    * 退出应用。如果希望在退出之后本App的所有进程也关闭，则需要加上权限：android.permission.KILL_BACKGROUND_PROCESSES。
    */
  def exit(): Unit = {
    mForceExit = true
    mainHandler.post(new Runnable() {
      override def run(): Unit = finishActivities
    })
  }

  /**
    * 是否是第一次启动该模块。
    */
  def isFirstTimeLaunch(moduleId: Int) = {
    val firstLaunch = UsedStorer.absApp.getFirstLaunch(moduleId)
    if (firstLaunch) UsedStorer.absApp.clearFirstLaunch(moduleId)
    firstLaunch
  }

  def isExiting = mForceExit

  //没有意义，已经退出了的话，没人会调用本方法
  // def isExited = isActivitieStackEmpty()

  private[core] def onActivityCreate(acty: AbsActy): Unit = {
    mActivitieStack.push(new WeakReference[AbsActy](acty))
  }

  private[core] def onActivityDestroy(acty: AbsActy): Boolean = {
    cleanStackOrDelete(acty)
    if (mForceExit) finishActivities
    val exit = isCurrentTheLastActivityToExit(acty)
    if (exit) mainHandler.post(new Runnable() {
      override def run(): Unit = doExit()
    })
    exit
  }

  private[core] def doExit(): Unit = {
    if (onExit(mFirstLaunch) && checkCallingOrSelfPermission(android.Manifest.permission.KILL_BACKGROUND_PROCESSES) == PackageManager.PERMISSION_GRANTED) {
      e("@@@@@@@@@@----[应用退出]----[将]自动结束进程（设置项）: %s", getProcessName(Process.myPid()).orNull.s)
      //只会对后台进程起作用，当本App最后一个Activity.onDestroy()的时候也会起作用，并且是立即起作用，即本语句后面的语句将不会执行。
      getSystemService(classOf[ActivityManager]).killBackgroundProcesses(getPackageName)
      e("@@@@@@@@@@----[应用退出]---走不到这里来")
    }
    mForceExit = false
    mFirstLaunch = false
    w("@@@@@@@@@@----[应用退出]---[未]自动结束进程: %s", getProcessName(Process.myPid()).orNull.s)
  }

  def getProcessName(pid: Int): Option[String] = {
    var name: Option[String] = None
    breakable {
      for (info <- getSystemService(classOf[ActivityManager]).getRunningAppProcesses.iterator().toSeq if info.pid == pid) {
        w("[process]id: %s, name: %s", info.pid, info.processName.s)
        name = Option(info.processName)
        break
      }
    }
    name
  }

  def isCurrentProcessOf(name: String): Boolean = getProcessName(Process.myPid()).exists(_.endsWith(name))

  /**
    * 关闭activity. 只可在onActivityDestroy()的内部调用，否则返回值会不准确。
    *
    * @return true 表示已经全部关闭完，false 表示还没有关闭完。
    */
  private def finishActivities: Boolean = {
    var actyRef: WeakReference[_ <: AbsActy] = null
    var refActy: AbsActy = null
    var result = false
    breakable {
      while (mActivitieStack.size() > 0) {
        actyRef = mActivitieStack.peek()
        refActy = actyRef.get()
        if (refActy.isNull) {
          mActivitieStack.remove(actyRef)
        } else {
          if (refActy.isFinishing) { //isFinishing表示是否调用过finish()
            //调用过finish()，则等待在onActivityDestroy()移除（都在UI线程，理论上不会有问题），
            //这里移除会导致同时进行多个finish()，并且size()不准确。
            //mActivitieStack.remove(actyRef);
          } else {
            refActy.finish() //（现在情况不一样了-->）不能直接finish(), 否则被系统销毁的Activity重建之后可能不会被finish。
          }
          break
        }
      }
      result = true
    }
    result
  }

  /**
    * 只可在onActivityDestroy()的内部调用，否则返回值会不准确。
    *
    * @return 当前是不是最后一个正在关闭的Activity。
    */
  private def isCurrentTheLastActivityToExit(acty: AbsActy): Boolean = isActivitieStackEmpty

  private def isActivitieStackEmpty: Boolean = {
    cleanStackOrDelete(null)
    mActivitieStack.isEmpty
  }

  private def cleanStackOrDelete(acty: AbsActy): Unit = {
    var actyRef: WeakReference[_ <: AbsActy] = null
    var refActy: AbsActy = null
    var i = 0
    while (i < mActivitieStack.size()) {
      actyRef = mActivitieStack.get(i)
      refActy = actyRef.get()
      if (refActy.isNull || (refActy eq acty)) {
        mActivitieStack.remove(actyRef)
      } else i += 1
    }
  }

  /**
    * 由于无论是进入新的Activity还是返回到旧的Activity，将要显示的页面B总是先创建，将要放入后台或销毁的页面A
    * 总是在B.onCreate()之后执行A.onDestroy()<u>（即使处于后台由于内存不足而要被系统销毁的，理论上也会执行onDestroy()，
    * 即使不执行，这里的软引用也会cover这种情况，对于高优先级App需要内存时，不会执行A.onDestroy()而直接killProcess，
    * 这种情况不考虑，因为进程已经kill了，一切都没了）</u>，因此只要有Activity还要显示，本变量的元素个数总大于0，
    * 即<h1>mActivitieStack.size()在App退出之前总是大于0，为0即认为退出。</h1><br/>
    * 这里关于退出的定义：
    * 认为只要有Activity在显示，就表示没有退出；当没有要显示的了，则表示App退出了。
    * 关于没有要显示的了，是指在系统Task返回栈里面没有Activity记录了，正在显示的Activity-->finish()-->onDestroy()了。
    * 而那些没有finish()但被系统直接onDestroy()的，Task返回栈里的记录仍然存在，只是内存里的实例对象被销毁，由于执行了onDestroy()，
    * 本变量会删除一条记录，size()减少（即使系统没有执行onDestroy()，同时本变量会丢失一个key），
    * 但是当返回时，会根据Task返回栈记录重建Activity，本变量会增加记录，在执行返回操作的B.onDestroy()之前。
    */
  //private final WeakHashMap<AbsActy, Object> mActivities = new WeakHashMap<AbsActy, Object>();
  private val mActivitieStack = new util.Stack[WeakReference[_ <: AbsActy]]
}

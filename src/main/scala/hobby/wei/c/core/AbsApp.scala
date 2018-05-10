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
import java.util.concurrent.atomic.AtomicBoolean
import android.app.{ActivityManager, Application}
import android.content.Context
import android.content.pm.PackageManager
import android.os.{Bundle, Handler, Looper, Process}
import android.view.Window
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.J2S.{NonNull, WrapIterator}
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.chenai.nakam.tool.cache.{Delegate, LazyGet, Memoize, WeakKey}
import hobby.wei.c
import hobby.wei.c.LOG._
import hobby.wei.c.core.EventHost.{EventReceiver, PeriodMode}
import hobby.wei.c.used.UsedStorer

import scala.collection.JavaConversions.asScalaBuffer
import scala.language.implicitConversions
import scala.util.control.Breaks._

/**
  * @author Wei Chou(weichou2010@gmail.com)
  * @version 1.1, 17/11/2017, 重构旧代码。
  */
object AbsApp {
  private var sInstance: AbsApp = _

  def get[A <: AbsApp]: A = sInstance.ensuring(_.nonNull).as[A]
}

abstract class AbsApp extends Application with EventHost with Ctx.Abs with TAG.ClassName {
  outer =>
  CrashHandler.startCaughtAllException(false, true)
  AbsApp.sInstance = this

  private lazy val mForceExit = new AtomicBoolean(false)
  private lazy val sEventHost_bundle_pid = "pid"
  private lazy val sEventHost_bundle_activities = "activities"
  private lazy val sEventHost_event4Exit = withPackageNamePrefix("GLOBAL_EVENT_4_EXIT")
  private lazy val sEventHost_event4FinishActivities = withPackageNamePrefix("GLOBAL_EVENT_4_FINISH_ACTIVITIES")
  private lazy val sHandlerMem = new Memoize[Looper, Handler] with WeakKey /*.Sync*/ with LazyGet {
    override protected val delegate = new Delegate[Looper, Handler] {
      override def load(looper: Looper) = Option(new Handler(looper))

      override def update(key: Looper, value: Handler) = Option(value)
    }
  }

  def withPackageNamePrefix(name: String) = getPackageName + "." + name

  /**
    * 获取一个全局的与UI线程相关联的Handler. 注意：不可在{@link AbsApp#onCreate()}前调用。
    */
  override def mainHandler = getHandler(getMainLooper)

  def getHandler(looper: Looper): Handler = sHandlerMem.get(looper).get

  implicit def activity: AbsActy = ???

  implicit def context: Context = this

  implicit def window: Window = ???

  override def onCreate(): Unit = {
    // registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks) // 不太可控
    hostingGlobalEventReceiver(sEventHost_event4Exit, PeriodMode.START_STOP, mEventReceiver4Exit)
    hostingGlobalEventReceiver(sEventHost_event4FinishActivities, PeriodMode.START_STOP, mEventReceiver4FinishActivities)
    eventDelegator.onStart()
    super.onCreate()
  }

  /**
    * 退出应用事件回调。
    *
    * @param processName 当前进程名称（可能是子进程）。
    * @param firstLaunch 是否第一次启动。
    * @return `true`表示 kill 当前进程（进程名称由参数报告），`false`则不 kill。
    *         注意：要实现 kill 能力，需要加上权限：android.permission.KILL_BACKGROUND_PROCESSES。
    */
  protected def onExit(processName: String, firstLaunch: Boolean): Boolean = false

  //////////////////////////////////////////////////////////////////////////////////////////
  private val mEventReceiver4Exit = new EventReceiver {
    override def onEvent(data: Bundle): Unit = exit()
  }
  private val mEventReceiver4FinishActivities = new EventReceiver {
    override def onEvent(data: Bundle): Unit = if (data.getInt(sEventHost_bundle_pid) != Process.myPid())
      finishActivitiesInner(data.getStringArray(sEventHost_bundle_activities).map(Class.forName(_).as[Class[AbsActy]]): _*)
  }

  def finishActivities(actyClasses: Class[_ <: AbsActy]*): Unit = {
    val data = new Bundle()
    data.putStringArray(sEventHost_bundle_activities, actyClasses.map(_.getName).toArray)
    data.putInt(sEventHost_bundle_pid, Process.myPid())
    sendGlobalEvent(sEventHost_event4FinishActivities, data)
    finishActivitiesInner()
  }

  private def finishActivitiesInner(actyClasses: Class[_ <: AbsActy]*): Unit = {
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
  def exit(): Unit = if (!mForceExit.getAndSet(true)) mainHandler.post(new Runnable() {
    override def run(): Unit = {
      sendGlobalEvent(sEventHost_event4Exit, null)
      finishActivities
    }
  })

  /** 是否是第一次启动某模块。 */
  def isFirstTimeLaunch(module: String, withVersion: Boolean = true) = UsedStorer.absApp.isFirstLaunch(withVer(module, withVersion))

  /** 在完成第一次启动某模块的某些动作后，清除该标识。 */
  def doneFirstTimeLaunch(module: String, withVersion: Boolean = true) = UsedStorer.absApp.clearFirstLaunchFlag(withVer(module, withVersion))

  private def withVer(module: String, ver: Boolean = true) = module + (if (ver) "_" + c.util.Manifest.getVersionName(this) else "")

  def isExiting = mForceExit.get()

  //没有意义，已经退出了的话，没人会调用本方法
  // def isExited = isActivitieStackEmpty()

  private[core] def onActivityCreated(acty: AbsActy): Unit = {
    mActivitieStack.push(new WeakReference[AbsActy](acty))
  }

  private[core] def onActivityDestroyed(acty: AbsActy): Boolean = {
    cleanStackOrDelete(acty)
    if (mForceExit.get) finishActivities
    val exit = isCurrentTheLastActivityToExit(acty)
    if (exit) mainHandler.post(new Runnable() {
      override def run(): Unit = doExit()
    })
    exit
  }

  private[core] def doExit(): Unit = {
    if (onExit(currentProcessName.get, isFirstTimeLaunch("0")) && checkCallingOrSelfPermission(android.Manifest.permission.KILL_BACKGROUND_PROCESSES) == PackageManager.PERMISSION_GRANTED) {
      e("@@@@@@@@@@----[应用退出]----[将]自动结束进程（设置项）: %s", getProcessName(Process.myPid()).orNull.s)
      //只会对后台进程起作用，当本App最后一个Activity.onDestroy()的时候也会起作用，并且是立即起作用，即本语句后面的语句将不会执行。
      getSystemService(Context.ACTIVITY_SERVICE).as[ActivityManager].killBackgroundProcesses(getPackageName)
      e("@@@@@@@@@@----[应用退出]---走不到这里来")
    }
    mForceExit.set(false)
    doneFirstTimeLaunch("0")
    w("@@@@@@@@@@----[应用退出]---[未]自动结束进程: %s", getProcessName(Process.myPid()).orNull.s)
  }

  def getProcessName(pid: Int): Option[String] = {
    var name: Option[String] = None
    breakable {
      for (info <- getSystemService(Context.ACTIVITY_SERVICE).as[ActivityManager].getRunningAppProcesses.iterator().toSeq if info.pid == pid) {
        w("[process]id: %s, name: %s", info.pid, info.processName.s)
        name = Option(info.processName)
        break
      }
    }
    name
  }

  def currentProcessName = getProcessName(Process.myPid())

  def isCurrentProcessOf(name: String): Boolean = currentProcessName.exists(_.endsWith(name))

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

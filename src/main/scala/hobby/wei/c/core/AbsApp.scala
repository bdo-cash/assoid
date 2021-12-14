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

import android.app.{ActivityManager, Application}
import android.content.Context
import android.os.{Bundle, Handler, Looper, Process}
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.J2S.{NonNull, WrapIterator}
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.chenai.nakam.tool.cache.{Delegate, LazyGet, Memoize, Weakey}
import hobby.wei.c
import hobby.wei.c.LOG._
import hobby.wei.c.core.EventHost.{EventReceiver, PeriodMode}
import hobby.wei.c.used.UsedStorer
import java.lang.ref.WeakReference
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.concurrent.TrieMap
import scala.language.implicitConversions
import scala.util.control.Breaks._

/**
  * @author Wei Chou(weichou2010@gmail.com)
  * @version 1.1, 17/11/2017, 重构旧代码。
  */
object AbsApp {
  @volatile private var sInstance: AbsApp = _

  def get[A <: AbsApp]: A = sInstance.ensuring(_.nonNull).as[A]
}

abstract class AbsApp extends Application with EventHost with Ctx.Abs with TAG.ClassName {
  outer =>
  CrashHandler.startCaughtAllException(false, true)
  AbsApp.sInstance = this

  private lazy val mForceExit                        = new AtomicBoolean(false)
  private lazy val sEventHost_bundle_pid             = "pid"
  private lazy val sEventHost_bundle_activities      = "activities"
  private lazy val sEventHost_event4Exit             = withPackageNamePrefix("GLOBAL_EVENT_4_EXIT")
  private lazy val sEventHost_event4FinishActivities = withPackageNamePrefix("GLOBAL_EVENT_4_FINISH_ACTIVITIES")
  private lazy val sSingleInstances                  = new TrieMap[String, AnyRef]

  private lazy val sHandlerMem = new Memoize[Looper, Handler] with Weakey with LazyGet {

    override protected val delegate = new Delegate[Looper, Handler] {
      override def load(looper: Looper)                = Option(new Handler(looper))
      override def update(key: Looper, value: Handler) = Option(value)
    }
  }

  def withPackageNamePrefix(name: String) = getPackageName + "." + name

  def cacheSingleInstance(any: AnyRef): Unit = sSingleInstances.put(any.getClass.getName, any)

  def getSingleInstance[O <: AnyRef](clazz: Class[O]): O = sSingleInstances.get(clazz.getName).orNull.as[O]

  def removeSingleInstance(clazz: Class[_]): Unit = sSingleInstances.remove(clazz.getName)

  /** 获取一个全局的与UI线程相关联的`Handler`。注意：不可在`AbsApp.onCreate()`前调用。 */
  override def mainHandler = getHandler(getMainLooper)

  def getHandler(looper: Looper): Handler = sHandlerMem.get(looper).get

  implicit def context: Context = this

  override def onCreate(): Unit = {
    //registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks) // 不太可控
    hostingGlobalEventReceiver(sEventHost_event4Exit, PeriodMode.START_STOP, mEventReceiver4Exit)
    hostingGlobalEventReceiver(sEventHost_event4FinishActivities, PeriodMode.START_STOP, mEventReceiver4FinishActivities)
    eventDelegator.onStart()
    super.onCreate()
  }

  /**
    * 在`Activity`或`Service`退出之后，询问是否 kill 进程。
    *
    * @param process 当前进程名称（可能是子进程）。
    * @return `true`表示 kill 当前进程（进程名称由参数报告），`false`则不 kill。
    *         注意：由于`WorkManager(JobScheduler)`之类的服务会运行在主进程，这里默认对于主进程会返回`false`。
    */
  protected def shouldKill(process: Option[String]): Boolean = !isMainProcess

  /**
    * 将要被 kill 之前会回到本方法（等同于`onDestroy()`）。
    *
    * @param process 当前进程名称（可能是子进程）。
    */
  protected def onKill(process: Option[String]): Unit

  //////////////////////////////////////////////////////////////////////////////////////////
  private val mEventReceiver4Exit = new EventReceiver {

    override def onEvent(data: Bundle): Unit = {
      e("Cross-process exit(), process: %s.", myProcessName)
      exit()
    }
  }

  private val mEventReceiver4FinishActivities = new EventReceiver {

    override def onEvent(data: Bundle): Unit = if (data.getInt(sEventHost_bundle_pid) != Process.myPid()) {
      val activities = data.getStringArray(sEventHost_bundle_activities)
      e("Cross-process finish activities. process: %s, activities: %s.", myProcessName, activities.mkString("\n").s)
      finishActivitiesInner(activities.map(Class.forName(_).as[Class[AbsActy]]): _*)
    }
  }

  def finishActivities(actyClasses: Class[_ <: AbsActy]*): Unit = {
    val data = new Bundle()
    data.putStringArray(sEventHost_bundle_activities, actyClasses.map(_.getName).toArray)
    data.putInt(sEventHost_bundle_pid, Process.myPid())
    sendGlobalEvent(sEventHost_event4FinishActivities, data)
    finishActivitiesInner(actyClasses: _*)
  }

  private def finishActivitiesInner(actyClasses: Class[_ <: AbsActy]*): Unit = {
    for (clazz <- actyClasses; ref <- mActivitieStack.toSeq) {
      Option(ref.get).foreach { acty =>
        w("[finishActivitiesInner]acty: %s.", acty.getClass.getSimpleName.s)
        if (clazz.isAssignableFrom(acty.getClass) && acty.getClass.isAssignableFrom(clazz)) {
          w("[finishActivitiesInner]finish: %s.", acty.getClass.getSimpleName.s)
          acty.finish()
        }
      }
    }
  }

  /**
    * 退出应用。
    */
  def exit(): Unit = if (!mForceExit.getAndSet(true)) post {
    sendGlobalEvent(sEventHost_event4Exit, null)
    finishActivities()
  }

  /** 是否是第一次启动某模块。 */
  def isFirstTimeLaunch(module: String, withVersion: Boolean = true) = UsedStorer.absApp.isFirstLaunch(withVer(module, withVersion))

  /** 在完成第一次启动某模块的某些动作后，清除该标识。 */
  def doneFirstTimeLaunch(module: String, withVersion: Boolean = true) = UsedStorer.absApp.clearFirstLaunchFlag(withVer(module, withVersion))

  private def withVer(module: String, ver: Boolean = true) = module + (if (ver) "_" + c.util.Manifest.getVersionName(this) else "")

  def isExiting = mForceExit.get()

  // 没有意义，已经退出了的话，没人会调用本方法。
  //def isExited = hasNoMoreActivities()

  private[core] def onActivityCreated(acty: AbsActy): Unit = mActivitieStack.push(new WeakReference[AbsActy](acty))

  private[core] def onActivityDestroyed(acty: AbsActy): Boolean = {
    cleanCollOrDeleteActy(acty)
    if (mForceExit.get) finishActivities()
    val exit = isCurrTheLastActyToExit(acty)
    if (exit) post(doExit())
    exit
  }

  private[core] def onServiceCreated(srvce: AbsSrvce): Unit = mServiceSet.put(srvce, this)

  private[core] def onServiceDestroyed(srvce: AbsSrvce): Unit = {
    cleanCollOrDeleteSrvce(srvce)
    if (hasNoMoreServices) post(doExit())
  }

  private[core] def doExit(): Unit = {
    // 为解决`onCreate()`和`onExit()`不对称的问题（即：`onExit()`之后如果进程没有被`kill`则下次不会再走`onCreate()`，导致一些调用也不对称），同时
    // 为了优化进程的可控度，现弃用`ActivityManager.killBackgroundProcesses(getPackageName)`这种方式。
    /*
    if (onExit(myProcessName) &&
      checkCallingOrSelfPermission(android.Manifest.permission.KILL_BACKGROUND_PROCESSES) == PackageManager.PERMISSION_GRANTED) {
    // 只会对后台进程起作用，当本App最后一个Activity.onDestroy()的时候也会起作用，并且是立即起作用，即本语句后面的语句将不会执行。
    getSystemService(Context.ACTIVITY_SERVICE).as[ActivityManager].killBackgroundProcesses(getPackageName)
     */
    // TODO: 在 android 12 上有问题。初步判断是 activity 返回时没有 finish，或生命周期的执行逻辑改变了。待测试修复…
    if (hasNoMoreActivities && hasNoMoreServices) {
      if (shouldKill(myProcessName)) {
        onKill(myProcessName)
        e("[doExit]--- @@@@ ---| App 退出 |---- [将]自动结束进程（设置项）----| process: %s.", myProcessName.orNull.s)
        Process.killProcess(Process.myPid())
        e("[doExit]--- @@@@ ---| App 退出 |---- 走不到这里来。")
      } else {
        mForceExit.set(false)
        d("[doExit]--- @@@@ ---| App 退出 |---- [未]自动结束进程 ----| process: %s.", myProcessName.orNull.s)
      }
    }
  }

  def getProcessName(pid: Int): Option[String] = {
    var name: Option[String] = None
    breakable {
      for (info <- getSystemService(classOf[ActivityManager]).getRunningAppProcesses.iterator().toSeq if info.pid == pid) {
        w("[process]id: %s, name: %s.", info.pid, info.processName.s)
        name = Option(info.processName)
        break
      }
    }
    name
  }

  lazy val myProcessName: Option[String] = getProcessName(Process.myPid())

  def isMyProcessOf(name: String): Boolean = myProcessName.exists(_.endsWith(name))

  def isMainProcess: Boolean = isMyProcessOf(getPackageName) // myProcessName.contains(getPackageName)

  private def finishActivities(): Unit = {
    var actyRef: WeakReference[_ <: AbsActy] = null
    var refActy: AbsActy                     = null
    breakable {
      while (mActivitieStack.size() > 0) {
        actyRef = mActivitieStack.peek()
        refActy = actyRef.get()
        if (refActy.isNull) {
          mActivitieStack.remove(actyRef)
        } else {
          if (refActy.isFinishing) { // 是否调用过`finish()`。
            // 这里表示调用过，那么等待回调上面的`onActivityDestroy()`，进而调用`cleanCollOrDeleteActy(acty)`移除（都在 UI 线程，逻辑上不会有问题），
            // 如果此时在这里移除，会导致同时进行多个`finish()`，并且`size()`不准确。
            //mActivitieStack.remove(actyRef);
          } else {
            refActy.finish() //（现在情况不一样了-->）不能直接finish(), 否则被系统销毁的Activity重建之后可能不会被finish。
          }
          break
        }
      }
    }
  }

  /**
    * 只可在onActivityDestroy()的内部调用，否则返回值会不准确。
    *
    * @return 当前是不是最后一个正在关闭的Activity。
    */
  private def isCurrTheLastActyToExit(acty: AbsActy): Boolean = hasNoMoreActivities

  private def hasNoMoreActivities: Boolean = {
    cleanCollOrDeleteActy(null)
    val b = mActivitieStack.isEmpty
    i("[hasNoMoreActivities]noMore:%s.", b)
    b
  }

  private def cleanCollOrDeleteActy(acty: AbsActy): Unit = {
    var ref: WeakReference[_ <: AbsActy] = null
    var ins: AbsActy                     = null
    var i: Int                           = 0
    while (i < mActivitieStack.size()) {
      ref = mActivitieStack.get(i)
      ins = ref.get()
      if (ins.isNull || (ins eq acty)) {
        mActivitieStack.remove(ref)
      } else i += 1
    }
  }

  private def hasNoMoreServices: Boolean = {
    cleanCollOrDeleteSrvce(null)
    mServiceSet.isEmpty
  }

  private def cleanCollOrDeleteSrvce(srvce: AbsSrvce): Unit = for (ins <- mServiceSet.keySet.toArray if ins eq srvce) mServiceSet.remove(ins)

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
  private val mServiceSet     = new util.WeakHashMap[AbsSrvce, AbsApp]
}

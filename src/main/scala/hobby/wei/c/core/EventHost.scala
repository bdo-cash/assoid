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
import android.app.Fragment
import android.content.{BroadcastReceiver, Context, Intent, IntentFilter}
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import hobby.chenai.nakam.lang.J2S.{NonNull, WrapIterator}
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.wei.c.core.EventHost.{EventReceiver, EventSession, PeriodMode}
import hobby.wei.c.core.EventHost.PeriodMode.PeriodMode

/**
  * @author Wei Chou(weichou2010@gmail.com)
  * @version 1.1, 17/11/2017, 重构旧代码。
  */
object EventHost {
  trait Acty extends AbsActy with EventHost with Ctx.Acty {
    override protected def onStart(): Unit = {
      super.onStart()
      eventDelegator.onStart()
    }

    override protected def onResume(): Unit = {
      super.onResume()
      eventDelegator.onResume()
    }

    override protected def onPause(): Unit = {
      eventDelegator.onPause()
      super.onPause()
    }

    override protected def onStop(): Unit = {
      eventDelegator.onStop()
      super.onStop()
    }
  }

  trait Fragmt extends Fragment with EventHost with Ctx.Fragmt {
    override def onActivityCreated(savedInstanceState: Bundle): Unit = {
      super.onActivityCreated(savedInstanceState)
      eventDelegator.onActivityCreated()
    }

    override protected def onStart(): Unit = {
      super.onStart()
      eventDelegator.onStart()
    }

    override protected def onResume(): Unit = {
      super.onResume()
      eventDelegator.onResume()
    }

    override protected def onPause(): Unit = {
      eventDelegator.onPause()
      super.onPause()
    }

    override protected def onStop(): Unit = {
      eventDelegator.onStop()
      super.onStop()
    }
  }

  object PeriodMode extends Enumeration {
    type PeriodMode = Value
    val PAUSE_RESUME, START_STOP = Value
  }

  private lazy val eventHostName = classOf[EventHost].getSimpleName

  private def bundleExtraName(implicit host: EventHost) = host.getClass.getName + "_" + eventHostName

  private def actionName(name: String) = AbsApp.get.withPackageNamePrefix(name)

  def sendLocalEvent(context: Context, eventName: String, data: Bundle)(implicit host: EventHost): Unit = {
    val intent = new Intent(actionName(eventName))
    if (data != null) intent.putExtra(bundleExtraName, data)
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
  }

  def sendGlobalEvent(context: Context, eventName: String, data: Bundle)(implicit host: EventHost): Unit = {
    val intent = new Intent(actionName(eventName))
    if (data != null) intent.putExtra(bundleExtraName, data)
    context.sendBroadcast(intent)
  }

  def registerReceiver(context: Context, session: EventSession): Unit = {
    try { //避免在重复注册的时候导致异常
      if (session.local) {
        LocalBroadcastManager.getInstance(context).registerReceiver(session.broadcastReceiver, session.intentFilter)
      } else {
        context.registerReceiver(session.broadcastReceiver, session.intentFilter)
      }
    } catch {
      case _: Exception =>
    }
  }

  def unregisterReceiver(context: Context, session: EventSession): Unit = {
    try { //避免在重复取消注册的时候导致异常
      if (session.local) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(session.broadcastReceiver)
      } else {
        context.unregisterReceiver(session.broadcastReceiver)
      }
    } catch {
      case _: Exception =>
    }
  }

  trait EventReceiver {
    def onEvent(data: Bundle): Unit
  }

  private[core] case class EventSession(eventName: String, period: PeriodMode, local: Boolean, receiver: EventReceiver)
                                       (implicit host: EventHost) extends Equals {
    lazy val broadcastReceiver = new BroadcastReceiver() {
      override def onReceive(context: Context, intent: Intent): Unit = receiver.onEvent(intent.getBundleExtra(bundleExtraName))
    }
    lazy val intentFilter = new IntentFilter(actionName(eventName))

    override def hashCode() = eventName.hashCode

    override def equals(any: scala.Any) = any match {
      case that: EventSession if that canEqual this => that.eventName == this.eventName
      case _ => false
    }

    override def canEqual(that: Any) = that.isInstanceOf[EventSession]
  }
}

trait EventHost extends Ctx.Abs {
  private implicit val host: EventHost = this

  private[core] lazy val eventDelegator = new EventDelegator(context)

  def sendLocalEvent(eventName: String, data: Bundle): Unit = {
    EventHost.sendLocalEvent(context, eventName, data)
  }

  def sendGlobalEvent(eventName: String, data: Bundle): Unit = {
    EventHost.sendGlobalEvent(context, eventName, data)
  }

  def hostingLocalEventReceiver(eventName: String, periodMode: PeriodMode, receiver: EventReceiver): Unit = {
    eventDelegator.hostingLocalEventReceiver(eventName, periodMode, receiver)
  }

  def hostingGlobalEventReceiver(eventName: String, periodMode: PeriodMode, receiver: EventReceiver): Unit = {
    eventDelegator.hostingGlobalEventReceiver(eventName, periodMode, receiver)
  }

  def unhostingLocalEventReceiver(eventName: String): Unit = {
    eventDelegator.unhostingLocalEventReceiver(eventName)
  }

  def unhostingGlobalEventReceiver(eventName: String): Unit = {
    eventDelegator.unhostingGlobalEventReceiver(eventName)
  }
}

class EventDelegator(context: => Context)(implicit host: EventHost) {
  private final val localBroadcastEvent = new util.HashMap[String, EventSession]
  private final val globalBroadcastEvent = new util.HashMap[String, EventSession]

  private var mStarted, mResumed: Boolean = false

  private[core] def onStart(): Unit = {
    mStarted = true
    registerReceiver(PeriodMode.START_STOP)
  }

  /**
    * {@link android.app.Fragment#onActivityCreated(Bundle) Fragment}会用到：旋转屏幕的时候，Activity会重建，但是Fragment不会。
    **/
  private[core] def onActivityCreated(): Unit = {
    unregisterReceiver(PeriodMode.PAUSE_RESUME)
    unregisterReceiver(PeriodMode.START_STOP)
    //以下代码是安全的，不会导致在不恰当的时候注册
    registerReceiver(PeriodMode.START_STOP)
    registerReceiver(PeriodMode.PAUSE_RESUME)
  }

  private[core] def onResume(): Unit = {
    mResumed = true
    registerReceiver(PeriodMode.PAUSE_RESUME)
  }

  private[core] def onPause(): Unit = {
    mResumed = false
    unregisterReceiver(PeriodMode.PAUSE_RESUME)
  }

  private[core] def onStop(): Unit = {
    mStarted = false
    unregisterReceiver(PeriodMode.START_STOP)
  }

  def hostingLocalEventReceiver(eventName: String, periodMode: PeriodMode, receiver: EventReceiver)(implicit host: EventHost): Unit = {
    val session = EventSession(eventName, periodMode, true, receiver)
    val session1 = localBroadcastEvent.put(eventName, session)
    if (session1.nonNull) unregisterReceiver(session1)
    registerReceiver(session)
  }

  def hostingGlobalEventReceiver(eventName: String, periodMode: PeriodMode, receiver: EventReceiver)(implicit host: EventHost): Unit = {
    val session = EventSession(eventName, periodMode, false, receiver)
    val session1 = globalBroadcastEvent.put(eventName, session)
    if (session1.nonNull) unregisterReceiver(session1)
    registerReceiver(session)
  }

  def unhostingLocalEventReceiver(eventName: String): Unit = {
    val session = localBroadcastEvent.remove(eventName)
    if (session.nonNull) unregisterReceiver(session)
  }

  def unhostingGlobalEventReceiver(eventName: String): Unit = {
    val session = globalBroadcastEvent.remove(eventName)
    if (session.nonNull) unregisterReceiver(session)
  }

  private def registerReceiver(period: PeriodMode): Unit = {
    for (session <- localBroadcastEvent.values().iterator().toSeq if session.period == period) registerReceiver(session)
    for (session <- globalBroadcastEvent.values().iterator().toSeq if session.period == period) registerReceiver(session)
  }

  private def unregisterReceiver(period: PeriodMode): Unit = {
    for (session <- localBroadcastEvent.values().iterator().toSeq if session.period == period) unregisterReceiver(session)
    for (session <- globalBroadcastEvent.values().iterator().toSeq if session.period == period) unregisterReceiver(session)
  }

  private def registerReceiver(session: EventSession): Unit = {
    if (session.period == PeriodMode.START_STOP && mStarted
      || session.period == PeriodMode.PAUSE_RESUME && mResumed) {
      EventHost.registerReceiver(context, session)
    }
  }

  private def unregisterReceiver(session: EventSession): Unit = {
    EventHost.unregisterReceiver(context, session)
  }
}

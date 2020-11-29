/*
 * Copyright (C) 2020-present, Chenai Nakam(chenai.nakam@gmail.com)
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

import android.app.ActivityManager
import android.os.Process

/**
  * 本类返回的单位都是 MB。
  *
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 29/11/2020
  */
trait Memory extends Ctx.Abs {
  protected def memoryUse() = s"${memSizeOfJavaHeap().formatted("%.2f")}/${memSizeOfTotalPss().formatted("%.1f")}(MB)"

  protected def memSizeOfJavaHeap(): Float =
    actyMgr.getProcessMemoryInfo(Array(Process.myPid()))(0).getMemoryStat("summary.java-heap").toInt / 1024f

  protected def memSizeOfNativeHeap(): Float =
    actyMgr.getProcessMemoryInfo(Array(Process.myPid()))(0).getMemoryStat("summary.native-heap").toInt / 1024f

  protected def memSizeOfCode(): Float =
    actyMgr.getProcessMemoryInfo(Array(Process.myPid()))(0).getMemoryStat("summary.code").toInt / 1024f

  protected def memSizeOfStack(): Float =
    actyMgr.getProcessMemoryInfo(Array(Process.myPid()))(0).getMemoryStat("summary.stack").toInt / 1024f

  protected def memSizeOfGraphics(): Float =
    actyMgr.getProcessMemoryInfo(Array(Process.myPid()))(0).getMemoryStat("summary.graphics").toInt / 1024f

  protected def memSizeOfPrivateOther(): Float =
    actyMgr.getProcessMemoryInfo(Array(Process.myPid()))(0).getMemoryStat("summary.private-other").toInt / 1024f

  protected def memSizeOfSystem(): Float =
    actyMgr.getProcessMemoryInfo(Array(Process.myPid()))(0).getMemoryStat("summary.system").toInt / 1024f

  /** PSS(proportional set size)实际使用的物理内存。 */
  protected def memSizeOfTotalPss(): Float =
    actyMgr.getProcessMemoryInfo(Array(Process.myPid()))(0).getMemoryStat("summary.total-pss").toInt / 1024f

  protected def memSizeOfTotalSwap(): Float =
    actyMgr.getProcessMemoryInfo(Array(Process.myPid()))(0).getMemoryStat("summary.total-swap").toInt / 1024f

  /** App 可使用内存的最大值。 */
  protected def memSizeOfHeapGrowthLimit(): Int = actyMgr.getMemoryClass

  /** App 可使用内存的最大值。 需要在`AndroidManifest.xml`里设置`android:largeHeap="true"`才能应用该值。 */
  protected def memSizeOfHeapSize(): Int = actyMgr.getLargeMemoryClass

  private lazy val actyMgr = context.getSystemService(classOf[ActivityManager])
}

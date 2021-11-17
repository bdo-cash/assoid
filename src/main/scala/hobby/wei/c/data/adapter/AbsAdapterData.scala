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

package hobby.wei.c.data.adapter

import android.content.Context
import android.view.LayoutInflater
import hobby.chenai.nakam.lang.J2S.NonNull

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 17/06/2018
  */
trait AbsAdapterData[T <: AnyRef] {
  private var mData: List[T] = Nil

  protected def initData(data: List[T]): Unit = if (data.nonNull) mData = data

  protected def context: Context

  protected def onDataSourceChanged(): Unit

  protected def onDataItemRangeInserted(positionStart: Int, itemCount: Int): Unit

  protected def onDataItemRangeRemoved(positionStart: Int, itemCount: Int): Unit

  def setDataSource(data: List[T]): Unit = {
    initData(data)
    onDataSourceChanged()
  }

  def appendData(items: T*): Unit = insertData(mData.size, items: _*)

  def insertData(positionStart: Int, items: T*): Unit = {
    val start = positionStart min mData.size
    if (start >= mData.size) {
      mData = mData ::: items.toList
    } else {
      val (left, right) = mData.splitAt(start) // 这一句还是会有多余的运算，所以前面加了`if`分支。
      mData = left ::: items.toList ::: right
    }
    onDataItemRangeInserted(start, items.size)
  }

  def removeData(positionStart: Int, itemCount: Int = 1): Unit = {
    val count = (mData.size - positionStart) max 0 min itemCount
    if (count > 0) {
      mData = mData.take(positionStart) ::: mData.drop(positionStart + count)
      onDataItemRangeRemoved(positionStart, count)
    }
  }

  lazy val inflater = LayoutInflater.from(context)

  protected def getInflater: LayoutInflater = inflater

  def getData: List[T] = mData

  def getCount: Int = mData.size

  // 与`ExpandableAdapter`里的实现相冲突：`final`。
  //def getItemCount = mData.size

  def getItem(position: Int): T = mData(position)

  def getItemId(position: Int): Long = position
}

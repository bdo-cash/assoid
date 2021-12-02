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
import androidx.recyclerview.widget.RecyclerView

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 17/06/2018
  */
abstract class AbsRecyclerAdapter[VH <: RecyclerView.ViewHolder, T <: AnyRef](override val context: Context, data: List[T] = Nil) extends RecyclerView.Adapter[VH] with AbsAdapterData[T] {
  initData(data)

  override protected def onDataSourceChanged(): Unit = notifyDataSetChanged()

  override protected def onDataItemRangeInserted(positionStart: Int, itemCount: Int): Unit = notifyItemRangeInserted(positionStart, itemCount)
  override protected def onDataItemRangeRemoved(positionStart: Int, itemCount: Int): Unit  = notifyItemRangeRemoved(positionStart, itemCount)
  override protected def onDataItemRangeReplaced(positionStart: Int, itemCount: Int): Unit = notifyItemRangeChanged(positionStart, itemCount)

  override def getItemId(position: Int) = super.getItemId(position)
  override def getItemCount             = getCount
}

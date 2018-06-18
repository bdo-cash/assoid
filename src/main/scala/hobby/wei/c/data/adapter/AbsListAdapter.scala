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

package hobby.wei.c.data.adapter

import android.content.Context
import android.widget.BaseAdapter

/**
  * @author Wei Chou(weichou2010@gmail.com)
  */
abstract class AbsListAdapter[T <: AnyRef](override val context: Context, data: List[T] = Nil)
  extends BaseAdapter with AbsAdapterData[T] {
  initData(data)

  override protected def onDataSourceChanged(): Unit = notifyDataSetChanged()

  override protected def onDataItemChanged(positionStart: Int, itemCount: Int): Unit = notifyDataSetChanged()

  override protected def onDataItemRangeRemoved(positionStart: Int, itemCount: Int): Unit = notifyDataSetChanged()
}

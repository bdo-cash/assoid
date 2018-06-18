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

package hobby.wei.c.core

import android.support.v7.widget.RecyclerView
import android.widget.AdapterView
import hobby.wei.c.anno.inject.Injector
import hobby.wei.c.data.adapter.{AbsListAdapter, AbsRecyclerAdapter}

/**
  * 标准化基于`Adapter`的列表`View`数据更新，避免写法混乱（基于旧库`AbsListViewActivity`改造）。
  *
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 16/06/2018
  */
object DataButt {
  trait AdapterV[V <: AdapterView[A], D <: AnyRef, A <: AbsListAdapter[D]] extends Ctx.Abs {
    protected lazy val listView: V = activity.findViewById(listViewId).asInstanceOf[V]
    protected lazy val listAdapter: A = {
      val adapter = newAdapter()
      // 不要重复调用setAdapter(), 否则会滚动到开头，而最好的办法就是在创建的时候set。
      listView.setAdapter(adapter)
      adapter
    }

    def updateListData(data: List[D]): Unit = listAdapter.setDataSource(data)

    protected def listViewId: Int = Injector.listViewID(context, getClass)

    protected def newAdapter(): A
  }

  /**
    * 用法示例：{{{
    * class XxxAdapter(context: android.content.Context) extends AbsRecyclerAdapter[XxxVH, String](context) {
    *   override def onCreateViewHolder(viewGroup: ViewGroup, i: Int) = ???
    *
    *   override def onBindViewHolder(vh: XxxVH, i: Int) = ???
    * }
    *
    * class XxxVH(item: View) extends RecyclerView.ViewHolder(item) {
    * }
    *
    * class A extends AbsActy with DataButt.RecyclerV[RecyclerView, String, XxxAdapter] with Ctx.Acty {
    *   override protected def newAdapter() = new XxxAdapter(this)
    * }
    * }}}
    */
  trait RecyclerV[V <: RecyclerView, D <: AnyRef, A <: AbsRecyclerAdapter[_ <: RecyclerView.ViewHolder, D]] extends Ctx.Abs {
    protected lazy val recyclerView: V = activity.findViewById(listViewId).asInstanceOf[V]
    protected lazy val vhAdapter: A = {
      val adapter = newAdapter()
      recyclerView.setAdapter(adapter)
      adapter
    }

    def updateListData(data: List[D]): Unit = vhAdapter.setDataSource(data)

    protected def listViewId: Int = Injector.listViewID(context, getClass)

    protected def newAdapter(): A
  }
}

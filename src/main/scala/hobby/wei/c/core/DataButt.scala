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

import android.widget.{Adapter, AdapterView}
import androidx.recyclerview.widget.RecyclerView
import hobby.wei.c.anno.inject.Injector
import hobby.wei.c.data.adapter.{AbsListAdapter, AbsRecyclerAdapter}

/**
  * 标准化基于`Adapter`的列表`View`数据更新，避免写法混乱（基于旧库`AbsListViewActivity`改造）。
  *
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 16/06/2018
  */
object DataButt {

  trait AdapterV[V <: AdapterView[_ >: AbsListAdapter[D] <: Adapter], D <: AnyRef, A <: AbsListAdapter[D]] extends Ctx.Abs {
    final lazy val listView: V = onSetupListView()

    final lazy val listAdapter: A = {
      val adapter = newAdapter()
      // 不要重复调用setAdapter(), 否则会滚动到开头，而最好的办法就是在创建的时候set。
      listView.setAdapter(adapter)
      adapter
    }

    def updateListData(data: List[D]): Unit = listAdapter.setDataSource(data)

    /** 可重写，也可在类头标注`@ViewListId`注解。也可直接重写`onSetupListView()`以忽略本调用。 */
    protected def listViewId: Int = Injector.listViewID(context, getClass)

    protected def newAdapter(): A
    protected def onSetupListView(): V // = activity.findViewById(listViewId).asInstanceOf[V]
  }

  /**
    * 用法示例：{{{
    * class XxxAdapter(implicit context: Context) extends AbsRecyclerAdapter[XxxVh, String](context) {
    *   //override def getItemViewType(position: Int) = super.getItemViewType(position) // 下一行的`viewType`。
    *   override def onCreateViewHolder(parent: ViewGroup, viewType: Int) = viewType match {
    *     case _ => new XxxVh(inflater, parent)
    *   }
    *   override def onBindViewHolder(vh: XxxVh, position: Int): Unit = vh.setData(getItem(position))
    * }
    *
    * @ViewLayoutId(R.layout.l_xxx)
    * class XxxVh(item: View) extends RecyclerView.ViewHolder(item) {
    *   @ViewId(value = R.id.text_title, visibility = View.VISIBLE)
    *   var title: TextView = _
    *   def this(inflater: LayoutInflater, parent: ViewGroup)(implicit context: Context) {
    *     this(inflater.inflate(Injector.layoutID(context, classOf[XxxVh]), parent, false))
    *     //TypedViewHolder.inflate(inflater, TR.layout.l_xxx, parent, attach = false)
    *     Injector.inject(this, itemView, classOf[XxxVh])
    *   }
    *   def setData(item: ???) {
    *     title.setText(item.xxx)
    *     ???
    *   }
    * }
    *
    * @ViewListId(R.id.recycler_view)
    * class XxxActy extends AbsActy with DataButt.RecyclerV[RecyclerView, String, XxxAdapter] with Ctx.Acty {
    *   override protected def newAdapter() = new XxxAdapter(this)
    *   protected def onSetupRecyclerView() = {
    *     val v = this.findViewById(listViewId).asInstanceOf[V]
    *     v.setLayoutManager(new LinearLayoutManager(implicitly))
    *     v
    *     // 或：
    *     vh.recycler_view.setLayoutManager(new LinearLayoutManager(implicitly))
    *     vh.recycler_view
    *   }
    * }
    * }}}
    */
  trait RecyclerV[V <: RecyclerView, D <: AnyRef, A <: AbsRecyclerAdapter[_ <: RecyclerView.ViewHolder, D]] extends Ctx.Abs {
    final lazy val recyclerView: V = onSetupRecyclerView()

    final lazy val vhAdapter: A = {
      val adapter = newAdapter()
      recyclerView.setAdapter(adapter)
      adapter
    }

    def updateListData(data: List[D]): Unit = vhAdapter.setDataSource(data)

    /** 可重写，也可在类头标注`@ViewListId`注解。也可直接重写`onSetupRecyclerView()`以忽略本调用。 */
    protected def listViewId: Int = Injector.listViewID(context, getClass)

    protected def newAdapter(): A
    protected def onSetupRecyclerView(): V
    /* = {
          val v = activity.findViewById(listViewId).asInstanceOf[V]
          v.setLayoutManager(new LinearLayoutManager(implicitly))
          v
        }*/
  }
}

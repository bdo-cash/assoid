/*
* Copyright (C) 2017-present, Wei.Chou(weichou2010@gmail.com)
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

package hobby.wei.c.persist

import java.util
import java.util.Collections
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.chenai.nakam.tool.cache.{Delegate, LazyGet, Lru, Memoize}
import hobby.wei.c.core.AbsApp

/**
  * @author Chenakam (chenai.nakam@gmail.com)
  * @version 1.0, 27/10/2017
  */
abstract class ModularStorer extends Storer.Wrapper

object noUser {
  def apply(): String = "no_user"
}

object ModularStorer {
  private val STORER_NAME = "storer-modular"
  private val STORER_META = STORER_NAME + "_meta"
  private val KEY_META = "meta"

  private val sCache = new Memoize[Key[ModularStorer], ModularStorer] with LazyGet with Lru {
    override protected val maxCacheSize = 5 // 保留5个实例
    override protected val delegate = new Delegate[Key[ModularStorer], ModularStorer] {
      /**
        * 从数据库加载内容。
        *
        * @param key 要加载的数据的键。
        * @return `Some(V)` 表示有数据，`None` 表示没有数据。
        */
      override def load(key: Key[ModularStorer]) = {
        val module = if (key.clearable) key.module + "_c" else key.module
        ensureModule2Meta(key.userId, module, key.clearable)
        Option(key.creator(getModule(key.userId, module))).ensuring(_.isDefined)
      }

      /**
        * 将数据存入到数据库。
        *
        * @param key   要存入的数据的键。
        * @param value 要存入的数据内容。
        * @return `Some(V)` 表示有数据，`None` 表示没有数据。
        */
      override def update(key: Key[ModularStorer], value: ModularStorer) = Option(value).ensuring(_.isDefined)
    }
  }

  private case class Key[+K <: ModularStorer](userId: String, module: String, clearable: Boolean, creator: Storer.Builder => K) extends Equals {
    require(userId.nonEmpty)
    require(module.nonEmpty)

    override def equals(any: scala.Any) = any match {
      case that: Key[_] if that.canEqual(this) => that.userId == this.userId && that.module == this.module && that.clearable == this.clearable
      case _ => false
    }

    override def canEqual(that: Any) = that.isInstanceOf[Key[_]]

    override def hashCode = 41 * (userId.hashCode + (41 * (module.hashCode + (if (clearable) 1 else 0))))
  }

  /**
    * 取得与参数指定的module关联的本对象。
    * <p>
    * 注意：虽然本对象里的方法可以在任何module被调用，但是请确保仅调用与参数module相关的方法，
    * 否则会造成混乱。因此，不建议将方法写在本类里面。
    * <p>
    * 不过也有在不同module下写同一个flag的需求，反正自己应该理清楚需求和存储关系。
    *
    * @param userId
    * @param module
    * @param clearable 是否可清除，以便在特定情况下（退出之后或登录之前）执行删除操作时。
    * @return
    */
  def get[K <: ModularStorer](userId: String, module: String, clearable: Boolean)(creator: Storer.Builder => K): K =
    sCache.get(Key(userId, module, clearable, creator)).get.as[K]

  private def getModule(userId: String, module: String) = Storer.Wrapper.get(AbsApp.get[AbsApp].getApplicationContext,
    STORER_NAME + "-" + module).withUser(userId).multiProcess

  private def ensureModule2Meta(userId: String, module: String, clear: Boolean): Unit = {
    val meta: Storer = getMeta(userId)
    val set: util.Set[String] = meta.getSharedPreferences.getStringSet(KEY_META, new util.HashSet[String] /*后面有add()操作*/)
    if (!set.contains(module)) {
      if (clear) meta.edit().putBoolean(module.ensuring(_ != KEY_META), true).commit()
      set.add(module)
      meta.edit.putStringSet(KEY_META, set).commit()
    }
  }

  private def getMeta(userId: String): Storer = Storer.Wrapper.get(AbsApp.get[AbsApp].getApplicationContext, STORER_META).withUser(userId).multiProcess.ok

  def clear(userId: String): Unit = {
    val meta: Storer = getMeta(userId)
    val set: util.Set[String] = meta.getSharedPreferences.getStringSet(KEY_META, Collections.emptySet[String])
    var b: Boolean = false
    import scala.collection.JavaConversions._
    set.toSeq.foreach { module =>
      if (meta.contains(module)) { // 是否有 clearable 标识，见上面的 meta.storeBoolean(module)。
        if (!b) b = true
        getModule(userId, module).ok.edit.clear.commit()
        set.remove(module)
      }
    }
    if (b) meta.edit.putStringSet(KEY_META, set).commit()
  }

  def clearNoUser(): Unit = clear(noUser())
}

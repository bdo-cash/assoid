/*
 * Copyright (C) 2017-present, Chenai Nakam(chenai.nakam@gmail.com)
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

package hobby.wei.c.persist.db

import android.database.sqlite.SQLiteOpenHelper
import com.fortysevendeg.mvessel.api.DatabaseProxy
import com.fortysevendeg.mvessel.api.impl.{AndroidCursor, AndroidDatabase, AndroidDatabaseFactory}

/**
  * @author Chenakam (chenai.nakam@gmail.com)
  * @version 1.0, 29/12/2017
  */
abstract class QuillAndroidDatabaseFactory extends AndroidDatabaseFactory {
  override def openDatabase(name: String, flags: Int): DatabaseProxy[AndroidCursor] =
    new AndroidDatabase(sqliteOpenHelper(name, flags).getWritableDatabase) {
      override def getDriverName: String = QuillAndroidDriver.driverName
    }

  /**
    * 把创建`SQLiteDatabase`的工厂指向`Android`提供的`SQLiteOpenHelper`，以确保`创建/升级`操作能正确完成。
    *
    * @param name  数据库名称（注意确保一致性）。
    * @param flags 数据库的读写模式参数（这里用不到）。
    * @return `SQLiteOpenHelper`实例。
    */
  def sqliteOpenHelper(name: String, flags: Int): SQLiteOpenHelper
}

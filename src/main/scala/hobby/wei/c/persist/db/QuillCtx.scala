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

package space.edge.d.wallet.persist.db

import java.util.Properties
import com.fortysevendeg.mvessel.DataSource
import com.fortysevendeg.mvessel.api.impl.AndroidCursor
import com.j256.ormlite.android.apptools.OpenHelperManager
import hobby.chenai.nakam.basis.IO.Close$
import hobby.wei.c.core.AbsApp
import hobby.wei.c.persist.db._
import io.getquill.{CamelCase, SqliteJdbcContext}

/**
  * @author Chenakam (chenai.nakam@gmail.com)
  * @version 1.0, 28/12/2017
  */
trait QuillCtx[HELPER <: AbsOrmLiteHelper] {
  protected def classOfDbHelper: Class[HELPER]

  def sqliteDbHelper: AbsOrmLiteHelper = OpenHelperManager.getHelper(AbsApp.get, classOfDbHelper)

  def mapDb[A](f: AbsOrmLiteHelper => A): A = {
    try f(sqliteDbHelper) finally OpenHelperManager.releaseHelper()
  }

  /**
    * `DriverManager`已经过时，`DataSource`是连接到数据源的`首选`方法。（见`DriverManager`开头的文档。）
    * `DriverManager` 用法示例（`https://github.com/47deg/mvessel`）：
    * {{{
    * val database: SQLiteDatabase = getReadableDatabase()
    * 2. Register the driver, making available for `java.sql`
    * com.fortysevendeg.mvessel.AndroidDriver.register()
    * 3. Open a connection using the path provided by the database
    * DriverManager.getConnection("jdbc:sqlite:" + database.getPath)
    * }}}
    */
  def dataSource: javax.sql.DataSource with java.io.Closeable = new DataSource[AndroidCursor](
    new QuillAndroidDriver {
      override def databaseFactory = new QuillAndroidDatabaseFactory {
        override def sqliteOpenHelper(name: String, flags: Int) = sqliteDbHelper.ensuring(_.getDatabaseName == name)
      }
    }, new Properties, mapDb(_.getReadableDatabase.getPath), DBLogWrapper) with java.io.Closeable {
    override def close(): Unit = {
      connection.close$()
      OpenHelperManager.releaseHelper() // 数据库关闭操作由
    }
  }

  lazy val quillCtx = new SqliteJdbcContext(CamelCase, dataSource)
}

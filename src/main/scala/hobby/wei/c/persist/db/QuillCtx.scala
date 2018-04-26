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

import java.io.File
import com.j256.ormlite.android.apptools.OpenHelperManager
import hobby.chenai.nakam.lang.J2S.Run
import hobby.wei.c.core.AbsApp
import hobby.wei.c.core.Ctx.%
import hobby.wei.c.reflow.Reflow
import hobby.wei.c.reflow.implicits._
import io.getquill.{CamelCase, SqliteJdbcContext}

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 28/12/2017
  */
trait QuillCtx[HELPER <: AbsOrmLiteHelper] extends %[AbsApp] {
  protected def classOfDbHelper: Class[HELPER]
  protected def databaseName: String

  private def sqliteDbHelper: AbsOrmLiteHelper = OpenHelperManager.getHelper(AbsApp.get, classOfDbHelper)

  def mapDb[A](f: AbsOrmLiteHelper => A): A = {
    try f(sqliteDbHelper) finally OpenHelperManager.releaseHelper()
  }

  def mapDb[A](toUi: A => Unit)(f: AbsOrmLiteHelper => A): Unit = {
    Reflow.submit {
      val value: A = mapDb(f)
      getApp.mainHandler.post(toUi(value).run$)
    }(TRANSIENT)
  }

  def mapCtx[A](f: SqliteJdbcContext[_] => A): A = {
    try f(quillCtx) finally OpenHelperManager.releaseHelper()
  }

  def mapCtx[A](toUi: A => Unit)(f: SqliteJdbcContext[_] => A): Unit = {
    Reflow.submit {
      val value: A = mapCtx(f)
      getApp.mainHandler.post(toUi(value).run$)
    }(TRANSIENT)
  }

  /**
    * `DriverManager`已经过时，`DataSource`是连接到数据源的`首选`方法。（见`DriverManager`开头的文档。）
    * `DriverManager`用法示例（`https://github.com/47deg/mvessel`）：
    * {{{
    * val database: SQLiteDatabase = getReadableDatabase()
    * 2. Register the driver, making available for `java.sql`
    * com.fortysevendeg.mvessel.AndroidDriver.register()
    * 3. Open a connection using the path provided by the database
    * DriverManager.getConnection("jdbc:sqlite:" + database.getPath)
    * }}}
    */
  def dataSource: javax.sql.DataSource with java.io.Closeable = new QuillDataSource(
    new QuillAndroidDriver {
      override def databaseFactory = new QuillAndroidDatabaseFactory {
        override def sqliteOpenHelper(path: String, flags: Int) = {
          sqliteDbHelper.ensuring(_.getDatabaseName == new File(path).getName, s"param{path: $path, flags: $flags}, ${sqliteDbHelper.getDatabaseName}")
        }
      }
    }, getApp.getDatabasePath(databaseName).getPath) {
    override def close(): Unit = {
      super.close()
      OpenHelperManager.releaseHelper() // 数据库关闭操作由它完成。
    }
  }

  lazy val quillCtx = new SqliteJdbcContext(CamelCase, dataSource)
}

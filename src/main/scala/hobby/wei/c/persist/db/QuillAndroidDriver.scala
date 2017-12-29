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

import java.util.Properties
import android.database.sqlite.SQLiteDatabase
import com.fortysevendeg.mvessel.{AndroidDriver, BaseDriver, Connection}
import com.fortysevendeg.mvessel.api.impl.AndroidCursor
import com.fortysevendeg.mvessel.util.DatabaseUtils.WrapSQLException
import hobby.wei.c.anno.proguard.Keep$

import scala.util.Try

/**
  * Copy 自`AndroidDriver`，目前是为了重写里面的`AndroidDatabaseFactory`和`LogWrapper`。
  *
  * @author Chenakam (chenai.nakam@gmail.com)
  * @version 1.0, 29/12/2017
  */
abstract class QuillAndroidDriver extends BaseDriver[AndroidCursor] {
  override def connect(connectionUrl: String, properties: Properties): Connection[AndroidCursor] =
    WrapSQLException(parseConnectionString(connectionUrl), s"Can't parse $connectionUrl") { values =>
      import com.fortysevendeg.mvessel.Connection._
      new Connection(
        databaseWrapperFactory = databaseFactory,
        databaseName = values.name,
        timeout = readTimeOut(values.params) getOrElse defaultTimeout,
        retryInterval = readRetry(values.params) getOrElse defaultRetryInterval,
        flags = readFlags(properties),
        logWrapper = DBLogWrapper)
    }

  def databaseFactory: QuillAndroidDatabaseFactory

  private[this] def readTimeOut(params: Map[String, String]): Option[Long] =
    readLongParam(BaseDriver.timeoutParam, params)

  private[this] def readRetry(params: Map[String, String]): Option[Int] =
    readLongParam(BaseDriver.retryParam, params) map (_.toInt)

  private[this] def readLongParam(name: String, params: Map[String, String]): Option[Long] =
    params.get(name) flatMap (value => Try(value.toLong).toOption)

  val flags = SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS

  private[this] def readFlags(properties: Properties): Int =
    Option(properties) match {
      case Some(p) =>
        completeFlags(
          p.getProperty(AndroidDriver.databaseFlags),
          p.getProperty(AndroidDriver.additionalDatabaseFlags))
      case _ => flags
    }

  private[this] def completeFlags(databaseFlags: String, androidFlags: String): Int =
    (Option(databaseFlags), Option(androidFlags)) match {
      case (Some(f), _) => parseInt(f, flags)
      case (None, Some(f)) => flags | parseInt(f, 0)
      case _ => flags
    }

  private[this] def parseInt(value: String, defaultValue: Int) =
    Try(value.toInt).toOption getOrElse defaultValue
}

@Keep$
object QuillAndroidDriver {
  val driverName = QuillAndroidDriver.getClass.getName
}

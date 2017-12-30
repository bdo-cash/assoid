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

import java.sql.{Connection => SQLConnection}
import java.util.Properties
import com.fortysevendeg.mvessel.{BaseDriver, Connection, DataSource}
import com.fortysevendeg.mvessel.api.impl.AndroidCursor
import com.fortysevendeg.mvessel.logging.LogWrapper
import hobby.chenai.nakam.basis.IO.Close$

/**
  * @author Chenakam (chenai.nakam@gmail.com)
  * @version 1.0, 30/12/2017
  */
class QuillDataSource(driver: BaseDriver[AndroidCursor],
                      dbPath: String,
                      properties: Properties = new Properties,
                      log: LogWrapper = DBLogWrapper)
  extends DataSource[AndroidCursor](driver: BaseDriver[AndroidCursor], properties: Properties, dbPath: String, log: LogWrapper) with java.io.Closeable {

  private[this] val url: String = "jdbc:sqlite:" + dbPath

  @deprecated(message = "用完就关闭了，不应该是单例。")
  override lazy val connection: Connection[AndroidCursor] = ???

  private var connect: Connection[AndroidCursor] = _

  override def getConnection: SQLConnection = {
    val conn = driver.connect(url, properties)
    synchronized(connect = conn)
    conn
  }

  override def close(): Unit = {
    Option(synchronized(connect)).foreach(_.close$())
  }
}

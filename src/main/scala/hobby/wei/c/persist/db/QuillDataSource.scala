/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2017-present, Chenai Nakam(chenai.nakam@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package hobby.wei.c.persist.db

import java.sql.{Connection => SQLConnection}
import java.util.Properties
import com.fortysevendeg.mvessel.{BaseDriver, DataSource}
import com.fortysevendeg.mvessel.api.impl.AndroidCursor
import com.fortysevendeg.mvessel.logging.LogWrapper

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 30/12/2017
  */
class QuillDataSource(driver: BaseDriver[AndroidCursor],
                      dbPath: String,
                      properties: Properties = new Properties,
                      log: LogWrapper = DBLogWrapper)
  extends DataSource[AndroidCursor](driver, properties, dbPath, log) {

  // 相关问题已经在`fork`的`mvessel`项目中的父类`DataSource`修复，但本类仍需存在：接入了`DBLogWrapper`。
  //@deprecated(message = "用完就关闭了，不应该是单例。")
  //override lazy val connection: Connection[AndroidCursor] = ???

  /**
    * 由于调用方通常会直接对`Connection`进行`close()`操作，如：
    * {{{
    *   val conn = dataSource.getConnection
    *       try f(conn)
    *       finally conn.close()
    * }}}
    * 所以这里不对返回对象进行任何缓存。
    *
    * @see `io.getquill.context.jdbc.JdbcContext`
    * @return `Connection`
    */
  override def getConnection: SQLConnection = driver.connect(url, properties)
}

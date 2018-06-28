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

import java.util.Properties
import com.fortysevendeg.mvessel.{AndroidDriver, Connection}
import com.fortysevendeg.mvessel.api.impl.AndroidCursor
import com.fortysevendeg.mvessel.util.DatabaseUtils.WrapSQLException
import hobby.wei.c.anno.proguard.Keep$

/**
  * Copy 自`AndroidDriver`，目的是为了重写里面的`AndroidDatabaseFactory`和`LogWrapper`。
  *
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 29/12/2017
  */
abstract class QuillAndroidDriver extends AndroidDriver {
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
}

@Keep$
object QuillAndroidDriver {
  val driverName = QuillAndroidDriver.getClass.getName
}

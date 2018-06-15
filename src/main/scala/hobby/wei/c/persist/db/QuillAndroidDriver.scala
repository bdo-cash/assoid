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
import android.database.sqlite.SQLiteDatabase
import com.fortysevendeg.mvessel.{AndroidDriver, BaseDriver, Connection}
import com.fortysevendeg.mvessel.api.impl.AndroidCursor
import com.fortysevendeg.mvessel.util.DatabaseUtils.WrapSQLException
import hobby.wei.c.anno.proguard.Keep$

import scala.util.Try

/**
  * Copy 自`AndroidDriver`，目的是为了重写里面的`AndroidDatabaseFactory`和`LogWrapper`。
  *
  * @author Chenai Nakam(chenai.nakam@gmail.com)
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

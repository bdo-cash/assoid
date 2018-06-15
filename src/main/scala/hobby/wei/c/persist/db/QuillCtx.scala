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

  private def referDbHelper(): AbsOrmLiteHelper = OpenHelperManager.getHelper(AbsApp.get, classOfDbHelper)
  private def releaseDbHelper(): Unit = OpenHelperManager.releaseHelper()

  def mapDb[A](f: AbsOrmLiteHelper => A): A = {
    try f(referDbHelper()) finally releaseDbHelper()
  }

  def mapDb[A](toUi: A => Unit)(f: AbsOrmLiteHelper => A): Unit = {
    Reflow.submit {
      val value: A = mapDb(f)
      getApp.mainHandler.post(toUi(value).run$)
    }(TRANSIENT)
  }

  def mapCtx[A](f: SqliteJdbcContext[_] => A): A = {
    try f(quillCtx) finally releaseDbHelper()
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
        override def referSqliteOpenHelper(path: String, flags: Int) = {
          val helper = referDbHelper()
          helper.ensuring(_.getDatabaseName == new File(path).getName, s"param{path: $path, flags: $flags}, ${helper.getDatabaseName}")
        }

        override def releaseSqliteOpenHelper(): Unit = releaseDbHelper()
      }
    }, getApp.getDatabasePath(databaseName).getPath) with java.io.Closeable {
    override def close(): Unit = {
      // Nothing to do. 仅为了满足`SqliteJdbcContext`的参数要求。
    }
  }

  lazy val quillCtx = new SqliteJdbcContext(CamelCase, dataSource)
}

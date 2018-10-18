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
import hobby.chenai.nakam.lang.J2S.Run
import hobby.wei.c.core.AbsApp
import hobby.wei.c.core.Ctx.%
import hobby.wei.c.reflow.Reflow
import hobby.wei.c.reflow.implicits._
import io.getquill.{ImplicitQuery, Literal, NamingStrategy, SqliteJdbcContext}

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 28/12/2017
  */
trait QuillCtx[HELPER <: AbsOrmLiteHelper] extends %[AbsApp] {
  private var dbOpenHelper: HELPER = _
  private var referCount = 0

  /** 延迟数据库的释放时间，以便引用计数较少地降低到`0`，从而减少触发数据库的真实打开和关闭。 */
  protected val delayReleaseTime = 90000 // 1.5 min

  protected def databaseName: String

  protected def newDbOpenHelper(): HELPER

  // 坑爹：这个不支持多个 helper 实例。
  // OpenHelperManager.getHelper(getApp, classOfDbHelper)
  private def referDbHelper(): AbsOrmLiteHelper = QuillCtx.this.synchronized {
    if (referCount == 0) dbOpenHelper = newDbOpenHelper()
    getApp.mainHandler.removeCallbacks(releaseRun)
    referCount += 1
    dbOpenHelper
  }

  private def releaseDbHelper(): Unit = QuillCtx.this.synchronized {
    if (referCount == 1) {
      getApp.mainHandler.postDelayed(releaseRun, delayReleaseTime)
    } else if (referCount > 1) referCount -= 1
  }

  private lazy val releaseRun = {
    // OpenHelperManager.releaseHelper()
    QuillCtx.this.synchronized {
      if (referCount == 1) {
        dbOpenHelper.close()
        dbOpenHelper = null.asInstanceOf[HELPER]
        referCount = 0
      }
    }
  }.run$

  def mapDb[A](f: AbsOrmLiteHelper => A): A = {
    try f(referDbHelper()) finally releaseDbHelper()
  }

  def mapDb[A](toUi: A => Unit)(f: AbsOrmLiteHelper => A): Unit = {
    Reflow.submit {
      val value: A = mapDb(f)
      getApp.mainHandler.post(toUi(value).run$)
    }(TRANSIENT)
  }

  def mapCtx[A](q: => A): A = try q finally {
    // nothing ...
  }

  def mapCtx[A](toUi: A => Unit)(q: => A): Unit = Reflow.submit {
    getApp.mainHandler.post(toUi(mapCtx(q)).run$)
  }(TRANSIENT)

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
  def dataSource: javax.sql.DataSource with java.io.Closeable = new QuillAndroidDataSource(
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

  /* 这种方式导致宏编译报错，经查源码，对于 Android Sqlite，
  `isEntityNamesMustBeUpCase()`总是返回 false，那就直接`Literal`好了。
  该方法主要是针对一些数据库的 bug 而设计，Sqlite 不存在这个问题。
  mapDb { h: AbsOrmLiteHelper =>
    new SqliteJdbcContext(if (h.getConnectionSource.getDatabaseType.
      isEntityNamesMustBeUpCase) UpperCase else CamelCase, dataSource)
  }*/

  class QuillDbCtx[N <: NamingStrategy](override val naming: N) extends SqliteJdbcContext(naming, dataSource) with ImplicitQuery

  lazy val quillCtx = new QuillDbCtx(Literal /*保持原样*/)

  /*abstract class RichTable[T, ID] extends Table[T, ID] {
    // 使用较频繁
    lazy val name: String = mapDb { h: AbsOrmLiteHelper =>
      DatabaseTableConfig.extractTableName(h.getConnectionSource.getDatabaseType, clazz)
    }

    lazy val columns: Seq[String] = mapDb { h: AbsOrmLiteHelper =>
      DatabaseTableConfigUtil.fromClass(h.getConnectionSource, clazz)
    }.getFieldConfigs.toSeq.map(_.getColumnName)

    lazy val fields: Seq[String] = ReflectUtils.getFields(clazz, clazz).map(_.getName)
  }*/
}

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

import android.database.sqlite.SQLiteOpenHelper
import com.fortysevendeg.mvessel.api.DatabaseProxy
import com.fortysevendeg.mvessel.api.impl.{AndroidCursor, AndroidDatabase, AndroidDatabaseFactory}

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 29/12/2017
  */
abstract class QuillAndroidDatabaseFactory extends AndroidDatabaseFactory {
  override def openDatabase(path: String, flags: Int): DatabaseProxy[AndroidCursor] =
    new AndroidDatabase(referSqliteOpenHelper(path, flags).getWritableDatabase) {
      override def getDriverName: String = QuillAndroidDriver.driverName

      override def close(): Unit = {
        // 不可以直接关闭
        // super.close()
        releaseSqliteOpenHelper()
      }
    }

  /**
    * 把创建`SQLiteDatabase`的工厂指向`Android`提供的`SQLiteOpenHelper`，以确保`创建/升级`操作能正确完成。
    *
    * @param path  数据库路径（注意确保一致性）。
    * @param flags 数据库的读写模式参数（这里用不到）。
    * @return `SQLiteOpenHelper`实例。
    */
  def referSqliteOpenHelper(path: String, flags: Int): SQLiteOpenHelper

  def releaseSqliteOpenHelper(): Unit
}

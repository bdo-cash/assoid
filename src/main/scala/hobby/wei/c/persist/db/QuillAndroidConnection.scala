/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2018-present, Chenai Nakam(chenai.nakam@gmail.com)
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

import java.sql.SQLException
import com.fortysevendeg.mvessel
import com.fortysevendeg.mvessel.api.impl.AndroidDatabaseFactory

/**
  * 修复`mvessel.Connection`无法启用事务（transaction）的问题。
  * 本实现参考：
  * [[com.j256.ormlite.android.AndroidDatabaseConnection]]。
  *
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 18/10/2018
  */
class QuillAndroidConnection(databaseWrapperFactory: AndroidDatabaseFactory,
                             databaseName: String,
                             timeout: Long = 0,
                             retryInterval: Int = 50,
                             flags: Int = 0) extends mvessel.Connection(
  databaseWrapperFactory, databaseName, timeout, retryInterval, flags, DBLogWrapper) {

  override def commit(): Unit = withOpenDatabase { db =>
    if (getAutoCommit)
      throw new SQLException(autoCommitErrorMessage)
    else {
      db.setTransactionSuccessful()
      db.endTransaction()
    }
  }

  override def getAutoCommit: Boolean = withOpenDatabase { db =>
    // You have to explicitly commit your transactions, so this is sort of correct
    return !db.database.inTransaction
  }

  override def setAutoCommit(autoCommit: Boolean): Unit = withOpenDatabase { db =>
    /*
		 * Sqlite does not support auto-commit. The various JDBC drivers seem to implement it with the use of a
		 * transaction. That's what we are doing here.
		 */
    if (autoCommit) {
      if (db.database.inTransaction) {
        db.setTransactionSuccessful()
        db.endTransaction()
      }
    } else {
      if (!db.database.inTransaction) {
        db.beginTransaction()
      }
    }
  }

  override def rollback(): Unit = withOpenDatabase { db =>
    if (getAutoCommit) {
      throw new SQLException(autoCommitErrorMessage)
    } else {
      db.execSQL(rollbackSql)
      // no setTransactionSuccessful() means it is a rollback
      db.endTransaction()
    }
  }
}

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

import com.fortysevendeg.mvessel.logging.LogWrapper
import hobby.chenai.nakam.basis.TAG
import hobby.wei.c.LOG

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 29/12/2017
  */
object DBLogWrapper extends LogWrapper with TAG.ClassName {
  override def d(msg: String): Unit = LOG.d(msg)

  override def e(msg: String, t: Option[Throwable]): Unit = t.fold(LOG.e(msg))(LOG.e(_, msg))

  override def i(msg: String): Unit = LOG.i(msg)

  override def v(msg: String): Unit = LOG.v(msg)

  override def w(msg: String): Unit = LOG.w(msg)
}

/*
 * Copyright (C) 2018-present, Chenai Nakam(chenai.nakam@gmail.com)
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

package hobby.wei.c.remote

import java.io.IOException
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.J2S.NonNull
import hobby.wei.c.core.AbsApp
import hobby.wei.c.file.FStoreLoc
import hobby.wei.c.LOG
import okhttp3._
import sbt.Path._

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 15/10/2018
  */
object NetAces {
  object Http {
    private[Http] trait AbsClient {
      protected val client: OkHttpClient
      def apply() = client

      @throws[IOException]
      def sync(request: Request): Response = client.newCall(request).execute().asChecked

      def async(request: Request, checked: CallbackChecked): Unit = client.newCall(request).enqueue(checked)
    }

    object Client extends AbsClient {
      private lazy val builder = new OkHttpClient.Builder()
      protected lazy val client = builder.build()

      def withInterceptor(interceptor: Interceptor): Client.type = {
        builder.addInterceptor(interceptor)
        this
      }

      def withCached(maxCacheSize: Int = 10 * 1024 * 1024 /*10 MB*/): Client.type = {
        builder.cache(
          new Cache(FStoreLoc.SURVIVE.getCacheDir(AbsApp.get,
            FStoreLoc.DirLevel.PRIVATE) / classOf[Cache].getName.toLowerCase,
            maxCacheSize)
        )
        this
      }
    }

    trait CallbackChecked extends Callback {
      override final def onResponse(call: Call, response: Response): Unit = {
        val resp =
          try response.asChecked
          catch {
            case e: IOException =>
              onFailure(call, e)
              null
          }
        if (resp.nonNull) onResponseOk(call, resp)
      }

      def onResponseOk(call: Call, response: Response): Unit
    }

    implicit class CheckSuccess(response: Response) extends TAG.ClassName {
      @throws[IOException]
      def asChecked: Response = {
        if (!response.isSuccessful) {
          val resp = response.toString
          LOG.e(resp)
          throw new IOException(resp)
        }
        response
      }
    }
  }
}

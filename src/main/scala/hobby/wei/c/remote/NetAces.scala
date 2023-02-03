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

import java.io.{File, IOException}
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.J2S.NonNull
import hobby.wei.c.core.AbsApp
import hobby.wei.c.file.FStoreLoc
import hobby.wei.c.LOG
import okhttp3._

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 15/10/2018
  */
object NetAces {
  object Http {
    private[Http] trait AbsClient {
      protected def client: OkHttpClient

      @throws[IOException]
      @throws[CodeNotSucceedException]
      def sync(request: Request): Response = client.newCall(request).execute().asChecked

      def async(request: Request, checked: CallbackChecked): Unit = client.newCall(request).enqueue(checked)
    }

    object Client extends AbsClient {
      private lazy val builder = new OkHttpClient.Builder()
      @volatile private var _client: OkHttpClient = _
      override def client = {
        if (_client == null) _client = builder.build()
        _client
      }

      def withBuilder(wizh: OkHttpClient.Builder => OkHttpClient.Builder): Client.type = {
        wizh(builder)
        _client = null
        this
      }

      def withCached(maxCacheSize: Int = 10 * 1024 * 1024 /*10 MB*/): Client.type = {
        builder.cache(
          new Cache(new File(FStoreLoc.SURVIVE.getCacheDir(AbsApp.get,
            FStoreLoc.DirLevel.PRIVATE), classOf[Cache].getName.toLowerCase),
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
            case e: CodeNotSucceedException =>
              onResponseNotSucceed(call, e)
              null
          }
        if (resp.nonNull) onResponseSucceed(call, resp)
      }

      def onResponseNotSucceed(call: Call, code: CodeNotSucceedException)
      def onResponseSucceed(call: Call, response: Response): Unit
    }

    implicit class CheckSuccess(response: Response) extends TAG.ClassName {
      @throws[CodeNotSucceedException]
      def asChecked: Response = {
        if (!response.isSuccessful) {
          LOG.e(response.toString)
          throw new CodeNotSucceedException(response.code(), response.message(), response)
        }
        response
      }
    }
  }

  class CodeNotSucceedException(val code: Int, val message: String, val response: Response) extends IOException(s"code: $code, message: $message.")
}

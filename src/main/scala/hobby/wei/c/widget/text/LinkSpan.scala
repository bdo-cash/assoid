/*
 * Copyright (C) 2014-present, Wei Chou (weichou2010@gmail.com)
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

package hobby.wei.c.widget.text

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.provider.Browser
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import hobby.chenai.nakam.lang.J2S.NonNull

/**
  * @author 周伟 Wei Chou(weichou2010@gmail.com)
  */
class LinkSpan(linkColor: ColorStateList, url: String) extends ClickableSpan with SpanLinkable {
  //[.]点字符放在括号里只能匹配其本身，等同于\\.，若单独放在外面则匹配除\n之外所有字符
  private val mUrlRegex = "^(http://).+"

  override protected val mLinkColor = linkColor

  def getURL = url

  override def onClick(widget: View) {
    if (url.nonNull && url.trim().matches(mUrlRegex)) {
      val uri = Uri.parse(url)
      val context = widget.getContext
      val intent = new Intent(Intent.ACTION_VIEW, uri)
      intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName)
      context.startActivity(intent)
    }
  }

  override def updateDrawState(ds: TextPaint): Unit = super.updateDrawState(ds)
}

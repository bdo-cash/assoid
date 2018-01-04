/*
 * Copyright (C) 2014-present, Wei Chou(weichou2010@gmail.com)
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

package hobby.wei.c.widget.text;

import android.text.Spannable;

public class SpanUtils {

    public static int setSpan(Spannable spannable, Object span, int[] loc) {
        if (loc[0] >= 0) {
            setSpan(spannable, span, loc[0], loc[1]);
            return loc[1];
        }
        return 0;
    }

    public static void setSpan(Spannable spannable, Object span, int start, int end) {
        spannable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /*评分结果专用，勿修改**/
    public static int setSpan(Spannable spannable, String string, int searchStart, Object span) {
        return setSpan(spannable, string, searchStart, span, true, false);
    }

    public static int setSpanWithColoredWord(Spannable spannable, String string, int searchStart, Object span) {
        return setSpan(spannable, string, searchStart, span, true, true);
    }

    /**
     * 给Spannable中指定字符串设定指定的span
     *
     * @param spannable
     * @param string
     * @param searchStart
     * @param span
     * @param includeEndPunctuation 是否将string后面的标点符号包含在span之内
     * @return 返回span的结束位置
     */
    public static int setSpan(Spannable spannable, String string, int searchStart, Object span, boolean irgnoreCase, boolean includeEndPunctuation) {
        int[] loc = getWordLocation(spannable.toString(), string, searchStart, irgnoreCase, includeEndPunctuation);
        return setSpan(spannable, span, loc);
    }

    public static int[] getWordLocation(String text, String word, int searchStart) {
        return getWordLocation(text, word, searchStart, false, false);
    }

    public static int[] getWordLocation(String text, String word, int searchStart, boolean irgnoreCase, boolean includeEndPunctuation) {
        if (irgnoreCase) {
            text = text.toLowerCase();
            word = word.toLowerCase();
        }
        int end = -1, start = text.indexOf(word, searchStart);
        if (start >= 0) {    //starts如果大于0，则必然start >= searchStart
            end = start + word.length();
            if (includeEndPunctuation) while (end < text.length() && isPunctuations(text.charAt(end))) end++;
        }
        return new int[]{start, end};
    }

    public static boolean isPunctuations(char c) {
        //return (c >= 33 && c <= 47) || (c >= 58 && c <= 64) || (c >= 91 && c <= 96) || (c >= 123 && c <= 254);
        return c >= 33 && c <= 254 && !(c == ' ' || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'));
    }
}

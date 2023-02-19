/*
 * Copyright (C) 2023-present, Chenai Nakam(chenai.nakam@gmail.com)
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

package scala.compat

import hobby.chenai.nakam.basis.TAG.ClassName
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.wei.c.LOG
import hobby.wei.c.LOG._
import java.lang.reflect.{Field, Method, Modifier}
import scala.reflect.ClassTag

/**
  * Set `cacheDisabled` in [[ClassTag]] to `true`.
  * <p>
  * The point is that after Proguard/R8, the names of variables and methods change (may also be deleted if is not used).
  * {{{
  *   private val cacheDisabled = java.lang.Boolean.getBoolean("scala.reflect.classtag.cache.disable")
  * }}}
  */
object classTagDisableCache extends ClassName {

  // The implementation of this method cannot have any implicit conversions, because
  // it is likely that the 'Class Tag' will be involved again, and it will die recursively.
  def apply(disable: Boolean = true): (Boolean, Option[Boolean]) = {
    val classTagClass = ClassTag.getClass // classOf[ClassTag[_]]
    /*val cacheDisabledMethods = classTagClass.getDeclaredMethods.filter { m =>
       m.getReturnType == classOf[Boolean] && m.getParameterCount == 1 && m.getParameterTypes()(0) == classOf[Boolean]
    }*/
    def filter[T](f: Class[_] => Array[T])(cond: T => Boolean): List[T] = {
      var lis = Nil.as[List[T]]
      val arr = f(classTagClass)
      var i   = 0
      while (i < arr.length) {
        def e = arr(i)
        if (cond(e)) { lis ::= e }
        i += 1
      }
      lis
    }
    val `'cacheDisabled'fields`: List[Field] = filter(_.getDeclaredFields) {
      _.getType == classOf[Boolean]
    }
    `'cacheDisabled'fields`.foreach { f =>
      LOG.e(s"%s | <'cacheDisabled' field> %s: %s = ?", classTagClass.getName.s, f.getName.s, f.getType.getName.s)
    }
    if (`'cacheDisabled'fields`.length == 1) {
      val cacheDisabled = `'cacheDisabled'fields`.head
      val aces          = cacheDisabled.isAccessible
      cacheDisabled.setAccessible(true)
      if (Modifier.isStatic(cacheDisabled.getModifiers))
        cacheDisabled.set(null, if (disable) java.lang.Boolean.TRUE else java.lang.Boolean.FALSE) // static
      else cacheDisabled.set(ClassTag, if (disable) java.lang.Boolean.TRUE else java.lang.Boolean.FALSE)
      cacheDisabled.setAccessible(aces)

      //ClassValue.isClassTagCacheDisable = disable
      return (true, Some(disable))
    }
    val `'cacheDisabled'methods`: List[Method] = filter(_.getDeclaredMethods) { m =>
      m.getReturnType == classOf[Boolean] && m.getParameterCount == 1 && m.getParameterTypes()(0) == classOf[Boolean]
    }
    `'cacheDisabled'methods`.foreach { m =>
      LOG.e(s"%s | <'cacheDisabled' method> %s(_: %s): %s", classTagClass.getName.s, m.getName.s, m.getParameterTypes()(0).getName.s, m.getReturnType.getName.s)
    }
    if (`'cacheDisabled'methods`.length == 1) {
      val cacheDisabled = `'cacheDisabled'methods`.head
      val aces          = cacheDisabled.isAccessible
      cacheDisabled.setAccessible(true)
      if (Modifier.isStatic(cacheDisabled.getModifiers))
        cacheDisabled.invoke(null, if (disable) java.lang.Boolean.TRUE else java.lang.Boolean.FALSE) // static
      else cacheDisabled.invoke(ClassTag, if (disable) java.lang.Boolean.TRUE else java.lang.Boolean.FALSE)
      cacheDisabled.setAccessible(aces)

      //ClassValue.isClassTagCacheDisable = disable
      return (true, Some(disable))
    }
    val `'cacheDisabled'gets`: List[Method] = filter(_.getDeclaredMethods) { m =>
      m.getReturnType == classOf[Boolean] && m.getParameterCount == 0
    }
    if (`'cacheDisabled'gets`.length == 1) {
      val cacheDisabled = `'cacheDisabled'gets`.head
      val aces          = cacheDisabled.isAccessible
      cacheDisabled.setAccessible(true)
      val disabled = (if (Modifier.isStatic(cacheDisabled.getModifiers))
                        cacheDisabled.invoke(null) // static
                      else cacheDisabled.invoke(ClassTag)).as[java.lang.Boolean]
      cacheDisabled.setAccessible(aces)

      //ClassValue.isClassTagCacheDisable = disabled.booleanValue()
      return (false, Some(disabled))
    }
    (false, None)
  }
}

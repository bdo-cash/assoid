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

package hobby.wei.c.persist.db

import java.lang.reflect.Field
import android.content.ContentValues
import android.database.{Cursor, MatrixCursor}
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.J2S.NonFlat$
import hobby.chenai.nakam.tool.cache.{Delegate, LazyGet, Lru, Memoize}
import hobby.wei.c.LOG._
import hobby.wei.c.data.abs.{AbsJson, TypeToken}
import hobby.wei.c.util.ReflectUtils

import scala.reflect.ClassTag

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 26/06/2018
  */
object implicits extends TAG.ClassName {
  private lazy val fieldsCache = new Memoize[Class[_], (Array[Field], Array[String])] with Lru with LazyGet {
    override protected val maxCacheSize = 20
    override protected val delegate = new Delegate[Class[_], (Array[Field], Array[String])] {
      override def load(clazz: Class[_]) = {
        val fields = ReflectUtils.getFields(clazz, clazz).toArray(new Array[Field](0))
        Option((fields, fields.map(_.getName)))
      }

      override def update(clazz: Class[_], value: (Array[Field], Array[String])) = Option(value)
    }
  }

  val KEY_4_JSON_CONTENT_VALUES = "key_4_json_content_values"

  implicit class Entity2ContentValues[T <: AnyRef](entity: T) {
    @inline def asCtValues: ContentValues = {
      val values = new ContentValues
      values.put(KEY_4_JSON_CONTENT_VALUES, AbsJson.toJsonWithAllFields(entity))
      values
    }
  }

  implicit class ContentValues2Entity(values: ContentValues) {
    @inline def asEntity[T <: AnyRef](clazz: Class[T]): T = AbsJson.fromJsonWithAllFields(values.getAsString(KEY_4_JSON_CONTENT_VALUES), clazz)

    @inline def asEntity[T <: AnyRef](token: TypeToken[T]): T = AbsJson.fromJsonWithAllFields(values.getAsString(KEY_4_JSON_CONTENT_VALUES), token)
  }

  implicit class Entity2Cursor[T <: AnyRef](rows: List[T]) {
    def asCursor(columns: Array[String])(implicit ctag: ClassTag[T]): Cursor = {
      val clazz = ctag.runtimeClass
      // val (fields, columns) = fieldsCache.get(clazz).get
      val fields = columns.map { name =>
        val field = ReflectUtils.getField(clazz, name)
        field.setAccessible(true)
        field
      }
      w("asCursor | name: %s, fields: %s.", clazz.getName.s, columns.mkString$.s)
      val cursor = new MatrixCursor(columns, rows.length)
      rows.foreach { o =>
        val values = fields.map { field =>
          var v = field.get(o)
          if (v == None) v = null
          v
        }
        i("asCursor | addRow: %s.", values.mkString$.s)
        cursor.addRow(values)
      }
      cursor
    }
  }

  implicit class Cursor2Entity(cursor: Cursor) {
    def asEntity[T <: AnyRef](columns: Array[String]) /*(clazz: Class[T])*/ (gen: (Cursor, Array[Int]) => T): List[T] = {
      var rows: List[T] = Nil
      // val (_, columns) = fieldsCache.get(clazz).get
      // i("asEntity | name: %s, fields: %s.", clazz.getName.s, columns.mkString$.s)
      val indexes: Array[Int] = columns.map(cursor.getColumnIndex)
      while (cursor.moveToNext()) {
        rows ::= gen(cursor, indexes)
      }
      cursor.close()
      rows.reverse
    }
  }
}

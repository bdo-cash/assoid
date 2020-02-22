/*
 * Copyright (C) 2017-present, Chenai Nakam(chenai.nakam@gmail.com)
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

package hobby.wei.c.core

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import hobby.chenai.nakam.lang.J2S.NonFlat
import hobby.wei.c.LOG._

import scala.collection.mutable

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 08/12/2017
  */
@SuppressLint(Array("NewApi"))
object PermissionReq {
  private[PermissionReq] trait Abs extends Ctx.AbsUi with ReqCode {
    private val permissionsDenied = mutable.HashSet[(String, Boolean)]()
    private var needShowRationale = List[String]()

    /**
      * 要申请的一些权限。 `_1`表示权限名称，见{{{
      * android.Manifest.permission.XXX
      * }}}，`_2`表示如果用户取消授权，是否忽略（即：是否认为全部通过）。
      */
    protected val permissions: Seq[(String, Boolean)]

    /**
      * 告诉用户需要这个权限的理由（之前又被拒绝过一次）。
      *
      * @param permission
      * @param feedback 如果用户同意再次授权，那么就调用本毁掉的`ok()`方法。
      */
    protected def onShowPermissionRationale(permission: String, feedback: Feedback)

    protected def onPermissionGranted(permission: String): Unit = {}

    protected def onPermissionDenied(permission: String): Unit = {}

    /** 所有必须要求授权的都通过了。见 {{{permissions}}}的第二个参数。 */
    protected def onForcePermissionGranted(): Unit = {}

    def ifGranted[T](permission: String, forceReq: Boolean = true)(action: => T): Option[T] = {
      if (isPermissionGranted(permission) || !forceReq) Option(action) else None
    }

    def isPermissionGranted(permission: String): Boolean = activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    /** 请求动态授权。 */
    def requirePermissions(): Boolean = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) true else {
      permissionsDenied.clear()
      needShowRationale = Nil
      var reqDirect = List[String]()
      for (p <- permissions; perm = p._1) {
        if (!isPermissionGranted(perm)) {
          permissionsDenied += p
          if (activity.shouldShowRequestPermissionRationale(perm)) {
            i("shouldShowRequestPermissionRationale: %s.", perm.s)
            needShowRationale ::= perm
          } else reqDirect ::= perm
        }
      }
      if (reqDirect.nonEmpty) activity.requestPermissions(reqDirect.toArray, REQUEST_CODE)
      else showReqPermRationale()
      !permissionsDenied.exists(_._2)
    }

    private def showReqPermRationale(): Unit = {
      while (needShowRationale.nonEmpty && isPermissionGranted(needShowRationale.head)) {
        needShowRationale = needShowRationale.tail
      }
      if (needShowRationale.nonEmpty) {
        val perm = needShowRationale.head
        needShowRationale = needShowRationale.tail
        onShowPermissionRationale(perm, new Feedback {
          override def ok(): Unit = activity.requestPermissions(Array(perm), REQUEST_CODE)
        })
      }
    }

    protected def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
      if (requestCode == REQUEST_CODE) {
        for (i <- permissions.indices; perm = permissions(i); result = grantResults(i)) {
          if (result == PackageManager.PERMISSION_GRANTED) {
            permissionsDenied.filter(_._1 == perm).foreach(permissionsDenied -= _)
            onPermissionGranted(perm)
          } else onPermissionDenied(perm)
        }
        // 再查询一次，不然由于在同一个组的权限可能仅显式授权一个，另一个不需要再次被授权而
        // permissionsDenied列表里又没有被删除，导致不会回调onForcePermissionGranted()。
        for (den <- permissionsDenied.clone() if isPermissionGranted(den._1)) {
          permissionsDenied.filter(_._1 == den._1).foreach(permissionsDenied -= _)
        }
        showReqPermRationale()
        w("permissionsDenied: %s.", permissionsDenied.toSeq.mkString$.s)
        if (!permissionsDenied.exists(_._2)) onForcePermissionGranted()
      }
    }
  }

  trait Acty extends Ctx.Acty with Abs {
    override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  trait Fragmt extends Ctx.Fragmt with Abs {
    override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  trait Dialog extends Ctx.Dialog with Fragmt
}

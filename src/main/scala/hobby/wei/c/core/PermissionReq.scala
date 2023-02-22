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
import hobby.chenai.nakam.lang.J2S.{NonFlat, Obiter}
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.wei.c.LOG._
import scala.collection.mutable

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 08/12/2017
  */
@SuppressLint(Array("NewApi"))
object PermissionReq {

  private[PermissionReq] trait Abs extends Ctx.AbsUi with ReqCode {
    private val permissionsDenied    = mutable.HashSet[(String, Boolean)]()
    private var needShowRationale    = Nil.as[List[String]]
    private var isForceGrantedCalled = false
    private var isRetried            = false
    private def reqCode              = REQUEST_CODE_(permissionReqCode)

    /** 符合[[permissions]]定义。 */
    private def isForceGranted = {
      // 再查询一次（从`onRequestPermissionsResult()`移过来的），不然由于在同一个组的权限可能仅显式授权一个，另一个不需要再次
      // 被授权而`permissionsDenied`列表里又没有被删除，导致不会回调`onForcePermissionGranted()`。
      for (p <- permissionsDenied.clone(); perm = p._1 if isPermissionGranted(perm)) {
        permissionsDenied.filter(_._1 == perm).foreach(permissionsDenied -= _)
      }
      w("[isForceGranted]permissionsDenied:%s", permissionsDenied.toSeq.mkString$.s)
      !permissionsDenied.exists(_._2)
    }
    ////////// ////////// ////////// ////////// ////////// ////////// ////////// ////////// ////////// //////////
    /**
      * 要申请的一些权限。<br>
      * `_1`表示权限名称，见[[android.Manifest.permission.XXX]]；<br>
      * `_2`表示如果用户拒绝该权限，是否忽略（即：是否认为全部通过）：
      * `true`不忽略（若用户点击拒绝，则[[requirePermissions]]返回`false`，也【不】会回调[[onForcePermissionGranted]]），
      * `false`可忽略（若用户点击拒绝，[[requirePermissions]]依然返回`true`，也会回调[[onForcePermissionGranted]]）。
      */
    protected val permissions: Seq[(String, Boolean)]
    protected val permissionReqCode: Int

    /**
      * 告诉用户需要这个权限的理由（之前又被拒绝过一次）。
      *
      * @param permission 被拒绝的权限
      * @param feedback 如果用户同意再次授权，那么就调用该回调的`ok()`方法。
      */
    protected def onShowPermissionRationale(permission: String, feedback: Feedback): Unit

    protected def onPermissionGranted(permission: String): Unit = {}

    protected def onPermissionDenied(permission: String): Unit = {}

    /** 所有必须要求授权的都通过了。见[[permissions]]的第二个参数。 */
    protected def onForcePermissionGranted(): Unit = {}

    ////////// ////////// ////////// ////////// ////////// ////////// ////////// ////////// ////////// //////////
    def ifGranted[T](permission: String, forceReq: Boolean = true)(action: => T): Option[T] = {
      if (isPermissionGranted(permission) || !forceReq) Option(action) else None
    }

    def isPermissionGranted(permission: String): Boolean = activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    /** 请求动态授权。 */
    def requirePermissions(retry: Boolean = false): Boolean =
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) true
      else {
        permissionsDenied.clear()
        needShowRationale = Nil
        if (!retry) isForceGrantedCalled = false
        var reqDirect = Nil.as[List[String]]
        for (p <- permissions; perm = p._1) {
          if (!isPermissionGranted(perm)) {
            permissionsDenied += p
            if (activity.shouldShowRequestPermissionRationale(perm)) {
              i("[requirePermissions]shouldShowRequestPermissionRationale:%s", perm.s)
              needShowRationale ::= perm
            } else reqDirect ::= perm
          }
        }
        if (reqDirect.nonEmpty) activity.requestPermissions(reqDirect.toArray, reqCode)
        else showReqPermRationale()
        isForceGranted.obiter { onForcePermissionGrantedInner() }
      }

    private def onForcePermissionGrantedInner() {
      if (!isForceGrantedCalled) {
        isForceGrantedCalled = true
        onForcePermissionGranted()
      }
    }

    private def showReqPermRationale(): Unit = {
      while (needShowRationale.nonEmpty && isPermissionGranted(needShowRationale.head)) {
        needShowRationale = needShowRationale.tail
      }
      if (needShowRationale.nonEmpty) {
        val perm = needShowRationale.head
        needShowRationale = needShowRationale.tail
        onShowPermissionRationale(
          perm,
          new Feedback {
            override def ok() = activity.requestPermissions(Array(perm), reqCode)
          }
        )
      }
    }

    protected def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
      if (requestCode == reqCode) {
        var isDenied = false
        for (i <- permissions.indices; perm = permissions(i); result = grantResults(i)) {
          if (result == PackageManager.PERMISSION_GRANTED) {
            permissionsDenied.filter(_._1 == perm).foreach(permissionsDenied -= _)
            onPermissionGranted(perm)
          } else {
            onPermissionDenied(perm)
            if (!isDenied) isDenied = true
          }
        }
        if (isDenied && !isRetried) {
          isRetried = true
          requirePermissions(true)
        } else {
          if (isForceGranted) onForcePermissionGrantedInner()
          showReqPermRationale()
        }
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

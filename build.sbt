enablePlugins(AndroidLib)

name := baseDirectory.value.getName // 意为android assists

organization := "hobby.chenai.nakam"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.7"

// 等同于两句：targetSdkVersion, compileSdkVersion
platformTarget in Android := "android-26"

buildToolsVersion in Android := Some("26.0.1")

minSdkVersion in Android := "23"

//lazy val root = (project in file(".")).dependsOn(project in file("../lang"))

// plugin.sbt 里面的设置已注释掉，官方文档没有那一句，从 Scaloid 里面拿来的。
proguardVersion := "5.2.1" // 必须高于 5.1，见 https://github.com/scala-android/sbt-android。

libraryDependencies += "com.android.support" % "appcompat-v7" % "26.+"

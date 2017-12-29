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

offline := true

libraryDependencies ++= Seq(
  "com.android.support" % "appcompat-v7" % "26.+",
  "com.j256.ormlite" % "ormlite-android" % "[4.48,)",

  "io.getquill" %% "quill-jdbc" % "[2.3.0,)",
  // 我们不用这个驱动（不过还是得导入，如果不导入的话，会自动导入一个低版本的）。
  "org.xerial" % "sqlite-jdbc" % "[3.18.0,)",
  // 这是基于 android SQLiteDatabase 开发的一个 jdbc 驱动。
  "com.fortysevendeg" %% "mvessel-android" % "[0.1,)"
)

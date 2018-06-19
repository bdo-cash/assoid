enablePlugins(AndroidLib)

name := baseDirectory.value.getName // 意为android assists

organization := "hobby.chenai.nakam"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.7"

// 等同于两句：targetSdkVersion, compileSdkVersion
platformTarget in Android := "android-26"

buildToolsVersion in Android := Some("26.0.1")

minSdkVersion in Android := "5"

//lazy val root = (project in file(".")).dependsOn(project in file("../lang"))

// plugin.sbt 里面的设置已注释掉，官方文档没有那一句，从 Scaloid 里面拿来的。
proguardVersion := "5.2.1" // 必须高于 5.1，见 https://github.com/scala-android/sbt-android。

offline := true

// 解决生成文档报错导致 jitpack.io 出错的问题。
publishArtifact in packageDoc := false

// 如果下载不下来 android support 包再开启。
//resolvers += Resolver.jcenterRepo
//resolvers += "google" at "https://maven.google.com"
// 如果要用 jitpack 打包的话就加上，打完了再注掉。
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(
  // 如果要用 jitpack 打包的话就加上，打完了再注掉。
  // TODO: 独立使用本库的话，应该启用本依赖。
  "com.github.dedge-space" % "annoguard" % "1.0.3-beta",
  "com.github.dedge-space" % "annoid" % "ac8b616eec",
  "com.github.dedge-space" % "scala-lang" % "4db02cf2fd",
  "com.github.dedge-space" % "reflow" % "7d1eac2937",

  "com.android.support" % "appcompat-v7" % "26.+",
  "com.android.support" % "recyclerview-v7" % "26.+",
  "com.j256.ormlite" % "ormlite-android" % "[5.0,)",

  "io.getquill" %% "quill-jdbc" % "[2.3.0,)",
  // 我们不用这个驱动（不过还是得导入，如果不导入的话，会自动导入一个低版本的）。
  "org.xerial" % "sqlite-jdbc" % "[3.18.0,)",
  // 这是基于 android SQLiteDatabase 开发的一个 jdbc 驱动。
  "com.fortysevendeg" %% "mvessel-android" % "[0.1,)"
)

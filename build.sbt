enablePlugins(AndroidLib)

name := baseDirectory.value.getName // 意为android assists

organization := "hobby.chenai.nakam"

version := "0.1.2-SNAPSHOT"

scalaVersion := "2.11.11"

crossScalaVersions := Seq(
  /*"2.11.7", 多余，不需要两个*/
  "2.11.11",
  /*"2.12.2", 有一些编译问题：`the interface is not a direct parent`。*/
  "2.12.6")

// 等同于两句：targetSdkVersion, compileSdkVersion
platformTarget in Android := "android-27"

buildToolsVersion in Android := Some("27.0.3")

minSdkVersion in Android := "21"

//lazy val root = (project in file(".")).dependsOn(project in file("../lang"))

// plugin.sbt 里面的设置已注释掉，官方文档没有那一句，从 Scaloid 里面拿来的。
proguardVersion := "5.2.1" // 必须高于 5.1，见 https://github.com/scala-android/sbt-android。

offline := true

// 解决生成文档报错导致 jitpack.io 出错的问题。
publishArtifact in packageDoc := false

resolvers += "google" at "https://maven.google.com"
resolvers += "jitpack" at "https://jitpack.io"
//resolvers += "sqlite-jdbc-driver-mvessel-android" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  // 如果要用 jitpack 打包的话就加上，打完了再注掉。
  // TODO: 独立使用本库的话，应该启用本依赖。
  "com.github.dedge-space" % "annoguard" % "1.0.3-beta",
  "com.github.dedge-space" % "annoid" % "993253b451",
  "com.github.dedge-space" % "scala-lang" % "ccaa2cb00f",
  "com.github.dedge-space" % "reflow" % "dc5b40e255",

  "com.squareup.okhttp3" % "okhttp" % "3.11.0",

  // 使用 sbt Nb 的库。
  "org.scala-sbt" % "io" % "0.13.16",
  // "org.scala-sbt" % "api" % "0.13.16",

  "com.android.support" % "appcompat-v7" % "27.1.1" excludeAll (
    ExclusionRule(organization = "android.arch.core") /*,
    // `Fragment`里面用到一些组件。
    ExclusionRule(organization = "android.arch.lifecycle")*/
    ),
  "com.android.support" % "recyclerview-v7" % "27.1.1",
  "com.j256.ormlite" % "ormlite-android" % "[5.1,)",

  "io.getquill" %% "quill-jdbc" % "[2.5.4,)",
  // 我们不用这个驱动（不过还是得导入，如果不导入的话，会自动导入一个低版本的）。
  "org.xerial" % "sqlite-jdbc" % "[3.23.1,)",
  // 这是基于 android SQLiteDatabase 开发的一个 jdbc 驱动。
  // "com.fortysevendeg" %% "mvessel-android" % "[0.1,)"
  // 更新上面`mvessel`库的 Scala 编译版本到`2.11.11`。
  // 由于是用`jitpack`打包的，首先需要用这样一行触发打包，然后再注掉仅启用下面一行。
  // "com.github.dedge-space" % "mvessel" % "d91e517998",
  "com.github.dedge-space.mvessel" %% "mvessel-android" % "d91e517998",

  // 由于一个奇葩的编译异常：
  // ... Class javax.annotation.Nullable not found - continuing with a stub.
  // [error]       protected val client: OkHttpClient
  // 实际上这里根本没有写任何与`javax.annotation.Nullable`相关的内容。
  "com.google.code.findbugs" % "jsr305" % "3.0.+"
)

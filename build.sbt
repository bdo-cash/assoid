// format: off
enablePlugins(AndroidLib)

name := baseDirectory.value.getName // 意为android assists

organization := "hobby.chenai.nakam"

version := "1.2.0"

scalaVersion := "2.11.12"

crossScalaVersions := Seq(
  /*"2.11.7", 多余，不需要两个*/
  "2.11.12",
  /*"2.12.2", 有一些编译问题：`the interface is not a direct parent`。*/
  "2.12.12"
)

// 等同于两句：targetSdkVersion, compileSdkVersion
platformTarget in Android := "android-30"

buildToolsVersion in Android := Some("30.0.3")

minSdkVersion in Android := "26"

//lazy val root = (project in file(".")).dependsOn(project in file("../lang"))

// plugin.sbt 里面的设置已注释掉，官方文档没有那一句，从 Scaloid 里面拿来的。
proguardVersion := "[5.2.1,)" // 必须高于 5.1，见 https://github.com/scala-android/sbt-android。

offline := true

// 解决生成文档报错导致 jitpack.io 出错的问题。
publishArtifact in packageDoc := false

resolvers += "google" at "https://maven.google.com"
resolvers += "jitpack" at "https://jitpack.io"
//resolvers += "sqlite-jdbc-driver-mvessel-android" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  // 如果要用 jitpack 打包的话就加上，打完了再注掉。
  // TODO: 独立使用本库的话，应该启用本依赖。
  "com.github.dedge-space" % "annoguard" % "v1.0.5-beta",
  "com.github.dedge-space" % "annoid" % "9dc018cf45",
  "com.github.dedge-space" % "scala-lang" % "04917192ee",
  "com.github.dedge-space" % "reflow" % "5f1712df42",

  "com.squareup.okhttp3" % "okhttp" % "[4.8.0,)",

  // 使用 sbt Nb 的库。
  "org.scala-sbt" % "io" % "0.13.18",
  // "org.scala-sbt" % "api" % "0.13.18",

  // 在主工程中加入 `transitiveAndroidLibs in Android := true` 可以解决`Error: more than one xxx "xxx"`的问题。
  "androidx.localbroadcastmanager" % "localbroadcastmanager" % "[1.0.0,)",
  "androidx.appcompat" % "appcompat" % "[1.1.0,)",
  "androidx.recyclerview" % "recyclerview" % "[1.0.0,)",
  "com.j256.ormlite" % "ormlite-android" % "[5.1,)",

  // 由于一个奇葩的编译异常：
  // ... Class javax.annotation.Nullable not found - continuing with a stub.
  // [error]       protected val client: OkHttpClient
  // 实际上这里根本没有写任何与`javax.annotation.Nullable`相关的内容。
  "com.google.code.findbugs" % "jsr305" % "3.0.+"
)

// TODO: 打算用 Slick for Sqlite 替换掉 Quill.
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.3.2",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  //"com.typesafe.slick" %% "slick-hikaricp" % "3.3.2",
  "org.sqldroid" % "sqldroid" % "[1.0.3,)"
)

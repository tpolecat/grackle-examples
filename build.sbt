val lucumaGraphQLRoutesVersion = "0.5.11"
val http4sBlazeVersion         = "0.23.13"

enablePlugins(NoPublishPlugin)

ThisBuild / scalaVersion       := "3.2.2"
ThisBuild / crossScalaVersions := Seq("3.2.2")

lazy val service = project
  .in(file("modules/service"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "lucuma-odb-service",
    libraryDependencies ++= Seq(
      "edu.gemini"  %% "lucuma-graphql-routes-grackle"  % lucumaGraphQLRoutesVersion,
      "org.http4s"  %% "http4s-blaze-server"            % http4sBlazeVersion,
    ),
  )


lazy val DAO = project in file("dao")
lazy val WorkflowCore = (project in file("workflow-core"))
  .dependsOn(DAO)
  .configs(Test)
  .dependsOn(DAO % "test->test") // test scope dependency
lazy val WorkflowOperator = (project in file("workflow-operator")).dependsOn(WorkflowCore)
lazy val WorkflowCompilingService = (project in file("workflow-compiling-service"))
  .dependsOn(WorkflowOperator)
  .settings(
    dependencyOverrides ++= Seq(
      // override it as io.dropwizard 4 require 2.16.1 or higher
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.16.1"
    )
  )

lazy val WorkflowExecutionService = (project in file("amber"))
  .dependsOn(WorkflowOperator)
  .configs(Test)
  .dependsOn(DAO % "test->test") // test scope dependency

// root project definition
lazy val CoreProject = (project in file("."))
  .aggregate(DAO, WorkflowCore, WorkflowOperator, WorkflowCompilingService, WorkflowExecutionService)
  .settings(
    name := "core",
    version := "0.1.0",
    organization := "edu.uci.ics",
    scalaVersion := "2.13.12",
    publishMavenStyle := true
  )

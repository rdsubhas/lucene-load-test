package gatling

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object Engine extends App {

  val props = new GatlingPropertiesBuilder
  props.dataDirectory(PathHelper.dataDirectory.toString)
  props.resultsDirectory(PathHelper.resultsDirectory.toString)
  props.bodiesDirectory(PathHelper.bodiesDirectory.toString)
  props.binariesDirectory(PathHelper.mavenBinariesDirectory.toString)

  Gatling.fromMap(props.build)
}

package gatling

import io.gatling.recorder.GatlingRecorder
import io.gatling.recorder.config.RecorderPropertiesBuilder

object Recorder extends App {

  val props = new RecorderPropertiesBuilder
  props.simulationOutputFolder(PathHelper.recorderOutputDirectory.toString)
  props.simulationPackage("com.goeuro")
  props.bodiesFolder(PathHelper.bodiesDirectory.toString)

  GatlingRecorder.fromMap(props.build, Some(PathHelper.recorderConfigFile))
}

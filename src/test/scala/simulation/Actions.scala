package simulation

import grpc.LuceneReader
import io.gatling.commons.stats.OK
import io.gatling.core.Predef._
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.action.{Action, ChainableAction, ExitableAction}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

class LookupAction(val reader: LuceneReader, val statsEngine: StatsEngine, val next: Action)
  extends ChainableAction with NameGen with ExitableAction {
  override def name: String = "disk"

  val hit_name = "disk-hit"
  val hit_code = Some("404")
  val miss_name = "disk-miss"
  val miss_code = Some("200")
  val paths = List("part1", "part2", "part3", "part4")

  override def execute(session: Session): Unit = {
    val startTime = System.currentTimeMillis
    val docId = paths.map(p => session(p).as[String]).dropWhile(_.isEmpty).mkString("/")
    val doc = reader.lookup(docId)

    val endTime = System.currentTimeMillis
    statsEngine.logResponse(
      session,
      if (doc.nonEmpty) hit_name else miss_name,
      new ResponseTimings(startTime, endTime),
      OK,
      if (doc.nonEmpty) hit_code else miss_code,
      None,
      List(docId)
    )

    next ! session
  }
}

class LookupActionBuilder extends ActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action = {
    new LookupAction(new LuceneReader, ctx.coreComponents.statsEngine, next)
  }
}

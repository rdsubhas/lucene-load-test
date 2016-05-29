package simulation

import java.lang.Integer.parseInt

import grpc.LuceneReader
import io.gatling.commons.stats.OK
import io.gatling.core.Predef._
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.action.{Action, ChainableAction, ExitableAction}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

import scala.concurrent.duration._

class LookupLocal extends Simulation {
  val threads = parseInt(System.getProperty("gat.threads", "10"))
  val users = parseInt(System.getProperty("gat.users", "1000"))
  val duration = parseInt(System.getProperty("gat.duration", "1")).minutes

  val reader = new LuceneReader

  val scenarios = (1 to threads).toList.map(n => {
    scenario(s"lookup-${n}")
      .feed(csv("part1.csv").random.circular)
      .feed(csv("part2.csv").random.circular)
      .feed(csv("part3.csv").random.circular)
      .feed(csv("part4.csv").random.circular)
      .exec(new LookupLocalActionBuilder(reader))
      .inject(constantUsersPerSec(users) during(duration))
  })

  setUp(scenarios)

}

class LookupLocalAction(val reader: LuceneReader, val statsEngine: StatsEngine, val next: Action)
  extends ChainableAction with NameGen with ExitableAction {

  val name = "local"
  val hit_name = "hit"
  val hit_code = Some("200")
  val miss_name = "miss"
  val miss_code = Some("404")
  val paths = List("part1", "part2", "part3", "part4")

  override def execute(session: Session): Unit = {
    val docId = paths.map(p => session(p).as[String]).dropWhile(_.isEmpty).mkString("/")

    val startTime = System.currentTimeMillis
    val doc = reader.lookup(docId)
    val endTime = System.currentTimeMillis

    statsEngine.logResponse(
      session,
      if (doc >= 0) hit_name else miss_name,
      new ResponseTimings(startTime, endTime),
      OK,
      if (doc >= 0) hit_code else miss_code,
      None,
      List(docId)
    )

    next ! session
  }
}

class LookupLocalActionBuilder(val reader: LuceneReader) extends ActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action = {
    new LookupLocalAction(reader, ctx.coreComponents.statsEngine, next)
  }
}

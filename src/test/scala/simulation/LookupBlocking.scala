package simulation

import java.lang.Integer.parseInt

import grpc.{GrpcServer, LookupServiceGrpc, Lucene}
import io.gatling.commons.stats.OK
import io.gatling.core.Predef._
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.action.{Action, ChainableAction, ExitableAction}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.grpc.ManagedChannelBuilder

import scala.concurrent.duration._

class LookupBlocking extends Simulation {
  val threads = parseInt(Option(System.getenv("TEST_THREADS")).getOrElse("10"))
  val users = parseInt(Option(System.getenv("TEST_USERS")).getOrElse("1000"))
  val duration = parseInt(Option(System.getenv("TEST_MINUTES")).getOrElse("1")).minutes
  val host = Option(System.getenv("TEST_HOST")).getOrElse("localhost")

  val channel = ManagedChannelBuilder.forAddress(host, GrpcServer.Port)
    .usePlaintext(true).build()
  val client = LookupServiceGrpc.newBlockingStub(channel)

  val scenarios = (1 to threads).toList.map(n => {
    scenario(s"lookup-${n}")
      .feed(csv("part1.csv").random)
      .feed(csv("part2.csv").random)
      .feed(csv("part3.csv").random)
      .feed(csv("part4.csv").random)
      .exec(new LookupBlockingActionBuilder(client))
      .inject(constantUsersPerSec(users) during(duration))
  })

  setUp(scenarios)

}

class LookupBlockingAction(val client: LookupServiceGrpc.LookupServiceBlockingStub, val statsEngine: StatsEngine, val next: Action)
  extends ChainableAction with NameGen with ExitableAction {

  val name = "lookup-blocking"
  val hit_name = "hit"
  val hit_code = Some("200")
  val miss_name = "miss"
  val miss_code = Some("404")
  val paths = List("part1", "part2", "part3", "part4")

  override def execute(session: Session): Unit = {
    val docId = paths.map(p => session(p).as[String]).dropWhile(_.isEmpty).mkString("/")

    val startTime = System.currentTimeMillis
    val request = Lucene.LookupRequest.newBuilder.setDocId(docId).build
    val response = client.lookup(request)
    val doc = response.getDocId
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

class LookupBlockingActionBuilder(val client: LookupServiceGrpc.LookupServiceBlockingStub) extends ActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action = {
    new LookupBlockingAction(client, ctx.coreComponents.statsEngine, next)
  }
}

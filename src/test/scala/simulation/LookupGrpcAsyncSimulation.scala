package simulation

import java.lang.Integer.parseInt

import grpc.Lucene.LookupReply
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
import io.grpc.stub.StreamObserver

import scala.concurrent.duration._

class LookupGrpcAsyncSimulation extends Simulation {
  val threads = parseInt(System.getProperty("gat.threads", "10"))
  val users = parseInt(System.getProperty("gat.users", "1000"))
  val duration = parseInt(System.getProperty("gat.duration", "1")).minutes

  val host = System.getProperty("gat.host", "localhost")
  val channel = ManagedChannelBuilder.forAddress(host, GrpcServer.Port)
    .usePlaintext(true).build()
  val client = LookupServiceGrpc.newStub(channel)

  val scenarios = (1 to threads).toList.map(n => {
    scenario(s"lookup-${n}")
      .feed(csv("part1.csv").random.circular)
      .feed(csv("part2.csv").random.circular)
      .feed(csv("part3.csv").random.circular)
      .feed(csv("part4.csv").random.circular)
      .exec(new LookupGrpcAsyncActionBuilder(client))
      .inject(constantUsersPerSec(users) during(duration))
  })

  setUp(scenarios)

}

class LookupGrpcAsyncAction(val client: LookupServiceGrpc.LookupServiceStub, val statsEngine: StatsEngine, val next: Action)
  extends ChainableAction with NameGen with ExitableAction {

  val name = "lookup-grpc"
  val hit_name = s"${name}-hit"
  val hit_code = Some("200")
  val miss_name = s"${name}-miss"
  val miss_code = Some("404")
  val paths = List("part1", "part2", "part3", "part4")

  override def execute(session: Session): Unit = {
    val docId = paths.map(p => session(p).as[String]).dropWhile(_.isEmpty).mkString("/")

    val startTime = System.currentTimeMillis
    val request = Lucene.LookupRequest.newBuilder.setDocId(docId).build
    client.lookup(request, new StreamObserver[LookupReply] {
      override def onError(throwable: Throwable): Unit = {
        next ! session
      }
      override def onCompleted(): Unit = {
        next ! session
      }
      override def onNext(response: LookupReply): Unit = {
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
      }
    })
  }
}

class LookupGrpcAsyncActionBuilder(val client: LookupServiceGrpc.LookupServiceStub) extends ActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action = {
    new LookupGrpcAsyncAction(client, ctx.coreComponents.statsEngine, next)
  }
}

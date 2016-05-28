package simulation

import java.lang.Integer.parseInt

import gatling.PathHelper
import io.gatling.commons.stats.OK
import io.gatling.core.Predef._
import io.gatling.core.action.{Action, ChainableAction, ExitableAction}
import io.gatling.core.action.builder.{ActionBuilder, SessionHookBuilder}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.search.{IndexSearcher, TermQuery}
import org.apache.lucene.store.FSDirectory

import scala.concurrent.duration._

class DiskSimulation extends Simulation {
  val noop = new SessionHookBuilder(session => session)
  val threads = parseInt(System.getProperty("test.threads", "10"))
  val users = parseInt(System.getProperty("test.users", "1000"))
  val duration = parseInt(System.getProperty("test.duration", "1")).minutes

  val scenarios = (1 to threads).toList.map(n => {
    scenario(s"lucene-disk-${n}")
      .exec(noop)
      .feed(csv("part1.csv").random.circular)
      .feed(csv("part2.csv").random.circular)
      .feed(csv("part3.csv").random.circular)
      .feed(csv("part4.csv").random.circular)
      .exec(new LookupActionBuilder)
      .inject(constantUsersPerSec(users) during(duration))
  })

  setUp(scenarios)

}

class LookupAction(val reader: DirectoryReader, val searcher: IndexSearcher, val statsEngine: StatsEngine, val next: Action)
  extends ChainableAction with NameGen with ExitableAction {
  override def name: String = "disk"

  val hit_name = "disk-hit"
  val hit_code = Some("404")
  val miss_name = "disk-miss"
  val miss_code = Some("200")
  val paths = List("part1", "part2", "part3", "part4")

  override def execute(session: Session): Unit = {
    val startTime = System.currentTimeMillis
    val doc_id = paths.map(p => session(p).as[String]).dropWhile(_.isEmpty).mkString("/")
    val query = new TermQuery(new Term("id", doc_id))
    val hits = searcher.search(query, 1)
    val hasHit = hits.totalHits > 0

    if (hasHit) {
      val hit = hits.scoreDocs(0)
      val doc = reader.document(hit.doc)
    }

    val endTime = System.currentTimeMillis
    statsEngine.logResponse(
      session,
      if (hasHit) hit_name else miss_name,
      new ResponseTimings(startTime, endTime),
      OK,
      if (hasHit) hit_code else miss_code,
      None,
      List(doc_id)
    )

    next ! session
  }
}

class LookupActionBuilder extends ActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action = {
    val reader = DirectoryReader.open(FSDirectory.open(PathHelper.luceneRoot))
    val searcher = new IndexSearcher(reader)
    new LookupAction(reader, searcher, ctx.coreComponents.statsEngine, next)
  }
}

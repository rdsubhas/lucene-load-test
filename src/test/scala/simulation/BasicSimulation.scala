package simulation

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

class BasicSimulation extends Simulation {
  val noop = new SessionHookBuilder(session => session)

  val scenarios = (1 to 10).toList.map(n => {
    scenario("lookup" + n)
      .exec(noop)
      .feed(csv("ids.csv").circular)
      .exec(new LookupActionBuilder)
      .inject(constantUsersPerSec(100) during 1.minutes)
  })

  setUp(scenarios)

}

class LookupAction(val reader: DirectoryReader, val searcher: IndexSearcher, val statsEngine: StatsEngine, val next: Action)
  extends ChainableAction with NameGen with ExitableAction {
  override def name: String = "lucene-lookup"

  val found = Some("404")
  val not_found = Some("200")

  override def execute(session: Session): Unit = {
    val startTime = System.currentTimeMillis
    val doc_id = session("doc_id").as[String]
    val query = new TermQuery(new Term("id", doc_id))
    val hits = searcher.search(query, 1)
    if (hits.totalHits > 0) {
      val hit = hits.scoreDocs(0)
      val doc = reader.document(hit.doc)
      val endTime = System.currentTimeMillis
      statsEngine.logResponse(session, name, new ResponseTimings(startTime, endTime), OK, found, None)
    } else {
      val endTime = System.currentTimeMillis
      statsEngine.logResponse(session, name, new ResponseTimings(startTime, endTime), OK, not_found, None)
    }
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

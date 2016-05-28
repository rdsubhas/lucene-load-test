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

  val scn = scenario("lookup").
    exec(noop).
    feed(csv("ids.csv").circular).
    exec(new LookupActionBuilder)

  setUp(scn.inject(constantUsersPerSec(100) during(1 minutes)))

}

object LookupAction {
  val reader = DirectoryReader.open(FSDirectory.open(PathHelper.luceneRoot))
  val searcher = new IndexSearcher(reader)
}

class LookupAction(val statsEngine: StatsEngine, val next: Action)
  extends ChainableAction with NameGen with ExitableAction {

  override def name: String = "lucene-lookup"

  override def execute(session: Session): Unit = {
    val startTime = System.currentTimeMillis
    val doc_id = session("doc_id").as[String]
    val query = new TermQuery(new Term("id", doc_id))
    val hits = LookupAction.searcher.search(query, 1)
    val hit = hits.scoreDocs(0)
    val doc = LookupAction.reader.document(hit.doc)
    val endTime = System.currentTimeMillis
    statsEngine.logResponse(session, name, new ResponseTimings(startTime, endTime), OK, None, None)
    next ! session
  }
}

class LookupActionBuilder extends ActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action = {
    new LookupAction(ctx.coreComponents.statsEngine, next)
  }
}

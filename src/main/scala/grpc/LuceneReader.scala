package grpc

import java.nio.file.Paths

import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.search.{IndexSearcher, TermQuery}
import org.apache.lucene.store.FSDirectory

class LuceneReader {
  val projectRoot = Paths.get(getClass.getClassLoader.getResource("lucene.proto").getPath).resolve("../../..").toRealPath()
  val luceneRoot = projectRoot.resolve(System.getProperty("lucene.root", "data"))
  val reader = DirectoryReader.open(FSDirectory.open(luceneRoot))
  val searcher = new IndexSearcher(reader)

  def lookup(docId: String): Option[Int] = {
    val query = new TermQuery(new Term("id", docId))
    val hits = searcher.search(query, 1)
    if (hits.totalHits > 0) {
      val hit = hits.scoreDocs(0)
      val doc = reader.document(hit.doc)
      Some(hit.doc)
    } else {
      None
    }
  }
}

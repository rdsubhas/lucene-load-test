package grpc

import java.util.logging.Logger

import grpc.Lucene.{LookupReply, LookupRequest}
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver

object GrpcServer {
  val Port = 57001
  val Log = Logger.getAnonymousLogger

  def main(args: Array[String]): Unit = {
    val builder = ServerBuilder.forPort(Port)
    builder.addService(new LookupServiceImpl())
    val server = builder.build()
    server.start()

    Log.info(s"gRPC Server running on ${Port}")
    server.awaitTermination()
  }

  class LookupServiceImpl extends LookupServiceGrpc.AbstractLookupService {
    val reader = new LuceneReader

    override def lookup(request: LookupRequest, responseObserver: StreamObserver[LookupReply]): Unit = {
      val doc = reader.lookup(request.getDocId)
      LookupReply.newBuilder.setDocId(doc.getOrElse(0)).build
    }
  }
}

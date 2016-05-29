TEST_OPTS="-Xms512m -Dgat.threads=1 -Dgat.users=100 -Dgat.duration=1"
TEST_PROCS=1

for i in "$@"; do
  case $i in
  server)
    mvn compile exec:java
    exit 0
    ;;

  local)
    mvn compile
    export JAVA_TOOL_OPTIONS="$TEST_OPTS -Dgat.grpc=false"
    seq $TEST_PROCS | xargs -I {} -P $TEST_PROCS mvn gatling:test
    ;;

  remote)
    mvn compile
    export JAVA_TOOL_OPTIONS="$TEST_OPTS -Dgat.grpc=true"
    seq $TEST_PROCS | xargs -I {} -P $TEST_PROCS mvn gatling:test
    ;;

  *)
    ;;
  esac
done
export JAVA_TOOL_OPTIONS="-Xms512m -Dgat.threads=4 -Dgat.users=100 -Dgat.duration=4"
export TEST_PROCS=4

for i in "$@"; do
  case $i in
  server)
    mvn compile exec:java
    exit 0
    ;;

  disk)
    mvn compile
    seq $TEST_PROCS | xargs -I {} -P $TEST_PROCS mvn -Dsimulation=LookupLocal gatling:test
    ;;

  blocking)
    mvn compile
    seq $TEST_PROCS | xargs -I {} -P $TEST_PROCS mvn -Dsimulation=LookupBlocking gatling:test
    ;;

  async)
    mvn compile
    seq $TEST_PROCS | xargs -I {} -P $TEST_PROCS mvn -Dsimulation=LookupAsync gatling:test
    ;;

  *)
    ;;
  esac
done

export JAVA_OPTS="-Xms512m -Dtest.threads=1 -Dtest.users=100 -Dtest.duration=1"
export JAVA_TOOL_OPTIONS=$JAVA_OPTS
mvn clean
seq 2 | xargs -I {} -P 0 mvn gatling:test

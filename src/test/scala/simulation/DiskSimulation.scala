package simulation

import java.lang.Integer.parseInt

import io.gatling.core.Predef._

import scala.concurrent.duration._

class DiskSimulation extends Simulation {
  val threads = parseInt(System.getProperty("test.threads", "10"))
  val users = parseInt(System.getProperty("test.users", "1000"))
  val duration = parseInt(System.getProperty("test.duration", "1")).minutes

  val scenarios = (1 to threads).toList.map(n => {
    scenario(s"sim-${n}")
      .feed(csv("part1.csv").random.circular)
      .feed(csv("part2.csv").random.circular)
      .feed(csv("part3.csv").random.circular)
      .feed(csv("part4.csv").random.circular)
      .exec(new LookupActionBuilder)
      .inject(constantUsersPerSec(users) during(duration))
  })

  setUp(scenarios)

}

package com.weather.scalacass

import com.datastax.driver.core.Session
import util.EmbedCassandra
import org.scalatest.FlatSpec
import ScalaCass._

class PerfTest extends FlatSpec with EmbedCassandra {
  var session: Session = null
  val db = "perfdb"

  override def beforeAll() {
    super.beforeAll()
    session = client.session
  }

  val th = ichi.bench.Thyme.warmed(verbose = print)

  "string repeats" should "be decent" in {
    session.execute(s"CREATE KEYSPACE $db WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};")
    session.execute(s"CREATE TABLE $db.ccperf (str varchar, str2 varchar, str3 varchar, str4 varchar, PRIMARY KEY ((str)))")
    def n = java.util.UUID.randomUUID.toString
    session.execute(s"INSERT INTO $db.ccperf (str, str2, str3, str4) VALUES (?,?,?,?)", n, n, n, n)
    val row = session.execute(s"SELECT * FROM $db.strperf").one()

    th.pbenchOffWarm(title = "compare implicit and native get")(th.Warm(List.fill(100000)(row.as[String]("str"))), 2048, "withImplicit")(th.Warm(List.fill(100000)(if (row.isNull("str")) throw new IllegalArgumentException(s"""Cassandra: "str" was not defined in ${row.getColumnDefinitions.getTable("str")}""") else row.getString("str"))), 2048, "native")

    th.pbenchOffWarm(title = "compare implicit and native getAs")(th.Warm(List.fill(100000)(row.getAs[String]("str"))), 2048, "with implicit")(th.Warm(List.fill(100000)(if (row.getColumnDefinitions.contains("str") && !row.isNull("str")) Some(row.getString("str")) else None)), 2048, "native")

    case class Strings(str: String, str2: String, str3: String, str4: Option[String])
    def g(name: String) = if (row.isNull("str")) throw new IllegalArgumentException(s"""Cassandra: "str" was not defined in ${row.getColumnDefinitions.getTable("str")}""") else row.getString("str")
    th.pbenchOffWarm(title = "compare implicit and native case class get")(th.Warm(List.fill(100000)(row.as[Strings])), 2048, "with implicit")(th.Warm(List.fill(100000)(Strings(g("str"), g("str2"), g("str3"), if (row.getColumnDefinitions.contains("str") && !row.isNull("str")) Some(row.getString("str")) else None))), 2048, "native")


    def ga(name: String) = if (row.getColumnDefinitions.contains(name) && !row.isNull(name)) Some(row.getString(name)) else None
    def getAs = for {
      s1 <- ga("str")
      s2 <- ga("str2")
      s3 <- ga("str3")
      s4  = ga("str4")
    } yield Strings(s1, s2, s3, s4)
    th.pbenchOffWarm(title = "compare implicit and native case class getAs")(th.Warm(List.fill(100000)(row.getAs[Strings])), 2048, "with implicit")(th.Warm(List.fill(100000)(getAs)), 2048, "native")
  }
}
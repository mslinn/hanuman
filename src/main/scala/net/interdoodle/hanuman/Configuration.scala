package net.interdoodle.hanuman

import net.lag.configgy.Config


/** Using Configgy because BlueEyes does, even though it is deprecated
 * @author Mike Slinn */
class Configuration(config:Config) {
  val defaultDocument    = config.getString("defaultDocument", "Forty-two and change.")
  val maxTicks           = config.getInt("maxTicks", 100)
  val minimumMatchLength = config.getInt("minimumMatchLength", 3)
  val monkeysPerVisor    = config.getInt("monkeysPerVisor", 10)
}

object Configuration {
  private val configuration = Config.fromResource("hanuman.conf", getClass.getClassLoader)
  private val config = new Configuration(configuration)

  def apply() = config
}
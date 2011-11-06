package net.interdoodle.hanuman

import net.lag.configgy.{Config, Configgy}


/** Using Configgy because BlueEyes does, even though it is deprecated
 * @author Mike Slinn */
class Configuration(config:Config) {
  val maxTicks = config.getInt("maxTicks", 100)
  val monkeysPerVisor = config.getInt("monkeysPerVisor", 10)
}

object Configuration {
  var cl = getClass.getClassLoader
  private val configuration = Config.fromResource("hanuman.conf", getClass.getClassLoader)
  private var config = new Configuration(configuration)

  def apply() = config
}
package net.timerboard.routefinder

import com.monovore.decline._

import java.nio.file.Path

object CLI {
  val config = Opts.argument[Path]("config.json").withDefault(Path.of("./config.json"))
  val server =
    Command("server", "run the main web server", helpFlag = true)(config)
}

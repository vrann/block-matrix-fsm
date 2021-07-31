package com.vrann.cholesky

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.setup
import akka.actor.typed.scaladsl.StashBuffer
import com.vrann.{Initialized, L21Applied, Message, Position, State}

import java.io.File

trait InitializeOperation {
  def initialize(position: Position,
                 buffer: StashBuffer[Message],
                 filePath: File,
                 state: (File, State, List[Position], StashBuffer[Message]) => Behavior[Message]): Behavior[Message] =
    setup { context =>
      context.log.info(s"Initializing aMN at $position")
      val nextState: Behavior[Message] = if (position.x == 0) {
        context.log.info(s"State become L21Applied at $position")
        state(filePath, L21Applied, List.empty[Position], buffer)
      } else {
        context.log.info(s"State become Initialized at $position")
        state(filePath, Initialized, List.empty[Position], buffer)
      }

      if (!buffer.isEmpty) {
        context.log.debug("L21 unstashing")
        buffer.unstashAll(nextState)
      } else nextState
    }
}

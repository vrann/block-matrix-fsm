package com.vrann

import java.io.File

import akka.actor.typed.ActorRef

class BlockMessage(pos: Position) extends Message {
  val position = pos
}

object BlockMessage extends Message {

  def apply(pos: Position): Unit = {
    new BlockMessage(pos)
  }

  final case class MatrixDataAvailable(pos: Position,
                                       filePath: File,
                                       sectionId: Int)
      extends BlockMessage(pos = pos)
  final case class GetState(pos: Position, replyTo: ActorRef[BlockMessage])
      extends BlockMessage(pos = pos)
  final case class StateMessage(pos: Position, state: State)
      extends BlockMessage(pos = pos)
}

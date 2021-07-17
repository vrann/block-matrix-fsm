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
  final case class DataReady(pos: Position,
                             blockMatrixType: BlockMatrixType,
                             filePath: File,
                             sectionId: Int,
                             fileTransferActor: ActorRef[Message])
      extends BlockMessage(pos = pos)

  final case class AllL21Applied(pos: Position) extends BlockMessage(pos = pos)

  /*
   * Data should be available locally to the actor in order to process it. Semantic of the message should allow to verify
   * that the data is on the same node as an actor itself. Therefore we have Section ID as a part of the signature along wioth
   */
  final case class AijData(pos: Position, filePath: File, sectionId: Int) extends BlockMessage(pos = pos)
  final case class L11Ready(pos: Position, filePath: File, sectionId: Int) extends BlockMessage(pos = pos)
  final case class L21Data(pos: Position, filePath: File, sectionId: Int) extends BlockMessage(pos = pos)

  final case class MatrixDataAvailable(pos: Position, filePath: File, sectionId: Int) extends BlockMessage(pos = pos)
  final case class GetState(pos: Position, replyTo: ActorRef[BlockMessage]) extends BlockMessage(pos = pos)
  final case class StateMessage(pos: Position, state: State) extends BlockMessage(pos = pos)
}

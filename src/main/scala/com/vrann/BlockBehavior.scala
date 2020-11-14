package com.vrann

import akka.actor.typed.Behavior

abstract class BlockBehavior {
  def ::(that: BlockBehavior): BlockBehavior = {
    new ComposedBehavior(Vector(this, that))
  }

  def apply: Behavior[BlockMessage]
}

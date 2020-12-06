package com.vrann

import akka.actor.typed.Behavior

trait RoleBehavior {
  def apply: Behavior[BlockMessage]
}

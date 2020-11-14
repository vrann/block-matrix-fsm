package com.vrann

trait RoleBehavior {
  def apply(state: State, blockMessage: BlockMessage): BlockBehavior
}

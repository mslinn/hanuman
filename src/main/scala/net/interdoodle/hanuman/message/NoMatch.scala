package net.interdoodle.hanuman.message

import akka.actor.ActorRef


/**
 @author Mike Slinn */
case class NoMatch (val workCellRef:ActorRef)
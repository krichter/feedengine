package com.sap.openfeed

import scala.actors.Actor._
import collection.concurrent._
import com.typesafe.scalalogging.slf4j.Logging
import com.sap.openfeed._

object GroupActorManager extends Logging {
  var actorMap = new TrieMap[String,GroupActor]
  
  def getGroupActor(ref:GroupActorRef):GroupActor = {
	return actorMap(ref.groupid)
  }
  
  def getGroupActor(groupid:String):GroupActor = {
    return actorMap(groupid)
  }  
  
  def addGroupActorWithID(groupid:String,grouptype:String,metadata:collection.immutable.Map[String,String]) {
    var newactor = new GroupActor(groupid,grouptype)
    if (metadata != null) {
    	newactor.metadata ++= metadata
    }
    newactor.start
    actorMap += ((groupid,newactor))
  }
}
package com.sap.openfeed

import scala.actors.Actor._
import collection.concurrent._
import com.typesafe.scalalogging.slf4j.Logging
import com.sap.openfeed._

object FeedActorManager extends Logging {
  
  var actorMap = new TrieMap[String,FeedActor]
  
  def getFeedActor(ref:FeedActorRef):FeedActor = {
	return actorMap(ref.feedactorid)
  }
  
  def getFeedActor(feedactorid:String):FeedActor = {
    return actorMap(feedactorid)
  }  
  
  def addFeedActorWithID(feedactorid:String,actortype:String,metadata:collection.immutable.Map[String,String]) {
    var newactor = new FeedActor(feedactorid,actortype)
    if (metadata != null) {
    	newactor.metadata ++= metadata
    }
    newactor.start
    actorMap += ((feedactorid,newactor))
  }
}
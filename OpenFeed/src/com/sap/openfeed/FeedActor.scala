package com.sap.openfeed

import scala.actors.Actor
import scala.actors.Actor._
import collection.concurrent._
import collection.mutable._
import com.typesafe.scalalogging.slf4j.Logging
import com.sap.openfeed._

class FeedActor(id:String,actortype:String) extends Actor with Logging {
  
  val myref = FeedActorRef(id)
  var metadata = new TrieMap[String,String]
  var groups = new TrieMap[String,(String,GroupActorRef)]
  var groups_by_relationship = new TrieMap[String,ListBuffer[GroupActorRef]]  // TrieMap[Relationship,ListBuffer[GroupActorRef]
  
  var feed = new ListBuffer[FeedItem]
  
  def post(post:Post) {
    //println(post)
    
    // Add message to own feed
    addPostToFeed(post.payload,id)
    
    // Propagate message outward based on filter
    if (!post.groupfilter.isEmpty) {
      post.groupfilter.get.foreach(f => {
    	  GroupActorManager.getGroupActor(f.groupid) ! PropagateMessage(post.payload,myref,f.relationship)
      })
  	}
  }
  
  def addPostToFeed(payload:PostPayload,sourceid:String) {
    feed += FeedItem(payload.content,payload.stamp,sourceid)
  }
  
  def act() {
    loop {
      react {
        case m:AddedToGroup => {
          logger.debug("Feedactor "+id+" got message: "+m)
          if (!groups_by_relationship.contains(m.relationship)) {
            groups_by_relationship += ((m.relationship,new ListBuffer[GroupActorRef]))
          }
          groups_by_relationship(m.relationship) += m.ref
        }
        case m:RemovedFromGroup => {
          
        }
        case m:PropagatedMessage => {
          logger.debug("FeedActor "+id+" got message: "+m)
          addPostToFeed(m.post,m.source.feedactorid)
        }
        case m:Post => {
          logger.debug("FeedActor "+id+" got message: "+m)
          post(m)
        }
        case m:GetFeed => {
          logger.debug("FeedActor "+id+" got message: "+m)
          sender ! feed
        }
        case m => {
          logger.debug("FeedActor "+id+" got a message it did not recognize: "+m)
        }
      }
    }
  }
}
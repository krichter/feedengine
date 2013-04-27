package com.sap.openfeed

import scala.actors.Actor
import scala.actors.Actor._
import collection.concurrent._
import collection.mutable._
import com.typesafe.scalalogging.slf4j.Logging
import com.sap.openfeed._

class GroupActor(id:String,grouptype:String) extends Actor with Logging {

  
  var metadata = new TrieMap[String,String]
  var name = "";
  var grouptype = "";
  val myref = GroupActorRef(id)
  
  //var feedactormembers = new TrieMap[String,(String,FeedActorRef)]
  //var groupactormembers = new TrieMap[String,(String,GroupActorRef)]
  
  var feedactors_by_relationship = new TrieMap[String,TrieMap[String,FeedActorRef]] // TrieMap[relationship,TrieMap[feedactorid,ref]]
  
  def addFeedActor(aref:FeedActorRef,relationship:String) {
    //feedactormembers += ((aref.feedactorid,(relationship,aref)))
    if (!feedactors_by_relationship.contains(relationship)) {
      feedactors_by_relationship += ((relationship,new TrieMap[String,FeedActorRef]))
    }
    feedactors_by_relationship(relationship) += ((aref.feedactorid,aref))
    FeedActorManager.getFeedActor(aref) ! AddedToGroup(myref,relationship)
  }
  
  def removeFeedActor(aref:FeedActorRef,relationship:String) {
    feedactors_by_relationship(relationship) -= aref.feedactorid
    FeedActorManager.getFeedActor(aref) ! RemovedFromGroup(myref,relationship)
  }
  
  
  def act() {
    loop {
      react {
        case m:AddFeedActorToGroup => {
          logger.debug("GroupActor "+id+" got message: "+m)
          addFeedActor(m.aref,m.relationship)
        }
        case m:RemoveFeedActorFromGroup => {
          logger.debug("GroupActor "+id+" got message: "+m)
          removeFeedActor(m.aref,m.relationship)
        }
        case m:PropagateMessage => {
          //var sendlist:ListBuffer[String] = new ListBuffer[String]
          var sendlist:TrieMap[String,ListBuffer[String]] = new TrieMap;
          
          if (m.relationship.isEmpty) {
            // This means just send to everyone
            feedactors_by_relationship.foreach(r => {
              r._2.foreach(fuser => {
                if (!sendlist.contains(fuser._1)) {
                  sendlist += ((fuser._1,new ListBuffer[String]))
                }
                sendlist(fuser._1) += r._1
              })
            })
          } else {
            m.relationship.get.foreach(filteredrelationship => {
              if (feedactors_by_relationship.contains(filteredrelationship)) {
                feedactors_by_relationship(filteredrelationship).foreach(fuser => {
                  //sendlist += fuser._1
                  if (!sendlist.contains(fuser._1)) {
	                sendlist += ((fuser._1,new ListBuffer[String]))
	              }
	              sendlist(fuser._1) += filteredrelationship
                })
              }
            })
          }
          
          var count = 0
          sendlist.foreach(feedactorid => {
            FeedActorManager.getFeedActor(feedactorid._1) ! PropagatedMessage(m.post,m.source,myref,feedactorid._2.distinct.toList)
            count += 1
          })
          logger.debug("GroupActor "+id+" got a PropagateMessage "+m+" Sent to "+count+" actors")
        }
        case m => {
          logger.debug("GroupActor "+id+" got a message it did not recognize: "+m)
        }
      }
    }
  }
}
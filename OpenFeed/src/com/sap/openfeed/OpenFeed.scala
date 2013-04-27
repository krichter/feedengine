package com.sap.openfeed

import unfiltered.response._
import unfiltered.request._
import unfiltered.netty._
import org.json4s._
import org.json4s.native.JsonMethods._
import collection.concurrent._
import com.typesafe.scalalogging.slf4j.Logging
import com.sap.openfeed._

object OpenFeed extends Logging {
  
  implicit val formats = DefaultFormats
  
  var server = async.Planify {
    case req => req match {
      case PUT(Path(Seg("actor" :: Nil))) => {
        val jsonbody = new String(Body.bytes(req))
        val actorbody = parse(jsonbody)
        val id = (actorbody \\ "id").extract[String]
        val actortype = (actorbody \\ "actortype").extract[String]
        val metadata = (actorbody \ "metadata").extract[collection.immutable.Map[String,String]]
        FeedActorManager.addFeedActorWithID(id, actortype, metadata)
        
        req.respond(ResponseString("Actor Added"))
      }
      
      case PUT(Path(Seg("group" :: Nil))) => {
        val jsonbody = new String(Body.bytes(req))
        val parsedbody = parse(jsonbody)
        val id = (parsedbody \\ "id").extract[String]
        val grouptype = (parsedbody \\ "grouptype").extract[String]
        GroupActorManager.addGroupActorWithID(id, grouptype, null)
        logger.debug("Group added "+id)
        req.respond(ResponseString("Group Added"))
      }
      
      case PUT(Path(Seg("group" :: groupid :: "addactor" :: Nil))) => {
        val jsonbody = new String(Body.bytes(req))
        val parsedbody = parse(jsonbody)
        val userid = (parsedbody \\ "actorid").extract[String]
        val relationship = (parsedbody \\ "relationship").extract[String]
        //val groupactor = GroupActorManager.getGroupActor(groupid)
        //groupactor.addFeedActor(FeedActorRef(userid), relationship)
       
        GroupActorManager.getGroupActor(groupid) ! AddFeedActorToGroup(FeedActorRef(userid),relationship)
        
        req.respond(ResponseString("Group Added"))
        logger.debug("Added user "+userid+" to group "+groupid+" as "+relationship)
      }
      
      case DELETE(Path(Seg("group" :: groupid :: "actor" :: feedactorid :: relationship :: Nil))) => {
        //GroupActorManager.getGroupActor(groupid).removeFeedActor(FeedActorRef(feedactorid), relationship)
        GroupActorManager.getGroupActor(groupid) ! RemoveFeedActorFromGroup(FeedActorRef(feedactorid),relationship)
        req.respond(ResponseString("User removed from group"))
      }
      
      case POST(Path(Seg("actor" :: feedactorid :: "post" :: Nil))) => {
        val jsonbody = new String(Body.bytes(req))
        val parsedbody = parse(jsonbody)
        var rawpost = parsedbody.extract[RawPost]
        var stamp = org.joda.time.DateTime.now(org.joda.time.DateTimeZone.UTC)
        if (rawpost.stamp.isDefined) {
          stamp = new org.joda.time.DateTime(rawpost.stamp.get)
        }
        var payload = PostPayload(rawpost.content,stamp)
        //FeedActorManager.getFeedActor(feedactorid).post(Post(payload,rawpost.groupfilter))
        FeedActorManager.getFeedActor(feedactorid) ! Post(payload,rawpost.groupfilter)
        
        logger.debug("User "+feedactorid+" made a post")
        req.respond(ResponseString("Posted"))
      }
      
      case GET(Path(Seg("actor" :: feedactorid :: "feed" :: Nil))) => {
        var feed = FeedActorManager.getFeedActor(feedactorid) !? GetFeed(null) // Get public feed
        logger.debug("Returned feed: "+feed.toString)
        req.respond(ResponseString(feed.toString))
      }
      
      case GET(Path("/actor")) => {
        req.respond(ResponseString("Here is the actor"))
      }
      
      case GET(Path("/test1")) => {
    	req.respond(ResponseString("hello test"))
      }
      
      case _ => {
        req.respond(ResponseString("moo"))
      }
    }
  }

  var h = Http(8080).plan(server)
  
  def main(args: Array[String]): Unit = {
    //println("Server Started")
    logger.info("Server Started")
    //unfiltered.netty.Http(8080).plan(temp).run()
    h.run();
  }
}

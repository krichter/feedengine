package com.sap.openfeed

case class GroupActorRef(groupid:String)
case class FeedActorRef(feedactorid:String)

case class AddedToGroup(ref:GroupActorRef,relationship:String)
case class RemovedFromGroup(ref:GroupActorRef,relationship:String)

case class GroupFilter(groupid:String,relationship:Option[List[String]])
case class RawPost(content:String, stamp:Option[java.util.Date],groupfilter:Option[List[GroupFilter]])
case class Post(payload:PostPayload,groupfilter:Option[List[GroupFilter]])

case class PostPayload(content:String, stamp:org.joda.time.DateTime)

case class PropagateMessage(post:PostPayload,source:FeedActorRef,relationship:Option[List[String]])
case class PropagatedMessage(post:PostPayload,source:FeedActorRef,sourcegroup:GroupActorRef,via:List[String]) // List[relationship]. Source is aways a single group

case class AddFeedActorToGroup(aref:FeedActorRef,relationship:String)
case class RemoveFeedActorFromGroup(aref:FeedActorRef,relationship:String)

case class FeedItem(content:String,stamp:org.joda.time.DateTime,author:String)
case class GetFeed(requesteractorid:String)
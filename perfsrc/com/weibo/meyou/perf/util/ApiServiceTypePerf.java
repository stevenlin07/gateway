package com.weibo.meyou.perf.util;


public enum ApiServiceTypePerf {
	
    statusesUpdates(1),
    statusesUpload(2),
    statusesClose_friends_timeline(3),
    statusesShow(4), 
    statusesDestroy(5),
    statusesRepost(6),
    commentsCreate(7),
    commentsDestroy(8),  
    attitudesCreate(9),

    statusesShowbatch(101),
    statusesUsertimeline(102),
    statusesUser_timelineIds(103),
    statusesMentions(104),
    statusesMentionsIds(105),

    attitudesTo_me(201),
    attitudesShow(202),
    
    
    commentsCommon_to_me(301),
    commentsMentions(302),
    commentsTo_me(303),
    commentsShow(304),
    commentsDestroy_batch(305), 
    commentsReply(306),
    
    friendshipsClose_friendsCreate(401),
    friendshipsClose_friendsDestroy(402),
    friendshipsClose_friends(403),
    friendshipsClose_friendsIds(404),
    friendshipsClose_friendsCounts(405),
    
    inviteHandle_receive(501),
    inviteGet_receive_list(502),

    directPrivateMsg(601),

    unknow(99);

    private final int value;

    private ApiServiceTypePerf(int value) {
	this.value = value;
    }

    public int get() {
	return value;
    }
		
    public static ApiServiceTypePerf valueOf(int value){
	for( ApiServiceTypePerf type: ApiServiceTypePerf.values() ){
	    if ( value == type.value ) return type;
	}
	return unknow;
    }
}

namespace java com.weibo.wesync.notify.service.rpc

service Notify {
   void send(1:string touid, 2:binary notice),
   void sendSimple(1:string text)
}
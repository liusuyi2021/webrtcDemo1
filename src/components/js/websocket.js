import { userStore } from "@/store/store.js";
let ws;

export const initWebsocket = () => {
  let uStore = userStore();
  let roomId = uStore.roomId;
  let userId = uStore.userId;
  let nickName = uStore.nickName;
  // 获取websocket地址
  const apiUrl = import.meta.env.VITE_VUE_APP_WSS_API_URL;

  //拼接websocket地址
  let webSocketURL =
    apiUrl +
    "?roomId=" +
    roomId +
    "&userId=" +
    userId +
    "&nickName=" +
    nickName;

  //websocket实例
  ws = new WebSocket(webSocketURL);
  ws.onopen = function () {
    console.log("websocket连接成功");
    heartBeat();
  };
  //接收消息
  ws.onmessage = function (event) {
    let message = JSON.parse(event.data);
    window.dispatchEvent(new CustomEvent("message", { detail: message }));
    //console.log("收到消息：" + event.data);
  };

  ws.onclose = function () {
    console.log("websocket连接关闭");
    clearInterval(heartBeat);
  };

  ws.onerror = function (error) {
    console.log("websocket连接出错：");
    console.log(error);
  };
};
// 发送消息
export const sendMessage = (type, targetUserId, message) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    let map = {
      type: type,
      targetUserId: targetUserId,
      content: message,
    };
    ws.send(JSON.stringify(map));
  }
};
// WebSocket心跳
let heartBeat = () => {
  setInterval(() => {
    sendMessage("heart", "", "心跳");
  }, 10000);
};

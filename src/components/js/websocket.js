import { userStore } from "@/store/store.js";
let ws;

export const openWebSocket = () => {
  let uStore = userStore();
  let userId = uStore.userId;
  let nickName = uStore.nickName;
  // 获取websocket地址
  const apiUrl = import.meta.env.VITE_VUE_APP_WSS_API_URL;

  //拼接websocket地址
  let webSocketURL =
    apiUrl +
    "?userId=" +
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
export const sendMessage = (message) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(message));
  }
};


// WebSocket心跳
let heartBeatInterval;
let heartBeat = () => {
   heartBeatInterval= setInterval(() => {
    let message = {
      type: "heart",
      data:{
        userId: userStore().userId,
      }
  }
    sendMessage(message);
  }, 10000);
};
// 断开连接
export const closeWebSocket = () => {
  if (ws) {
    // 关闭 WebSocket 连接
    ws.close();
    console.log("websocket连接已断开");
  }
  // 清除心跳定时器
  clearInterval(heartBeatInterval);
  console.log("心跳定时器已清除");
};

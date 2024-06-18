import { userStore } from "@/store/store.js";
let ws;
export const initWebsocket = () => {
  let roomId = userStore().roomId;
  let userId = userStore().userId;
  // 获取websocket地址
  const apiUrl = import.meta.env.VITE_VUE_APP_WSS_API_URL;

  //拼接websocket地址
  let webSocketURL = apiUrl + "?roomId=" + roomId + "&userId=" + userId;

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
export const sendMessage = (type, message) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    //console.log("发送消息：" + message);
    let map = {
      "type":type,
      "content": message
    };
    ws.send(JSON.stringify(map));
  }
};
// WebSocket心跳
let heartBeat = () => {
  setInterval(() => {
    sendMessage("heart","心跳");
  }, 10000);
};

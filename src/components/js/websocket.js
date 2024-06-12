let ws;
export const initWebsocket = (roomId, userId) => {
  //websocket
  ws = new WebSocket("ws://localhost:8080/ws/" + roomId + "/" + userId);
  ws.onopen = function () {
    console.log("websocket连接成功");
  };

  ws.onmessage = function (event) {
    let message = JSON.parse(event.data);
    window.dispatchEvent(new CustomEvent("message", { detail: message }));
    console.log("收到消息："+message);
  };

  ws.onclose = function () {
    console.log("websocket连接关闭");
  };

  ws.onerror = function (error) {
    console.log("websocket连接出错：" + error);
  };
};

export const sendMessage = (message) => {
  console.log("发送消息：" + message);
  ws.send(message);
};

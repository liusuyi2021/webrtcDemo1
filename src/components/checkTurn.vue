<template>
    <div class="turn-tester-container">
      <h2>TURN 服务器检测</h2>
      
      <!-- 用户输入 TURN 信息 -->
      <div class="turn-config">
        <label for="turn-url">TURN 服务器地址:</label>
        <input type="text" id="turn-url" v-model="turnConfig.urls" placeholder="例如 turn:192.168.2.169" />
        
        <label for="turn-username">用户名:</label>
        <input type="text" id="turn-username" v-model="turnConfig.username" placeholder="请输入用户名" />
        
        <label for="turn-credential">凭证:</label>
        <input type="password" id="turn-credential" v-model="turnConfig.credential" placeholder="请输入凭证" />
      </div>
  
      <button @click="checkServer" :disabled="loading" class="check-btn">开始检测</button>
  
      <div v-if="loading" class="loading">检测中，请稍候...</div>
      
      <div v-if="candidates.length" class="candidates-container">
        <h3>ICE 候选信息</h3>
        <table class="candidates-table">
          <thead>
            <tr>
              <th>Time (ms)</th>
              <th>Type</th>
              <th>Foundation</th>
              <th>Protocol</th>
              <th>Address</th>
              <th>Port</th>
              <th>Priority</th>
              <th>URL (if present)</th>
              <th>relayProtocol (if present)</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(candidate, index) in candidates" :key="index">
              <td>{{ candidate.time }}</td>
              <td>{{ candidate.type }}</td>
              <td>{{ candidate.foundation }}</td>
              <td>{{ candidate.protocol }}</td>
              <td>{{ candidate.address }}</td>
              <td>{{ candidate.port }}</td>
              <td>{{ candidate.priority }}</td>
              <td>{{ candidate.url || '-' }}</td>
              <td>{{ candidate.relayProtocol || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <div v-if="status !== null" class="status">
        <p>检测结果: <strong>{{ status ? "TURN 服务器正常" : "TURN 服务器不可用" }}</strong></p>
      </div>
    </div>
  </template>
  
  <script>
  import { ref } from 'vue';
  
  export default {
    name: 'TurnTester',
    setup() {
      // 配置数据，支持用户输入
      const turnConfig = ref({
        urls: 'turn:111.40.46.199:3478', // 默认 TURN 服务器地址
        username: 'admin', // 默认用户名
        credential: '123456', // 默认凭证
      });
  
      const candidates = ref([]); // 存储 ICE 候选
      const status = ref(null); // 检测状态 (null 表示尚未检测, true 表示正常, false 表示不可用)
      const loading = ref(false); // 检测是否正在进行
      let startTime = 0; // 用于记录开始时间
  
      // 检测 TURN 服务器状态
      const checkServer = async () => {
        loading.value = true;
        status.value = null;
        candidates.value = [];
  
        startTime = Date.now(); // 记录开始时间
  
        try {
          const result = await checkTURNServer(turnConfig.value);
          status.value = result;
        } catch (error) {
          console.error("检测失败:", error);
          status.value = false;
        } finally {
          loading.value = false;
        }
      };
  
      // 检查 TURN 服务器是否可用，并收集 ICE 候选
      const checkTURNServer = (turnConfig, timeout = 5000) => {
        return new Promise((resolve, reject) => {
          let promiseResolved = false;
          const iceCandidates = [];
  
          const timer = setTimeout(() => {
            if (!promiseResolved) {
              resolve(false); // 超时返回不可用
              promiseResolved = true;
            }
          }, timeout);
  
          try {
            const pc = new RTCPeerConnection({ iceServers: [turnConfig] });
            pc.createDataChannel(""); // 创建一个数据通道
            pc.createOffer()
              .then((offer) => pc.setLocalDescription(offer))
              .catch(() => {
                clearTimeout(timer);
                resolve(false);
                promiseResolved = true;
              });
  
            pc.onicecandidate = (event) => {
              if (
                !promiseResolved &&
                event.candidate &&
                event.candidate.candidate &&
                event.candidate.candidate.indexOf("typ relay") !== -1
              ) {
                iceCandidates.push(event.candidate.candidate);
              }
  
              if (event.candidate && event.candidate.candidate) {
                // 计算每个候选的时间差，并将其添加到 candidates 数组
                const timeElapsed = (Date.now() - startTime) / 1000; // 计算耗时（秒）
                candidates.value.push({
                  time: (timeElapsed).toFixed(3), // 耗时（毫秒）
                  type: event.candidate.type,
                  foundation: event.candidate.foundation,
                  protocol: event.candidate.protocol,
                  address: event.candidate.address,
                  port: event.candidate.port,
                  priority: event.candidate.priority,
                  url: event.candidate.url || '',
                  relayProtocol: event.candidate.relayProtocol || ''
                });
              }
              if (
                !promiseResolved &&
                event.candidate &&
                event.candidate.candidate &&
                event.candidate.candidate.indexOf("typ relay") !== -1
              ) {
                clearTimeout(timer);
                resolve(true); // 找到 relay 类型的候选，证明 TURN 可用
                promiseResolved = true;
              }
            };
          } catch (e) {
            clearTimeout(timer);
            reject(e);
          }
        });
      };
  
      return {
        turnConfig,
        candidates,
        status,
        loading,
        checkServer,
      };
    },
  };
  </script>
  
  <style scoped>
  .turn-tester-container {
    font-family: Arial, sans-serif;
    padding: 20px;
    max-width: 800px;
    margin: 0 auto;
  }
  
  h2 {
    font-size: 1.8em;
    color: #333;
    margin-bottom: 15px;
  }
  
  .turn-config {
    margin-bottom: 20px;
  }
  
  label {
    display: block;
    margin-bottom: 8px;
    color: #555;
  }
  
  input {
    width: 100%;
    padding: 8px;
    margin-bottom: 15px;
    border: 1px solid #ccc;
    border-radius: 4px;
  }
  
  .check-btn {
    padding: 10px 20px;
    background-color: #007bff;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    margin-bottom: 20px;
  }
  
  .check-btn:disabled {
    background-color: #ccc;
    cursor: not-allowed;
  }
  
  .loading {
    font-size: 1.2em;
    color: #666;
  }
  
  .candidates-container {
    margin-top: 20px;
  }
  
  .candidates-table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 10px;
    border: 1px solid #ddd;
  }
  
  .candidates-table th,
  .candidates-table td {
    padding: 12px 15px;
    text-align: left;
    border: 1px solid #ddd;
  }
  
  .candidates-table th {
    background-color: #f4f4f4;
    color: #333;
  }
  
  .candidates-table tr:nth-child(even) {
    background-color: #f9f9f9;
  }
  
  .status {
    margin-top: 20px;
    font-size: 1.2em;
  }
  
  .status strong {
    font-weight: bold;
    color: #333;
  }
  </style>
  
<template>
    <div>
      <h1>Hello Webrtc Demo</h1>
      <div class="video">
        <video ref="localVideo" class="video-js" controls preload="auto" width=50% height="480"></video>
        <br />
        <video ref="remoteVideo" class="video-js" controls preload="auto" width=50% height="480"></video>
      </div>
      <div>
        <button id="create-offer" @click="createOffer">create-offer</button>
        <div>
          <textarea id="offer-sdp"></textarea>
        </div>
      </div>
      <div>
        <button id="create-answer" @click="createAnswer">create-answer</button>
        <div>
          <textarea id="answer-sdp"></textarea>
        </div>
        <button id="add-answer" @click="addAnswer">add-answer</button>
      </div>
    </div>
  </template>
  
  <script setup>
  import { onMounted, ref } from 'vue';
  
  let localStream;
  let remoteStream;
  
  let peerConnection = new RTCPeerConnection();
  
  let localVideo = ref()
  let remoteVideo = ref()
  
  onMounted(() => {
    init();
  })
  
  let init = () => {
  
    openLocalCamera();
  
    //远端视频流轨道加入
    peerConnection.ontrack = (event) => {
      event.streams[0].getTracks().forEach((track) => {
        remoteStream.addTrack(track);
      });
    };
  
    remoteStream = new MediaStream();
    remoteVideo.value.srcObject = remoteStream;   
  }
  
  const openLocalCamera = () => {
    // 检测浏览器是否支持mediaDevices
    if (navigator.mediaDevices) {
      navigator.mediaDevices
        // 开启视频，关闭音频
        .getUserMedia({ audio: false, video: true })
        .then((stream) => {
          // 将视频流传入viedo控件           
          localVideo.value.srcObject = stream;
          localStream = stream;
          //本地视频流轨道加入
          localStream.getTracks().forEach(track => {
            peerConnection.addTrack(track, localStream);
          });
  
          localVideo.value.play();
        })
        .catch((err) => {
          console.log(err);
        });
    } else {
      window.alert("该浏览器不支持开启摄像头，请更换最新版浏览器");
    }
  };
  //A端
  let createOffer = async () => {
  
    //ice
    peerConnection.onicecandidate= async (event)=>{
      if(event.candidate){
        console.log(event.candidate);
        document.getElementById("offer-sdp").value = JSON.stringify(peerConnection.localDescription);
      }
    }
  
    const offer = await peerConnection.createOffer();
    await peerConnection.setLocalDescription(offer);
    console.log(offer);
    document.getElementById("offer-sdp").value = JSON.stringify(offer);
  }
  //B端
  let createAnswer = async () => {
  
    const offer = JSON.parse(document.getElementById("offer-sdp").value);
    await peerConnection.setRemoteDescription(offer);
  
    //ice
    peerConnection.onicecandidate= async (event)=>{
      if(event.candidate){
        console.log(event.candidate);
        document.getElementById("answer-sdp").value = JSON.stringify(peerConnection.localDescription);
      }
    }
  
    //A端发送offer，B端收到后，生成answer
    const answer = await peerConnection.createAnswer();
    await peerConnection.setLocalDescription(answer);
    console.log(answer);
    document.getElementById("answer-sdp").value = JSON.stringify(answer);;
  }
  //A
  function addAnswer() {
    const answer = JSON.parse(document.getElementById("answer-sdp").value);
    if (!peerConnection.currentRemoteDescription) {
      peerConnection.setRemoteDescription(answer);
    }
    remoteVideo.value.play();
  }
  
  
  </script>
  
  <style scoped>
  .video-js {
    flex: 5;
    margin: 0 10px
  }
  
  .video {
    border: 1px solid #1ca8e9;
    padding: 20px;
    display: flex;
  }
  
  textarea {
    width: 100%;
    height: 100px;
  }
  </style>
  
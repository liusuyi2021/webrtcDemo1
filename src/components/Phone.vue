<template>
    <div
        style="width: 100%; height: 100%; display: flex; flex-direction: column; justify-content: center; align-items: center">
        <h1>Hello Webrtc Demo</h1>
        <div class="video-container">
            <div id="local-video-container">
                <video ref="localVideo" id="local-video"></video>
            </div>

            <div id="remote-video-container">
                <video ref="remoteVideo" id="remote-video"></video>
            </div>

            <el-button id="create-offer" type="primary" icon="phone" v-if="callButton"
                @click="createOffer">拨打</el-button>
            <el-button id="create-answer" type="danger" icon="phone" v-if="answerButton"
                @click="createAnswer">接听</el-button>
            <el-button id="disconnect" type="danger" icon="phone" v-if="disconnectButton"
                @click="disconnect">挂断</el-button>
        </div>
        <div>
            <el-input v-model="roomId" vlue="123" placeholder="请输入房间号"></el-input>
            <el-input v-model="userId" vlue="123" placeholder="请输入用户ID"></el-input>
            <el-button id="enter-room" type="primary" round @click="enterRoom">加入房间</el-button>
            <el-button id="share-screen" type="primary" round @click="shareScreen">共享屏幕</el-button>
            <el-button id="video-call" type="primary" round @click="videoCall">视频通话</el-button>
            <div>
                <textarea id="offer-sdp" ref="offerSdp"></textarea>
            </div>
            <div>
                <textarea id="answer-sdp" ref="answerSdp"></textarea>
            </div>
        </div>
    </div>
</template>
<script setup lang="ts">

import { ref } from 'vue';
import { initWebsocket, sendMessage } from './js/websocket.js';

const roomId = ref('')
const userId = ref('')
let localStream: MediaStream;
let remoteStream: MediaStream;

let peerConnection;

let localVideo = ref()
let remoteVideo = ref()
let offerSdp = ref()
let answerSdp = ref()

const callButton = ref(false);
const answerButton = ref(false);
const disconnectButton = ref(false);

//进入房间
let enterRoom = () => {
    peerConnection = new RTCPeerConnection();
    callButton.value = true;
    setLocalCamera()
    setRemoteCamera()
    initWebsocket(roomId.value, userId.value)
}

const setLocalCamera = () => {
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


let setRemoteCamera = () => {
    remoteStream = new MediaStream();
    //远端视频流轨道加入
    peerConnection.ontrack = (event) => {
        event.streams[0].getTracks().forEach((track) => {
            remoteStream.addTrack(track);
        });
    };
}

//A端
let createOffer = async () => {

    callButton.value = false;
    //ice
    peerConnection.onicecandidate = async (event) => {
        if (event.candidate) {
            offerSdp.value.value = JSON.stringify(peerConnection.localDescription);
        }
    }
    //创建offer
    const offer = await peerConnection.createOffer();
    await peerConnection.setLocalDescription(offer);

    //显示offer
    offerSdp.value.value = JSON.stringify(offer);

    //发送websocket
    sendMessage(offerSdp.value.value);
}

//B端
let createAnswer = async () => {
    answerButton.value = false;
    //B端解析offer 设置远端描述
    const offer = JSON.parse(offerSdp.value.value);
    await peerConnection.setRemoteDescription(offer);

    //ice
    peerConnection.onicecandidate = async (event) => {
        if (event.candidate) {
            answerSdp.value.value = JSON.stringify(peerConnection.localDescription);
            sendMessage(answerSdp.value.value);
        }
    }

    //A端发送offer，B端收到后，生成answer
    const answer = await peerConnection.createAnswer();
    await peerConnection.setLocalDescription(answer);
    answerSdp.value.value = JSON.stringify(answer);
    addAnswer()
}
//A
function addAnswer() {
    const answer = JSON.parse(answerSdp.value.value);
    if (!peerConnection.currentRemoteDescription) {
        peerConnection.setRemoteDescription(answer);
    }
    console.log("1233333333333333333333333333333333333")
    remoteVideo.value.srcObject = remoteStream;
    remoteVideo.value.play();
}
//挂断
let disconnect = async () => {
    // 关闭流

    remoteStream.getTracks().forEach(track => {
        track.stop();
    });
    // 关闭PeerConnection
    peerConnection.close();
    peerConnection = null;

    // 重置按钮状态
    callButton.value = true;
    answerButton.value = false;
    disconnectButton.value = false;
}
//分享本地屏幕
let shareScreen = async () => {
    let newStream = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false });

    localStream = newStream;
    // 将视频流传入viedo控件           
    localVideo.value.srcObject = localStream;

    //本地视频流轨道加入
    localStream.getTracks().forEach(track => {
        peerConnection.addTrack(track, localStream);
    });

    localVideo.value.play();

    // 替换本地视频流轨道
    localStream.getVideoTracks().forEach(track => {
        peerConnection.getSenders().find(sender => sender.track.kind === 'video')
            .replaceTrack(track)
    })

};

//切换视频通话
let videoCall = async () => {
    let newStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
    localStream = newStream;
    // 将视频流传入viedo控件           
    localVideo.value.srcObject = localStream;

    //本地视频流轨道加入
    localStream.getTracks().forEach(track => {
        peerConnection.addTrack(track, localStream);
    });

    localVideo.value.play();

    // 替换本地视频流轨道
    localStream.getVideoTracks().forEach(track => {
        peerConnection.getSenders().find(sender => sender.track.kind === 'video')
            .replaceTrack(track)
    })

};
//接收websocket数据
let getWebsocketData = (e) => {
    let message = e.detail
    console.log(message)
    let room = message.room;
    let user = message.user;
    let type = message.type;
    console.log("room--->" + room)
    console.log("user--->" + user)
    console.log("------>" + roomId.value + "-------->" + userId.value)
    if (room === roomId.value && user !== userId.value) {
        if (type==='message') {
            console.log(type)
            let sdp = message.message;
            let sdpType = JSON.parse(sdp).type;
            switch (sdpType) {
                case "offer": offerSdp.value.value = sdp;
                    answerButton.value = true;
                    disconnectButton.value = true;
                    callButton.value = false;
                    break;
                case "answer": answerSdp.value.value = sdp; addAnswer(); answerButton.value = false; break;
            }
        }
    }
}
window.addEventListener("message", getWebsocketData);

</script>

<style scoped>
.video-container {
    border: 1px solid #1ca8e9;
    padding: 2px;
    position: sticky;
    object-fit: cover;
}

#local-video {
    object-fit: fill;
    width: 100%;
    height: 100%;
}

#remote-video {
    object-fit: fill;
    width: 100%;
    height: 100%;
}

#remote-video-container {
    width: 250px;
    height: 250px;
    position: absolute;
    border: 5px solid #920697;
    top: 30px;
    /* 根据需要调整位置 */
    right: 30px;
    /* 根据需要调整位置 */
    z-index: 1;
    /* 确保按钮在video之上 */
}

#local-video-container {
    width: 600px;
    height: 400px;
}

#create-offer {
    position: absolute;
    bottom: 80px;
    /* 根据需要调整位置 */
    left: 200px;
    /* 根据需要调整位置 */
    z-index: 1;
    /* 确保按钮在video之上 */
}

#create-answer {
    position: absolute;
    bottom: 80px;
    /* 根据需要调整位置 */
    right: 200px;
    /* 根据需要调整位置 */
    z-index: 1;
    /* 确保按钮在video之上 */
}

#disconnect {
    position: absolute;
    bottom: 80px;
    /* 根据需要调整位置 */
    right: 120px;
    /* 根据需要调整位置 */
    z-index: 1;
    /* 确保按钮在video之上 */
}

textarea {
    width: 100%;
    height: 100px;
}
</style>
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
                @click="callRemote">拨打</el-button>
            <el-button id="create-answer" type="danger" icon="phone" v-if="answerButton"
                @click="acceptCall">接听</el-button>
            <el-button id="disconnect" type="danger" icon="phone" v-if="disconnectButton"
                @click="disconnect">挂断</el-button>
        </div>
        <div>
            <el-input v-model="room" vlue="123" placeholder="请输入房间号"></el-input>
            <el-input v-model="user" vlue="123" placeholder="请输入用户ID"></el-input>
            <el-button id="enter-room" type="primary" round @click="enterRoom">加入房间</el-button>

            <el-button id="change" type="primary" round @click="change">{{ changeValue }}</el-button>
        </div>
    </div>
</template>
<script setup lang="ts">
import { ref } from 'vue';
import { initWebsocket, sendMessage } from './js/websocket.js';
import { userStore } from '@/store/store.js';
import { sdpStore, iceStore } from '@/store/store.js';

const room = ref('')
const user = ref('')
let localStream;
let remoteStream;

let peerConnection;
let localVideo = ref()
let remoteVideo = ref()

let changeValue = ref("共享屏幕")


let offerSdp = sdpStore().offerSdp;
let answerSdp = sdpStore().answerSdp;
let offerCandidate = iceStore().offerCandidate;
let answerCandidate = iceStore().answerCandidate;
let caller = ref(false);//发起方
let called = ref(false);//被叫方
let calling = ref(false);//呼叫中
let communicating = ref(false);//正在通话

//拨号按钮
const callButton = ref(false);
//接听按钮
const answerButton = ref(false);
//挂断按钮
const disconnectButton = ref(false);

//进入房间
let enterRoom = () => {
    callButton.value = true;
    userStore().setWSConfig(room.value, user.value)
    initWebsocket();
}
//获取本地摄像头流
let getLocalStream = async () => {
    // const stream = await navigator.mediaDevices.getUserMedia({ audio: false, video: true });
    // localVideo.value.srcObject = stream;
    // localVideo.value.play();
    // localStream = stream;
    // return stream;
    await navigator.mediaDevices.getUserMedia({ audio: true, video: true })
        .then(stream => {
            localVideo.value.srcObject = stream;
            localVideo.value.play();
            localStream = stream;
            return stream;
        })
        .catch(error => {
            console.error("获取本地摄像头流失败", error);
        });
}

let createPeer = async () => {
    peerConnection = new RTCPeerConnection({
        iceServers: [
            {
                urls: "turn:111.40.46.199:3478",
                username: "myuser",
                credential: "mypass"
            }
        ]
    });
}

//请求视频通话
let callRemote = async () => {
    caller.value = true;//标识当前用户为发起方
    calling.value = true;//表示正在呼叫
    await getLocalStream();
    sendMessage("call", "请求视频通话");
}

//同意视频通话
let acceptCall = async () => {
    sendMessage("accept", "同意视频通话");
}

//创建发送offer
let createOffer = async () => {

    //创建自己的RTCPeerConnection
    createPeer();

    //添加本地音视频流
    if (localStream) {
        //peerConnection.addStream(localStream)
        localStream.getTracks().forEach(track => {
            peerConnection.addTrack(track, localStream);
        });
    }
    //通过监听onicecandidates事件来获取ICE候选信息
    peerConnection.onicecandidate = async (event) => {
        if (event.candidate) {
            if (event.candidate.candidate.includes("relay")) {
                console.log("Using TURN server for media relay.");
            }
            console.log("用户" + user.value + "生成并发送offerCandidate", event.candidate);
            sendMessage("offerCandidate", JSON.stringify(event.candidate));
        }
    }
    //监听onaddstream来获取对方的音视频流
    peerConnection.onaddstream = (event) => {
        console.log("用户" + user.value + "收到对方音视频流", event.stream);
        communicating.value = true;
        calling.value = false;
        remoteVideo.value.srcObject = event.stream;
        remoteVideo.value.play();
    }

    //创建offer
    const offer = await peerConnection.createOffer();
    await peerConnection.setLocalDescription(offer);

    //显示offer
    offerSdp = JSON.stringify(offer);

    //发送websocket
    sendMessage("offer", offerSdp);
    console.log("用户" + user.value + "生成并发送offer", offer);
}

//创建发送answer
let createAnswer = async () => {
    //创建自己的RTCPeerConnection
    createPeer();
    //获取本地音视频流
    await getLocalStream();
    //添加本地音视频流
    if (localStream) {
        //peerConnection.addStream(localStream);//已过时
        localStream.getTracks().forEach(track => {
            peerConnection.addTrack(track, localStream);
        });
    }
    //通过监听onicecandidates事件来获取ICE候选信息
    peerConnection.onicecandidate = async (event) => {
        if (event.candidate) {
            if (event.candidate.candidate.includes("relay")) {
                console.log("Using TURN server for media relay.");
            }
            console.log("用户" + user.value + "生成并发送answerCandidate", event.candidate);
            sendMessage("answerCandidate", JSON.stringify(event.candidate));
        }
    }
    //监听onaddstream来获取对方的音视频流
    peerConnection.onaddstream = (event) => {
        console.log("用户" + user.value + "收到对方音视频流", event.stream);
        communicating.value = true;
        calling.value = false;
        remoteVideo.value.srcObject = event.stream;
        remoteVideo.value.play();
    }

    //监听addEventListener来获取对方的音视频流
    // peerConnection.addEventListener('track', (event) => {
    //     console.log("用户" + user.value + "收到对方音视频流");
    //     communicating.value = true;
    //     calling.value = false;
    //     if (remoteVideo.value.srcObject !== event.streams[0]) {
    //         remoteVideo.value.srcObject = event.streams[0];
    //         remoteVideo.value.play();
    //     }
    // });

    //设置远端描述信息
    await peerConnection.setRemoteDescription(JSON.parse(offerSdp))

    //生成answer
    const answer = await peerConnection.createAnswer();
    //设置本地answer信息
    await peerConnection.setLocalDescription(answer);


    //显示answer
    answerSdp = JSON.stringify(answer);

    //发送answer
    sendMessage("answer", JSON.stringify(answer));
    console.log("用户" + user.value + "生成并发送answer", answer)
}
//设置远端answer信息
let setAnswer = async () => {
    await peerConnection.setRemoteDescription(JSON.parse(answerSdp));
}
//设置远端candidate信息
let setCandidate = async (event) => {
    if (peerConnection.remoteDescription == null) {
        return;
    }
    await peerConnection.addIceCandidate(JSON.parse(event));
}
//挂断
let disconnect = async () => {
    // 关闭RTC连接
    if (peerConnection) {
        peerConnection.close();
    }

    // 清理DOM元素和相关变量
    remoteVideo.value.srcObject = null;
    remoteVideo.value.src = '';
    remoteStream = null;
    peerConnection = null;

    // 重置按钮状态
    callButton.value = true;
    answerButton.value = false;
    disconnectButton.value = false;
    sendMessage("disconnect", "disconnect");
}

//分享本地屏幕
let shareScreen = async () => {
    let newStream = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false });
    if (newStream) {
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
        return true;
    }
    else {
        return false;
    }
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
let flag = -1;
let change = async () => {
    if (flag < 0) {
        let result = await shareScreen();
        if (result) {
            changeValue.value = "视频通话";
            flag = 1;
        }
        else {
            videoCall();
            changeValue.value = "共享屏幕";
        }
    }
    else {
        videoCall();
        changeValue.value = "共享屏幕";
        flag = -1;
    }
}
//接收websocket数据
let getWebsocketData = (e) => {
    let message = e.detail
    let roomId = message.roomId;
    let userId = message.userId;
    let type = message.type;
    //判断是否是当前房间
    if (roomId === room.value) {
        switch (type) {
            case "call":
                if (!caller.value) {
                    called.value = true;
                    calling.value = true;
                    answerButton.value = true;
                    console.log("用户" + userId + "请求视频通话");
                }
                else {
                    answerButton.value = false;
                }
                callButton.value = false;
                disconnectButton.value = true;
                break;
            case "accept":
                if (caller.value) {
                    //用户A收到用户B同意视频的请求
                    console.log("用户" + userId + "同意视频通话");
                    createOffer();
                }
                answerButton.value = false;
                disconnectButton.value = true;
                callButton.value = false;
                break;
            case "offer":
                //用户B收到用户A的offer
                if (called.value) {
                    offerSdp = message.message;
                    console.log("接收到用户" + userId + "的offer", JSON.parse(offerSdp));
                    createAnswer();
                }
                break;
            case "answer":
                //用户B收到用户A的answer
                if (caller.value) {
                    answerSdp = message.message;
                    console.log("接收到用户" + userId + "的answer", JSON.parse(answerSdp));
                    setAnswer();
                }
                break;
            case "answerCandidate":
                if (caller.value) {
                    answerCandidate = message.message;
                    console.log("接收到1用户" + userId + "的candidate", JSON.parse(answerCandidate));
                    setCandidate(answerCandidate);
                }
                break;
            case "offerCandidate":
                if (called.value) {
                    offerCandidate = message.message;
                    console.log("接收到用户" + userId + "的candidate", JSON.parse(offerCandidate));
                    setCandidate(offerCandidate);
                }
                break;
            case "heart":
                break;
            case "disconnect":
                // 关闭RTC连接
                if (peerConnection) {
                    peerConnection.close();
                }

                // 清理DOM元素和相关变量
                remoteVideo.value.srcObject = null;
                remoteVideo.value.src = '';
                remoteStream = null;
                peerConnection = null;

                // 重置按钮状态
                callButton.value = true;
                answerButton.value = false;
                disconnectButton.value = false;
                break;
        }
    }
}
window.addEventListener("message", getWebsocketData)

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
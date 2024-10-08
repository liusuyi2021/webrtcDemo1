<!-- 1v1通话 拨号版
    指定拨打的用户id进行1对1通话
    实现同一个房间内，一人发起 一个人接收的视频通话
-->
<template>
    <el-row>
        <el-col :span="12">
            <div class="video-call-container">
                <p>本地视频</p>
                <video ref="localVideo" id="local-video"></video>
                <div>
                    <el-button type="success" @click="startLocalRecording" :disabled="localRecording">开始录像</el-button>
                    <el-button type="danger" @click="stopLocalRecording" :disabled="!localRecording">停止录像</el-button>
                    <el-button type="warning" @click="localSnap">截图</el-button>
                    <canvas ref="canvas" style="display:none;"></canvas>
                </div>
                <br />
                <p>远程视频</p>
                <video ref="remoteVideo" id="remote-video"></video>
                <div>
                    <el-button type="success" @click="startRemoteRecording" :disabled="remoteRecording">开始录像</el-button>
                    <el-button type="danger" @click="stopRemoteRecording" :disabled="!remoteRecording">停止录像</el-button>
                    <el-button type="warning" @click="remoteSnap">截图</el-button>
                </div>
            </div>
        </el-col>
        <el-col :span="12">
            <div id="info" class="user-container">
                <el-row>
                    <el-col>
                        <div style="width: 100%;">
                            <p>用户列表</p>
                            <div class="user-list">
                                <ul v-infinite-scroll="load" class="infinite-list" style="overflow: auto">
                                    <li class="infinite-list-item" v-for="user in uStore.list"
                                        :class="{ selected: user === selectedUser }" @click="selectUser(user)"
                                        :key="user.id">{{
                                            user.nickName }}</li>
                                </ul>
                            </div>
                        </div>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col>
                        <div style="padding: 10px">
                            房间： <el-input v-model="room" vlue="1" placeholder="请输入房间号" />
                        </div>
                        <div style="padding: 10px">
                            编号： <el-input v-model="user" vlue="1" placeholder="请输入用户ID" />
                        </div>
                        <div style="padding: 10px">
                            姓名： <el-input v-model="nickName" vlue="张三" placeholder="请输入用户姓名" />
                        </div>
                        <div style="padding: 10px">
                            <el-button id="enter-room" type="primary" round @click="enterRoom">加入房间</el-button>
                            <el-button id="switch-call" type="primary" round @click="switchCall">{{ changeValue
                                }}</el-button>

                        </div>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col>
                        <div style="padding: 10px">
                            <el-button id="create-offer" type="success" round icon="phone" v-if="callButtonShow"
                                @click="callRemote">拨打</el-button>
                            <el-button id="create-answer" type="danger" round icon="phone" v-if="answerButtonShow"
                                @click="acceptCall">接听</el-button>
                            <el-button id="disconnect" type="danger" round icon="phone" v-if="disconnectButtonShow"
                                @click="disconnect">挂断</el-button>
                        </div>
                    </el-col>
                </el-row>
                <el-row>
                    <el-input id="sendText" v-model="sendText" placeholder="请输入发送内容"></el-input>
                    <el-button id="sendMsg" type="success" round icon="phone" @click="sendMsg">发送</el-button>
                </el-row>
            </div>
        </el-col>
    </el-row>
</template>
<script setup lang="ts">
import { ref } from 'vue';
import { initWebsocket, sendMessage } from './js/websocket.js';
import { userStore, sdpStore, iceStore } from '@/store/store.js';
import RecordRTC from 'recordrtc';
const sendText = ref('')
const uStore = userStore();
const sStore = sdpStore();
const iStore = iceStore();
let offerSdp = sStore.offerSdp;
let answerSdp = sStore.answerSdp;
let offerCandidate = iStore.offerCandidate;
let answerCandidate = iStore.answerCandidate;

const room = ref('')//房间号
const user = ref('')//用户
const userTarget = ref('')//目标用户
const nickName = ref('') //用户姓名
let selectedUser = ref() //选中用户

//录像
const localRecordRTC = ref(null);
const remoteRecordRTC = ref(null);
const localRecording = ref(false);
const remoteRecording = ref(false);

//webrtc-streamer
let webRtcServer; //rtsp
let webRtcServer1;//本地摄像头

let localStream;//本地流
let screenStream;//本地流
let remoteStream;//远程流

let peerConnection;//RTCPeerConnection实例
let localDataChannel;//本地数据通道
let remoteDataChannel;//远程数据通道
let localVideo = ref()//本地视频元素
let remoteVideo = ref()//远程视频元素

let changeValue = ref("共享屏幕")

let caller = ref(false);//发起方
let called = ref(false);//被叫方
let calling = ref(false);//呼叫中
let communicating = ref(false);//正在通话
let detectStream = ref(false);//检测流是否存在
let callback = ref(false); //回拨标志

const callButtonShow = ref(false);//拨号按钮显示
const answerButtonShow = ref(false);//接听按钮显示
const disconnectButtonShow = ref(false);//挂断按钮显示

//进入房间
let enterRoom = () => {
    callButtonShow.value = true;
    uStore.setWSConfig(room.value, user.value, nickName.value)
    initWebsocket();
}

//获取本地摄像头流
let getLocalStream = async () => {
    await navigator.mediaDevices.getUserMedia({ audio: false, video: true })
        .then(stream => {
            localVideo.value.srcObject = stream;
            localVideo.value.play();
            localStream = stream;
            detectStream.value = true;
            return stream;
        })
        .catch(error => {
            console.error("获取本地视频流失败", error);
            detectStream.value = false;
        });
}

//创建RTCPeerConnection实例
let createPeer = async () => {
    peerConnection = new RTCPeerConnection();
    localDataChannel = peerConnection.createDataChannel("dataChannel");
    localDataChannel.onopen = () => {
        console.log("DataChannel已打开");
    }
    peerConnection.ondatachannel = (event) => {
        remoteDataChannel = event.channel;
        remoteDataChannel.onmessage = (event) => {
            console.log("收到对方消息：", event.data);
        }
        remoteDataChannel.onopen = () => console.log("*** receive：", remoteDataChannel.readyState);
        remoteDataChannel.onclose = () => {
            console.log("*** receive：", remoteDataChannel.readyState);
        };

    }
}
let sendMsg = async () => {
    localDataChannel.send(sendText.value);
}
//请求视频通话
let callRemote = async () => {
    if (selectedUser.value) {
        if (selectedUser.value.id === user.value) {
            alert("不能呼叫自己")
            return;
        }
        caller.value = true;//标识当前用户为发起方
        calling.value = true;//表示正在呼叫
        await getLocalStream();
        userTarget.value = selectedUser.value.id;
        if (detectStream.value) {
            sendMessage("call", userTarget.value, "请求视频通话");
        }
        else {
            sendMessage("call", userTarget.value, "请求回拨视频通话");
            callback.value = true;
        }
    }
    else {
        alert("请选择用户")
    }
}

//同意视频通话
let acceptCall = async () => {
    answerButtonShow.value = false;
    sendMessage("accept", userTarget.value, "同意视频通话");
    if (callback.value) {
        await getLocalStream();
        createOffer();
    }
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
            setTimeout(() => {
                if (event.candidate.candidate.includes("relay")) {
                    console.log("Using TURN server for media relay.");
                }
                console.log("用户" + user.value + "生成并发送至用户" + userTarget.value + "offerCandidate", event.candidate);
                sendMessage("offerCandidate", userTarget.value, JSON.stringify(event.candidate));
            }, 10);

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
    sendMessage("offer", userTarget.value, JSON.stringify(offer));
    console.log("用户" + user.value + "生成并发送至用户" + userTarget.value + "offer", offer);
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
            setTimeout(() => {
                if (event.candidate.candidate.includes("relay")) {
                    console.log("Using TURN server for media relay.");
                }
                console.log("用户" + user.value + "生成并发送至用户" + userTarget.value + "answerCandidate", event.candidate);
                sendMessage("answerCandidate", userTarget.value, JSON.stringify(event.candidate));
            }, 10);

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
    sendMessage("answer", userTarget.value, JSON.stringify(answer));
    console.log("用户" + user.value + "生成并发送至用户" + userTarget.value + "answer", answer)
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

    if (localStream) {
        localStream.getTracks().forEach(track => track.stop());
    }
    if (remoteStream) {
        remoteStream.getTracks().forEach(track => track.stop());
    }
    // 重置按钮状态
    callButtonShow.value = true;
    answerButtonShow.value = false;
    disconnectButtonShow.value = false;
    sendMessage("disconnect", "", "disconnect");
}

//分享屏幕
let shareScreen = async () => {
     screenStream = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: true });
    if (screenStream) {
        localStream = screenStream;
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
let stopScreenShare=async()=> {
      if (screenStream) {
        const tracks = screenStream.getTracks();
        tracks.forEach(track => track.stop());
        screenStream = null;
        localVideo.value.srcObject = null;
      }
    }
//视频通话
let videoCall = async () => {
    let newStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
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

//切换视频通话和屏幕共享
let flag = -1;
let switchCall = async () => {
    if (flag < 0) {
        let result = await shareScreen();
        if (result) {
            changeValue.value = "视频通话";
            flag = 1;
        }
        else {
            stopScreenShare();
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
    let users = message.users;
    let targetUserId = message.targetUserId;
    //判断是否是当前房间
    if (roomId === room.value) {
        switch (type) {
            case "call":
                called.value = true;
                calling.value = true;
                answerButtonShow.value = true;
                userTarget.value = userId; //请求用户作为目标用户
                console.log("用户" + userId + "请求用户" + targetUserId + "视频通话");
                if (message.message === "请求回拨视频通话") {
                    callback.value = true;
                }
                callButtonShow.value = false;
                disconnectButtonShow.value = true;
                break;
            case "accept":
                if (callback.value) {
                    if (caller.value) {
                        //用户A收到用户B同意视频的请求
                        console.log("用户" + userId + "同意回拨视频通话");
                    }
                }
                else {
                    if (caller.value) {
                        //用户A收到用户B同意视频的请求
                        console.log("用户" + userId + "同意视频通话");
                        createOffer();
                    }
                }
                answerButtonShow.value = false;
                disconnectButtonShow.value = true;
                callButtonShow.value = false;
                break;
            case "offer":

                offerSdp = message.message;
                if (callback.value) {
                    if (caller.value) {
                        console.log("接收到用户" + userId + "的offer", JSON.parse(offerSdp));
                        createAnswer();
                    }
                } else {
                    //用户B收到用户A的offer
                    if (called.value) {
                        console.log("接收到用户" + userId + "的offer", JSON.parse(offerSdp));
                        createAnswer();
                    }
                }

                break;
            case "answer":

                answerSdp = message.message;
                if (callback.value) {
                    if (called.value) {
                        console.log("接收到用户" + userId + "的answer", JSON.parse(answerSdp));
                        setAnswer();
                    }
                }
                else {
                    //用户B收到用户A的answer
                    if (caller.value) {
                        console.log("接收到用户" + userId + "的answer", JSON.parse(answerSdp));
                        setAnswer();
                    }
                }
                break;
            case "answerCandidate":
                answerCandidate = message.message;
                if (callback.value) {
                    if (called.value) {
                        console.log("接收到1用户" + userId + "的candidate", JSON.parse(answerCandidate));
                        setCandidate(answerCandidate);
                    }
                } else {
                    if (caller.value) {
                        console.log("接收到1用户" + userId + "的candidate", JSON.parse(answerCandidate));
                        setCandidate(answerCandidate);
                    }
                }
                break;
            case "offerCandidate":
                offerCandidate = message.message;
                if (callback.value) {
                    if (caller.value) {
                        console.log("接收到用户" + userId + "的candidate", JSON.parse(offerCandidate));
                        setCandidate(offerCandidate);
                    }
                }
                else {
                    if (called.value) {
                        console.log("接收到用户" + userId + "的candidate", JSON.parse(offerCandidate));
                        setCandidate(offerCandidate);
                    }
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
                if (localStream) {
                    localStream.getTracks().forEach(track => track.stop());
                }
                if (remoteStream) {
                    remoteStream.getTracks().forEach(track => track.stop());
                }
                // 重置按钮状态
                callButtonShow.value = true;
                answerButtonShow.value = false;
                disconnectButtonShow.value = false;
                break;
            case "join":
                uStore.setUsers(users)
                break;
            case "leave":
                uStore.setUsers(users)
                break;
        }
    }
}
window.addEventListener("message", getWebsocketData)

const count = ref(0)
const load = () => {
    count.value += 2
}
//选择用户
let selectUser = async (user) => {
    selectedUser.value = user;
}

/*********************************************/
let realViewCam = async () => {
    //@ts-ignore
    webRtcServer = new WebRtcStreamer(localVideo.value, "https://192.168.1.227:8083");
    let rtspUrl = "rtsp://admin:ard511ar@112.98.126.2:21500/h264/ch1/main/av_stream";
    let option = "rtptransport=udp";
    webRtcServer.connect(rtspUrl, null, option, null);
    //@ts-ignore
    webRtcServer1 = new WebRtcStreamer(remoteVideo.value, "https://192.168.1.227:8083");
    let rtspUrl1 = "videocap://0";
    let option1 = "rtptransport=udp";
    webRtcServer1.connect(rtspUrl1, null, option1, null);
}
let realViewCamClose = async () => {
    webRtcServer.disconnect();
    webRtcServer1.disconnect();
}
/*****************本地录像*********************/
//开始本地录像
const startLocalRecording = () => {
    localRecordRTC.value = new RecordRTC(localVideo.value.srcObject, { type: 'video', mimeType: 'video/webm' });
    localRecordRTC.value.startRecording();
    localRecording.value = true;
};
//停止本地录像
const stopLocalRecording = () => {
    if (localRecordRTC.value) {
        localRecordRTC.value.stopRecording(() => {
            const blob = localRecordRTC.value.getBlob();
            const url = URL.createObjectURL(blob);
            downloadFile(url)
            localRecording.value = false;
        });
    }
};
//开始远程录像
const startRemoteRecording = () => {
    remoteRecordRTC.value = new RecordRTC(remoteVideo.value.srcObject, { type: 'video', mimeType: 'video/webm' });
    remoteRecordRTC.value.startRecording();
    remoteRecording.value = true;
};
//停止远程录像
const stopRemoteRecording = () => {
    if (remoteRecordRTC.value) {
        remoteRecordRTC.value.stopRecording(() => {
            const blob = remoteRecordRTC.value.getBlob();
            const url = URL.createObjectURL(blob);
            downloadFile(url)

            remoteRecording.value = false;
        });
    }
};
//下载录像
const downloadFile = async (fileUrl) => {
    try {
        const response = await fetch(fileUrl);
        const blob = await response.blob();
        getSeekableBlob(blob, 'video/webm', function (seekableBlob) {
            const url = window.URL.createObjectURL(new Blob([seekableBlob], { type: 'video/mp4' }));
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            a.download = fileUrl.split('/').pop().split('?')[0]
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
        })

    } catch (error) {
        console.error('Error downloading the file', error);
    }
}

let canvas = ref()
//本地截图
const localSnap = async () => {
    const context = canvas.value.getContext('2d');
    canvas.value.width = localVideo.value.videoWidth;
    canvas.value.height = localVideo.value.videoHeight;
    context.drawImage(localVideo.value, 0, 0, canvas.value.width, canvas.value.height);

    canvas.value.toBlob(blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const timestamp = new Date().getTime();
        a.download = `screenshot_${timestamp}.jpeg`;
        a.click();
        URL.revokeObjectURL(url);
    }, 'image/jpeg');
}
//远程截图
const remoteSnap = async () => {
    const context = canvas.value.getContext('2d');
    canvas.value.width = remoteVideo.value.videoWidth;
    canvas.value.height = remoteVideo.value.videoHeight;
    context.drawImage(remoteVideo.value, 0, 0, canvas.value.width, canvas.value.height);

    canvas.value.toBlob(blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const timestamp = new Date().getTime();
        a.download = `screenshot_${timestamp}.jpeg`;
        a.click();
        URL.revokeObjectURL(url);
    }, 'image/jpeg');
}
/**
 * @param {Blob} file - File or Blob object.
 * @param {function} callback - Callback function.
 * @example
 * getSeekableBlob(blob or file, callback);
 * @see {@link https://github.com/muaz-khan/RecordRTC|RecordRTC Source Code}
 */
const getSeekableBlob = async (inputBlob, mediaType, callback) => {
    //@ts-ignore
    const reader = new EBML.Reader()
    //@ts-ignore
    const decoder = new EBML.Decoder()
    //@ts-ignore
    const tools = EBML.tools

    const fileReader = new FileReader()
    fileReader.onload = function () {
        const ebmlElms = decoder.decode(this.result)
        ebmlElms.forEach(function (element) {
            reader.read(element)
        })
        reader.stop()
        const refinedMetadataBuf = tools.makeMetadataSeekable(
            reader.metadatas,
            reader.duration,
            reader.cues
        )
        const body = this.result.slice(reader.metadataSize)
        const newBlob = new Blob([refinedMetadataBuf, body], {
            type: mediaType
        })

        callback(newBlob)
    }
    fileReader.readAsArrayBuffer(inputBlob)
}
</script>
<style scoped>
.video-call-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 90vh;
    background-color: rgb(243, 245, 242);
    border-radius: 20px;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
    padding: 10px;
}

.user {
    padding: 10px;
    cursor: pointer;
}

.selected {
    background-color: rgb(192, 19, 62) !important;
}

.user-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 90vh;
    background-color: white;
    border-radius: 20px;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
    padding: 10px;
}

.user-list {
    align-items: left;
    background-color: rgb(212, 226, 230);
    border-radius: 20px;
    box-shadow: 0 0 10px rgba(17, 17, 17, 0.1);

}

.el-col {
    padding: 5px;
}

.el-row {
    height: 100%;
    width: 100%;
}

video {
    width: 100%;
    max-width: 400px;
    margin-bottom: 20px;
    border-radius: 10px;
    background-color: black;

}

.showContainer {
    display: flex;
    justify-content: center;
    /* 水平居中 */
    align-items: center;
    /* 垂直居中 */
    height: 100%;
    /* 设定高度，可以根据需要调整 */
    margin: 0;
}

.video-container {
    border: 1px solid #1ca8e9;
    padding: 2px;
    position: sticky;
    object-fit: cover;
}

#local-video {
    flex: 1;
    object-fit: fill;
    width: 100%;
    height: 100%;
}

#remote-video {
    flex: 1;
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

textarea {
    width: 100%;
    height: 100px;
}

.infinite-list {
    width: 100%;
    height: 300px;
    padding: 0;
    margin: 0;
    list-style: none;
}

.infinite-list .infinite-list-item {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 30px;
    background: var(--el-color-primary-light-9);
    margin: 10px;
    color: var(--el-color-primary);
    border-radius: 30px;
}

.infinite-list .infinite-list-item+.list-item {
    margin-top: 10px;
}
</style>
<!-- 多v多通话-->
<template>
    <el-row>
        <el-col :span="16">
            <div class="video-container">
                <div ref="videos" class="videos">
                    <div v-for="user in users" :key="user.id" class="video-wrapper">
                        <h1 style="background-color: aliceblue;">用户{{ user.id }}</h1>
                        <video :id="`userVideo-${user.id}`" playsinline autoplay></video>
                    </div>
                </div>
            </div>
        </el-col>
        <el-col :span="8">
            <div id="info" class="user-container">
                <el-row>
                    <el-col>
                        <div style="width: 100%;">
                            <p>用户列表</p>
                            <div class="user-list">
                                <ul v-infinite-scroll="load" class="infinite-list" style="overflow: auto">
                                    <li class="infinite-list-item" v-for="user in users"
                                        :class="{ selected: user === selectedUser }" :key="user.id"
                                        @click="selectUser(user)">{{
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
                                @click="callRemote">加入群聊</el-button>
                            <el-button id="create-answer" type="danger" round icon="phone" v-if="answerButtonShow"
                                @click="acceptCall">接听</el-button>
                            <el-button id="disconnect" type="danger" round icon="phone" v-if="disconnectButtonShow"
                                @click="disconnect">挂断</el-button>
                        </div>
                    </el-col>
                </el-row>
            </div>
        </el-col>
    </el-row>
</template>
<script setup lang="ts">
import { ref, nextTick, watch } from 'vue';
import { openWebSocket, sendMessage } from './js/websocket.js';
import { userStore, sdpStore, iceStore } from '@/store/store.js';

const hoster = ref(false);//主持人

const uStore = userStore();
const sStore = sdpStore();
const iStore = iceStore();

let offerSdp = sStore.offerSdp;
let answerSdp = sStore.answerSdp;
let offerCandidate = iStore.offerCandidate;
let answerCandidate = iStore.answerCandidate;

let peerConnections = {}; // 存储每个用户的 RTCPeerConnection 实例

const room = ref('')//房间号
const user = ref('')//用户
const users = ref([])//所有用户
const userTarget = ref('')//目标用户
const nickName = ref('') //用户姓名
let selectedUser = ref() //选中用户

let localStream;//本地流
let remoteStream;//远程流
let videos = ref()
let changeValue = ref("共享屏幕")

const callButtonShow = ref(false);//拨号按钮显示
const answerButtonShow = ref(false);//接听按钮显示
const disconnectButtonShow = ref(false);//挂断按钮显示

//获取本地摄像头流
let getLocalStream = async () => {
    await navigator.mediaDevices.getUserMedia({ audio: true, video: true })
        .then(stream => {
            localStream = stream;
            return stream;
        })
        .catch(error => {
            console.error("获取本地视频流失败", error);
        });
}
//选择用户
let selectUser = async (user) => {
    selectedUser.value = user;
}
//进入房间
let enterRoom = async () => {
    callButtonShow.value = true;
    uStore.setWSConfig(room.value, user.value, nickName.value)
    openWebSocket();

}
//监听到用户变化后取本地流播放
watch(users, async (newValue, oldValue) => {
    // 检查变化是否涉及当前用户
    const newUser = newValue.find(u => u.id === user.value);
    const oldUser = oldValue.find(u => u.id === user.value);
    if (newUser && !oldUser) {
        console.log("打开本地摄像头");
        await getLocalStream();
        const videoElement = document.getElementById(`userVideo-${user.value}`) as HTMLVideoElement | null;
        if (videoElement) {
            console.log("获取本地视频流成功", localStream);
            videoElement.srcObject = localStream; // 设置视频流
        }
    }
}, {
    deep: true // 深度监听对象或数组的变化
});

let createPeerConnections = async () => {
    users.value.forEach((_user) => {
        if (_user.id == user.value) {
            return;
        }
        if (peerConnections.hasOwnProperty(_user.id) == false) {
            createPeerConnection(_user.id);
        }
    })
}

// 创建单个用户的 RTCPeerConnection 实例
const createPeerConnection = (userId) => {
    const peerConnection = new RTCPeerConnection();
    peerConnections[userId] = peerConnection;
    console.log("创建" + userId + "的peerConnection")
    return peerConnection;
}
//请求加入群聊
let callRemote = async () => {
    //给除了自己的其他用户发送offer             
    users.value.forEach((_user) => {
        if (_user.id !== user.value) {
            userTarget.value = _user.id;
            createOffer(_user.id)
        }
    })
}

//同意视频通话
let acceptCall = async () => {
    answerButtonShow.value = false;
    let message = {
        type: "accept",
        roomId: room.value,
        userId: user.value,
        targetUserId: userTarget.value,
        content: "同意视频通话",
    }
    sendMessage(message);
}

//创建发送offer
let createOffer = async (userId) => {
    if (peerConnections.hasOwnProperty(userId) == false) {
        createPeerConnection(userId);
        console.log(peerConnections[userId])
        console.log(peerConnections)
    }
    //获取本地音视频流
    await getLocalStream();
    //添加本地音视频流
    if (localStream) {
        peerConnections[userId].addStream(localStream)
    }
    //通过监听onicecandidates事件来获取ICE候选信息
    peerConnections[userId].onicecandidate = (event) => {
        if (event.candidate) {
            setTimeout(() => {
                if (event.candidate.candidate.includes("relay")) {
                    console.log("Using TURN server for media relay.");
                }
                console.log("用户" + user.value + "生成并发送至用户" + userId + "offerCandidate", event.candidate);
                let message = {
                    type: "offerCandidate",
                    roomId: room.value,
                    userId: user.value,
                    targetUserId: userTarget.value,
                    content: JSON.stringify(event.candidate),
                }
                sendMessage(message);
            }, 10);

        }
    }
    //监听onaddstream来获取对方的音视频流
    peerConnections[userId].onaddstream = (event) => {
        nextTick(); // 等待 Vue 更新 DOM
        console.log("用户" + user.value + "收到对方" + userId + "音视频流", event.stream);
        const videoElement = document.getElementById(`userVideo-${userId}`) as HTMLVideoElement | null;
        if (videoElement) {
            videoElement.srcObject = event.stream; // 设置视频流
        }
    }
    //监听addEventListener来获取对方的音视频流
    // peerConnections[userId].addEventListener('track', (event) => {
    //     let stream = new MediaStream()
    //     event.streams[0].getTracks().forEach(track => {
    //         stream.addTrack(track);
    //     });
    //     console.log("用户" + user.value + "收到对方" + userId + "音视频流", stream);
    //     const videoElement = document.getElementById(`userVideo-${userId}`) as HTMLVideoElement | null;
    //     if (videoElement) {
    //         console.log("设置视频流" + userId)
    //         videoElement.srcObject = stream; // 设置视频流
    //     }
    // });

    //创建offer
    const offer = await peerConnections[userId].createOffer();
    await peerConnections[userId].setLocalDescription(offer);

    //显示offer
    offerSdp = JSON.stringify(offer);

    //发送websocket
    let message = {
                    type: "offer",
                    roomId: room.value,
                    userId: user.value,
                    targetUserId: userTarget.value,
                    content: JSON.stringify(offer),
                }
    sendMessage(message);
    console.log("用户" + user.value + "生成并发送至用户" + userId + "offer", offer);
}

//创建发送answer
let createAnswer = async (userId) => {
    if (peerConnections.hasOwnProperty(userId) == false) {
        createPeerConnection(userId);
        console.log(peerConnections[userId])
        console.log(peerConnections)
    }
    //获取本地音视频流
    await getLocalStream();
    //添加本地音视频流
    if (localStream) {
        peerConnections[userId].addStream(localStream)
    }
    //通过监听onicecandidates事件来获取ICE候选信息
    peerConnections[userId].onicecandidate = (event) => {
        if (event.candidate) {
            setTimeout(() => {
                if (event.candidate.candidate.includes("relay")) {
                    console.log("Using TURN server for media relay.");
                }
                console.log("用户" + user.value + "生成并发送至用户" + userId + "answerCandidate", event.candidate);
                let message = {
                    type: "answerCandidate",
                    roomId: room.value,
                    userId: user.value,
                    targetUserId: userTarget.value,
                    content: JSON.stringify(event.candidate),
                }
                sendMessage(message);
            }, 10);

        }
    }
    //监听onaddstream来获取对方的音视频流
    peerConnections[userId].onaddstream = (event) => {
        nextTick(); // 等待 Vue 更新 DOM
        console.log("用户" + user.value + "收到对方" + userId + "音视频流", event.stream);
        const videoElement = document.getElementById(`userVideo-${userId}`) as HTMLVideoElement | null;
        if (videoElement) {
            videoElement.srcObject = event.stream; // 设置视频流
        }
    }
    // peerConnections[userId].addEventListener('track', (event) => {
    //     let stream = new MediaStream()
    //     event.streams[0].getTracks().forEach(track => {
    //         stream.addTrack(track);
    //     });
    //     console.log("用户" + user.value + "收到对方" + userId + "音视频流", stream);
    //     const videoElement = document.getElementById(`userVideo-${userId}`) as HTMLVideoElement | null;
    //     if (videoElement) {
    //         console.log(stream)
    //         console.log("设置视频流" + userId)
    //         videoElement.srcObject = stream; // 设置视频流
    //     }
    // });
    //设置远端描述信息
    await peerConnections[userId].setRemoteDescription(JSON.parse(offerSdp))

    //生成answer
    const answer = await peerConnections[userId].createAnswer();
    //设置本地answer信息
    await peerConnections[userId].setLocalDescription(answer);

    //显示answer
    answerSdp = JSON.stringify(answer);

    //发送answer
    let message = {
                    type: "answer",
                    roomId: room.value,
                    userId: user.value,
                    targetUserId: userTarget.value,
                    content: JSON.stringify(answer),
                }
    sendMessage(message);
    console.log("用户" + user.value + "生成并发送至用户" + userId + "answer", answer)
}

//设置远端answer信息
let setAnswer = async (userId) => {
    await peerConnections[userId].setRemoteDescription(JSON.parse(answerSdp));
}

//设置远端candidate信息
let setCandidate = async (userId, event) => {
    if (peerConnections[userId].remoteDescription == null) {
        return;
    }
    await peerConnections[userId].addIceCandidate(JSON.parse(event));
}

//挂断
let disconnect = async (userId) => {
    // 关闭RTC连接
    if (peerConnections[userId]) {
        peerConnections[userId].close();
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
    let message = {
                    type: "disconnect",
                    roomId: room.value,
                    userId: user.value,
                    targetUserId: userTarget.value,
                    content: "disconnect",
                }
    sendMessage(message);
}

//分享屏幕
let shareScreen = async (userId) => {

    console.log(user.value)
    let newStream = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false });
    if (newStream) {
        localStream = newStream;
        const videoElement = document.getElementById(`userVideo-${user.value}`) as HTMLVideoElement | null;
        if (videoElement) {
            videoElement.srcObject = localStream; // 设置视频流
        }

        //本地视频流轨道加入
        localStream.getTracks().forEach(track => {
            peerConnections[userId].addTrack(track, localStream);
        });

        // 替换本地视频流轨道
        localStream.getVideoTracks().forEach(track => {
            peerConnections[userId].getSenders().find(sender => sender.track.kind === 'video')
                .replaceTrack(track)
        })
        return true;
    }
    else {
        return false;
    }
};

//视频通话
let videoCall = async (userId) => {
    let newStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
    localStream = newStream;
    // 将视频流传入viedo控件  
    const videoElement = document.getElementById(`userVideo-${user.value}`) as HTMLVideoElement | null;
    if (videoElement) {
        videoElement.srcObject = localStream; // 设置视频流
    }

    //本地视频流轨道加入
    localStream.getTracks().forEach(track => {
        peerConnections[userId].addTrack(track, localStream);
    });

    // 替换本地视频流轨道
    localStream.getVideoTracks().forEach(track => {
        peerConnections[userId].getSenders().find(sender => sender.track.kind === 'video')
            .replaceTrack(track)
    })

};

//切换视频通话和屏幕共享
let flag = -1;
let switchCall = async () => {
    if (!selectedUser.value) {
        alert("请选择给哪个用户共享")
        return;
    }
    if (flag < 0) {
        let result = await shareScreen(selectedUser.value.id);
        if (result) {
            changeValue.value = "视频通话";
            flag = 1;
        }
        else {
            videoCall(selectedUser.value.id);
            changeValue.value = "共享屏幕";
        }
    }
    else {
        videoCall(selectedUser.value.id);
        changeValue.value = "共享屏幕";
        flag = -1;
    }
}
//接收websocket数据
let getWebsocketData = (e) => {
    let message = e.detail
    let roomId = message.roomId;
    //判断是否是当前房间
    if (roomId === room.value) {
        let userId
        let type = message.type;
        switch (type) {
            case "offer":
                userId = message.userId;
                offerSdp = message.message;
                console.log("接收到用户" + userId + "的offer", JSON.parse(offerSdp));
                createAnswer(userId);
                break;
            case "answer":
                userId = message.userId;
                answerSdp = message.message;
                console.log("接收到用户" + userId + "的answer", JSON.parse(answerSdp));
                setAnswer(userId);
                break;

            case "offerCandidate":
                userId = message.userId;
                offerCandidate = message.message;
                console.log("接收到用户" + userId + "的candidate", JSON.parse(offerCandidate));
                setCandidate(userId, offerCandidate);
                break;

            case "answerCandidate":
                userId = message.userId;
                answerCandidate = message.message;
                console.log("接收到用户" + userId + "的candidate", JSON.parse(answerCandidate));
                setCandidate(userId, answerCandidate);
                break;

            case "heart":
                break;
            case "disconnect":
                // 关闭RTC连接
                if (peerConnections[userId]) {
                    peerConnections[userId].close();
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
                users.value = message.users;
                userId = message.userId;
                // 创建RTC连接
                // createPeerConnections();
                //console.log(peerConnections)

                // if (users.value.length === 1) {
                //     console.log("我是第一个进来的人")
                // }
                // if (!hoster.value) {
                //     //给除了自己的其他用户发送offer             
                //     users.value.forEach((_user) => {
                //         if (_user.id !== user.value) {
                //             userTarget.value = _user.id;
                //             createOffer(_user.id)
                //         }
                //     })
                // }
                // hoster.value = true;
                break;
            case "leave":
                //uStore.setUsers(users)
                break;
        }
    }
}
window.addEventListener("message", getWebsocketData)

const count = ref(0)
const load = () => {
    count.value += 2
}

</script>
<style scoped>
.video-call-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 80vh;
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
    height: 80vh;
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

.video-container {
    display: flex;
    flex-direction: column;
    align-items: center;
}

.videos {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    align-items: center;
    overflow: auto;
    width: 100%;
    max-height: 80vh;
}

.video-wrapper {
    flex: 0 1 40%;
    max-width: 40%;
    margin: 5px;
    box-sizing: border-box;
}
</style>
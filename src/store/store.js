import { defineStore } from "pinia";

// 用户 Store
export const userStore = defineStore("store", {
  state: () => {
    return {
      roomId: "",
      userId: "",
      nickName: "",
      list: [],  // 存储用户列表
    };
  },
  getters: {
    getRoomId: (state) => state.roomId,
    getUserId: (state) => state.userId,
    getUsers: (state) => state.list, 
  },
  actions: {
    // 设置房间配置
    setWSConfig(_roomId, _userId, _nickName) {
      this.roomId = _roomId;
      this.userId = _userId;
      this.nickName = _nickName;
    },
    // 设置用户列表
    setUsers(users) {
      this.list = users;
    },
    // 添加用户
    addUser(item) {
      this.list.push(item);
    },
    // 移除用户
    removeUser(userId) {
      this.list = this.list.filter(user => user !== userId);  // 通过 userId 移除
    },
    // 更新用户
    updateUser(userId, newItem) {
      const index = this.list.findIndex(user => user.id === userId);
      if (index !== -1) {
        this.list[index] = newItem;
      }
    },
    // 清空用户列表
    clearList() {
      this.list = [];
    },
  },
});

// SDP Store
export const sdpStore = defineStore("sdp", {
  state: () => {
    return {
      offerSdp: "",
      answerSdp: "",
    };
  },
  actions: {
    setOfferSdp(offerSdp) {
      this.offerSdp = offerSdp;
    },
    setAnswerSdp(answerSdp) {
      this.answerSdp = answerSdp;
    },
  },
});

// ICE Store
export const iceStore = defineStore("ice", {
  state: () => {
    return {
      iceCandidate: ""
    };
  },
  actions: {
    setCandidate(iceCandidate) {
      this.iceCandidate = iceCandidate;
    },
  },
});

import { defineStore } from "pinia";
export const userStore = defineStore("store", {
  state: () => {
    return {
      roomId: "",
      userId: "",
      nickName: "",
      list: [],
    };
  },
  getters: {
    getRoomId: (state) => state.roomId,
    getUserId: (state) => state.userId,
    getUsers: (state) => state.users,
  },
  actions: {
    setWSConfig(_roomId, _userId,_nickName) {
      this.roomId = _roomId;
      this.userId = _userId;
      this.nickName = _nickName;
    },
    setUsers(users) {
      this.list = users;
      console.log(this.list);
    },
    addUser(item) {
      this.list.push(item);
    },
    removeUser(index) {
      this.list.splice(index, 1);
    },
    updateUser(index, newItem) {
      this.list[index] = newItem;
    },
    clearList() {
      this.list = [];
    },
  },
  // your other options...
});
export const sdpStore = defineStore("sdp", {
  state: () => {
    return {
      offerSdp: "",
      answerSdp: "",
    };
  },
});
export const iceStore = defineStore("ice", {
  state: () => {
    return {
      offerCandidate: "",
      answerCandidate: "",
    };
  },
});

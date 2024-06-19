import { defineStore } from "pinia";
export const userStore = defineStore("store", {
  state: () => {
    return {
      roomId: "",
      userId: "",
    };
  },
  getters: {
      getRoomId: (state) => state.roomId,
      getUserId: (state) => state.userId,
  },
  actions: {
    setWSConfig(_roomId, _userId) {
      this.roomId = _roomId;
      this.userId = _userId;
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
      candidate: "",
    };
  },
});

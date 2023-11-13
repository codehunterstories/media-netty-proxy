<template>
  <div>
    <video
      ref="videoElement"
      controls
    ></video>
  </div>
</template>

<script>
import flvjs from "flv.js";

export default {
  mounted() {
    let username = "media";
    let password = "media!12345";
    let encodedCredentials = window.btoa(username + ":" + password);

    if (flvjs.isSupported()) {
      const videoElement = this.$refs.videoElement;
      const flvPlayer = flvjs.createPlayer(
        {
          type: "flv",
          url: "http://127.0.0.1:9999/1",
          isLive: true,
          cors: true,
        },
        {
          headers: {
            Authorization: "Basic " + encodedCredentials,
          },
          isLive: true,
        }
      );
      flvPlayer.attachMediaElement(videoElement);
      flvPlayer.load();
      flvPlayer.play();
    }
  },
};
</script>

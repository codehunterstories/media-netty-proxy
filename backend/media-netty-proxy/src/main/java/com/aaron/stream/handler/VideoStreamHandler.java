package com.aaron.stream.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Real-time stream processing
 */
@Slf4j
public class VideoStreamHandler extends AbstractChannelInboundHandler {
    private String defaultStreamUrl;

    private final static ExecutorService executorService = Executors.newFixedThreadPool(30);

    public VideoStreamHandler() {
    }

    /**
     * Initial stream url
     *
     * @param defaultStreamUrl default stream url
     */
    public VideoStreamHandler(String defaultStreamUrl) {
        this.defaultStreamUrl = defaultStreamUrl;
    }

    /**
     * Processing video stream
     *
     * @param ctx       Channel context
     * @param msg       Request info
     * @param streamUrl Streaming address
     * @throws Exception error
     */
    @Override
    protected void doChannelRead0(ChannelHandlerContext ctx, FullHttpRequest msg, String streamUrl) throws IOException {
        if ((streamUrl == null || streamUrl.isBlank()) && (defaultStreamUrl == null || defaultStreamUrl.isBlank())) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        if (streamUrl == null || streamUrl.isBlank()) {
            streamUrl = this.defaultStreamUrl;
        }

        FFmpegLogCallback.set();

        // Response initialization
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpUtil.setTransferEncodingChunked(response, true);
        ctx.write(response);

        // Set up the grabber and recorder
        FFmpegFrameGrabber grabber = new LiveGrabber(streamUrl);
        grabber.start();
        executorService.execute(() -> {
            // Start grab and encode frames
            try (FFmpegFrameRecorder recorder = new LiveRecorder(
                    new FrameToByteBufOutputStream(ctx),
                    grabber.getImageWidth(),
                    grabber.getImageHeight(),
                    grabber.getAudioChannels())) {
                recorder.start();

                Frame frame;
                while ((frame = grabber.grab()) != null && ctx.channel().isActive()) {
                    recorder.setTimestamp(grabber.getTimestamp());
                    recorder.record(frame);
                }
            } catch (IOException e) {
                log.error("Error process recorder：", e);
                sendErrorResponse(ctx);
            } finally {
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
                try {
                    grabber.stop();
                } catch (FrameGrabber.Exception e) {
                    log.error("Error stop grabber：", e);
                }
            }
        });
    }

    /**
     * Output Frame stream
     */
    static class FrameToByteBufOutputStream extends ByteArrayOutputStream {
        private final ChannelHandlerContext ctx;

        public FrameToByteBufOutputStream(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        public void write(byte[] b, int off, int len) {
            ByteBuf buffer = Unpooled.wrappedBuffer(b, off, len);
            ctx.writeAndFlush(new DefaultHttpContent(buffer));
        }
    }

    /**
     * FFmpegFrameGrabber Initial
     */
    static class LiveGrabber extends FFmpegFrameGrabber {
        private final static String BUFF_SIZE = "8096000";

        public LiveGrabber(String filename) {
            super(filename);
            initLiveGrabber();
        }

        void initLiveGrabber() {
            super.setVideoOption("vcodec", "copy");
            super.setOption("buffer_size", BUFF_SIZE); // Set buffer size to improve video quality and reduce stuttering and artifacting
            super.setOption("rtsp_transport", "tcp");
        }
    }

    /**
     * FFmpegFrameRecorder Initial
     */
    static class LiveRecorder extends FFmpegFrameRecorder {
        private static final int BITRATE = 1000000; // Reduce bitrate to decrease CPU load, e.g., 1000 kbps
        private static final int FRAME_RATE = 30; // Adjust frame rate as needed, fps
        private static final int GOP_SIZE = 30; // Set GOP size to one key frame every two seconds
        private static final String FORMAT_FLV = "flv"; // Set encapsulation format

        private static final int AUDIO_BITRATE = 192000; // Set bitrate, e.g., 192 kbps
        private static final int SAMPLE_RATE = 44100; // Set sample rate
        private static final int AUDIO_CHANNELS = 2; // Set the number of audio channels, here set to stereo

        private static final Map<String, String> VIDEO_OPTION = new HashMap<>() {{
            put("tune", "zerolatency"); // Reduce startup latency, see https://trac.ffmpeg.org/wiki/StreamingGuide)
            put("preset", "ultrafast"); // 'ultrafast' has the lowest CPU usage
            put("crf", "24"); // Constant Rate Factor (see: https://trac.ffmpeg.org/wiki/Encode/H.264)
            put("threads", "8");
            // put("video_size", "640x480"); // Set resolution
        }};

        private static final Map<String, String> AUDIO_OPTION = new HashMap<>() {{
            put("crf", "0");
        }};

        public LiveRecorder(OutputStream outputStream, int imageWidth, int imageHeight, int audioChannels) {
            super(outputStream, imageWidth, imageHeight, audioChannels);
            initLiveRecorder();
        }

        void initLiveRecorder() {
            // Setting video
            for (Map.Entry<String, String> entry : VIDEO_OPTION.entrySet()) {
                super.setVideoOption(entry.getKey(), entry.getValue());
            }
            super.setVideoBitrate(BITRATE);
            super.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            super.setFormat(FORMAT_FLV);
            super.setFrameRate(FRAME_RATE);
            super.setGopSize(GOP_SIZE);
            // super.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            // super.setOption("color_range", "jpeg"); // Use "jpeg" for full range. Use "mpeg" for limited range.

            // Setting audio
            for (Map.Entry<String, String> entry : AUDIO_OPTION.entrySet()) {
                super.setAudioOption(entry.getKey(), entry.getValue());
            }
            super.setAudioQuality(0);
            super.setAudioBitrate(AUDIO_BITRATE);
            super.setSampleRate(SAMPLE_RATE);
            super.setAudioChannels(AUDIO_CHANNELS);
            super.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        }
    }
}

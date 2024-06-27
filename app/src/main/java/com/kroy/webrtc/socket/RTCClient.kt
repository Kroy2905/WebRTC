package com.kroy.webrtc.socket

import android.app.Application
import android.media.MediaActionSound
import com.kroy.webrtc.models.MessageModel
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import java.lang.IllegalStateException

class RTCClient (private val application:Application,
    private val username:String,
    private val socketRepository: SocketRepository,
                   private val observer: PeerConnection.Observer){


    init {
        initPeerConnectionfactory(application)
    }
    private  val eglContext = EglBase.create()
    private  val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:iphone-stun.strato-iphone.de:3478").createIceServer(),
        PeerConnection.IceServer.builder("stun:openrelay.metered.ca:80").createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer()
        )
    private val peerConnection by lazy { createPeerConnection(observer) }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private  fun initPeerConnectionfactory(application: Application){
        val peerconncetionOption = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(peerconncetionOption)
    }

    private  fun createPeerConnectionFactory():PeerConnectionFactory{
        return  PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(
                eglContext.eglBaseContext,
                true,
                true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            }).createPeerConnectionFactory()
    }
    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(iceServer, observer)
    }

    fun initializeSurfaceView(surface:SurfaceViewRenderer){

        surface.run {
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglContext.eglBaseContext,null)
        }
    }
    fun startLocalVideo(surface: SurfaceViewRenderer){
        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name,eglContext.eglBaseContext)
        val videoCapturer = getVideoCapturer(application)
        videoCapturer.initialize(surfaceTextureHelper,
            surface.context,localVideoSource.capturerObserver)
        videoCapturer.startCapture(
            281,  // width of frame
            352,   // height of frame
            30   // fps
        )
        val localVideoTrack = peerConnectionFactory.createVideoTrack("local_track",localVideoSource)
        localVideoTrack.addSink(surface)
        val localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio_track",localAudioSource)
        val localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        localStream.addTrack(localAudioTrack)  // connecting audio and stream
        localStream.addTrack(localVideoTrack)  // connecting video and stream
        peerConnection?.addStream(localStream)  // adding the stream to peerCOnnection






    }


    private fun getVideoCapturer(application: Application):VideoCapturer{
        return  Camera2Enumerator(application).run {
            deviceNames.find {
                isFrontFacing(it)  //check for front facing camera
            }?.let {
                createCapturer(it,null)
            }?:throw
                    IllegalStateException()
        }
    }

    fun call(target: String) {
        val mediaConstraints =MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        peerConnection?.createOffer(object : SdpObserver{
            override fun onCreateSuccess(desc: SessionDescription?) {
             peerConnection?.setLocalDescription(object :SdpObserver{
                 override fun onCreateSuccess(p0: SessionDescription?) {

                 }

                 override fun onSetSuccess() {
                     val offer = hashMapOf(
                         "sdp" to desc?.description,
                         "type" to desc?.type
                     )

                     socketRepository.sendMessageToSocket( // Sending session description data to call Activity
                         MessageModel(
                             "create_offer", username, target, offer
                         )
                     )
                 }

                 override fun onCreateFailure(p0: String?) {

                 }

                 override fun onSetFailure(p0: String?) {

                 }

             },desc)
            }

            override fun onSetSuccess() {
                TODO("Not yet implemented")
            }

            override fun onCreateFailure(p0: String?) {
                TODO("Not yet implemented")
            }

            override fun onSetFailure(p0: String?) {
                TODO("Not yet implemented")
            }

        },mediaConstraints)
    }

    fun onRemoteSesionReceived(session: SessionDescription) {
            peerConnection?.setRemoteDescription(object  :SdpObserver{
                override fun onCreateSuccess(p0: SessionDescription?) {

                }

                override fun onSetSuccess() {

                }

                override fun onCreateFailure(p0: String?) {

                }

                override fun onSetFailure(p0: String?) {
                 
                }

            },session )
    }

    fun answer(target: String?) {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        peerConnection?.createAnswer(object :SdpObserver{
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver{
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onSetSuccess() {
                        val answer = hashMapOf(
                            "sdp" to desc?.description,
                            "type" to desc?.type
                        )
                        socketRepository.sendMessageToSocket(
                            MessageModel(
                                "create_answer", username, target, answer
                            )
                        )
                    }

                    override fun onCreateFailure(p0: String?) {
                        TODO("Not yet implemented")
                    }

                    override fun onSetFailure(p0: String?) {
                        TODO("Not yet implemented")
                    }

                },desc)
            }

            override fun onSetSuccess() {
                TODO("Not yet implemented")
            }

            override fun onCreateFailure(p0: String?) {
                TODO("Not yet implemented")
            }

            override fun onSetFailure(p0: String?) {
                TODO("Not yet implemented")
            }

        },constraints)

    }

    fun addIceCandidate(p0: IceCandidate?) {
        peerConnection?.addIceCandidate(p0)

    }


}
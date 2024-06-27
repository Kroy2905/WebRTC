package com.kroy.webrtc

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kroy.webrtc.databinding.ActivityCallBinding
import com.kroy.webrtc.models.MessageModel
import com.kroy.webrtc.socket.RTCClient
import com.kroy.webrtc.socket.SocketRepository
import com.kroy.webrtc.utils.NewMessageInterface
import com.kroy.webrtc.utils.PeerConnectionObserver
import org.webrtc.IceCandidate
import org.webrtc.MediaStream

class CallActivity : AppCompatActivity(),NewMessageInterface{
    lateinit var  binding : ActivityCallBinding
    private var  username:String?=null
    private  var socketRepository:SocketRepository?=null
    private  var rtcClient:RTCClient?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()

    }
    private  fun init(){
        username = intent.getStringExtra("username")
        socketRepository = SocketRepository(this)
        socketRepository!!.initSocket(username!!)
        rtcClient = RTCClient(application,username!!,socketRepository!!,object :
            PeerConnectionObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
            }

        })
        rtcClient?.initializeSurfaceView(binding.localView)
        rtcClient?.startLocalVideo(binding.localView)


    }

    override fun onNewMessage(message: MessageModel) {
        TODO("Not yet implemented")
    }
}
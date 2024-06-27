package com.kroy.webrtc

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import com.kroy.webrtc.databinding.ActivityCallBinding
import com.kroy.webrtc.models.IceCandidateModel
import com.kroy.webrtc.models.MessageModel
import com.kroy.webrtc.socket.RTCClient
import com.kroy.webrtc.socket.SocketRepository
import com.kroy.webrtc.utils.NewMessageInterface
import com.kroy.webrtc.utils.PeerConnectionObserver
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

class CallActivity : AppCompatActivity(),NewMessageInterface{
    lateinit var  binding : ActivityCallBinding
    private var  username:String?=null
    private  var socketRepository:SocketRepository?=null
    private  var rtcClient:RTCClient?=null
    private var target:String = ""
    private val gson = Gson()
    val TAG = "CallActivity"
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
                rtcClient?.addIceCandidate(p0)
                val candidate = hashMapOf(
                    "sdpMid" to p0?.sdpMid,
                    "sdpMLineIndex" to p0?.sdpMLineIndex,
                    "sdpCandidate" to p0?.sdp
                )

                socketRepository?.sendMessageToSocket(
                    MessageModel("ice_candidate",username,target,candidate)
                )


            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                Log.d(TAG,"video tracks-> ${p0!!.videoTracks}")
                p0.videoTracks?.get(0)?.addSink(binding.remoteView)
            }

        })

        binding.apply {
            binding.callBtn.setOnClickListener {
                socketRepository?.sendMessageToSocket(
                    message =  MessageModel(
                        type = "start_call",
                        name =  username,
                        target = targetUserNameEt.text.toString(),
                        null
                    ))
                target = targetUserNameEt.text.toString()
            }
        }


//        rtcClient?.initializeSurfaceView(binding.localView)
//        rtcClient?.startLocalVideo(binding.localView)


    }

    override fun onNewMessage(message: MessageModel) {
       Log.d(TAG,"onMessageReceived $message")
        when(message.type ){
            "call_response"->{
                if (message.data == "user is not ready for call"){
                    //user is not reachable
                    runOnUiThread {
                        Toast.makeText(this,"user is not reachable",Toast.LENGTH_LONG).show()

                    }
                }else{
                    //we are ready for call, we started a call
                    runOnUiThread {
                        setWhoToCallLayoutGone()
                        setCallLayoutVisible()
                        binding.apply {
                            rtcClient?.initializeSurfaceView(localView)
                            rtcClient?.initializeSurfaceView(remoteView)
                            rtcClient?.startLocalVideo(localView)
                            rtcClient?.call(targetUserNameEt.text.toString())
                        }
                    }
                }
            }
            "answer_received" ->{

                val session = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
                )
                rtcClient?.onRemoteSesionReceived(session)
//                runOnUiThread {
//                    binding.remoteViewLoading.visibility = View.GONE
//                }
            }
            "offer_received"->{
                runOnUiThread {
                    setIncomingCallLayoutVisible()
                    binding.incomingNameTV.text = "${message.name} is calling you "
                    binding.acceptButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                        setCallLayoutVisible()
                        setWhoToCallLayoutGone()

                        binding.apply {
                            rtcClient?.initializeSurfaceView(localView)
                            rtcClient?.initializeSurfaceView(remoteView)
                            rtcClient?.startLocalVideo(localView)
                        }
                        val session = SessionDescription(
                            SessionDescription.Type.OFFER,
                            message.data.toString()
                        )
                        rtcClient?.onRemoteSesionReceived(session)
                        rtcClient?.answer(message.name)
                        target = message.name!!
                    }
                    binding.rejectButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                    }
                    runOnUiThread {
                        binding.remoteViewLoading.visibility = View.GONE
                    }
                }

            }

            "ice_candidate"->{
                try {
                    val receivingCandidate = gson.fromJson(gson.toJson(message.data),
                        IceCandidateModel::class.java)
                    rtcClient?.addIceCandidate(IceCandidate(receivingCandidate.sdpMid,
                        Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),receivingCandidate.sdpCandidate))
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }

        }
    }

    private fun setIncomingCallLayoutGone(){
        binding.incomingCallLayout.visibility = View.GONE
    }
    private fun setIncomingCallLayoutVisible() {
        binding.incomingCallLayout.visibility = View.VISIBLE
    }

    private fun setCallLayoutGone() {
        binding.callLayout.visibility = View.GONE
    }

    private fun setCallLayoutVisible() {
        binding.callLayout.visibility = View.VISIBLE
    }

    private fun setWhoToCallLayoutGone() {
        binding.whoToCallLayout.visibility = View.GONE
    }

    private fun setWhoToCallLayoutVisible() {
        binding.whoToCallLayout.visibility = View.VISIBLE
    }
}
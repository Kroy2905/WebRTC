package com.kroy.webrtc.socket

import android.util.Log
import com.kroy.webrtc.models.MessageModel
import com.google.gson.Gson
import com.kroy.webrtc.utils.NewMessageInterface
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import kotlin.Exception

class SocketRepository(private val messageInterface: NewMessageInterface) {
    private  var webSocket:WebSocketClient?=null
    private  var userName:String? = null
    val TAG  ="SocketRepository->"
    private val gson = Gson()
    fun initSocket(username:String){
        userName = username
        //if you are using android emulator your local websocket address is going to be "ws://10.0.2.2:3000"
        //if you are using your phone as emulator your local address, use cmd and then write ipconfig
        // and get your ethernet ipv4 , mine is : "ws://192.168.1.3:3000"
        //but if your websocket is deployed you add your websocket address here
       // webSocket = object :WebSocketClient(URI("ws://192.168.0.107:3000")){
        webSocket = object :WebSocketClient(URI("ws://192.168.1.199:3000")){
            override fun onOpen(handshakedata: ServerHandshake?) {
                    sendMessageToSocket(
                        MessageModel(
                            type = "store_user",
                            name = userName,
                            target = null,
                            data = null
                        )
                    )
            }

            override fun onMessage(message: String?) {
                try {
                    messageInterface.onNewMessage(gson.fromJson(message,MessageModel::class.java))

                }catch (e:Exception){
                    e.printStackTrace()
                }

            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
               Log.d(TAG,"$reason")
            }

            override fun onError(ex: Exception?) {
              Log.d(TAG,"$ex")
            }

        }
        webSocket!!.connect()

    }
    fun sendMessageToSocket(message: MessageModel){
        try{
            Log.d(TAG, "sendMessageToSocket: $message")
            webSocket?.send(Gson().toJson(message))
        }catch (e:Exception){
            Log.d(TAG, "sendMessageToSocket: $e")
            e.printStackTrace()
        }
    }
}
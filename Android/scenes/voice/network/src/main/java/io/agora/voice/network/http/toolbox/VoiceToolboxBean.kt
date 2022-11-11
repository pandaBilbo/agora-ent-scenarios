package io.agora.voice.network.http.toolbox

/**
 * @author create by zhangwei03
 */
open class VoiceToolboxBaseResponse<out T> {
    val tip: String = "";
    val code: Int = 0;
    val msg: String = "";
    val data: T? = null
}

data class VRGenerateTokenResponse(
    val token: String,
) : VoiceToolboxBaseResponse<VRGenerateTokenResponse>()

data class VRCreateRoomResponse(
    val appId: String,
    val chatId: String, // 聊天室ID, 这里返回环信的聊天室ID
    val token: String, // 环信登录Token, 频道名使用聊天室I
    val userName: String,
    val uid: String, // 用户ID, 这里返回环信的用户uuid
) : VoiceToolboxBaseResponse<VRCreateRoomResponse>()

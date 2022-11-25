package io.agora.voice.buddy

import android.app.Application

/**
 * @author create by zhangwei03
 */
interface IVoiceBuddy {

    /** app */
    fun application(): Application

    /** api url */
    fun toolboxServiceUrl(): String

    /** user avatar */
    fun headUrl(): String

    /** user nickname */
    fun nickName(): String

    /** user id */
    fun userId(): String

    /** user token */
    fun userToken(): String

    /** rtc user id */
    fun rtcUid(): Int

    /** rtc app id*/
    fun rtcAppId(): String

    /** rtc app certificate*/
    fun rtcAppCert(): String

    /** rtc channel token */
    fun rtcToken(): String

    /** im app key */
    fun chatAppKey(): String

    /** im user login token */
    fun chatToken(): String

    /** im user id */
    fun chatUid(): String

    fun chatUserUuid(): String

    fun setupRtcToken(rtcToken: String)

    fun setupChatConfig(chatUid: String, chatToken: String, chatUuid: String)
}
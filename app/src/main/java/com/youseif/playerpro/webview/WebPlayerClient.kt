package com.youseif.playerpro.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.youseif.playerpro.data.model.PlayerError

class WebPlayerClient(
    private val onPageStarted: (String?) -> Unit = {},
    private val onPageFinished: (String?) -> Unit = {},
    private val onError: (PlayerError) -> Unit = {},
    private val onHttpError: (Int, String?) -> Unit = { _, _ -> },
    private val extraHeaders: Map<String, String> = emptyMap(),
    private val defaultReferer: String = ""
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished(url)
        view?.evaluateJavascript(VIDEO_DETECT_JS, null)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val desc = error?.description?.toString() ?: "Unknown error"
            onError(PlayerError.NetworkError(desc))
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame == true) {
            val code = errorResponse?.statusCode ?: 0
            onHttpError(code, errorResponse?.reasonPhrase)
            if (code >= 400) {
                onError(PlayerError.HttpError(code, errorResponse?.reasonPhrase ?: "HTTP $code"))
            }
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.cancel()
        onError(PlayerError.SslError(error?.toString() ?: "SSL Error"))
    }

    companion object {
        const val VIDEO_DETECT_JS = """
            (function() {
                function findVideo() {
                    var v = document.querySelector('video');
                    if (v) return v;
                    var iframes = document.querySelectorAll('iframe');
                    for (var i = 0; i < iframes.length; i++) {
                        try {
                            var doc = iframes[i].contentDocument || (iframes[i].contentWindow && iframes[i].contentWindow.document);
                            if (doc) {
                                v = doc.querySelector('video');
                                if (v) return v;
                            }
                        } catch (e) {}
                    }
                    return null;
                }
                window.__youseifPlayerBridge = {
                    findVideo: findVideo,
                    getState: function() {
                        var v = findVideo();
                        if (!v) return JSON.stringify({found:false});
                        return JSON.stringify({
                            found: true,
                            paused: !!v.paused,
                            ended: !!v.ended,
                            currentTime: v.currentTime || 0,
                            duration: (isFinite(v.duration) ? v.duration : 0),
                            volume: v.volume,
                            muted: !!v.muted,
                            playbackRate: v.playbackRate || 1,
                            videoWidth: v.videoWidth || 0,
                            videoHeight: v.videoHeight || 0,
                            readyState: v.readyState || 0,
                            networkState: v.networkState || 0,
                            src: v.currentSrc || v.src || ''
                        });
                    },
                    play: function() {
                        var v = findVideo();
                        if (v) { var p = v.play(); if (p && p.catch) p.catch(function(){}); return true; }
                        return false;
                    },
                    pause: function() {
                        var v = findVideo();
                        if (v) { v.pause(); return true; }
                        return false;
                    },
                    seek: function(seconds) {
                        var v = findVideo();
                        if (v && isFinite(v.currentTime)) {
                            var d = isFinite(v.duration) ? v.duration : Number.MAX_VALUE;
                            v.currentTime = Math.max(0, Math.min(d, (v.currentTime || 0) + seconds));
                            return true;
                        }
                        return false;
                    },
                    setCurrentTime: function(t) {
                        var v = findVideo();
                        if (v && isFinite(t)) {
                            var d = isFinite(v.duration) ? v.duration : Number.MAX_VALUE;
                            v.currentTime = Math.max(0, Math.min(d, t));
                            return true;
                        }
                        return false;
                    },
                    setVolume: function(vol) {
                        var v = findVideo();
                        if (v) { v.volume = Math.max(0, Math.min(1, vol)); return true; }
                        return false;
                    },
                    setMuted: function(m) {
                        var v = findVideo();
                        if (v) { v.muted = !!m; return true; }
                        return false;
                    },
                    setPlaybackRate: function(r) {
                        var v = findVideo();
                        if (v) { v.playbackRate = r; return true; }
                        return false;
                    },
                    requestFullscreen: function() {
                        var v = findVideo();
                        if (!v) return false;
                        var req = v.requestFullscreen || v.webkitRequestFullscreen || v.mozRequestFullScreen;
                        if (req) { try { req.call(v); return true; } catch(e) {} }
                        return false;
                    },
                    hideVideoVisual: function(hide) {
                        var v = findVideo();
                        if (!v) return false;
                        if (hide) {
                            v.setAttribute('data-youseif-ao', '1');
                            v.style.setProperty('opacity', '0', 'important');
                            v.style.setProperty('width', '1px', 'important');
                            v.style.setProperty('height', '1px', 'important');
                            v.style.setProperty('position', 'fixed', 'important');
                            v.style.setProperty('left', '-9999px', 'important');
                        } else if (v.getAttribute('data-youseif-ao') === '1') {
                            v.removeAttribute('data-youseif-ao');
                            v.style.removeProperty('opacity');
                            v.style.removeProperty('width');
                            v.style.removeProperty('height');
                            v.style.removeProperty('position');
                            v.style.removeProperty('left');
                        }
                        return true;
                    }
                };
            })();
        """
    }
}

package com.example.webviewpopupplugin;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WebViewPopUp extends GodotPlugin {

    Activity activity;
    WebViewPopupDialog webViewPopupDialog;

    public WebViewPopUp(Godot godot) {
        super(godot);
        activity = godot.getActivity();
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "WebViewPopUp";
    }

    @SuppressWarnings("deprecation")
    @NonNull
    @Override
    public List<String> getPluginMethods() {
        return Arrays.asList("open_url", "close_dialog");
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new HashSet<SignalInfo>();
        signals.add(new SignalInfo("on_dialog_open"));
        signals.add(new SignalInfo("on_dialog_dismiss"));
        signals.add(new SignalInfo("on_error", int.class, String.class));
        return signals;
    }

    public void open_url(String url){
        if (webViewPopupDialog != null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webViewPopupDialog = new WebViewPopupDialog(activity, url, onDialogState);
                webViewPopupDialog.show();
            }
        });
    }

    public void close_dialog(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (webViewPopupDialog != null){
                    webViewPopupDialog.close();
                    webViewPopupDialog = null;
                }
            }
        });
    }

    private OnDialogState onDialogState = new OnDialogState() {
        @Override
        public void dialogOpen() {
            emitSignal("on_dialog_open");
        }

        @Override
        public void dialogDismiss() {
            webViewPopupDialog = null;
            emitSignal("on_dialog_dismiss");
        }

        @Override
        public void webViewErrorDismiss(int errorCode, String description) {
            emitSignal("on_error", errorCode, description);
        }
    };

    private static class WebViewPopupDialog {
        AlertDialog dialog;
        Activity activity;
        String url;
        OnDialogState onDialogState;

        public WebViewPopupDialog(Activity activity, String url, OnDialogState onDialogState) {
            this.activity = activity;
            this.url = url;
            this.onDialogState = onDialogState;
            this.initDialog();
        }

        @SuppressLint("SetJavaScriptEnabled")
        private void initDialog(){
           AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);

            LinearLayout wrapper = new LinearLayout(activity);
            WebView webView = new WebView(activity);
            EditText keyboardHack = new EditText(activity);

            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.addView(webView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            wrapper.addView(keyboardHack, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            alertDialog.setView(wrapper);

            webView.loadUrl(url);

            keyboardHack.setVisibility(View.GONE);
            webView.setFocusable(true);
            webView.setFocusableInTouchMode(true);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }

                @SuppressWarnings("deprecation")
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    onDialogState.webViewErrorDismiss(errorCode, description);
                }

                @TargetApi(android.os.Build.VERSION_CODES.M)
                @Override
                public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError error) {
                    // Redirect to deprecated method, so you can use it in all SDK versions
                    onReceivedError(view, error.getErrorCode(),error.getDescription().toString(), req.getUrl().toString());
                }
            });

            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);

            // Set User Agent
            webSettings.setUserAgentString("Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543a Safari/419.3");

            // Enable Cookies
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

            // Handle Popups
            webView.setWebChromeClient(new CustomChromeClient());
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
            webSettings.setSupportMultipleWindows(true);

            // WebView Tweaks
            webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            webSettings.setDomStorageEnabled(true);
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
            webSettings.setUseWideViewPort(true);
            webSettings.setSaveFormData(true);
            webSettings.setEnableSmoothTransition(true);
            webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

            alertDialog.setCancelable(false);
            alertDialog.setOnKeyListener(new Dialog.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        onDialogState.dialogDismiss();
                        dialogInterface.dismiss();
                    }
                    return true;
                }
            });
            dialog = alertDialog.create();
        }

        void show(){
            dialog.show();
            onDialogState.dialogOpen();
        }

        void close(){
            dialog.dismiss();
        }
    }

    private static class  CustomChromeClient extends WebChromeClient {}

    private interface OnDialogState {
        void dialogOpen();
        void dialogDismiss();
        void webViewErrorDismiss(int errorCode, String description);
    }
}

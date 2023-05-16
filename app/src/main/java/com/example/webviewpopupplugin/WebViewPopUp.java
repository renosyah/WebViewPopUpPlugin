package com.example.webviewpopupplugin;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.ArraySet;
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

import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class WebViewPopUp extends GodotPlugin {

    Activity activity;
    WebViewPopupDialog webViewPopupDialog;
    ArrayList<String> errorMessages = new ArrayList<String>();

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
        return Arrays.asList("open_url", "close_dialog", "get_error_messages");
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        // cannot emit any signal with param from here
        // error SETGEV bullcrab
        // so instead we store error message
        // that later be check on dialog dismiss
        Set<SignalInfo> signals = new ArraySet<>();
        signals.add(new SignalInfo("on_dialog_dismiss"));
        signals.add(new SignalInfo("on_error"));
        return signals;
    }

    public void open_url(String url){
        if (webViewPopupDialog != null){
            return;
        }

        errorMessages.clear();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webViewPopupDialog = new WebViewPopupDialog(activity, url, onDialogState, errorMessages);
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

    public String[] get_error_messages(){
        String[] stockArr = new String[errorMessages.size()];
        stockArr = errorMessages.toArray(stockArr);
        return stockArr;
    }

    private OnDialogState onDialogState = new OnDialogState() {
        @Override
        public void dialogDismiss() {
            webViewPopupDialog = null;
            emitSignal("on_dialog_dismiss");
        }

        @Override
        public void webViewError() {
            webViewPopupDialog = null;
            emitSignal("on_error");
        }
    };

    private static class WebViewPopupDialog {
        AlertDialog dialog;
        Activity activity;
        String url;
        OnDialogState onDialogState;

        ArrayList<String> errorMessages;
        Boolean finish = false;

        private boolean isLocalIp(String checkUrl){
            try {
                Uri uri = Uri.parse(checkUrl);
                String domain = uri.getHost();
                return domain.contains("127.0.0.1");

            } catch (Exception ignored){  }

            try {
                InetAddress domain = InetAddress.getByName(new URL(checkUrl).getHost());
                return domain.isAnyLocalAddress();

            } catch (Exception ignored){  }

            return false;

        }

        public WebViewPopupDialog(Activity activity, String url, OnDialogState onDialogState, ArrayList<String> errorMessages) {
            this.activity = activity;
            this.url = url;
            this.onDialogState = onDialogState;
            this.errorMessages = errorMessages;
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

            keyboardHack.setVisibility(View.GONE);
            webView.setFocusable(true);
            webView.setFocusableInTouchMode(true);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String loadingUrl) {
                    view.loadUrl(loadingUrl);
                    return true;
                }

                @Override
                public void onPageFinished(WebView view, String openedUrl) {
                    super.onPageFinished(view, openedUrl);
                    if (!errorMessages.isEmpty() && !isLocalIp(openedUrl)){
                        dialog.dismiss();
                    }
                }

                @SuppressWarnings("deprecation")
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    if (description != null && !isLocalIp(failingUrl)){
                        errorMessages.add(description);
                    }
                }

                @TargetApi(android.os.Build.VERSION_CODES.M)
                @Override
                public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError error) {
                    // Redirect to deprecated method, so you can use it in all SDK versions
                    onReceivedError(view, error.getErrorCode(), error.getDescription().toString(), req.getUrl().getHost());
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
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            webSettings.setDomStorageEnabled(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setSaveFormData(true);
            webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

            alertDialog.setCancelable(false);
            alertDialog.setOnKeyListener(new Dialog.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        dialogInterface.dismiss();
                    }
                    return true;
                }
            });
            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    // dismiss dialog and return errors
                    if (!errorMessages.isEmpty()){
                        onDialogState.webViewError();
                        return;
                    }

                    // dismiss dialog normal
                    if (finish){
                        return;
                    }

                    // dismiss dialog on purpose by user
                    onDialogState.dialogDismiss();
                }
            });

            dialog = alertDialog.create();
            webView.loadUrl(url);
        }

        void show(){
            dialog.show();
        }

        void close(){
            finish = true;
            dialog.dismiss();
        }
    }

    private static class  CustomChromeClient extends WebChromeClient {}

    private interface OnDialogState {
        void dialogDismiss();
        void webViewError();
    }
}

package com.example.webviewpopupplugin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
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

    public void showSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            boolean isShowing = imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            if (!isShowing){
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        }
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
        return Arrays.asList("OpenUrl", "ClosePopUp");
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new HashSet<SignalInfo>();
        signals.add(new SignalInfo("on_error", String.class));
        return signals;
    }

    public void OpenUrl(String url){
        if (webViewPopupDialog != null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webViewPopupDialog = new WebViewPopupDialog(activity, url);
                webViewPopupDialog.show();
            }
        });
    }

    public void ClosePopUp(){
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

    @Override
    public boolean onMainBackPressed() {
        if (webViewPopupDialog != null) {
            ClosePopUp();
            return false;
        }
        return super.onMainBackPressed();
    }

    private static class WebViewPopupDialog {
        AlertDialog dialog;

        @SuppressLint("SetJavaScriptEnabled")
        public WebViewPopupDialog(Activity activity, String url) {
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

            dialog = alertDialog.create();
        }

        void show(){
            dialog.show();
        }

        void close(){
            dialog.dismiss();
        }
    }

    private static class  CustomChromeClient extends WebChromeClient {}
}

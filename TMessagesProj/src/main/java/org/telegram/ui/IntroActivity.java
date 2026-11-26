/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.SimpleThemeDescription;
import org.telegram.ui.Components.voip.CellFlickerDrawable;

import java.util.ArrayList;

public class IntroActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private final int currentAccount = UserConfig.selectedAccount;

    private TextView switchLanguageTextView;
    private GradientDrawable startMessagingButtonBackground;
    private TextView startMessagingButton;
    private TextView titleTextView;
    private TextView messageTextView;
    private TextView termsTextView;
    private ImageView kuarkLogoView;
    private LinearLayout centerLayout;
    private LinearLayout bottomLayout;
    private FrameLayout frameContainerView;

    private RLottieDrawable darkThemeDrawable;

    private boolean startPressed = false;
    private LocaleController.LocaleInfo localeInfo;
    private boolean destroyed;
    private boolean isOnLogout;

    @Override
    public boolean onFragmentCreate() {
        MessagesController.getGlobalMainSettings().edit().putLong("intro_crashed_time", System.currentTimeMillis()).apply();
        return true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setAddToContainer(false);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        RLottieImageView themeIconView = new RLottieImageView(context);
        FrameLayout themeFrameLayout = new FrameLayout(context);
        themeFrameLayout.addView(themeIconView, LayoutHelper.createFrame(28, 28, Gravity.CENTER));

        int themeMargin = 4;
        frameContainerView = new FrameLayout(context) {
            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);

                int width = right - left;
                int height = bottom - top;

                int topSafe = (AndroidUtilities.isTablet() ? 0 : AndroidUtilities.statusBarHeight) + dp(48);
                int bottomSafe = Math.max(dp(20), AndroidUtilities.navigationBarHeight + dp(12));

                int bottomH = bottomLayout.getMeasuredHeight();
                int bottomY = Math.max(topSafe + centerLayout.getMeasuredHeight() + dp(16), height - bottomSafe - bottomH);
                int bottomX = (width - bottomLayout.getMeasuredWidth()) / 2;
                bottomLayout.layout(bottomX, bottomY, bottomX + bottomLayout.getMeasuredWidth(), bottomY + bottomH);

                int availableForCenter = bottomY - topSafe;
                int centerH = centerLayout.getMeasuredHeight();
                int centerY = topSafe + Math.max(0, (availableForCenter - centerH) / 2);
                int centerX = (width - centerLayout.getMeasuredWidth()) / 2;
                centerLayout.layout(centerX, centerY, centerX + centerLayout.getMeasuredWidth(), centerY + centerH);

                MarginLayoutParams marginLayoutParams = (MarginLayoutParams) themeFrameLayout.getLayoutParams();
                int newTopMargin = dp(themeMargin) + (AndroidUtilities.isTablet() ? 0 : AndroidUtilities.statusBarHeight);
                if (marginLayoutParams.topMargin != newTopMargin) {
                    marginLayoutParams.topMargin = newTopMargin;
                    themeFrameLayout.requestLayout();
                }
            }
        };
        scrollView.addView(frameContainerView, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP));

        darkThemeDrawable = new RLottieDrawable(R.raw.sun, String.valueOf(R.raw.sun), dp(28), dp(28), true, null);
        darkThemeDrawable.setPlayInDirectionOfCustomEndFrame(true);
        darkThemeDrawable.beginApplyLayerColors();
        darkThemeDrawable.commitApplyLayerColors();

        darkThemeDrawable.setCustomEndFrame(Theme.getCurrentTheme().isDark() ? darkThemeDrawable.getFramesCount() - 1 : 0);
        darkThemeDrawable.setCurrentFrame(Theme.getCurrentTheme().isDark() ? darkThemeDrawable.getFramesCount() - 1 : 0, false);
        themeIconView.setContentDescription(LocaleController.getString(Theme.getCurrentTheme().isDark() ? R.string.AccDescrSwitchToDayTheme : R.string.AccDescrSwitchToNightTheme));

        themeIconView.setAnimation(darkThemeDrawable);
        themeFrameLayout.setOnClickListener(v -> {
            if (DialogsActivity.switchingTheme) return;
            DialogsActivity.switchingTheme = true;

            String dayThemeName = "Blue";
            String nightThemeName = "Night";

            Theme.ThemeInfo themeInfo;
            boolean toDark;
            if (toDark = !Theme.isCurrentThemeDark()) {
                themeInfo = Theme.getTheme(nightThemeName);
            } else {
                themeInfo = Theme.getTheme(dayThemeName);
            }

            Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_NONE;
            Theme.saveAutoNightThemeConfig();
            Theme.cancelAutoNightThemeCallbacks();

            darkThemeDrawable.setCustomEndFrame(toDark ? darkThemeDrawable.getFramesCount() - 1 : 0);
            themeIconView.playAnimation();

            int[] pos = new int[2];
            themeIconView.getLocationInWindow(pos);
            pos[0] += themeIconView.getMeasuredWidth() / 2;
            pos[1] += themeIconView.getMeasuredHeight() / 2;
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme, themeInfo, false, pos, -1, toDark, themeIconView);
            themeIconView.setContentDescription(LocaleController.getString(toDark ? R.string.AccDescrSwitchToDayTheme : R.string.AccDescrSwitchToNightTheme));
        });

        centerLayout = new LinearLayout(context);
        centerLayout.setOrientation(LinearLayout.VERTICAL);
        centerLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        frameContainerView.addView(centerLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 24, 0, 24, 0));

        kuarkLogoView = new ImageView(context);
        kuarkLogoView.setImageResource(R.drawable.kuark_logo_intro);
        kuarkLogoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        centerLayout.addView(kuarkLogoView, LayoutHelper.createLinear(160, 160, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 20));

        titleTextView = new TextView(context);
        titleTextView.setText(LocaleController.getString(R.string.AppName));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26);
        titleTextView.setTypeface(AndroidUtilities.bold());
        titleTextView.setGravity(Gravity.CENTER);
        centerLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 10));

        messageTextView = new TextView(context);
        messageTextView.setText(AndroidUtilities.replaceTags(LocaleController.getString(R.string.Page1Message)));
        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        messageTextView.setTypeface(AndroidUtilities.getRegularTypeface());
        messageTextView.setLineSpacing(dpf2(2.33f), 1f);
        messageTextView.setGravity(Gravity.CENTER);
        centerLayout.addView(messageTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        bottomLayout = new LinearLayout(context);
        bottomLayout.setOrientation(LinearLayout.VERTICAL);
        bottomLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        frameContainerView.addView(bottomLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 16, 0, 16, 20));

        switchLanguageTextView = new TextView(context);
        switchLanguageTextView.setGravity(Gravity.CENTER);
        switchLanguageTextView.setTypeface(AndroidUtilities.getRegularTypeface());
        switchLanguageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        switchLanguageTextView.setVisibility(View.GONE);
        bottomLayout.addView(switchLanguageTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 14));
        switchLanguageTextView.setOnClickListener(v -> {
            if (startPressed || localeInfo == null) {
                return;
            }
            startPressed = true;

            AlertDialog loaderDialog = new AlertDialog(v.getContext(), AlertDialog.ALERT_TYPE_SPINNER);
            loaderDialog.setCanCancel(false);
            loaderDialog.showDelayed(1000);

            NotificationCenter.getGlobalInstance().addObserver(new NotificationCenter.NotificationCenterDelegate() {
                @Override
                public void didReceivedNotification(int id, int account, Object... args) {
                    if (id == NotificationCenter.reloadInterface) {
                        loaderDialog.dismiss();

                        NotificationCenter.getGlobalInstance().removeObserver(this, id);
                        AndroidUtilities.runOnUIThread(() -> {
                            presentFragment(new LoginActivity().setIntroView(frameContainerView, startMessagingButton), true);
                            destroyed = true;
                        }, 100);
                    }
                }
            }, NotificationCenter.reloadInterface);
            LocaleController.getInstance().applyLanguage(localeInfo, true, false, currentAccount);
        });

        startMessagingButtonBackground = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, null);
        startMessagingButton = new TextView(context) {
            private final CellFlickerDrawable cellFlickerDrawable = new CellFlickerDrawable(); {
                cellFlickerDrawable.drawFrame = false;
                cellFlickerDrawable.repeatProgress = 2f;
            }

            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                startMessagingButtonBackground.setBounds(0, 0, w, h);
                startMessagingButtonBackground.setCornerRadius(Math.min(w, h) / 2f);
                cellFlickerDrawable.setParentWidth(w);
            }

            @Override
            public void draw(@NonNull Canvas canvas) {
                startMessagingButtonBackground.draw(canvas);
                super.draw(canvas);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                AndroidUtilities.rectTmp.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                cellFlickerDrawable.draw(canvas, AndroidUtilities.rectTmp, getMeasuredHeight() / 2f, null);
                invalidate();
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int size = MeasureSpec.getSize(widthMeasureSpec);
                if (size > dp(260)) {
                    super.onMeasure(MeasureSpec.makeMeasureSpec(dp(320), MeasureSpec.EXACTLY), heightMeasureSpec);
                } else {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }
            }
        };
        ScaleStateListAnimator.apply(startMessagingButton, .02f, 1.2f);
        startMessagingButton.setText(LocaleController.getString(R.string.StartMessaging));
        startMessagingButton.setGravity(Gravity.CENTER);
        startMessagingButton.setTypeface(AndroidUtilities.bold());
        startMessagingButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        startMessagingButton.setPadding(dp(34), 0, dp(34), 0);
        bottomLayout.addView(startMessagingButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, Gravity.CENTER_HORIZONTAL));
        startMessagingButton.setOnClickListener(view -> {
            if (startPressed) {
                return;
            }
            startPressed = true;

            presentFragment(new LoginActivity().setIntroView(frameContainerView, startMessagingButton), true);
            destroyed = true;
        });

        termsTextView = new TextView(context);
        termsTextView.setGravity(Gravity.CENTER);
        termsTextView.setTypeface(AndroidUtilities.getRegularTypeface());
        termsTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        termsTextView.setLineSpacing(dpf2(2f), 1f);
        termsTextView.setMovementMethod(new AndroidUtilities.LinkMovementMethodMy());
        termsTextView.setHighlightColor(Color.TRANSPARENT);
        termsTextView.setPadding(dp(8), 0, dp(8), 0);
        bottomLayout.addView(termsTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 0));

        frameContainerView.addView(themeFrameLayout, LayoutHelper.createFrame(64, 64, Gravity.TOP | Gravity.RIGHT, 0, themeMargin, themeMargin, 0));

        fragmentView = scrollView;

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.suggestedLangpack);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.configLoaded);
        ConnectionsManager.getInstance(currentAccount).updateDcSettings();
        LocaleController.getInstance().loadRemoteLanguages(currentAccount);
        checkContinueText();

        updateColors(false);

        return fragmentView;
    }

    private void updateTermsText() {
        if (termsTextView == null) {
            return;
        }
        String lang = LocaleController.getInstance().getCurrentLocaleInfo().shortName;
        boolean isPersian = LocaleController.isRTL || (lang != null && lang.startsWith("fa"));
        String fullText;
        String linkPart;
        if (isPersian) {
            fullText = "ورود به برنامه به معنای پذیرش قوانین و مقررات کوآرک است";
            linkPart = "قوانین و مقررات";
        } else {
            fullText = "By signing in, you agree to Kuark's Terms of Service";
            linkPart = "Terms of Service";
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(fullText);
        int start = fullText.indexOf(linkPart);
        if (start >= 0) {
            builder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://k.zetser.ir/rules/fa"));
                        Activity activity = getParentActivity();
                        if (activity != null) {
                            activity.startActivity(intent);
                        } else {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            widget.getContext().startActivity(intent);
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
                    ds.setUnderlineText(false);
                }
            }, start, start + linkPart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        termsTextView.setText(builder);
        termsTextView.setTextColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 175));
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onResume() {
        super.onResume();
        AndroidUtilities.lockOrientation(getParentActivity(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onPause() {
        super.onPause();
        AndroidUtilities.unlockOrientation(getParentActivity());
    }

    @Override
    public boolean hasForceLightStatusBar() {
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        destroyed = true;
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.suggestedLangpack);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.configLoaded);
        MessagesController.getGlobalMainSettings().edit().putLong("intro_crashed_time", 0).apply();
    }

    private void checkContinueText() {
        LocaleController.LocaleInfo englishInfo = null;
        LocaleController.LocaleInfo systemInfo = null;
        LocaleController.LocaleInfo currentLocaleInfo = LocaleController.getInstance().getCurrentLocaleInfo();
        String systemLang = MessagesController.getInstance(currentAccount).suggestedLangCode;
        if (systemLang == null || systemLang.equals("en") && LocaleController.getInstance().getSystemDefaultLocale().getLanguage() != null && !LocaleController.getInstance().getSystemDefaultLocale().getLanguage().equals("en")) {
            systemLang = LocaleController.getInstance().getSystemDefaultLocale().getLanguage();
            if (systemLang == null) {
                systemLang = "en";
            }
        }

        String arg = systemLang.contains("-") ? systemLang.split("-")[0] : systemLang;
        String alias = LocaleController.getLocaleAlias(arg);
        for (int a = 0; a < LocaleController.getInstance().languages.size(); a++) {
            LocaleController.LocaleInfo info = LocaleController.getInstance().languages.get(a);
            if (info.shortName.equals("en")) {
                englishInfo = info;
            }
            if (info.shortName.replace("_", "-").equals(systemLang) || info.shortName.equals(arg) || info.shortName.equals(alias)) {
                systemInfo = info;
            }
            if (englishInfo != null && systemInfo != null) {
                break;
            }
        }
        if (englishInfo == null || systemInfo == null || englishInfo == systemInfo) {
            return;
        }
        TLRPC.TL_langpack_getStrings req = new TLRPC.TL_langpack_getStrings();
        if (systemInfo != currentLocaleInfo) {
            req.lang_code = systemInfo.getLangCode();
            localeInfo = systemInfo;
        } else {
            req.lang_code = englishInfo.getLangCode();
            localeInfo = englishInfo;
        }
        req.keys.add("ContinueOnThisLanguage");
        String finalSystemLang = systemLang;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (response instanceof Vector) {
                Vector vector = (Vector) response;
                if (vector.objects.isEmpty()) {
                    return;
                }
                final TLRPC.LangPackString string = (TLRPC.LangPackString) vector.objects.get(0);
                if (string instanceof TLRPC.TL_langPackString) {
                    AndroidUtilities.runOnUIThread(() -> {
                        if (!destroyed && !TextUtils.isEmpty(string.value)) {
                            switchLanguageTextView.setText(string.value);
                            switchLanguageTextView.setVisibility(View.VISIBLE);
                            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                            preferences.edit().putString("language_showed2", finalSystemLang.toLowerCase()).apply();
                        }
                    });
                }
            }
        }, ConnectionsManager.RequestFlagWithoutLogin);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.suggestedLangpack || id == NotificationCenter.configLoaded) {
            checkContinueText();
        }
    }

    public IntroActivity setOnLogout() {
        isOnLogout = true;
        return this;
    }

    @Override
    public AnimatorSet onCustomTransitionAnimation(boolean isOpen, Runnable callback) {
        if (isOnLogout) {
            AnimatorSet set = new AnimatorSet().setDuration(50);
            set.playTogether(ValueAnimator.ofFloat());
            return set;
        }
        return null;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return SimpleThemeDescription.createThemeDescriptions(() -> updateColors(true), Theme.key_windowBackgroundWhite,
                Theme.key_windowBackgroundWhiteBlueText4, Theme.key_chats_actionBackground, Theme.key_chats_actionPressedBackground,
                Theme.key_featuredStickers_buttonText, Theme.key_windowBackgroundWhiteBlackText);
    }

    private void updateColors(boolean fromTheme) {
        startMessagingButtonBackground.setColors(new int[]{getThemedColor(Theme.key_featuredStickers_addButton), getThemedColor(Theme.key_featuredStickers_addButton2)});
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        messageTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        switchLanguageTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        startMessagingButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        startMessagingButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(24), Color.TRANSPARENT, Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        darkThemeDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton), PorterDuff.Mode.SRC_IN));
        updateTermsText();
    }

    @Override
    public boolean isLightStatusBar() {
        int color = Theme.getColor(Theme.key_windowBackgroundWhite, null, true);
        return ColorUtils.calculateLuminance(color) > 0.7f;
    }
}

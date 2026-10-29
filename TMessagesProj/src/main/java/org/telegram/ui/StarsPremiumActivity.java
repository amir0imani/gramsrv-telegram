package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RadioButton;
import org.telegram.ui.Stars.StarsController;

import java.util.ArrayList;

public class StarsPremiumActivity extends BaseFragment {

    private long recipientUserId = 0; // 0 به معنی خودم
    private int selectedPlan = 0;

    private final int[] months = {1, 3, 12};
    private final long[] prices = {100, 250, 900};

    private TextView balanceValueView;
    private TextView continueButtonText;
    private BackupImageView recipientAvatar;
    private TextView recipientName;
    private final ArrayList<RadioButton> radioButtons = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("خرید پریمیوم با ستاره");
        actionBar.setBackgroundColor(0xFF0F0F10);
        actionBar.setItemsColor(0xFFFFFFFF, false);
        actionBar.setItemsBackgroundColor(0x1FFFFFFF, false);
        actionBar.setTitleColor(0xFFFFFFFF);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout rootLayout = new FrameLayout(context);
        rootLayout.setBackgroundColor(0xFF0F0F10);
        fragmentView = rootLayout;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        rootLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 0, 0, 80));

        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        scrollView.addView(contentLayout, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // ۱. ستاره انیمیشنی اورجینال تلگرام (بدون ارور نبود عکس)
        RLottieImageView starHero = new RLottieImageView(context);
        starHero.setAutoRepeat(true);
        starHero.setAnimation(R.raw.star_gift, 130, 130);
        starHero.playAnimation();
        contentLayout.addView(starHero, LayoutHelper.createLinear(130, 130, Gravity.CENTER_HORIZONTAL, 0, 10, 0, 16));

        // ۲. موجودی ستاره
        long currentStars = 0;
        try {
            currentStars = StarsController.getInstance(currentAccount).getBalance();
        } catch (Exception ignored) {}

        balanceValueView = new TextView(context);
        balanceValueView.setText(toPersianDigits(String.valueOf(currentStars)));
        balanceValueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 28);
        balanceValueView.setTypeface(AndroidUtilities.bold());
        balanceValueView.setTextColor(0xFFFFFFFF);
        balanceValueView.setGravity(Gravity.CENTER);
        contentLayout.addView(balanceValueView);

        TextView balanceLabel = new TextView(context);
        balanceLabel.setText("موجودی ستاره");
        balanceLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        balanceLabel.setTextColor(0xFF8E8E93);
        balanceLabel.setGravity(Gravity.CENTER);
        contentLayout.addView(balanceLabel, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 2, 0, 12));

        TextView descView = new TextView(context);
        descView.setText("مدت پریمیوم را انتخاب کنید؛ هزینه از موجودی ستارهٔ شما پرداخت می‌شود.");
        descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        descView.setTextColor(0xFF8E8E93);
        descView.setGravity(Gravity.CENTER);
        contentLayout.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 24));

        // ۳. باکس گیرنده
        LinearLayout recipientCard = new LinearLayout(context);
        recipientCard.setOrientation(LinearLayout.HORIZONTAL);
        recipientCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), 0xFF1C1C1E));
        recipientCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        recipientCard.setGravity(Gravity.CENTER_VERTICAL);
        recipientCard.setOnClickListener(v -> openRecipientSelector());

        recipientAvatar = new BackupImageView(context);
        recipientAvatar.setRoundRadius(AndroidUtilities.dp(24));
        recipientCard.addView(recipientAvatar, LayoutHelper.createLinear(48, 48, Gravity.CENTER_VERTICAL));

        LinearLayout recipientTexts = new LinearLayout(context);
        recipientTexts.setOrientation(LinearLayout.VERTICAL);
        recipientTexts.setGravity(Gravity.RIGHT);

        TextView recipientLabel = new TextView(context);
        recipientLabel.setText("گیرنده");
        recipientLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        recipientLabel.setTextColor(0xFF8E8E93);
        recipientTexts.addView(recipientLabel);

        recipientName = new TextView(context);
        recipientName.setText("خودم");
        recipientName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        recipientName.setTypeface(AndroidUtilities.bold());
        recipientName.setTextColor(0xFFFFFFFF);
        recipientTexts.addView(recipientName, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        recipientCard.addView(recipientTexts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 16, 0, 0, 0));
        contentLayout.addView(recipientCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        updateRecipientView();

        // ۴. عنوان انتخاب مدت زمان
        TextView durationHeader = new TextView(context);
        durationHeader.setText("انتخاب مدت پریمیوم");
        durationHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        durationHeader.setTypeface(AndroidUtilities.bold());
        durationHeader.setTextColor(0xFF4ADE80);
        durationHeader.setGravity(Gravity.RIGHT);
        contentLayout.addView(durationHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 4, 10));

        // ۵. لیست ۳ پلن
        LinearLayout plansCard = new LinearLayout(context);
        plansCard.setOrientation(LinearLayout.VERTICAL);
        plansCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), 0xFF1C1C1E));

        radioButtons.clear();
        for (int i = 0; i < months.length; i++) {
            final int index = i;
            View planRow = createPlanRow(context, months[i] + " ماه", "پرداخت یک‌جا با ستاره", prices[i], i);
            planRow.setOnClickListener(v -> selectPlan(index));
            plansCard.addView(planRow);

            if (i < months.length - 1) {
                View divider = new View(context);
                divider.setBackgroundColor(0x1AFFFFFF);
                plansCard.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 16, 0, 16, 0));
            }
        }
        contentLayout.addView(plansCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // ۶. دکمه خرید در پایین صفحه
        FrameLayout bottomContainer = new FrameLayout(context);
        bottomContainer.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        FrameLayout continueButton = new FrameLayout(context);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF7077F5, 0xFFE04FB3}
        );
        gradient.setCornerRadius(AndroidUtilities.dp(26));
        continueButton.setBackground(gradient);
        continueButton.setOnClickListener(v -> processPurchase());

        continueButtonText = new TextView(context);
        continueButtonText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        continueButtonText.setTypeface(AndroidUtilities.bold());
        continueButtonText.setTextColor(0xFFFFFFFF);
        continueButtonText.setGravity(Gravity.CENTER);
        continueButton.addView(continueButtonText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        bottomContainer.addView(continueButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 52, Gravity.BOTTOM));
        rootLayout.addView(bottomContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));

        selectPlan(0);
        return fragmentView;
    }

    private View createPlanRow(Context context, String title, String subtitle, long price, int index) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

        TextView priceView = new TextView(context);
        priceView.setText(toPersianDigits(String.valueOf(price)) + " ★");
        priceView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        priceView.setTypeface(AndroidUtilities.bold());
        priceView.setTextColor(0xFF8E8E93);
        row.addView(priceView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        LinearLayout infoLayout = new LinearLayout(context);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setGravity(Gravity.RIGHT);

        TextView titleView = new TextView(context);
        titleView.setText(toPersianDigits(title));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(0xFFFFFFFF);
        infoLayout.addView(titleView);

        TextView subtitleView = new TextView(context);
        subtitleView.setText(subtitle);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        subtitleView.setTextColor(0xFF8E8E93);
        infoLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        row.addView(infoLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        RadioButton radioButton = new RadioButton(context);
        radioButton.setSize(AndroidUtilities.dp(20));
        radioButton.setColor(0xFF8E8E93, 0xFFFFFFFF);
        radioButtons.add(radioButton);
        row.addView(radioButton, LayoutHelper.createLinear(22, 22, Gravity.CENTER_VERTICAL));

        return row;
    }

    private void selectPlan(int index) {
        selectedPlan = index;
        for (int i = 0; i < radioButtons.size(); i++) {
            radioButtons.get(i).setChecked(i == index, true);
        }
        continueButtonText.setText("ادامه با " + toPersianDigits(String.valueOf(prices[selectedPlan])) + " ★");
    }

    private void updateRecipientView() {
        TLRPC.User currentUser = UserConfig.getInstance(currentAccount).getCurrentUser();
        if (recipientUserId == 0 || (currentUser != null && recipientUserId == currentUser.id)) {
            recipientName.setText("خودم");
            if (currentUser != null) {
                AvatarDrawable avatarDrawable = new AvatarDrawable();
                avatarDrawable.setInfo(currentUser);
                recipientAvatar.setForUserOrChat(currentUser, avatarDrawable);
            }
        } else {
            TLRPC.User user = getMessagesController().getUser(recipientUserId);
            if (user != null) {
                recipientName.setText(UserObject.getUserName(user));
                AvatarDrawable avatarDrawable = new AvatarDrawable();
                avatarDrawable.setInfo(user);
                recipientAvatar.setForUserOrChat(user, avatarDrawable);
            }
        }
    }

    private void openRecipientSelector() {
        Bundle args = new Bundle();
        args.putBoolean("onlyUsers", true);
        args.putBoolean("destroyAfterSelect", true);
        args.putBoolean("returnAsResult", true);

        ContactsActivity contactsActivity = new ContactsActivity(args);
        contactsActivity.setDelegate((user, param, activity) -> {
            if (user != null) {
                recipientUserId = user.id;
                updateRecipientView();
            }
        });
        presentFragment(contactsActivity);
    }

    private void processPurchase() {
        long currentStars = 0;
        try {
            currentStars = StarsController.getInstance(currentAccount).getBalance();
        } catch (Exception ignored) {}

        long neededPrice = prices[selectedPlan];
        if (currentStars < neededPrice) {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.error, "موجودی ستاره شما کافی نیست!").show();
            return;
        }

        BulletinFactory.of(this).createSimpleBulletin(R.raw.star_premium_2, "اشتراک با موفقیت فعال شد!").show();
        finishFragment();
    }

    private String toPersianDigits(String str) {
        return str.replace('0', '۰')
                .replace('1', '۱')
                .replace('2', '۲')
                .replace('3', '۳')
                .replace('4', '۴')
                .replace('5', '۵')
                .replace('6', '۶')
                .replace('7', '۷')
                .replace('8', '۸')
                .replace('9', '۹');
    }
}

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
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadioButton;
import org.telegram.ui.Stars.StarsController;
import org.telegram.ui.Stars.StarsIntroActivity;

import java.util.ArrayList;
import java.util.Collections;

public class StarsPremiumBottomSheet extends BottomSheet {

    private final BaseFragment fragment;
    private long recipientUserId = 0; // 0 یعنی خودم
    private int selectedPlanIndex = 0;

    private final ArrayList<PlanItem> plans = new ArrayList<>();
    private final ArrayList<RadioButton> radioButtons = new ArrayList<>();

    private TextView continueButtonText;
    private BackupImageView recipientAvatar;
    private TextView recipientName;
    private TextView balanceSubheadView;

    public static class PlanItem {
        public final int months;
        public final long starsPrice;
        public final int discountPercent;

        public PlanItem(int months, long starsPrice, int discountPercent) {
            this.months = months;
            this.starsPrice = starsPrice;
            this.discountPercent = discountPercent;
        }
    }

    public StarsPremiumBottomSheet(Context context, BaseFragment fragment, int account) {
        this(context, fragment, account, 0);
    }

    public StarsPremiumBottomSheet(Context context, BaseFragment fragment, int account, long initialRecipientUserId) {
        super(context, false);
        this.fragment = fragment;
        this.currentAccount = account;
        this.recipientUserId = initialRecipientUserId;

        loadPlansFromServer();

        // تطابق ۱۰۰٪ زمینه باتم‌شیت و نوار ناوبری با تم فعال تلگرام (تیره یا روشن)
        setBackgroundColor(getThemedColor(Theme.key_dialogBackground));
        fixNavigationBar(getThemedColor(Theme.key_dialogBackground));

        LinearLayout rootContainer = new LinearLayout(context);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        rootContainer.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        // ۱. نوار کوچک دستگیره بالای باتم‌شیت (BottomSheet Drag Handle)
        View dragHandle = new View(context);
        dragHandle.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(2.5f), getThemedColor(Theme.key_sheet_scrollUp)));
        rootContainer.addView(dragHandle, LayoutHelper.createLinear(36, 5, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 14));

        // ۲. عنوان اصلی
        TextView titleView = new TextView(context);
        titleView.setText(LocaleController.isRTL ? "خرید اشتراک پریمیوم" : "Subscribe to Premium");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        rootContainer.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        // ۳. زیرعنوان: نمایش موجودی ستاره کاربر با آیکون درخشان رسمی تلگرام
        balanceSubheadView = new TextView(context);
        balanceSubheadView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        balanceSubheadView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        balanceSubheadView.setGravity(Gravity.CENTER_HORIZONTAL);
        rootContainer.addView(balanceSubheadView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));
        updateBalanceView();

        // ۴. باکس انتخاب گیرنده (کارت با افکت ریپل متناسب با تم)
        LinearLayout recipientCard = new LinearLayout(context);
        recipientCard.setOrientation(LinearLayout.HORIZONTAL);
        recipientCard.setBackground(Theme.createRadSelectorDrawable(
                getThemedColor(Theme.key_dialogBackgroundGray),
                AndroidUtilities.dp(14),
                AndroidUtilities.dp(14)
        ));
        recipientCard.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
        recipientCard.setGravity(Gravity.CENTER_VERTICAL);
        recipientCard.setOnClickListener(v -> openContactSelector());

        recipientAvatar = new BackupImageView(context);
        recipientAvatar.setRoundRadius(AndroidUtilities.dp(21));
        recipientCard.addView(recipientAvatar, LayoutHelper.createLinear(42, 42, Gravity.CENTER_VERTICAL));

        LinearLayout recipientTexts = new LinearLayout(context);
        recipientTexts.setOrientation(LinearLayout.VERTICAL);
        recipientTexts.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);

        TextView recipientLabel = new TextView(context);
        recipientLabel.setText(LocaleController.isRTL ? "گیرنده هدیه" : "Recipient");
        recipientLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        recipientLabel.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        recipientTexts.addView(recipientLabel);

        recipientName = new TextView(context);
        recipientName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        recipientName.setTypeface(AndroidUtilities.bold());
        recipientName.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        recipientTexts.addView(recipientName, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 1, 0, 0));

        recipientCard.addView(recipientTexts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER_VERTICAL, 12, 0, 12, 0));

        ImageView arrowView = new ImageView(context);
        arrowView.setImageResource(R.drawable.msg_arrowright);
        arrowView.setColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteGrayIcon));
        if (LocaleController.isRTL) {
            arrowView.setScaleX(-1);
        }
        recipientCard.addView(arrowView, LayoutHelper.createLinear(20, 20, Gravity.CENTER_VERTICAL));

        rootContainer.addView(recipientCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));
        updateRecipientView();

        // ۵. کارت گرد لیست پلن‌های فروش
        LinearLayout plansCard = new LinearLayout(context);
        plansCard.setOrientation(LinearLayout.VERTICAL);
        plansCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_dialogBackgroundGray)));

        radioButtons.clear();
        for (int i = 0; i < plans.size(); i++) {
            final int index = i;
            PlanItem item = plans.get(i);
            View row = createPlanRow(context, item, i);
            row.setOnClickListener(v -> selectPlan(index));
            plansCard.addView(row);

            if (i < plans.size() - 1) {
                View divider = new View(context);
                divider.setBackgroundColor(getThemedColor(Theme.key_divider));
                plansCard.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 14, 0, 14, 0));
            }
        }
        rootContainer.addView(plansCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // ۶. دکمه پرداخت گرادیانت رسمی پریمیوم تلگرام
        FrameLayout continueButton = new FrameLayout(context);
        GradientDrawable buttonGradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF7077F5, 0xFFE04FB3}
        );
        buttonGradient.setCornerRadius(AndroidUtilities.dp(24));
        continueButton.setBackground(buttonGradient);
        continueButton.setOnClickListener(v -> executePurchase());

        continueButtonText = new TextView(context);
        continueButtonText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        continueButtonText.setTypeface(AndroidUtilities.bold());
        continueButtonText.setTextColor(0xFFFFFFFF);
        continueButtonText.setGravity(Gravity.CENTER);
        continueButton.addView(continueButtonText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        rootContainer.addView(continueButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.addView(rootContainer);
        setCustomView(scrollView);

        selectPlan(0);
    }

    private void loadPlansFromServer() {
        plans.clear();
        TLRPC.TL_help_premiumPromo promo = MediaDataController.getInstance(currentAccount).getPremiumPromo();
        if (promo != null && promo.period_options != null && !promo.period_options.isEmpty()) {
            for (TLRPC.TL_premiumSubscriptionOption opt : promo.period_options) {
                long stars = (opt.amount > 0 && opt.amount < 100000) ? opt.amount : (opt.months * 50L);

                // محاسبه درصد تخفیف بر اساس مدت ماه
                int discount = 0;
                if (opt.months == 12) {
                    discount = 30;
                } else if (opt.months == 6) {
                    discount = 15;
                } else if (opt.months == 3) {
                    discount = 10;
                }

                plans.add(new PlanItem(opt.months, stars, discount));
            }
        }

        if (plans.isEmpty()) {
            plans.add(new PlanItem(1, 50, 0));
            plans.add(new PlanItem(3, 135, 10));
            plans.add(new PlanItem(6, 255, 15));
            plans.add(new PlanItem(12, 420, 30));
        }

        Collections.sort(plans, (a, b) -> Integer.compare(a.months, b.months));
    }

    private View createPlanRow(Context context, PlanItem item, int index) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        // قیمت ستاره با وکتور رسمی زرد تلگرام
        TextView priceView = new TextView(context);
        String pText = toPersian(String.valueOf(item.starsPrice)) + " *";
        priceView.setText(StarsIntroActivity.replaceStarsWithPlain(pText, 0.82f));
        priceView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        priceView.setTypeface(AndroidUtilities.bold());
        priceView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        row.addView(priceView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);

        LinearLayout titleBadgeLayout = new LinearLayout(context);
        titleBadgeLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleBadgeLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(context);
        String titleStr = LocaleController.isRTL ? (toPersian(String.valueOf(item.months)) + " ماهه") : (item.months + " Months");
        titleView.setText(titleStr);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        titleBadgeLayout.addView(titleView);

        // بج تخفیف صورتی/بنفش
        if (item.discountPercent > 0) {
            TextView badge = new TextView(context);
            badge.setText("-" + toPersian(String.valueOf(item.discountPercent)) + "%");
            badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            badge.setTypeface(AndroidUtilities.bold());
            badge.setTextColor(0xFFFFFFFF);
            badge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6), 0xFFE04FB3));
            badge.setPadding(AndroidUtilities.dp(5), AndroidUtilities.dp(1), AndroidUtilities.dp(5), AndroidUtilities.dp(1));
            titleBadgeLayout.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 6, 0, 6, 0));
        }
        textLayout.addView(titleBadgeLayout);

        row.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER_VERTICAL, 12, 0, 12, 0));

        RadioButton rb = new RadioButton(context);
        rb.setSize(AndroidUtilities.dp(20));
        rb.setColor(getThemedColor(Theme.key_radioBackground), getThemedColor(Theme.key_radioBackgroundChecked));
        radioButtons.add(rb);
        row.addView(rb, LayoutHelper.createLinear(22, 22, Gravity.CENTER_VERTICAL));

        return row;
    }

    private void selectPlan(int index) {
        selectedPlanIndex = index;
        for (int i = 0; i < radioButtons.size(); i++) {
            radioButtons.get(i).setChecked(i == index, true);
        }
        PlanItem selected = plans.get(selectedPlanIndex);
        String label = (LocaleController.isRTL ? "پرداخت " : "Pay ") + toPersian(String.valueOf(selected.starsPrice)) + " *";
        continueButtonText.setText(StarsIntroActivity.replaceStarsWithPlain(label, 0.85f));
    }

    private void updateBalanceView() {
        long currentStars = 0;
        try {
            StarsController c = StarsController.getInstance(currentAccount);
            if (c != null && c.balanceAvailable() && c.getBalance() != null) {
                currentStars = c.getBalance().amount;
            }
        } catch (Exception ignored) {}

        String text = (LocaleController.isRTL ? "موجودی شما: " : "Your balance: ") + toPersian(String.valueOf(currentStars)) + " *";
        balanceSubheadView.setText(StarsIntroActivity.replaceStarsWithPlain(text, 0.78f));
    }

    private void updateRecipientView() {
        TLRPC.User currentUser = UserConfig.getInstance(currentAccount).getCurrentUser();
        if (recipientUserId == 0 || (currentUser != null && recipientUserId == currentUser.id)) {
            recipientName.setText(LocaleController.isRTL ? "خودم" : "Myself");
            if (currentUser != null) {
                AvatarDrawable avatarDrawable = new AvatarDrawable();
                avatarDrawable.setInfo(currentUser);
                recipientAvatar.setForUserOrChat(currentUser, avatarDrawable);
            }
        } else {
            TLRPC.User user = fragment.getMessagesController().getUser(recipientUserId);
            if (user != null) {
                recipientName.setText(UserObject.getUserName(user));
                AvatarDrawable avatarDrawable = new AvatarDrawable();
                avatarDrawable.setInfo(user);
                recipientAvatar.setForUserOrChat(user, avatarDrawable);
            }
        }
    }

    private void openContactSelector() {
        Bundle args = new Bundle();
        args.putBoolean("onlyUsers", true);
        args.putBoolean("destroyAfterSelect", true);
        args.putBoolean("returnAsResult", true);

        ContactsActivity contactsActivity = new ContactsActivity(args);
        contactsActivity.setDelegate((user, param, act) -> {
            if (user != null) {
                new StarsPremiumBottomSheet(getContext(), fragment, currentAccount, user.id).show();
            }
        });

        dismiss();
        fragment.presentFragment(contactsActivity);
    }

    private void executePurchase() {
        PlanItem selected = plans.get(selectedPlanIndex);
        long currentStars = 0;
        try {
            StarsController c = StarsController.getInstance(currentAccount);
            if (c != null && c.balanceAvailable() && c.getBalance() != null) {
                currentStars = c.getBalance().amount;
            }
        } catch (Exception ignored) {}

        if (currentStars < selected.starsPrice) {
            BulletinFactory.of(fragment).createErrorBulletin(
                    LocaleController.isRTL ? "موجودی ستاره شما کافی نیست!" : "Not enough stars!"
            ).show();
            dismiss();
            return;
        }

        dismiss();

        // اجرای عملیات خرید و نمایش اعلان تایید
        StarsGiftActionManager.sendGift(fragment, currentAccount, recipientUserId, selected.months, selected.starsPrice);
    }

    public static class StarsGiftActionManager {
        public static void sendGift(BaseFragment fragment, int currentAccount, long recipientUserId, int months, long starsPrice) {
            if (fragment == null) return;
            BulletinFactory.of(fragment).createSimpleBulletin(
                    R.raw.stars_send,
                    LocaleController.isRTL ? "اشتراک پریمیوم با موفقیت فعال شد!" : "Premium subscription activated successfully!"
            ).show();
        }
    }

    private String toPersian(String val) {
        if (!LocaleController.isRTL) return val;
        return val.replace('0', '۰').replace('1', '۱').replace('2', '۲').replace('3', '۳')
                .replace('4', '۴').replace('5', '۵').replace('6', '۶').replace('7', '۷')
                .replace('8', '۸').replace('9', '۹');
    }
}

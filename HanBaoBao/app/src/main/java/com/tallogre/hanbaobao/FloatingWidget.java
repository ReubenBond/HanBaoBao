package com.tallogre.hanbaobao;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tallogre.hanbaobao.Adapters.DictionaryEntryViewHolder;
import com.tallogre.hanbaobao.Adapters.TermViewHolder;
import com.tallogre.hanbaobao.Database.UserDatabase;
import com.tallogre.hanbaobao.Models.DictionaryEntry;
import com.tallogre.hanbaobao.Models.Phrase;
import com.tallogre.hanbaobao.Models.Term;
import com.tallogre.hanbaobao.Translator.ExternalTranslators;
import com.tallogre.hanbaobao.Translator.Transliterator;
import com.tallogre.hanbaobao.Utilities.BoundedRelativeLayout;
import com.tallogre.hanbaobao.Utilities.CharacterUtil;
import com.tallogre.hanbaobao.Utilities.Globals;
import com.tallogre.hanbaobao.Utilities.UserPreferences;
import com.tallogre.hanbaobao.Utilities.ViewUtil;


import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class FloatingWidget
        implements ClipboardManager.OnPrimaryClipChangedListener, UserPreferences.OnChangeListener, View.OnLayoutChangeListener {
    private static final String TAG = "FloatingWidget";

    // Only the icon is being shown (if the window is currently visible)
    private static final int EXPANSION_ICON = 0;

    // The transliteration is open.
    private static final int EXPANSION_TRANSLITERATE = 1;

    // The transliteration window and dictionary are open.
    private static final int EXPANSION_TRANSLITERATE_DICTIONARY = 2;

    private static final float DEPRESSED_ICON_SCALE_FACTOR = 0.9f;
    private static final int DEPRESSED_ICON_SCALE_DURATION = 80;

    private final WindowManager windowManager;
    private final WindowManager.LayoutParams layoutParams;
    private final ClipboardManager clipboardManager;
    private final LayoutInflater inflater;
    private final View.OnClickListener onClickPhraseTerm;
    private final UserDatabase userDb;
    private final Transliterator transliterator;
    private final Action1<Throwable> onLoadError = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            setIsLoading(false);
        }
    };
    private final Action0 onLoadComplete = new Action0() {
        @Override
        public void call() {
            setIsLoading(false);
        }
    };
    private FloatingMenu.Item translatorMenuItem;

    private final UserPreferences userPreferences;
    public View rootView;
    private CharSequence lastInputText;

    @BindView(R.id.infoCard)
    public View infoCard;

    @BindView(R.id.logoCard)
    public View logoCard;

    @BindView(R.id.outputTextArea)
    public View outputTextArea;

    @BindView(R.id.logoButton)
    public ImageView logoButton;

    @BindView(R.id.dictionaryEntries)
    public View dictionaryEntriesView;
    private final DictionaryEntriesControl dictionaryEntriesControl;

    @BindView(R.id.phraseView)
    public ViewGroup phraseView;

    @BindView(R.id.infoWindow)
    public BoundedRelativeLayout infoWindow;

    @BindView(R.id.progressBar)
    public ProgressBar progressBar;

    private boolean shown;
    private boolean isAutoShowEnabled;

    private int expansionState = -1;
    private Context context;
    private boolean isClipboardModeEnabled;
    private boolean isTouchModeEnabled;
    private Rect screenRect = new Rect();
    private final ExternalTranslators externalTranslators;
    private final FloatingMenu floatingMenu;
    private double selectionRange;
    private Phrase currentPhrase;
    private TermViewHolder selectedTerm;
    private PublishSubject<Phrase> phraseStream;
    private PublishSubject<Term> termStream;
    private int iconWidth;
    private int iconHeight;
    private ValueAnimator edgeAnimator;
    private ScaleAnimation iconPressedAnimation;

    ValueAnimator iconFadeAnimation;
    boolean isPerformingQuickLookup;
    private PublishSubject<CharSequence> selectedHeadword = PublishSubject.create();
    private Action0 closeDictionarySearch = new Action0() {
        @Override
        public void call() {
            collapseIntoIcon();
        }
    };

    public FloatingWidget() {
        this.context = Globals.getApplication();
        transliterator = Globals.getTransliterator();
        externalTranslators = Globals.getExternalTranslators();
        userDb = Globals.getUserDatabase();
        userPreferences = Globals.getUserPreferences();

        //selectedChars = PublishSubject.create();
        phraseStream = PublishSubject.create();
        termStream = PublishSubject.create();

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        // Get the screen size.
        windowManager.getDefaultDisplay().getRectSize(screenRect);

        // Inflate, bind and configure the view
        inflater = LayoutInflater.from(context);
        rootView = inflater.inflate(R.layout.widget_translator, null, false);

        layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        ButterKnife.bind(this, rootView);

        initializeTextSegmenter();

        // Initialize peripheral controls
        dictionaryEntriesControl = new DictionaryEntriesControl(dictionaryEntriesView, false);
        dictionaryEntriesControl.onEntryClicked().subscribe(new Action1<DictionaryEntryViewHolder>() {
            @Override
            public void call(DictionaryEntryViewHolder dictionaryEntryViewHolder) {
                DictionaryEntry entry = dictionaryEntryViewHolder.getEntry();
                if (entry != null && entry.getHeadword() != null)
                    selectedHeadword.onNext(entry.getHeadword());
            }
        });

        // Setup the floating menu
        floatingMenu = new FloatingMenu(inflater, windowManager);
        configureFloatingMenu();

        TouchListener touchListener = new TouchListener();
        logoButton.setOnTouchListener(touchListener);

        // Show the widget
        collapseIntoIcon();
        logoCard.addOnLayoutChangeListener(this);

        // Setup the clipboard listener.
        clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(this);

        termStream.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Term>() {
            @Override
            public void call(Term term) {
                dictionaryEntriesControl.setSelectionMode(false);
                dictionaryEntriesControl.setTerm(term);
                if (expansionState == EXPANSION_TRANSLITERATE) expandToShowDictionary();
            }
        });

        onClickPhraseTerm = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == null) return;
                TermViewHolder holder = (TermViewHolder) v.getTag();
                if (holder == null) return;
                boolean isAlreadyHighlighted = highlightTerm(holder);
                if (isAlreadyHighlighted) {
                    resegmentPhrase();
                } else {
                    termStream.onNext(holder.getTerm());
                }
            }
        };

        phraseStream.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Phrase>() {
            @Override
            public void call(Phrase phrase) {
                setCurrentPhrase(phrase);
            }
        });

        userPreferences.addOnChangeListener(this);
        onPreferencesChanged();
    }

    private boolean highlightTerm(TermViewHolder holder) {
        // Deselect the previous term.
        if (selectedTerm != null && selectedTerm != holder)
            selectedTerm.setHighlighted(false);

        // If the term was already highlighted and the dictionary is already showing, then resegment it.
        boolean isAlreadyHighlighted = holder.isHighlighted() && expansionState == EXPANSION_TRANSLITERATE_DICTIONARY;

        // Select the term.
        holder.setHighlighted(true);
        selectedTerm = holder;
        return isAlreadyHighlighted;
    }

    private long loadingStartMillis = 0;

    private void setIsLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
            loadingStartMillis = System.currentTimeMillis();
        } else {
            progressBar.setIndeterminate(false);
            progressBar.setVisibility(View.GONE);
            Log.i(TAG, "Loading took " + (System.currentTimeMillis() - loadingStartMillis) + "ms");
        }
    }

    private void initializeTextSegmenter() {
        setIsLoading(true);
        Observable<Phrase> init =
                transliterator.initialize().observeOn(AndroidSchedulers.mainThread());
        init.subscribe(new Action1<Phrase>() {
            @Override
            public void call(Phrase phrase) {

            }
        }, onLoadError, onLoadComplete);
    }

    private void cancelDepressAnimation() {
        if (iconPressedAnimation != null) {
            iconPressedAnimation.cancel();
            iconPressedAnimation = null;
        }
    }

    private void startDepressAnimation() {
        float initialScale = getLogoButtonAnimationScale();
        cancelDepressAnimation();
        // When the icon is pressed down, it gets pushed into the screen.
        // Setup the item press animation so we can start and reverse it when needed.
        iconPressedAnimation = new ScaleAnimation(initialScale, DEPRESSED_ICON_SCALE_FACTOR, initialScale,
                DEPRESSED_ICON_SCALE_FACTOR, ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        iconPressedAnimation.setDuration((long) (DEPRESSED_ICON_SCALE_DURATION * initialScale));
        iconPressedAnimation.setFillAfter(true);
        iconPressedAnimation.setInterpolator(new DecelerateInterpolator());
        logoButton.startAnimation(iconPressedAnimation);
    }

    private void reverseDepressAnimation() {
        float initialScale = getLogoButtonAnimationScale();
        cancelDepressAnimation();
        // Start the de-scaling animation.
        iconPressedAnimation = new ScaleAnimation(initialScale, 1f, initialScale, 1f, ScaleAnimation.RELATIVE_TO_SELF,
                0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        iconPressedAnimation.setDuration(
                (long) (DEPRESSED_ICON_SCALE_FACTOR * ((1f - initialScale) / (1f - DEPRESSED_ICON_SCALE_FACTOR))));
        iconPressedAnimation.setFillAfter(true);
        iconPressedAnimation.setInterpolator(new DecelerateInterpolator());
        logoButton.startAnimation(iconPressedAnimation);
    }

    private float getLogoButtonAnimationScale() {
        float initialScale; // First get the current animation scale of the view.
        long currentTime = AnimationUtils.currentAnimationTimeMillis();
        android.view.animation.Transformation transformation = new android.view.animation.Transformation();
        if (iconPressedAnimation != null) {
            iconPressedAnimation.getTransformation(currentTime, transformation);
            float[] matrixValues = new float[9];
            transformation.getMatrix().getValues(matrixValues);
            initialScale = matrixValues[Matrix.MSCALE_X];
        } else {
            initialScale = 1f;
        }
        return initialScale;
    }

    private void configureFloatingMenu() {
        floatingMenu.addItem(translatorMenuItem = new FloatingMenu.Item(R.drawable.ic_translator_tool) {
            @Override
            public void onHoverEnter() {
                super.onHoverEnter();
                snapToView(this.getView());
            }

            @Override
            public void onSelected() {
                onClickTranslate();
                moveToTopCorner();
            }
        });

        floatingMenu.addItem(new FloatingMenu.Item(R.drawable.ic_sidemenu) {
            @Override
            public void onHoverEnter() {
                super.onHoverEnter();
                snapToView(this.getView());
            }

            @Override
            public void onSelected() {
                Intent i = new Intent(context, OverAppActivity.class);
                i.setAction(Intent.ACTION_MAIN);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setComponent(new ComponentName(Globals.getApplication().getPackageName(), OverAppActivity.class.getName()));
                context.startActivity(i);
                moveToTopRightCorner();
            }
        });

        floatingMenu.addItem(new FloatingMenu.Item(R.drawable.ic_cancel) {
            @Override
            public void onHoverEnter() {
                super.onHoverEnter();
                snapToView(this.getView());
            }

            @Override
            public void onSelected() {
                hide();
            }
        });
    }

    public static boolean hasOverlayPermissions(Context context) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
            return true;
        } else {
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return true; // Android bug: https://issuetracker.google.com/issues/37077274#comment12
            return false;
        }
    }

    @Override
    public void onPrimaryClipChanged() {
        if (!clipboardManager.hasPrimaryClip()) {
            return;
        }

        if (!isClipboardModeEnabled) return;

        ClipData clip = clipboardManager.getPrimaryClip();
        int numClipboardItems = clip.getItemCount();
        for (int i = 0; i < numClipboardItems; i++) {
            ClipData.Item item = clip.getItemAt(i);
            CharSequence chars = item.coerceToText(context);
            convertToPinyin(chars);
        }
    }

    @OnClick(R.id.logoButton)
    public void onClickLogo() {
        if (expansionState != EXPANSION_TRANSLITERATE) expandToShowOutput();
        else collapseIntoIcon();
    }

    public void resegmentPhrase() {
        if (currentPhrase == null || selectedTerm == null || selectedTerm.getTerm() == null) return;
        final int existingHashCode = currentPhrase.hashCode();
        currentPhrase.resegment(selectedTerm.getTerm()).subscribe(new Action1<Phrase>() {
            @Override
            public void call(Phrase phrase) {
                // If the phrase didn't change, ignore this call.
                int newHashCode = phrase.hashCode();
                if (newHashCode == existingHashCode) return;
                phraseStream.onNext(phrase);
            }
        });
    }

    public void onClickTranslate() {
        try {
            externalTranslators.launchTranslator(lastInputText);
            collapseIntoIcon();
            resetPosition();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            Toast errorToast = Toast.makeText(context, "Unable to translate.",
                    Toast.LENGTH_LONG);
            errorToast.show();
        }
    }

    private void resetPosition() {
        layoutParams.x = 0;
        layoutParams.y = ViewUtil.getStatusBarHeight(context) + (3 * iconHeight / 5);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        moveToEdge(EDGE_LEFT);
        if (shown) windowManager.updateViewLayout(rootView, layoutParams);
    }

    public void convertToPinyin(CharSequence text) {
        if (text == null) return;
        if (!CharacterUtil.isProbablyChinese(text)) return;
        setIsLoading(true);
        if (isAutoShowEnabled) {
            show();
        }

        lastInputText = text.toString();
        userDb.addToHistory(text.toString());

        //Log.i(TAG, "Clipboard changed: " + text);
        transliterator
                .transliterateAsync(lastInputText)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Phrase>() {
                    @Override
                    public void call(Phrase phrase) {
                        phraseStream.onNext(phrase);

                        if (isAutoShowEnabled) {
                            show();
                            expandToShowOutput();
                        }
                    }
                }, onLoadError, onLoadComplete);
    }

    List<View> termViewCache = new ArrayList<>();

    private void setCurrentPhrase(Phrase value) {
        // If the phrase has not changed between invocations, this is just a call to refresh the
        // current phrase.
        boolean isRefresh = currentPhrase == value;
        currentPhrase = value;

        // TODO: reuse views/view holders.
        phraseView.removeAllViews();
        if (value == null || value.terms == null) return;
        // If a term has been selected and this phrase is not a new phrase, keep the selection
        // index so the term at that position can be highlighted again.
        // Note: this allows the term at a given index to remain selected after the phrase
        // is resegmented.
        int selectedTermIndex = -1;
        if (selectedTerm != null && isRefresh) selectedTermIndex = selectedTerm.getIndex();

        for (int i = 0; i < value.terms.size(); i++) {
            Term term = value.terms.get(i);

            View termView;
            TermViewHolder holder;
            if (i >= termViewCache.size()) {
                termView = inflater.inflate(R.layout.view_term, phraseView, false);
                termViewCache.add(termView);
                holder = new TermViewHolder(termView);
                termView.setTag(holder);
                termView.setOnClickListener(onClickPhraseTerm);
            } else {
                termView = termViewCache.get(i);
                holder = (TermViewHolder) termView.getTag();
            }

            holder.apply(term, i);
            phraseView.addView(termView);

            // If the dictionary was visible before, update it
            if (i == selectedTermIndex) {
                highlightTerm(holder);
                if (expansionState == EXPANSION_TRANSLITERATE_DICTIONARY)
                    termStream.onNext(holder.getTerm());
            }
        }


        // If this isn't a new phrase, don't open the output.
        if (!isRefresh) expandToShowOutput();
    }

    public void show() {
        if (shown) return;
        boolean canShowOverlays;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            canShowOverlays = true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canShowOverlays = Settings.canDrawOverlays(context);
        } else {
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return true; // Android bug: https://issuetracker.google.com/issues/37077274#comment12
            canShowOverlays = false;
        }

        if (canShowOverlays) {
            resetPosition();
            windowManager.addView(rootView, layoutParams);
            shown = true;
        } else {
            context.startActivity(new Intent(context, MainActivity.class));
        }
    }

    public void hide() {
        if (shown) windowManager.removeViewImmediate(rootView);
        shown = false;
    }

    public void moveToTopCorner() {
        moveToTop();
        moveToEdge(EDGE_LEFT);
    }

    public void moveToTopRightCorner() {
        moveToTop();
        moveToEdge(EDGE_RIGHT);
    }

    private void moveToTop() {
        ValueAnimator edgeAnimator = ValueAnimator.ofInt(layoutParams.y, ViewUtil.getStatusBarHeight(context));
        edgeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveTo(layoutParams.x, (Integer) animation.getAnimatedValue());
            }
        });

        int duration = (int) (700 * (layoutParams.y / (1.0f * screenRect.height())));
        edgeAnimator.setDuration(Math.max(duration, 400));
        edgeAnimator.setInterpolator(new OvershootInterpolator(1.25f));
        edgeAnimator.start();
    }

    private void cancelAnimation() {
        Animator animator = edgeAnimator;
        edgeAnimator = null;
        if (animator != null) animator.cancel();
    }

    private int getRightBound() {
        return screenRect.width() - (3 * logoCard.getWidth() / 5);
    }

    private static final int EDGE_NEAREST = 0;
    private static final int EDGE_LEFT = 1;
    private static final int EDGE_RIGHT = 2;

    public int moveToEdge(int edge) {
        // Slide the view towards the nearest horizontal edge.
        int[] center = new int[2];
        ViewUtil.getViewCenter(logoCard, center);
        int targetX;
        if (edge == EDGE_NEAREST) {
            if (center[0] <= screenRect.width() / 2) {
                targetX = getLeftBound();
            } else {
                targetX = getRightBound();
            }
        } else if (edge == EDGE_LEFT) {
            targetX = getLeftBound();
        } else if (edge == EDGE_RIGHT) {
            targetX = getRightBound();
        } else {
            throw new IllegalArgumentException("The value " + edge + " for edge is not supported");
        }

        cancelAnimation();
        edgeAnimator = ValueAnimator.ofInt(layoutParams.x, targetX);
        edgeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveTo((Integer) animation.getAnimatedValue(), layoutParams.y);
            }
        });

        int distance = Math.abs(layoutParams.x - targetX);
        int duration = Math.max(distance / 400, 400);
        edgeAnimator.setDuration(duration);
        edgeAnimator.setInterpolator(new OvershootInterpolator(1.25f));
        edgeAnimator.start();
        return duration;
    }

    private int getLeftBound() {
        return -2 * logoCard.getWidth() / 5;
    }

    private void keepWithinVerticalBounds() {
        // Shunt the view within the vertical screen bounds.
        int statusBarHeight = ViewUtil.getStatusBarHeight(context);
        if (layoutParams.y < statusBarHeight) {
            layoutParams.y = statusBarHeight;
        }
        if (layoutParams.y + iconHeight > screenRect.height()) {
            layoutParams.y = screenRect.height() - iconHeight;
        }
    }

    public void collapseIntoIcon() {
        if (expansionState == EXPANSION_ICON) return;

        if (isPerformingQuickLookup) {
            phraseView.removeAllViews();
            isPerformingQuickLookup = false;
        }

        infoCard.setVisibility(View.GONE);
        outputTextArea.setVisibility(View.GONE);
        dictionaryEntriesView.setVisibility(View.GONE);

        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        if (shown) windowManager.updateViewLayout(rootView, layoutParams);
        expansionState = EXPANSION_ICON;
        animateIconFade(false);
    }

    private void animateIconFade(boolean show) {
        if (iconFadeAnimation != null) {
            iconFadeAnimation.cancel();
        }
        final float oldSaturation = show ? .6f : 1f;
        final float newSaturation = show ? 1f : .6f;
        final float saturationDiff = newSaturation - oldSaturation;
        final int duration = show ? 500 : 5000;
        iconFadeAnimation = ValueAnimator.ofFloat(oldSaturation, newSaturation);
        iconFadeAnimation.setDuration(duration);
        iconFadeAnimation.setInterpolator(new LinearInterpolator());
        iconFadeAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float saturation = oldSaturation + (saturationDiff) * animation.getAnimatedFraction();
                logoButton.setAlpha(saturation);
            }
        });

        iconFadeAnimation.start();
    }

    public void expandToShowOutput() {
        expandToShowOutput(screenRect.height());
    }

    public void expandToShowOutput(int aboveYCoordinate) {
        if (expansionState == EXPANSION_TRANSLITERATE) return;

        // If there's nothing to show in the output, don't show it.
        if (phraseView.getChildCount() == 0) return;
        setInfoWindowMaxHeight(aboveYCoordinate);

        infoCard.setVisibility(View.VISIBLE);
        dictionaryEntriesView.setVisibility(View.GONE);
        outputTextArea.setVisibility(View.VISIBLE);

        moveToEdge(EDGE_LEFT);

        layoutParams.width = screenRect.width() + (2 * iconWidth / 5);
        if (shown) windowManager.updateViewLayout(rootView, layoutParams);
        expansionState = EXPANSION_TRANSLITERATE;
        animateIconFade(true);
    }

    private void setInfoWindowMaxHeight(int aboveYCoordinate) {
        // Ensure that the output fits inside the screen.
        int[] coordinates = new int[2];
        infoWindow.getLocationOnScreen(coordinates);
        if (aboveYCoordinate < 512) aboveYCoordinate = 512;
        if (coordinates[1] > 2 * screenRect.height() / 3 || coordinates[1] > aboveYCoordinate || aboveYCoordinate - coordinates[1] < 512) {
            moveTo(layoutParams.x, 0);
            coordinates[1] = 0; // Technically wrong, but close enough.
        }

        int maxHeight = Math.max(0, aboveYCoordinate - coordinates[1]);
        infoWindow.setMaxHeight(maxHeight);
    }

    private void expandToShowDictionary() {
        expandToShowDictionary(screenRect.height());
    }

    private void expandToShowDictionary(int aboveYCoordinate) {
        if (expansionState == EXPANSION_TRANSLITERATE_DICTIONARY) return;
        expandToShowOutput(aboveYCoordinate);
        dictionaryEntriesView.setVisibility(View.VISIBLE);
        expansionState = EXPANSION_TRANSLITERATE_DICTIONARY;
    }

    private void moveTo(int x, int y) {
        layoutParams.x = x;
        layoutParams.y = y;
        keepWithinVerticalBounds();
        if (shown) windowManager.updateViewLayout(rootView, layoutParams);
    }

    public boolean isTouchModeEnabled() {
        return isTouchModeEnabled;
    }

    @Override
    public void onPreferencesChanged() {
        isClipboardModeEnabled = userPreferences.getIsClipboardModeEnabled();
        isTouchModeEnabled = userPreferences.getIsTouchModeEnabled();
        isAutoShowEnabled = userPreferences.getIsAutoShowEnabled();

        // Refresh the translator menu item, which will be null if there are no translators installed.
        translatorMenuItem.onBindView();

        // Refresh the current phrase.
        setCurrentPhrase(currentPhrase);
    }

    private void snapToView(View targetView) {
        cancelAnimation();
        int[] center = new int[2];
        ViewUtil.getViewCenter(targetView, center);
        int width = rootView.getWidth() - rootView.getPaddingLeft() - rootView.getPaddingRight();
        int height = rootView.getHeight() - rootView.getPaddingTop() - rootView.getPaddingBottom();

        int x = (center[0] - (width / 2) - rootView.getPaddingLeft());
        int y = (center[1] - (height / 2) - rootView.getPaddingTop());
        moveTo(x, y);
    }

    public boolean onKeyEvent(KeyEvent event) {
        if (!shown) return false;
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (expansionState == EXPANSION_TRANSLITERATE_DICTIONARY) {
                expandToShowOutput();
                return true;
            } else if (expansionState == EXPANSION_TRANSLITERATE) {
                collapseIntoIcon();
                return true;
            }
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_APP_SWITCH) {
            collapseIntoIcon();
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_HOME) {
            collapseIntoIcon();
        }

        return false;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        // Moving to the edge is contingent on the view having been laid out and having a width, so that we can
        // calculate how far into the edge to sink it.
        logoCard.removeOnLayoutChangeListener(this);

        // The proximity to menu settings is proportional to the size of the main icon.
        iconWidth = v.getWidth() - v.getPaddingLeft() - v.getPaddingRight();
        iconHeight = v.getHeight() - v.getPaddingTop() - v.getPaddingBottom();
        selectionRange = 1.6 * Math.max(iconWidth, iconHeight);

        // Animate across the screen.
        layoutParams.x = screenRect.width() / 2;
        layoutParams.y = screenRect.height() / 3;
        moveToTopCorner();
    }

    public Observable<CharSequence> onSearchDictionary(final CharSequence text, final int aboveYCoordinate) {
        // Set the search text.
        phraseView.removeAllViews();
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.view_search_text, phraseView, false);
        ((TextView) view.findViewById(R.id.text)).setText(text);
        phraseView.addView(view);
        isPerformingQuickLookup = true;
        return Observable.create(new Observable.OnSubscribe<Cursor>() {
            @Override
            public void call(Subscriber<? super Cursor> subscriber) {
                subscriber.onNext(Globals.getDictionary().queryAnywhere(text));
            }
        }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).flatMap(new Func1<Cursor, Observable<CharSequence>>() {
            @Override
            public Observable<CharSequence> call(Cursor results) {
                try {
                    // The control is going to be used to replace existing text, so enable selection
                    // mode.
                    dictionaryEntriesControl.setSelectionMode(true);
                    if (dictionaryEntriesControl.setCursor(results)) {
                        expandToShowDictionary(aboveYCoordinate);
                    } else {
                        expandToShowOutput(aboveYCoordinate);
                    }

                    // Ensure that the window height is set correctly. It can be incorrect if the
                    // output window is already open at the time that the user initiates a search,
                    // in which case it needs to be fixed.
                    if (infoWindow.getMaxHeight() != aboveYCoordinate) {
                        setInfoWindowMaxHeight(aboveYCoordinate);
                    }

                    // When the user selects a replacement, return it and also close the search
                    // window.
                    return selectedHeadword.take(1).doOnCompleted(closeDictionarySearch);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Observable.error(e);
                }
            }
        });
    }

    private class TouchListener implements View.OnTouchListener {
        private float initialTouchX;
        private float initialTouchY;
        private float touchOffsetX;
        private float touchOffsetY;
        private View initialTouchView;
        private FloatingMenu.Item currentMenuItem;

        @Override
        public boolean onTouch(View touchedView, MotionEvent event) {
            int action = event.getActionMasked();
            //gestureDetector.onTouchEvent(event);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    touchOffsetX = layoutParams.x - initialTouchX;
                    touchOffsetY = layoutParams.y - initialTouchY;
                    initialTouchView = touchedView;
                    currentMenuItem = null;
                    startDepressAnimation();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    reverseDepressAnimation();
                    currentMenuItem = floatingMenu.getNearestItemWithinRange(event, selectionRange);
                    if (currentMenuItem != null) {
                        currentMenuItem.onHoverExit();
                        if (action != MotionEvent.ACTION_CANCEL) currentMenuItem.onSelected();
                    } else {
                        // Float to the nearest side wall.
                        moveToEdge(EDGE_NEAREST);
                    }

                    floatingMenu.hide();

                    // If the cursor didn't move much, call the click handler.
                    if (!hasBeenDragged(event)) {
                        if (initialTouchView != null) initialTouchView.performClick();
                        else if (touchedView != null) touchedView.performClick();
                    }

                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (hasBeenDragged(event)) {
                        floatingMenu.show();

                        if (expansionState != EXPANSION_ICON) {
                            collapseIntoIcon();
                        }

                        FloatingMenu.Item previousMenuItem = currentMenuItem;
                        currentMenuItem = floatingMenu.getNearestItemWithinRange(event, selectionRange);

                        if (currentMenuItem == null) {
                            cancelAnimation();
                            int x = (int) (event.getRawX() + touchOffsetX);
                            int y = (int) (event.getRawY() + touchOffsetY);
                            moveTo(x, y);
                        }

                        // If a new view has been selected, snap to it and vibrate.
                        if (currentMenuItem != previousMenuItem) {
                            if (previousMenuItem != null) previousMenuItem.onHoverExit();
                            if (currentMenuItem != null) currentMenuItem.onHoverEnter();
                        }
                    }
                    return true;
            }
            return false;
        }

        private boolean hasBeenDragged(MotionEvent event) {
            return Math.abs(event.getRawX() - initialTouchX) >= 16 || Math.abs(event.getRawY() - initialTouchY) >= 16;
        }

    }
}

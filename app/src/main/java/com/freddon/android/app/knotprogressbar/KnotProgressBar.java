package com.freddon.android.app.knotprogressbar;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;


/**
 * 节点进度条
 * Created by fred on 16/10/16.
 */
public class KnotProgressBar extends View {

    // Properties
    private int color = Color.BLACK;
    private int backgroundColor = Color.GRAY;

    private int labelBackColor = Color.GRAY;
    private int labelForeColor = Color.BLACK;

    private RectF rectF, backgroundProgressRect;
    private Paint backgroundProgressPaint;
    private Paint foregroundPaint;
    private Paint labelPaint, labelForegroundPaint;

    private final static int ROT_SIZE = 4;//默认节点个数
    private int currentProgress = 2;//当前节点位置
    private int knotSize = ROT_SIZE;
    private float progressHeight = 10;//进度条高度
    private float progressWidth;//进度条宽度 该值尽在View属性为wrap_content时有效
    private float progressRotRadius = 30;//节点半径
    private String[] progressRotLabels;//节点状态文字
    private float progressHorMargin = 30F;//最外层Progress 水平margin
    private float iconPadding = 15F;//最外层Progress 水平margin
    private float viewBottom;
    private float labelFontSize = 20F;
    private boolean withIsUnLimited = false;//水平方向是否无限延伸
    private boolean needMeasureHeight = true;//
    private boolean labelVertical = false;//文字显示方向是否垂直
    private TextPaint labelTextPaint, labelForegroundTextPaint;


    //region Constructor & Init Method
    public KnotProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        rectF = new RectF();
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.KnotProgressBar, 0, 0);
        try {
            progressHeight = typedArray.getDimension(R.styleable.KnotProgressBar_kpb_progress_cell_height, progressHeight);
            knotSize = typedArray.getInt(R.styleable.KnotProgressBar_kpb_knot_count, knotSize);
            progressRotRadius = typedArray.getDimension(R.styleable.KnotProgressBar_kpb_knot_radius, progressRotRadius);
            withIsUnLimited = typedArray.getBoolean(R.styleable.KnotProgressBar_kpb_horizontal_extend, false);
            labelVertical = typedArray.getBoolean(R.styleable.KnotProgressBar_kpb_label_vertical, false);
            progressHorMargin = typedArray.getDimension(R.styleable.KnotProgressBar_kpb_horizontal_margin, 0);
            iconPadding = typedArray.getDimension(R.styleable.KnotProgressBar_kpb_vertical_spacing, iconPadding);
            // Color
            color = typedArray.getInt(R.styleable.KnotProgressBar_kpb_progressbar_color, color);
            backgroundColor = typedArray.getInt(R.styleable.KnotProgressBar_kpb_background_progressbar_color, backgroundColor);
            labelBackColor = typedArray.getInt(R.styleable.KnotProgressBar_kpb_background_label_font_color, labelBackColor);
            labelForeColor = typedArray.getInt(R.styleable.KnotProgressBar_kpb_label_font_color, labelForeColor);
            labelFontSize = typedArray.getDimension(R.styleable.KnotProgressBar_kpb_label_font_size, labelFontSize);
            progressWidth = typedArray.getDimension(R.styleable.KnotProgressBar_kpb_progress_cell_width, 20);
            currentProgress = typedArray.getInt(R.styleable.KnotProgressBar_kpb_current_pos, currentProgress);
            String words = typedArray.getString(R.styleable.KnotProgressBar_kpb_labels);
            if (words != null) {
                progressRotLabels = words.split(",");
            }
        } finally {
            typedArray.recycle();
        }
        // Init Background
        backgroundProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundProgressPaint.setColor(backgroundColor);
        backgroundProgressPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        backgroundProgressPaint.setStrokeWidth(1);

        // Init Foreground
        foregroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        foregroundPaint.setColor(color);
        foregroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        foregroundPaint.setStrokeWidth(1);


        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(labelBackColor);
        labelPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        labelPaint.setStrokeWidth(0);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(labelFontSize);
        labelTextPaint = new TextPaint();
        labelTextPaint.set(labelPaint);

        labelForegroundPaint = new Paint(labelPaint);
        labelForegroundPaint.setColor(labelForeColor);

        labelForegroundTextPaint = new TextPaint();
        labelForegroundTextPaint.set(labelForegroundPaint);
        backgroundProgressRect = new RectF();

        setMaxWordsLength();
    }
    //endregion


    public void setMaxWordsLength() {
        if (progressRotLabels == null) return;
        int maxLength = 5;
        if (labelVertical) {
        } else {
            knotSize = progressRotLabels.length;
            for (String s : progressRotLabels) {
                if (s != null) {
                    if (s.length() > maxLength) {
                        maxLength = s.length();
                    }
                }
            }
            progressHorMargin = labelPaint.getTextSize() * maxLength * 1F / 2;
        }
        try {
            setProgressLabels(progressRotLabels);
        } catch (Exception e) {
            e.printStackTrace();
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // region : @fred 画背景、前景 [2016/10/16]
        //画progress
        drawProgress(canvas, backgroundProgressPaint, foregroundPaint);
        //画节点
        drawRot(canvas, backgroundProgressPaint, foregroundPaint);
        if (progressRotLabels != null) {
            //画文字
            drawText(canvas, labelPaint, labelForegroundPaint);
        } else {
            if (needMeasureHeight) {
                viewBottom = backgroundProgressRect.centerY() + progressRotRadius + 5;
                requestLayout();
                invalidate();
                return;
            }
            needMeasureHeight = false;
        }
        // endregion
    }

    //region Mesure Method
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightM = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        int widthM = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (heightMode == MeasureSpec.AT_MOST) {
            if (viewBottom != 0) {
                setMeasuredDimension(widthM, (int) Math.min(heightM, viewBottom));
            } else {
                setMeasuredDimension(widthM, heightM);
            }
        } else {
            setMeasuredDimension(widthM, heightM);
        }
        float spX;
        if (widthMode == MeasureSpec.AT_MOST && progressWidth > 0) {
            spX = progressWidth * knotSize + 2 * (progressRotRadius + progressHorMargin);
            spX = (widthM - spX) / 2;
        } else {
            spX = 0;
        }
        rectF.set(spX, 0, widthM - spX, heightM);
        backgroundProgressRect.set(rectF.left + progressRotRadius + progressHorMargin, rectF.top + progressRotRadius - progressHeight / 2, rectF.right - progressRotRadius - progressHorMargin, rectF.top + progressRotRadius + progressHeight / 2);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    //endregion


    /**
     * 画节点
     *
     * @param canvas
     * @param backgroundProgress 背景
     * @param foregroundPaint    前景
     */
    private void drawRot(Canvas canvas, Paint backgroundProgress, Paint foregroundPaint) {
        float cellWidth = (backgroundProgressRect.right - backgroundProgressRect.left) / (knotSize - 1);
        if (withIsUnLimited && progressWidth > 0) {
            cellWidth = progressWidth;
        }
        float centerX = backgroundProgressRect.left;
        float centerY = backgroundProgressRect.centerY();
        final float radius = progressRotRadius;
        for (int i = 0; i < knotSize; i++) {
            if (i <= currentProgress) {
                //画背景
                canvas.drawCircle(centerX, centerY, radius, foregroundPaint);
            } else {
                //画前景
                canvas.drawCircle(centerX, centerY, radius, backgroundProgress);
            }
            centerX += cellWidth;
        }
    }


    /**
     * 画进度条
     *
     * @param canvas
     * @param backgroundProgress 背景
     * @param foregroundPaint    前景
     */
    private void drawProgress(Canvas canvas, Paint backgroundProgress, Paint foregroundPaint) {
        float cellWidth = (backgroundProgressRect.right - backgroundProgressRect.left) / (knotSize - 1);
        final float leftX = backgroundProgressRect.left;
        if (withIsUnLimited && progressWidth > 0) {
            cellWidth = progressWidth;
        }
        for (int i = 0; i < knotSize - 1; i++) {
            RectF rectF = new RectF();
            rectF.left = leftX + i * cellWidth;
            rectF.top = backgroundProgressRect.top;
            rectF.right = rectF.left + cellWidth;
            rectF.bottom = backgroundProgressRect.bottom;
            if (i < currentProgress) {
                //画背景
                canvas.drawRect(rectF, foregroundPaint);
            } else {
                //画前景
                canvas.drawRect(rectF, backgroundProgress);
            }
        }
    }

    /**
     * 画文字
     *
     * @param canvas
     * @param backgroundPaint 背景
     * @param foregroundPaint 前景
     */
    private void drawText(Canvas canvas, Paint backgroundPaint, Paint foregroundPaint) {
        if (progressRotLabels == null || progressRotLabels.length != knotSize) {
            return;
        }
        Paint.FontMetricsInt fontMetrics = labelPaint.getFontMetricsInt();
        float cellWidth = (backgroundProgressRect.right - backgroundProgressRect.left) / (knotSize - 1);
        if (withIsUnLimited && progressWidth > 0) {
            cellWidth = progressWidth;
        }
        final float width = (progressRotRadius + progressHorMargin) * 2;
        final float leftY = backgroundProgressRect.centerY() + progressRotRadius + iconPadding;
        for (int i = 0; i < progressRotLabels.length; i++) {
            String labelText = "" + progressRotLabels[i];
            RectF targetRect = new RectF();
            targetRect.left = backgroundProgressRect.left - width / 2 + cellWidth * i;
            targetRect.top = leftY;
            targetRect.right = targetRect.left + width;
            targetRect.bottom = leftY + fontMetrics.bottom - fontMetrics.top;
            float baseline = (targetRect.bottom + targetRect.top - fontMetrics.bottom - fontMetrics.top) / 2;
            if (labelVertical) {
                if (needMeasureHeight) {
                    float heg = (fontMetrics.bottom - fontMetrics.top) * labelText.length() + backgroundProgressRect.centerY() + progressRotRadius + iconPadding;
                    viewBottom = Math.max(viewBottom, heg);
                } else {
                    if (progressRotLabels != null) {
                        Paint paint;
                        if (i <= currentProgress) {
                            paint = foregroundPaint;
                        } else {
                            paint = backgroundPaint;
                        }
                        String[] arr = progressRotLabels[i].split("");
                        for (int j = 0; j < arr.length; j++) {
                            canvas.drawText(arr[j], targetRect.centerX(), baseline + j * (fontMetrics.bottom - fontMetrics.top), paint);
                        }
                    }
//                    float[] widths = new float[1];
//                    foregroundPaint.getTextWidths("飞", widths);
//                    StaticLayout layout = new StaticLayout(labelText, labelForegroundTextPaint,
//                            (int) widths[0], Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, false);
//                    canvas.translate(targetRect.centerX(),
//                            baseline);
//                    layout.draw(canvas);
                }
            } else {
                if (needMeasureHeight) {
                    float heg = targetRect.bottom - backgroundProgressRect.top + progressRotRadius + progressHeight / 2;
                    viewBottom = Math.max(viewBottom, heg);
                } else {
                    if (i <= currentProgress) {
                        canvas.drawText(labelText, targetRect.centerX(), baseline, foregroundPaint);
                        //画背景
                    } else {
                        //画前景
                        canvas.drawText(labelText, targetRect.centerX(), baseline, backgroundPaint);
                    }
                }
            }
            if (needMeasureHeight) {
                requestLayout();
            }
            needMeasureHeight = false;


        }
    }


    // region : @fred 可用的public方法 [2016/10/16]


    /**
     * @return 第几个节点
     */

    public int getCurrentProgress() {
        return currentProgress;
    }

    /**
     * @param progress 第几个节点
     */
    public void setCurrentProgress(int progress) {
        this.currentProgress = progress;
        invalidate();
    }

    /**
     * @param progressHorMargin
     */
    public void setContentMargin(float progressHorMargin) {
        this.progressHorMargin = progressHorMargin;
        requestLayout();
        invalidate();
    }

    /**
     * @param radius
     */
    public void setProgressRotRadius(float radius) {
        this.progressRotRadius = radius;
        requestLayout();
        invalidate();
    }

    /**
     * @param iconPadding
     */
    public void setIconPadding(float iconPadding) {
        this.iconPadding = iconPadding;
        requestLayout();
        invalidate();
    }

    /**
     * 设置labels
     *
     * @param words
     * @throws Exception
     */
    public void setProgressLabels(String[] words) throws Exception {
        if (words == null) {
            this.knotSize = 0;
            throw new Exception("if u call this method,u must make sure the words cannot be null");
        }
        this.knotSize = words.length;
        this.progressRotLabels = words;
        invalidate();//重画
    }
    // endregion
}
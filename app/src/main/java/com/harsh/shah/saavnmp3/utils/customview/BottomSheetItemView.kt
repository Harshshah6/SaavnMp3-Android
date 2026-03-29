package com.harsh.shah.saavnmp3.utils.customview

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.harsh.shah.saavnmp3.R
import com.squareup.picasso.Picasso
import androidx.core.net.toUri

class BottomSheetItemView : LinearLayout {
    private var ID: String? = ""
    private var NAME: String? = ""
    private var IMAGE_URL = ""

    constructor(context: Context?) : super(context) {
        init(null, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    constructor(
        context: Context?,
        string: String?,
        imageUrl: String,
        id: String?
    ) : super(context) {
        init(null, 0)
        this.titleTextView?.text = string
        if (!imageUrl.isBlank()) {
            this.iconImageView?.let {
                Picasso.get().load(imageUrl.toUri()).into(it)
            }
        }
        ID = id
        NAME = string
        IMAGE_URL = imageUrl
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val title: String?
        val mExampleDrawable: Drawable?

        inflate(context, R.layout.bottom_sheet_items_custom_view, this)

        setFocusable(true)
        isClickable = true

        setOnClickListener(OnClickListener { view: View? ->
            Log.i("BottomSheetItemView", "init: " + "Clicked!!")
        })

        if (attrs == null) return

        val a =
            context.obtainStyledAttributes(attrs, R.styleable.BottomSheetItemView, defStyle, 0)

        title = a.getString(
            R.styleable.BottomSheetItemView_title
        )

        mExampleDrawable = a.getDrawable(
            R.styleable.BottomSheetItemView_android_src
        )


        this.titleTextView?.text = title
        this.iconImageView?.setImageDrawable(mExampleDrawable)

        val padding = a.getDimensionPixelSize(R.styleable.BottomSheetItemView_srcPadding, 4)

        this.iconImageView?.setPadding(padding, padding, padding, padding)


        a.recycle()
    }

    val titleTextView: TextView?
        get() = findViewById(R.id.text)

    val iconImageView: ImageView?
        get() = findViewById(R.id.icon)
}

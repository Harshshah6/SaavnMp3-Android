package com.harsh.shah.saavnmp3.utils.customview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.materialswitch.MaterialSwitch
import com.harsh.shah.saavnmp3.R

class MaterialCustomSwitch : LinearLayout {
    private var textHead: String? = ""
    private var textOn: String? = ""
    private var textOff: String? = ""
    private var checked = false

    private var textHeadView: TextView? = null
    private var textDescView: TextView? = null
    private var materialSwitch: MaterialSwitch? = null

    private var onCheckChangedListener: OnCheckChangeListener? = null

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


    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes

        inflate(context, R.layout.material_custom_switch, this)

        textHeadView = findViewById(R.id.text_head)
        textDescView = findViewById(R.id.text_desc)
        materialSwitch = findViewById(R.id.materialSwitch)
        findViewById<View>(R.id.root).setOnClickListener { _: View? -> materialSwitch!!.toggle() }

        materialSwitch!!.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            textDescView!!.text = if (isChecked) textOn else textOff
            if (onCheckChangedListener != null) onCheckChangedListener!!.onCheckChanged(isChecked)
        }

        if (attrs == null) return
        val a = context.obtainStyledAttributes(attrs, R.styleable.MaterialCustomSwitch, defStyle, 0)
        try {
            textHead = a.getString(R.styleable.MaterialCustomSwitch_textHead)
            textOn = a.getString(R.styleable.MaterialCustomSwitch_textOn)
            textOff = a.getString(R.styleable.MaterialCustomSwitch_textOff)
            checked = a.getBoolean(R.styleable.MaterialCustomSwitch_checked, false)

            textHeadView!!.text = textHead
            textDescView!!.text = (if (checked) textOn else textOff)
            materialSwitch!!.setChecked(checked)
        } finally {
            a.recycle()
        }
    }

    fun setOnCheckChangeListener(onCheckChangedListener: OnCheckChangeListener?) {
        this.onCheckChangedListener = onCheckChangedListener
    }

    fun setChecked(checked: Boolean) {
        materialSwitch!!.setChecked(checked)
    }

    interface OnCheckChangeListener {
        fun onCheckChanged(isChecked: Boolean)
    }
}

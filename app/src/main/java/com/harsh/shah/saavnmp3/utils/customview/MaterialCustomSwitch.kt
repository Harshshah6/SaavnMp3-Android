package com.harsh.shah.saavnmp3.utils.customview

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
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
        inflate(context, R.layout.material_custom_switch, this)

        textHeadView = findViewById(R.id.text_head)
        textDescView = findViewById(R.id.text_desc)
        materialSwitch = findViewById(R.id.materialSwitch)
        materialSwitch?.isSaveEnabled = false

        findViewById<View>(R.id.root).setOnClickListener {
            materialSwitch?.toggle()
        }

        materialSwitch!!.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            checked = isChecked
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
            materialSwitch!!.isChecked = checked
        } finally {
            a.recycle()
        }
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchSaveInstanceState(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchRestoreInstanceState(container)
    }

    override fun onSaveInstanceState(): Parcelable? {
        return SavedState(super.onSaveInstanceState(), materialSwitch?.isChecked ?: checked)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            setChecked(state.checked)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    fun setOnCheckChangeListener(onCheckChangedListener: OnCheckChangeListener?) {
        this.onCheckChangedListener = onCheckChangedListener
    }

    fun setChecked(checked: Boolean) {
        this.checked = checked
        textDescView?.text = if (checked) textOn else textOff
        if (materialSwitch?.isChecked != checked) {
            val listener = onCheckChangedListener
            onCheckChangedListener = null
            materialSwitch?.isChecked = checked
            onCheckChangedListener = listener
        }
    }

    interface OnCheckChangeListener {
        fun onCheckChanged(isChecked: Boolean)
    }

    private class SavedState : View.BaseSavedState {
        val checked: Boolean

        constructor(superState: Parcelable?, checked: Boolean) : super(superState) {
            this.checked = checked
        }

        constructor(source: Parcel) : super(source) {
            checked = source.readInt() != 0
        }

        override fun writeToParcel(destination: Parcel, flags: Int) {
            super.writeToParcel(destination, flags)
            destination.writeInt(if (checked) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> =
                object : Parcelable.Creator<SavedState> {
                    override fun createFromParcel(source: Parcel): SavedState = SavedState(source)

                    override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
                }
        }
    }
}

package com.auvious.auviousproject.cobrowse

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.auvious.auvioussdk.cobrowse.AuviousCobrowse
import com.auvious.auvioussdk.cobrowse.auviousMasked
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/** A card number field, masked by the `viewClasses` rule in MainActivity */
class CardNumberField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {
    init {
        inputType = InputType.TYPE_CLASS_NUMBER
        hint = "Card number"
    }
}

/**
 * Co-browse demo in Android Views. Each private field is masked by a different rule:
 * - account: `auviousMasked = true`
 * - IBAN: identifier `payment.iban` (view tag)
 * - card: class rule (`CardNumberField`)
 * - reference: opted out with `auviousMasked = false` (stays visible even with "Mask all text inputs")
 * - PIN: a password field, masked by default (and the keyboard while it's focused)
 */
class SendMoneyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
        }
        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        content.addView(title("Send money"))

        content.addView(label("From account"))
        content.addView(TextView(this).apply {
            text = "Everyday account · GR16 0110 1250 0000 0001 2300 695\nBalance €2,430.18"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(0, dp(4), 0, dp(12))
            if (CobrowseDemoSettings.marksViews) auviousMasked = true
        })

        content.addView(field("Recipient IBAN") {
            tag = "payment.iban"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        })

        content.addView(label("Card"))
        content.addView(CardNumberField(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })

        content.addView(field("Amount (€)") {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        })

        content.addView(field("Reference (visible to the agent)") {
            if (CobrowseDemoSettings.marksViews) auviousMasked = false
        })

        content.addView(field("PIN") {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        })

        content.addView(toggle("Mask all text inputs", AuviousCobrowse.maskingRules.maskAllTextInputs) { checked ->
            AuviousCobrowse.maskingRules = AuviousCobrowse.maskingRules.copy(maskAllTextInputs = checked)
        })
        content.addView(toggle("Highlight masks (debug)", AuviousCobrowse.debugHighlightMasks) { checked ->
            AuviousCobrowse.debugHighlightMasks = checked
        })

        content.addView(MaterialButton(this).apply {
            text = "Continue"
            setOnClickListener { startActivity(Intent(this@SendMoneyActivity, ConfirmTransferActivity::class.java)) }
        }, matchWidth(top = 16))

        content.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Open the Compose version"
            setOnClickListener { startActivity(Intent(this@SendMoneyActivity, ComposeSendMoneyActivity::class.java)) }
        }, matchWidth(top = 8))

        content.addView(MaterialButton(this, null, androidx.appcompat.R.attr.borderlessButtonStyle).apply {
            text = "End co-browse"
            setOnClickListener { AuviousCobrowse.stop() }
        }, matchWidth(top = 8))
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(8), 0, dp(16))
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        alpha = 0.7f
        setPadding(0, dp(8), 0, 0)
    }

    private fun field(hint: String, configure: TextInputEditText.() -> Unit): View {
        val layout = TextInputLayout(this)
        layout.hint = hint
        val edit = TextInputEditText(layout.context)
        edit.configure()
        layout.addView(edit)
        layout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
        }
        return layout
    }

    private fun toggle(text: String, checked: Boolean, onChange: (Boolean) -> Unit) = SwitchMaterial(this).apply {
        this.text = text
        isChecked = checked
        setOnCheckedChangeListener { _, isChecked -> onChange(isChecked) }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
        }
    }

    private fun matchWidth(top: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        topMargin = dp(top)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

/** Second screen: the session keeps running while the customer navigates */
class ConfirmTransferActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val density = resources.displayMetrics.density
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        content.addView(TextView(this).apply {
            text = "Confirm transfer"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTypeface(typeface, Typeface.BOLD)
        })

        // A long list to fling while sharing: the masked rows must stay black
        repeat(30) { index ->
            content.addView(TextView(this).apply {
                text = "Payment ${index + 1}"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setPadding(0, (16 * density).toInt(), 0, 0)
            })
            content.addView(TextView(this).apply {
                text = "GR16 0110 1250 0000 0001 23%02d 695 · €%d.00".format(index, (index + 1) * 25)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                if (CobrowseDemoSettings.marksViews) auviousMasked = true
            })
        }

        content.addView(MaterialButton(this).apply {
            text = "Back"
            setOnClickListener { finish() }
        })
    }
}

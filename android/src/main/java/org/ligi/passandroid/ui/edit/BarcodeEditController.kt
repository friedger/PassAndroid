package org.ligi.passandroid.ui.edit

import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.RadioButton
import kotlinx.android.synthetic.main.barcode_edit.view.*
import org.ligi.kaxt.doAfterEdit
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassBarCodeFormat.EAN_13
import org.ligi.passandroid.model.pass.PassBarCodeFormat.QR_CODE
import org.ligi.passandroid.ui.BarcodeUIController
import org.ligi.passandroid.ui.PassViewHelper
import java.util.*

class BarcodeEditController(val rootView: View, internal val context: AppCompatActivity, barCode: BarCode) {

    var barcodeFormat: PassBarCodeFormat?

    internal val intentFragment: Fragment

    class IntentFragment : Fragment() {

        var scanCallback: (String) -> Unit = {}

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (data != null && data.hasExtra("SCAN_RESULT")) {
                scanCallback(data.getStringExtra("SCAN_RESULT"))
            }
        }
    }

    private fun bindRadio(formats: Array<PassBarCodeFormat>) {
        formats.forEach {
            val radioButton = RadioButton(context)
            rootView.barcodeRadioGroup.addView(radioButton)

            radioButton.text = it.name
            radioButton.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    barcodeFormat = it
                    refresh()
                }
            }

            radioButton.isChecked = barcodeFormat == it
        }

    }

    init {
        intentFragment = IntentFragment()
        barcodeFormat = barCode.format

        rootView.randomButton.setOnClickListener({
            if (barcodeFormat == EAN_13) {
                rootView.messageInput.setText(getRandomEAN13())
            } else {
                rootView.messageInput.setText(UUID.randomUUID().toString().toUpperCase())
            }

            refresh()
        })

        rootView.scanButton.setOnClickListener({
            val barCodeIntentIntegrator = BarCodeIntentIntegrator(intentFragment)

            if (barcodeFormat == QR_CODE) {
                barCodeIntentIntegrator.initiateScan(BarCodeIntentIntegrator.QR_CODE_TYPES)
            } else {
                barCodeIntentIntegrator.initiateScan(setOf(barcodeFormat!!.name))
            }

        })

        intentFragment.scanCallback = { newMessage ->
            rootView.messageInput.setText(newMessage)
            refresh()
        }
        context.supportFragmentManager.beginTransaction().add(intentFragment, "intent_fragment").commit()

        bindRadio(PassBarCodeFormat.values())

        rootView.messageInput.setText(barCode.message)
        rootView.messageInput.doAfterEdit {
            refresh()
        }

        rootView.alternativeMessageInput.setText(barCode.alternativeText)

        refresh()
    }

    fun refresh() {
        val barcodeUIController = BarcodeUIController(rootView, barCode, context, PassViewHelper(context))
        val isBarcodeShown = barcodeUIController.getBarcodeView().visibility == View.VISIBLE

        if (!isBarcodeShown) {
            rootView.messageInput.error = "Invalid message"
        } else {
            rootView.messageInput.error = null
        }
    }

    val barCode: BarCode
        get() {
            val barCode = BarCode(barcodeFormat, rootView.messageInput.text.toString())
            val alternativeText = rootView.alternativeMessageInput.text.toString()
            if (!alternativeText.isEmpty()) {
                barCode.alternativeText = alternativeText
            }
            return barCode
        }
}

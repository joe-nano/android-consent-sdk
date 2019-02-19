package com.smartlook.consentsdk.ui.consent

import android.content.ContextWrapper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.smartlook.consentsdk.ConsentSDK
import com.smartlook.consentsdk.R
import com.smartlook.consentsdk.data.ConsentFormData
import com.smartlook.consentsdk.data.ConsentFormItem
import com.smartlook.consentsdk.listeners.ConsentItemListener
import android.support.v4.content.ContextCompat
import android.widget.FrameLayout
import com.smartlook.consentsdk.helpers.UtilsHelper

class ConsentBase(
    private val consentFormData: ConsentFormData,
    rootView: View,
    private val resultListener: ResultListener,
    consentResults: HashMap<String, Boolean>? = null) : ContextWrapper(rootView.context) {

    private val consentApi = ConsentSDK(this)
    var consentResults: HashMap<String, Boolean>

    // We are doing it oldschool because it works for both dialog and activity
    private val tvTitle = rootView.findViewById<TextView>(R.id.consent_title)
    private val tvDescription = rootView.findViewById<TextView>(R.id.consent_description)
    private val lvConsentItemsRoot = rootView.findViewById<LinearLayout>(R.id.consent_items_root)
    private val bConfirm = rootView.findViewById<Button>(R.id.consent_confirm_button)

    init {
        this.consentResults = consentResults ?: obtainConsentResults(consentFormData.consentFormItems)
    }

    fun displayConsent() {
        displayTexts()
        displayConsentItems()

        updateConfirmButton()
        handleConfirmButton()
    }

    private fun updateConfirmButton() {
        var enable = true

        consentFormData.consentFormItems.forEachIndexed { index, item ->
            if (item.required && consentResults[keyOnIndex(index)] != true) {
                enable = false
            }
        }

        bConfirm.isEnabled = enable
    }

    private fun displayTexts() {
        with(consentFormData) {
            tvTitle.text = titleText
            tvDescription.text = descriptionText
            bConfirm.text = confirmButtonText
        }
    }

    // recycler view should have nested scroll
    private fun displayConsentItems() {
        consentFormData.consentFormItems.forEachIndexed { index, consentItem ->
            addDivider()
            lvConsentItemsRoot.addView(ConsentFormItemView(this).apply {
                setData(consentResults[keyOnIndex(index)] ?: false, consentItem)
                registerListener(index, createConsentItemListener())
            })
        }
        addDivider()
    }

    private fun addDivider() {
        lvConsentItemsRoot.addView(View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                UtilsHelper.convertDpToPixel(this@ConsentBase,1f).toInt()
            )
            background = ContextCompat.getDrawable(this@ConsentBase, R.color.consent_form_divider_color)
        })
    }

    private fun handleConfirmButton() {
        bConfirm.setOnClickListener {
            storeGrantResults()
            resultListener.onResult(consentResults)
        }
    }

    private fun storeGrantResults() {
        consentApi.setConsentResultsStored()

        for (entry in consentResults.entries) {
            consentApi.saveConsentResult(entry.key, entry.value)
        }
    }

    private fun obtainConsentResults(consentFormItems: Array<ConsentFormItem>) =
        hashMapOf<String, Boolean>().apply {
            consentFormItems.forEach {
                put(it.consentKey, consentApi.loadConsentResult(it.consentKey) ?: false)
            }
        }

    private fun createConsentItemListener() = object : ConsentItemListener {
        override fun onConsentChange(itemIndex: Int, consent: Boolean) {
            consentResults[keyOnIndex(itemIndex)] = consent
            updateConfirmButton()
        }
    }

    private fun keyOnIndex(itemIndex: Int) = consentFormData.consentFormItems[itemIndex].consentKey

    interface ResultListener {
        fun onResult(consentResults: HashMap<String, Boolean>)
    }

}
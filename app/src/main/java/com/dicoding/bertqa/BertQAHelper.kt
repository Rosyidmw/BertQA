package com.dicoding.bertqa

import android.content.Context
import android.os.Build
import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.text.qa.BertQuestionAnswerer
import org.tensorflow.lite.task.text.qa.QaAnswer

class BertQAHelper (
    val context: Context,
    private val resultAnswerListener: ResultAnswerListener?
) {
    private var bertQuestionAnswerer: BertQuestionAnswerer? = null

    fun clearBertQuestionAnswerer() {
        bertQuestionAnswerer?.close()
        bertQuestionAnswerer = null
    }

    private fun setupBertQuestionAnswerer() {

        val baseOptionsBuilder = BaseOptions.builder()

        if (CompatibilityList().isDelegateSupportedOnThisDevice) {
            baseOptionsBuilder.useGpu()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            baseOptionsBuilder.useNnapi()
        } else {
            baseOptionsBuilder.setNumThreads(4)
        }

        val options = BertQuestionAnswerer.BertQuestionAnswererOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .build()

        try {
            bertQuestionAnswerer = BertQuestionAnswerer.createFromFileAndOptions(context, BERT_QA_MODEL, options)
        } catch (e: IllegalStateException) {
            resultAnswerListener
                ?.onError("Bert Question Answerer gagal untuk terinisialisasi")
            Log.e(TAG, "TFLite gagal untuk load model dengan error: " + e.message)
        }

    }

    fun getQuestionAnswer(topicsContext: String, question: String) {
        if (bertQuestionAnswerer == null) {
            setupBertQuestionAnswerer()
        }

        var inferenceTime = System.currentTimeMillis()

        val answers = bertQuestionAnswerer?.answer(topicsContext, question)

        inferenceTime = System.currentTimeMillis() - inferenceTime

        resultAnswerListener?.onResults(answers, inferenceTime)
    }

    interface ResultAnswerListener {
        fun onError(error: String)
        fun onResults(
            results: List<QaAnswer>?,
            inferenceTime: Long
        )
    }

    companion object {
        private const val BERT_QA_MODEL = "mobilebert.tflite"
        private const val TAG = "BertQaHelper"
    }
}
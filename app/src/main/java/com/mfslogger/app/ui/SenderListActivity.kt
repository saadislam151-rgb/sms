package com.mfslogger.app.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.mfslogger.app.Prefs
import com.mfslogger.app.databinding.ActivitySenderListBinding

/**
 * Simple screen to maintain the list of SMS sender IDs / numbers the
 * receiver should watch, e.g. "bKash", "Nagad", "Rocket", a short code
 * like "16216", or a specific phone number.
 */
class SenderListActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySenderListBinding
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySenderListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Watched senders"

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.listSenders.adapter = adapter
        refreshList()

        binding.btnAddSender.setOnClickListener {
            val value = binding.editNewSender.text.toString().trim()
            if (value.isNotEmpty()) {
                Prefs.addWatchedSender(this, value)
                binding.editNewSender.text.clear()
                refreshList()
            }
        }

        binding.listSenders.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position) ?: return@setOnItemClickListener
            Prefs.removeWatchedSender(this, item)
            refreshList()
        }
    }

    private fun refreshList() {
        adapter.clear()
        adapter.addAll(Prefs.getWatchedSenders(this).sorted())
        adapter.notifyDataSetChanged()
        binding.textHint.text = if (adapter.isEmpty)
            "Add a sender ID below (e.g. bKash). Tap an entry to remove it."
        else
            "Tap an entry to remove it."
    }
}

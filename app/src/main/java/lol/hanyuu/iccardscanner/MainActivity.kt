package lol.hanyuu.iccardscanner

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import lol.hanyuu.iccardscanner.ui.navigation.NavGraph
import lol.hanyuu.iccardscanner.ui.theme.IcCardScannerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent {
            IcCardScannerTheme {
                val scanState = mainViewModel.scanState.collectAsStateWithLifecycle()
                NavGraph(
                    scanState = scanState.value,
                    onScanStateReset = mainViewModel::resetScanState
                )
            }
        }
        // Handle NFC intent that launched this Activity cold (manifest filter)
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // enableReaderMode uses a direct callback — no PendingIntent, avoids MIUI BAL block
        nfcAdapter?.enableReaderMode(
            this,
            { tag -> mainViewModel.handleNfcTag(tag) },
            NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            tag?.let { mainViewModel.handleNfcTag(it) }
        }
    }
}

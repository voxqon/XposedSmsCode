package io.github.magisk317.uikit.shell

import android.os.Bundle
import androidx.activity.ComponentActivity
import io.github.magisk317.uikit.theme.applyEdgeToEdge

/**
 * Shared base activity that applies edge-to-edge behavior on create.
 */
abstract class BaseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge(this)
    }
}

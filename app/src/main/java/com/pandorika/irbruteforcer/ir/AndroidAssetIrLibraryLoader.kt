package com.pandorika.irbruteforcer.ir

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidAssetIrLibraryLoader(
    private val context: Context,
    private val parser: FlipperIrLibraryParser = FlipperIrLibraryParser(),
) {
    suspend fun load(path: String): IrLibrary = withContext(Dispatchers.IO) {
        context.assets.open(path).bufferedReader().use { reader ->
            parser.parse(reader.readText())
        }
    }
}


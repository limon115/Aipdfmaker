import java.io.File
import android.os.Environment

fun main() {
    val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    println(documentsDir.absolutePath)
}

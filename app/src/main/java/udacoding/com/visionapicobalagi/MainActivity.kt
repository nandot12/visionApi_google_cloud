package udacoding.com.visionapicobalagi

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import android.provider.MediaStore
import android.content.Intent
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayList
import android.widget.Toast




class MainActivity : AppCompatActivity() {

    private val CLOUD_VISION_API_KEY = "AIzaSyB1dlXenUWXYe-PewJVe337Oa0Fj28d0aI"

    private var CAMERA_REQUEST_CODE = 1
    private var bitmap : Bitmap? = null
    private var feature: Feature? = null

    private val visionAPI =
        arrayOf("LANDMARK_DETECTION", "LOGO_DETECTION", "SAFE_SEARCH_DETECTION", "IMAGE_PROPERTIES", "LABEL_DETECTION")
    private var api = visionAPI[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        feature = Feature()
        feature?.setType("TEXT_DETECTION")
        feature?.setMaxResults(10)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

          var permission = checkSelfPermission(android.Manifest.permission.CAMERA)

          if(permission != PackageManager.PERMISSION_GRANTED){
              requestPermissions(arrayOf(android.Manifest.permission.CAMERA),21)
          }
          else{

          }
      } else {
          TODO("VERSION.SDK_INT < M")
      }


        btn.onClick {

            takePictureFromCamera()

        }
    }

    fun takePictureFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            bitmap = (data!!.extras!!.get("data") as Bitmap)
            img.setImageBitmap(bitmap)
            feature?.let { callCloudVision(bitmap!!, it) }
        }
    }

    private fun getImageEncodeImage(bitmap: Bitmap): Image {
        val base64EncodedImage = Image()
        // Convert the bitmap to a JPEG
        // Just in case it's a format that Android understands but Cloud Vision
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()

        // Base64 encode the JPEG
        base64EncodedImage.encodeContent(imageBytes)
        return base64EncodedImage
    }

    private fun callCloudVision(bitmap: Bitmap, feature: Feature) {
        val featureList = ArrayList<Feature>()
        featureList.add(feature)

        val annotateImageRequests = ArrayList<AnnotateImageRequest>()

        val annotateImageReq = AnnotateImageRequest()
        annotateImageReq.features = featureList
        annotateImageReq.image = getImageEncodeImage(bitmap)
        annotateImageRequests.add(annotateImageReq)


        object : AsyncTask<Any, Void, String>() {
            override fun doInBackground(vararg params: Any): String {
                try {

                    val httpTransport = AndroidHttp.newCompatibleTransport()
                    val jsonFactory = GsonFactory.getDefaultInstance()

                    val requestInitializer = VisionRequestInitializer(CLOUD_VISION_API_KEY)

                    val builder = Vision.Builder(httpTransport, jsonFactory, null)
                    builder.setVisionRequestInitializer(requestInitializer)

                    val vision = builder.build()

                    val batchAnnotateImagesRequest = BatchAnnotateImagesRequest()
                    batchAnnotateImagesRequest.requests = annotateImageRequests

                    val annotateRequest = vision.images().annotate(batchAnnotateImagesRequest)
                    annotateRequest.disableGZipContent = true
                    val response = annotateRequest.execute()
                    return convertResponseToString(response)
                } catch (e: GoogleJsonResponseException) {

                    Log.d("error app nih", e.toString())

                } catch (e: IOException) {
                    Log.d("error app nih 2", e.toString())
                }

                return "Cloud Vision API request failed. Check logs for details."
            }

            override fun onPostExecute(result: String) {
                txthasil.text = result
               // visionAPIData.setText(result)
              //  imageUploadProgress.setVisibility(View.INVISIBLE)
            }
        }.execute()
    }

    private fun getImageProperty(imageProperties: ImageProperties): String {
        var message = ""
        val colors = imageProperties.dominantColors
        for (color in colors.colors) {
            message =
                message + "" + color.pixelFraction + " - " + color.color.red + " - " + color.color.green + " - " + color.color.blue
            message = message + "\n"
        }
        return message
    }

    private fun getImageAnnotation(annotation: SafeSearchAnnotation): String {
        return String.format(
            "adult: %s\nmedical: %s\nspoofed: %s\nviolence: %s\n",
            annotation.adult,
            annotation.medical,
            annotation.spoof,
            annotation.violence
        )
    }

    private fun convertResponseToString(response: BatchAnnotateImagesResponse): String {


       val text = response.responses.get(0).textAnnotations




//        val faces = response.responses.get(0).faceAnnotations
//
//        // Count faces
//        val numberOfFaces = faces.size
//
//// Get joy likelihood for each face
//        var likelihoods = ""
//        for (i in 0 until numberOfFaces) {
//            likelihoods += "\n It is " +
//                    faces[i].joyLikelihood +
//                    " that face " + i + " is happy"
//        }
//
//// Concatenate everything
//        val message = "This photo has $numberOfFaces faces$likelihoods"
//
//// Display toast on UI thread
//        runOnUiThread {
//            Toast.makeText(
//                applicationContext,
//                message, Toast.LENGTH_LONG
//            ).show()
//        }

        return text.get(0).toString()

    }

    private fun formatAnnotation(entityAnnotation: List<EntityAnnotation>?): String {
        var message = ""

        if (entityAnnotation != null) {
            for (entity in entityAnnotation) {
                message = message + "    " + entity.description + " " + entity.score
                message += "\n"
            }
        } else {
            message = "Nothing Found"
        }
        return message
    }

}
